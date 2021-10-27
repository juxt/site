;; Copyright © 2021, JUXT LTD.

;; This is an alternative template engine that uses GraphQL

(ns juxt.site.alpha.graphql.templating
  (:require
   [clojure.tools.logging :as log]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.parser :as parser]
   [crux.api :as xt]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))

(defn template-model
  [{::site/keys [crux-node db resource]
    ::pass/keys [subject] :as req}
   {graphql-schema-id ::site/graphql-schema
    :as stored-document-entity}]

  (assert graphql-schema-id "No graphql-schema reference in stored document entity")

  (let [graphql-query-bytes (::http/body stored-document-entity)
        _ (assert graphql-query-bytes (pr-str stored-document-entity))

        operation-name (:juxt.site.alpha/graphql-operation-name resource)

        graphql-query (String. graphql-query-bytes "UTF-8")

        graphql-schema-entity (xt/entity db graphql-schema-id)

        _ (assert graphql-schema-entity (str "No graphql-schema in database with id of " graphql-schema-id))

        schema (::grab/schema graphql-schema-entity)

        _ (assert schema (str "GraphQL schema entity does not have a current schema"))

        _ (log/tracef "GraphQL query is %s" graphql-query)

        document
        (try
          ;; TODO: Obviously we should pre-parse, pre-compile and pre-validate GraphQL queries!
          (let [document
                (try
                  (parser/parse graphql-query)
                  (catch Exception e
                    (throw (ex-info "Failed to parse document" {:errors [{:message (.getMessage e)}]} e))))]
            (document/compile-document document schema))
          (catch Exception e
            (log/error e "Error parsing or compiling GraphQL query")
            (let [errors (:errors (ex-data e))]
              (throw
               (ex-info
                "Error parsing or compiling GraphQL query for template model"
                (into
                 req
                 (cond-> {:ring.response/status 500}
                   (seq errors) (assoc ::errors errors)))
                e)))))]

    (assert graphql-query)
    (log/debugf "Executing GraphQL query for template: %s" graphql-query)
    ;; TODO: How to communicate back if there are any errors? Throw an exception?
    (:data (graphql/query schema document operation-name crux-node db subject))))
