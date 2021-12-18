;; Copyright © 2021, JUXT LTD.

;; This is an alternative template engine that uses GraphQL
(ns juxt.site.alpha.graphql.templating
  (:require
   [clojure.tools.logging :as log]
   [jsonista.core :as json]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.parser :as parser]
   [clojure.walk :refer [postwalk prewalk]]
   [ring.util.codec :refer [form-decode]]
   [selmer.parser :as selmer]
   [xtdb.api :as xt]
   [clojure.string :as str]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))

(defn template-model
  [{::site/keys [xt-node db resource selected-representation]
    ::pass/keys [subject] :as req}
   {graphql-schema-id ::site/graphql-schema
    :as stored-document-entity}]

  (assert graphql-schema-id "No graphql-schema reference in stored document entity")

  (let [graphql-query-bytes (::http/body stored-document-entity)
        _ (assert graphql-query-bytes (pr-str stored-document-entity))

        operation-name (or
                        (:juxt.site.alpha/graphql-operation-name selected-representation)
                        (:juxt.site.alpha/graphql-operation-name resource))

        variables (merge
                   ;; The currently logged in user is so commonly needed, it is
                   ;; automatically bound.
                   {"username" (-> subject ::pass/username)}
                   (or
                    (:juxt.site.alpha/graphql-variables selected-representation)
                    (:juxt.site.alpha/graphql-variables resource)
                    {}))

        graphql-query (String. graphql-query-bytes "UTF-8")

        graphql-schema-entity (xt/entity db graphql-schema-id)

        _ (assert graphql-schema-entity (str "No graphql-schema in database with id of " graphql-schema-id))

        schema (::grab/schema graphql-schema-entity)

        _ (assert schema (str "GraphQL schema entity does not have a current schema"))

        document
        ;; TODO: We should parse, compile and validate GraphQL queries ahead of time
        (let [document
              (try
                (parser/parse graphql-query)
                (catch Exception e
                  (throw (ex-info "Failed to parse document" {:errors [{:message (.getMessage e)}]} e))))]
          (try
            (document/compile-document document schema)
            (catch Exception e
              (let [errors (:errors (ex-data e))]
                (log/errorf e "Error compiling GraphQL query: %s" (seq errors))
                (throw
                 (ex-info
                  "Error parsing or compiling GraphQL query for template model"
                  (into
                   req
                   (cond-> {:ring.response/status 500}
                     (seq errors) (assoc ::errors errors)))
                  e))))))]

    (assert graphql-query)
    #_(log/debugf "Executing GraphQL query (op-name %s) for template: %s" operation-name graphql-query)
    ;; TODO: How to communicate back if there are any errors? Throw an exception?
    (let [results (graphql/query schema document operation-name variables req)]
      (when (seq (:errors results))
        (throw (ex-info "GraphQL errors in template model" results))
        )
      #_(log/debugf "Results are %s" (pr-str results))

      ;; Process results to apply dynamic queries - siteTemplateModel
      (let [data (:data results)]
        (postwalk
         ;; Deprecated because _siteTemplateModel has become an abandoned
         ;; experiment, superceded by simply generating a template and calling
         ;; Selmer again. However, it's a bit too early to delete the code.
         (fn [x]
           (cond
             (and (map? x) (contains? x :_siteTemplateModel))
             (let [siteTemplateModel (json/read-value (:_siteTemplateModel x))]

               ;; If there is a special _siteTemplateModel, we process the trees
               ;; of all its siblings, replacing any variable reference ('{{
               ;; foo.bar }}') with the value in the template model. We also
               ;; look for tables.

               (reduce-kv
                (fn [acc k v]
                  (cond-> acc
                    (not= k :_siteTemplateModel)
                    (assoc k (postwalk
                              (fn [s] (if (and (string? s) (str/index-of s "{{"))
                                        (selmer/render s siteTemplateModel)
                                        s)) v))))
                x x))
             :else x))
         data)))))


;; Deprecated. This has been abandoned in favour of using an OpenAPI approach,
;; which is easier to use as a container of configuration (e.g. GraphQL
;; operation names)
(defn ^:deprecated
  post-handler [{::site/keys [db resource xt-node]
                 ::pass/keys [subject]
                 :as req}]

  (let [input-body-as-string
        (-> req :juxt.site.alpha/received-representation
            :juxt.http.alpha/body
            (String.))
        form (form-decode input-body-as-string)

        document-resource-uri
        (or
         (get form "queryUri")
         ;; Currently the resource can have a 'string' template model which
         ;; refers to a GraphQL. This design is somewhat arbitary and may change
         ;; future.
         (when (string? (::site/template-model resource))
           (::site/template-model resource)))

        document-resource (when document-resource-uri (xt/entity db document-resource-uri))

        graphql-query (when (:juxt.http.alpha/body document-resource)
                        (String. (:juxt.http.alpha/body document-resource)))

        operation-name (get form "operationName")

        graphql-schema-id (::site/graphql-schema document-resource)
        graphql-schema-entity (xt/entity db graphql-schema-id)
        schema (::grab/schema graphql-schema-entity)]

    (try
      ;; TODO: Obviously we should pre-parse, pre-compile and pre-validate GraphQL queries!
      (let [document (parser/parse graphql-query)
            compiled-document
            (try
              (document/compile-document document schema)
              (catch Exception e
                (log/errorf "document is %s" document)
                (throw e)))

            result (graphql/query schema compiled-document operation-name (dissoc form "operationName") req)

            ;; TODO: Show result, if any errors list them, be ready to return application/json
            ]

        (if (seq (:errors result))
          (assoc req
                 :ring.response/status 400
                 :ring.response/body (pr-str result))

          (assoc req
                 :ring.response/status 303
                 :ring.response/headers {"location" (::site/uri req)})))

      (catch Exception e
        (let [errors (:errors (ex-data e))]
          (log/errorf e "Error parsing or compiling GraphQL query: %s" (seq errors))
          (throw
           (ex-info
            "Error parsing or compiling GraphQL query for template model"
            (into
             req
             (cond-> {:ring.response/status 500}
               (seq errors) (assoc ::errors errors)))
            e)))))))
