;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.graphql
  (:require
   [juxt.site.alpha.graphql :as graphql]
   [ring.util.codec :refer [form-decode]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'jinx (create-ns 'juxt.jinx.alpha))

;;(-> req ::site/resource :juxt.apex.alpha/operation (get "x-juxt-site-graphql-query-resource"))

(defn post-handler [req]
  ;;  (def req req)

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

        variables (into {}
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

    (if (seq (:errors result))
          (assoc req
                 :ring.response/status 400
                 :ring.response/body (pr-str result))

          (assoc req
                 :ring.response/status 303
                 ;; TODO: Only for browsers
                 :ring.response/headers {"location" (::site/uri req)}))))
