;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.graphql
  (:require
   [juxt.site.alpha.graphql :as graphql]
   [ring.util.codec :refer [form-decode]]
   [juxt.apex.alpha.representation-generation :refer [entity-body]]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'jinx (create-ns 'juxt.jinx.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))

(defn post-handler [req]
  (let [resource (::site/resource req)
        stored-query-resource-path (get-in resource [::apex/operation "x-juxt-site-graphql-query-resource"])
        stored-query-id (if (.startsWith stored-query-resource-path "/")
                          (str (:juxt.site.alpha/base-uri req) stored-query-resource-path)
                          stored-query-resource-path)
        operation-name (or
                        (get-in resource [::apex/operation "x-juxt-site-graphql-operation-name"])
                        (get-in resource [::apex/operation "operationId"]))

        input-body-as-string
        (-> req :juxt.site.alpha/received-representation
            :juxt.http.alpha/body
            (String.))
        form (form-decode input-body-as-string)

        variables (into
                   (graphql/common-variables req)
                   (concat
                    ;; Path parameters
                    (for [[k v] (::apex/openapi-path-params resource)
                          :when (::jinx/valid? v)]
                      [k (:value v)])
                    ;; TODO: Query parameters
                    form
                    ))
        result (graphql/graphql-query
                req
                stored-query-id operation-name variables)]

    (log/debugf
     "Calling GraphQL at %s operation %s with variables %s yielded %s"
     stored-query-resource-path operation-name variables (pr-str result))

    (if (seq (:errors result))
      (throw
       (ex-info
        "Errors on GraphQL mutation"
        {::site/graphql-type "SiteGraphqlExecutionError"
         ::site/graphql-stored-query-resource-path stored-query-resource-path
         ::site/graphql-operation-name operation-name
         ::site/graphql-variables variables
         ::site/graphql-result result
         ::grab/errors (:errors result)
         ::site/request-context (assoc req :ring.response/status 400)}))

      (if-let [ok-response (get-in resource [::apex/operation "responses" "200"])]
        (assoc req
               :ring.response/status 200
               :ring.response/body
               (entity-body
                (assoc req
                       ;; TODO: Negotiate the required representation from the
                       ;; request. We're hardcoding html for now.
                       ::site/selected-representation
                       {::http/content-type "text/html;charset=utf-8"})
                (:data result)))

        (assoc req
               :ring.response/status 303
               ;; TODO: Only for browsers
               :ring.response/headers {"location" (::site/uri req)})))))
