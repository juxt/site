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

;;(identity selected-representation)

(defn template-model-provider
  [{::site/keys [db selected-representation]
    ::pass/keys [subject] :as req}]

  ;;(def selected-representation selected-representationa)

  (log/tracef "selected-rep is %s" selected-representation)

  (let [{::site/keys [graphql-endpoint graphql-query graphql-operation]} selected-representation

        schema (some-> (xt/entity db graphql-endpoint) ::grab/schema)

        _ (log/tracef "GraphQL query is %s" graphql-query)

        document
        (try
          ;; TODO: Obviously we should pre-parse, pre-compile and pre-validate GraphQL queries!
          (let [document
                (try
                  (parser/parse graphql-query)
                  (catch Exception e
                    (throw (ex-info "Failed to parse document" {:errors [{:message (.getMessage e)}]}))))]
            (document/compile-document document schema))
          (catch Exception e
            (let [errors (:errors (ex-data e))]
              (throw
               (ex-info
                "Error parsing or compiling GraphQL query"
                (into
                 req
                 (cond-> {:ring.response/status 400}
                   (seq errors) (assoc ::errors errors)))
                e)))))]

    (assert graphql-query)
    (log/debugf "Executing GraphQL query for template: %s" graphql-query)
    ;; TODO: Check for errors
    (:data (graphql/query schema document graphql-operation db subject))))
