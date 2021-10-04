;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql
  (:require
   [clojure.pprint :refer [pprint]]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.grab.alpha.parser :as parser]
   [jsonista.core :as json]
   [clojure.string :as str]
   [crux.api :as xt]
   [clojure.tools.logging :as log]
   [clojure.edn :as edn]
   [clojure.walk :refer [postwalk]]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))
(alias 'g (create-ns 'juxt.grab.alpha.graphql))

(defn query [schema document operation-name db]
  (execute-request
   {:schema schema
    :document document
    :operation-name operation-name
    :field-resolver
    (fn [args]
      (let [lookup-type (::schema/provided-types schema)
            field (get-in args [:object-type ::schema/fields-by-name (get-in args [:field-name])])
            xtdb-args (get-in field [::schema/directives-by-name "xtdb" ::g/arguments])
            site-args (get-in field [::schema/directives-by-name "site" ::g/arguments])
            field-kind (-> field ::g/type-ref ::g/name lookup-type ::g/kind)
            lookup-entity (fn [id] (xt/entity db id))]

        (cond
          (get xtdb-args "q")
          (if-let [id (get-in args [:object-value :crux.db/id])]
            (for [[e] (apply
                       xt/q db
                       (assoc
                        (edn/read-string (get xtdb-args "q"))
                        :in (vec (concat ['object] (map symbol (keys (:argument-values args))))))
                       id (vals (:argument-values args)))]
              (xt/entity db e))

            ;; No object
            (for [[e] (apply
                       xt/q db (assoc
                                (edn/read-string (get xtdb-args "q"))
                                :in (mapv symbol (keys (:argument-values args))))
                       (vals (:argument-values args)))]
              (xt/entity db e)))

          (get xtdb-args "a")
          (let [att (get xtdb-args "a")
                val (get-in args [:object-value (keyword att)])]
            (if (= field-kind :object)
              (lookup-entity val)
              val))

          (get site-args "resolver")
          (let [resolver (requiring-resolve (symbol (get site-args "resolver")))]
            (resolver args))

          :else (throw
                 (ex-info
                  (format "TODO: resolve field: %s" (:field-name args)) {:args args})))))}))

(defn post-handler [{::site/keys [uri db] :as req}]
  (let [schema (some-> (xt/entity db uri) ::grab/schema)
        body (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (log/tracef "request keys are" (pr-str (keys (some-> req :juxt.site.alpha/received-representation))))

        ;; TODO: Should also support application/graphql+json
        [document-str operation-name]
        (case (some-> req ::site/received-representation ::http/content-type)
          "application/json"
          (let [json (some-> body json/read-value)]
            [(get json "query") (get json "operationName")])

          "application/graphql"
          [body "Query"]

          (throw (ex-info "Unknown content type for GraphQL request" req)))

        _ (when (nil? document-str)
            (throw (ex-info "Nil GraphQL query" (-> req
                                                    (update-in [::site/resource] dissoc ::grab/schema)
                                                    (dissoc :juxt.pass.alpha/request-context)))))
        document
        (try
          (document/compile-document (parser/parse document-str) schema)
          (catch clojure.lang.ExceptionInfo e
            (let [errors (:errors (ex-data e))]
              (throw
               (ex-info
                "Errors in query"
                (into
                 req
                 (cond-> {:ring.response/status 400}
                   (seq errors) (assoc ::errors errors)))
                e)))))

        ;; TODO: If JSON, get operationName and use it here
        results (query schema document operation-name db)

        ;; Map to application/json
        results (postwalk
                  (fn [x]
                    (cond-> x
                      (and (vector? x) (= :kind (first x)))
                      (update 1 (comp str/upper-case name))))
                  results)]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string results)))))

(defn put-schema [crux-node resource schema-str]
  (let [schema (schema/compile-schema (parser/parse schema-str))]
    (xt/await-tx
     crux-node
     (xt/submit-tx
      crux-node
      [[:crux.tx/put (assoc resource ::grab/schema schema)]]))))

(defn put-handler [{::site/keys [uri db crux-node] :as req}]
  (let [schema-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (when-not schema-str
            (throw
             (ex-info
              "No schema in request"
              (into
               req
               {:ring.response/status 400}))))

        resource (xt/entity db uri)]

    (when (nil? resource)
      (throw (ex-info "GraphQL resource not configured" {:uri uri})))

    (try
      (put-schema crux-node resource schema-str)
      (assoc req :ring.response/status 204)
      (catch clojure.lang.ExceptionInfo e
        (let [errors (:errors (ex-data e))]
          (if (seq errors)
            (do
              (log/trace e "error")
              (log/tracef "schema errors: %s" (pr-str (:errors (ex-data e))))
              (throw
               (ex-info
                "Errors in schema"
                (into
                 req
                 (cond-> {:ring.response/status 400}
                   (seq errors) (assoc ::errors errors)))
                e)))
            (throw
             (ex-info
              "Failed to put schema"
              (into
               req
               {:ring.response/status 500})
              e))))))))

(defn put-error-json-body [req]
  (json/write-value-as-string
   {:message "Schema compilation errors"
    :errors (::errors req)}))

(defn put-error-text-body [req]
  (log/tracef "put-error-text-body: %d errors" (count (::errors req)))
  (cond
    (::errors req)
    (->>
     (for [error (::errors req)]
       (cond-> (str \tab (:error error))
         (:location error) (str " (line " (-> error :location :row inc) ")")))
     (into ["Schema compilation errors"])
     (str/join (System/lineSeparator)))
    (:ring.response/body req) (:ring.response/body req)
    :else "Unknown error, check stack trace"))

(defn put-error-json-body [req]
  (json/write-value-as-string
   {:message "Schema compilation errors"
    :errors (::errors req)}))

(defn post-error-text-body [req]
  (log/tracef "put-error-text-body: %d errors" (count (::errors req)))
  (->>
   (for [error (::errors req)]
     (cond-> (str \tab (:error error))
       (:location error) (str " (line " (-> error :location :row inc) ")")))
   (into ["Query errors"])
   (str/join (System/lineSeparator))))
