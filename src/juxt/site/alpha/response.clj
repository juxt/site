;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.response
  (:require
   [clojure.tools.logging :as log]
   [juxt.site.alpha.selmer :as selmer]
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.graphql.templating :as graphql-templating]
   [xtdb.api :as xt]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha :as-alias site]))

(declare add-payload)

(defn xt-template-loader [req db]
  (proxy [java.net.URLStreamHandler] []
    (openConnection [url]
      ;;      (log/tracef "Open connection: url=%s" url)
      (proxy [java.net.URLConnection] [url]
        (getInputStream []
          ;;          (log/tracef "Loading template: url=%s" url)

          (let [res (xt/entity db (str url))
                _ (when-not res
                    (throw
                     (ex-info
                      (format "Failed to find template resource: %s" (str url))
                      {:template-uri (str url)})))
                response (add-payload
                          (assoc req ::site/selected-representation res))
                template-body (:ring.response/body response)
                _ (when-not template-body
                    (throw
                     (ex-info
                      "Template body is nil"
                      {:template (str url)
                       :template-resource res})))]

            (cond
              (string? template-body) (java.io.ByteArrayInputStream. (.getBytes template-body))
              (bytes? template-body) (java.io.ByteArrayInputStream. template-body)
              (instance? java.io.InputStream template-body) template-body
              :else
              (throw
               (ex-info
                "Unexpected type of template body"
                {:template-body-type (type template-body)})))))))))

(defn expand-queries [template-model db]
  (postwalk
   (fn [m]
     (cond
       (and (map? m) (contains? m ::site/query))
       (cond->> (xt/q db
                       ;; TODO: Add ACL checks
                       (::site/query m))

         ;; Deprecate? Anything using this?
         (= (::site/results m) 'first) first
         ;; Copied from juxt.apex.alpha.representation_generation
         (::site/extract-first-projection? m) (map first)
         (::site/extract-entry m) (map #(get % (::site/extract-entry m)))
         (::site/singular-result? m) first)
       :else m))
   template-model))

(defn process-template-model [template-model {::site/keys [db] :as req}]
  ;; A template model can be a stored query.
  (let [f (cond
            (nil? template-model)
            (throw
             (ex-info
              "Nil template-model. Template resources must have a :juxt.site.alpha/template-model attribute."
              {:resource (::site/resource req)
               ::site/request-context (assoc req :ring.response/status 500)}))

            ;; If a symbol, it is expected to be a resolvable internal function
            ;; (to support basic templates built on the request and internal
            ;; Site data).
            (symbol? template-model)
            (try
              (or
               (requiring-resolve template-model)
               (throw
                (ex-info
                 (format "Requiring resolve of %s returned nil" template-model)
                 {:template-model template-model
                  ::site/request-context (assoc req :ring.response/status 500)})))
              (catch Exception e
                (throw
                 (ex-info
                  (format "template-model fn '%s' not resolveable" template-model)
                  {:template-model template-model
                   ::site/request-context (assoc req :ring.response/status 500)}
                  e))))

            ;; It can also be an id of an entity in the database which contains
            ;; metadata, e.g. for a GraphQL query. TODO: This is smelly
            ;; coupling, why should a String imply a reference to a GraphQL doc?
            (string? template-model)
            ;; This is treated as a reference to another entity
            (if-let [template-model-entity (xt/entity db template-model)]
              (cond
                (:juxt.site.alpha/graphql-schema template-model-entity)
                (fn [req]
                  (graphql-templating/template-model req template-model-entity))

                :else
                (throw
                 (ex-info
                  "Unsupported template-model entity"
                  {:template-model-entity template-model-entity
                   ::site/request-context (assoc req :ring.response/status 500)})))
              (throw
               (ex-info
                "Template model entity not found in database"
                {:template-model template-model
                 ::site/request-context (assoc req :ring.response/status 500)})))

            (map? template-model)
            (fn [_] template-model)

            :else
            (throw
             (ex-info
              "Unsupported form of template-model"
              {:type (type template-model)
               ::site/request-context (assoc req :ring.response/status 500)})))]
    (f req)))

(defn render-template
  "Methods should return a modified request (first arg), typically associated
  a :ring.response/body entry."
  [{::site/keys [resource selected-representation db] :as req} template]

  (let [template-models (or (::site/template-model selected-representation)
                            (::site/template-model resource))
        template-models (if (sequential? template-models) template-models [template-models])

        ;; TODO: We should strongly consider reverting to a single template
        ;; model, now that we have GraphQL. Having the option for multiple
        ;; template models over-complicates the modelling.
        processed-template-models
        (for [tm template-models]
          (process-template-model tm req))

        combined-template-model
        (->> processed-template-models
             (map #(dissoc % :xt/id))
             (apply merge-with
                    (fn [a b] (concat (if (sequential? a) a [a])
                                      (if (sequential? b) b [b])))))]

    ;; Possibly we can make this an open-for-extension thing in the future. We
    ;; could register template dialects in the database and look them up here.
    (case (::site/template-dialect template)
      "selmer" (selmer/render-template
                (assoc req ::site/template-loader (xt-template-loader req db))
                template combined-template-model)
      nil
      (throw
       (ex-info
        "No template dialect found"
        {::site/request-context (assoc req :ring.response/status 500)}))
      (throw
       (ex-info
        "Unsupported template dialect"
        {::site/template-dialect (::site/template-dialect template)
         ::site/request-context (assoc req :ring.response/status 500)})))))

(defn ^:deprecated add-payload [{::site/keys [selected-representation db] :as req}]
  (let [{::http/keys [body content] ::site/keys [get-fn body-fn]} selected-representation
        template (some->> selected-representation ::site/template (xt/entity db))]
    (cond
      get-fn
      (if-let [f (cond-> get-fn (symbol? get-fn) requiring-resolve)]
        (do
          (log/debugf "Calling get-fn: %s %s" get-fn (type get-fn))
          (f req))
        (throw
         (ex-info
          (format "get-fn cannot be resolved: %s" body-fn)
          {:get-fn get-fn
           ::site/request-context (assoc req :ring.response/status 500)})))

      body-fn
      (if-let [f (cond-> body-fn (symbol? body-fn) requiring-resolve)]
        (do
          (log/debugf "Calling body-fn: %s %s" body-fn (type body-fn))
          (assoc req :ring.response/body (f req)))
        (throw
         (ex-info
          (format "body-fn cannot be resolved: %s" body-fn)
          {:body-fn body-fn
           ::site/request-context (assoc req :ring.response/status 500)})))

      template (render-template req template)

      content (assoc req :ring.response/body content)
      body (assoc req :ring.response/body body)
      :else req)))


(defn redirect [req status location]
  (-> req
      (assoc :ring.response/status status)
      (update :ring.response/headers assoc "location" location)))
