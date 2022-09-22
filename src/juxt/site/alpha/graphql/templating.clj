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
  [{::site/keys [xt-node db resource selected-representation uri]
    ::pass/keys [subject] :as req}
   {stored-query-resource-path :xt/id
    graphql-schema-id ::site/graphql-schema
    graphql-compiled-query ::site/graphql-compiled-query
    :as stored-document-entity}]

  (assert graphql-schema-id "No graphql-schema reference in stored document entity")

  (let [operation-name (or
                        (:juxt.site.alpha/graphql-operation-name selected-representation)
                        (:juxt.site.alpha/graphql-operation-name resource))

        variables (merge
                   ;; The currently logged in user is so commonly needed, it is
                   ;; automatically bound.  These should be the same set as
                   ;; those in
                   ;; juxt.apex.alpha.representation-generation/entity-bytes-generator
                   (graphql/common-variables req)
                   (or
                    (:juxt.site.alpha/graphql-variables selected-representation)
                    (:juxt.site.alpha/graphql-variables resource)
                    {}))

        graphql-schema-entity (xt/entity db graphql-schema-id)

        _ (when-not graphql-schema-entity
            (throw
             (ex-info
              (format "No graphql-schema in database with id of %s" graphql-schema-id)
              {:entity graphql-schema-entity})))

        schema (::site/graphql-compiled-schema graphql-schema-entity)]

    (when-not schema
      (throw (ex-info "GraphQL schema entity does not have a current schema" {:entity graphql-schema-entity})))

    (assert graphql-compiled-query)

    (let [result (graphql/query schema graphql-compiled-query operation-name variables req)]
      (when-let [errors (seq (:errors result))]
        (throw
         (ex-info
          "GraphQL errors in template model"
          {::grab/errors errors
           ::site/graphql-type "SiteGraphqlExecutionError"
           ::site/graphql-stored-query-resource-path stored-query-resource-path
           ::site/graphql-operation-name operation-name
           ::site/graphql-variables variables
           ::site/graphql-result result
           ::site/request-context (into req {::site/graphql-stored-query-resource-path stored-query-resource-path
                                             ::site/graphql-operation-name operation-name
                                             ::site/graphql-variables variables})})))

      #_(log/debugf "GraphQL result is %s" (pr-str result))

      (:data result))))


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
            (cond-> {::site/request-context req}
              (seq errors) (assoc ::errors errors))
            e)))))))
