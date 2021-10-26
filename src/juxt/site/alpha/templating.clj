;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating
  (:require
   [juxt.site.alpha.selmer :as selmer]
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.graphql.templating :as graphql-templating]
   [clojure.tools.logging :as log]
   [crux.api :as xt]))

(alias 'site (create-ns 'juxt.site.alpha))

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
                 {:template-model template-model})))
              (catch Exception e
                (throw
                 (ex-info
                  (format "template-model fn '%s' not resolveable" template-model)
                  {:template-model template-model}))))

            ;; It can also be an id of an entity in the database which contains
            ;; metadata, e.g. for a GraphQL query.
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
                  {:template-model-entity template-model-entity})))
              (throw
               (ex-info
                "Template model entity not found in database"
                {:template-model template-model})))

            :else
            (throw (ex-info "Unsupported form of template-model" {:type (type template-model)})))]
    (f req)))

(defn render-template
  "Methods should return a modified request (first arg), typically associated
  a :ring.response/body entry."
  [{::site/keys [db selected-representation] :as req} template]

  (let [template-models
        (map first
             (xt/q db '{:find [tm]
                        :where [[e ::site/template-model tm]]
                        :in [e]} (:crux.db/id selected-representation)))

        processed-template-models
        (for [tm template-models]
          (process-template-model tm req))

        combined-template-model
        (->> processed-template-models
             (map #(dissoc % :crux.db/id))
             (apply merge-with
                    (fn [a b] (concat (if (sequential? a) a [a])
                                      (if (sequential? b) b [b])))))]

    ;; Possibly we can make this an open-for-extension thing in the future. We
    ;; could register template dialects in the database and look them up here.
    (case (::site/template-dialect template)
      "selmer" (selmer/render-template req template combined-template-model)
      nil (throw (ex-info "No template dialect found" {}))
      (throw (ex-info "Unsupported template dialect" {::site/template-dialect (::site/template-dialect template)})))))
