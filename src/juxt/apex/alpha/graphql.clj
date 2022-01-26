;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.graphql
  (:require
   [juxt.site.alpha.graphql :as graphql]
   [ring.util.codec :refer [form-decode]]
   [juxt.apex.alpha.representation-generation :refer [entity-body]]
   [juxt.site.alpha.content-negotiation :as conneg]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'jinx (create-ns 'juxt.jinx.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))

(defn mutation [{::site/keys [resource db uri xt-node] :as req}]
  (let [exists-before-mutation? (xt/entity db uri)

        stored-query-resource-path (get-in resource [::apex/operation "x-juxt-site-graphql-query-resource"])
        stored-query-id (if (.startsWith stored-query-resource-path "/")
                          (str (:juxt.site.alpha/base-uri req) stored-query-resource-path)
                          stored-query-resource-path)
        operation-name (or
                        (get-in resource [::apex/operation "x-juxt-site-graphql-operation-name"])
                        (get-in resource [::apex/operation "operationId"]))

        form (::apex/request-payload req)
        _ (assert form)

        ;; If POST, add on ::apex/new-uri. In future we might have different
        ;; strategies of minting a new URI for a POST, but the UUID is good for
        ;; now and follows Site's idioms. This new-uri is useful because a POST
        ;; often results in a single new resource being created. This is the
        ;; right level of abstraction to determine the new URI, since it is at
        ;; this OpenAPI layer where the URI details are known and the best place
        ;; to decide on what this new URI is, rather than making this the
        ;; responsibility of a lower layer such as GraphQL.
        new-uri (when (= (:ring.request/method req) :post)
                  (str uri (java.util.UUID/randomUUID)))

        req (cond-> req
              (= :post (:ring.request/method req))
              (assoc ::apex/new-uri new-uri))

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

        ;; Associate GraphQL related attributes onto the request. These help
        ;; with debugging.
        req (into req
                  {::site/graphql-stored-query-resource-path stored-query-resource-path
                   ::site/graphql-operation-name operation-name
                   ::site/graphql-variables variables})

        result (graphql/graphql-query req stored-query-id operation-name variables)]

    (log/debugf
     "Calling GraphQL at %s operation %s with variables %s yielded %s"
     stored-query-resource-path operation-name variables (pr-str result))

    (when (seq (:errors result))
      (throw
       (ex-info
        "Errors on GraphQL mutation"
        {::site/graphql-type "SiteGraphqlExecutionError"
         ::site/graphql-stored-query-resource-path stored-query-resource-path
         ::site/graphql-operation-name operation-name
         ::site/graphql-variables variables
         ::site/graphql-result result
         ::grab/errors (:errors result)
         ::site/request-context (assoc req :ring.response/status 400)})))

    ;; The status and response is determined by both the method and which
    ;; responses are available.

    (let [method (:ring.request/method req)

          responses (get-in resource [::apex/operation "responses"])

          [status response]
          (some
           #(find responses %)
           (case method
             :put [(when-not exists-before-mutation? "201") "200" "204"]
             ;; Since POST logic should use ::site/new-uri a 201 must always be
             ;; preferred. We could check for new-uri existence but that would
             ;; involve awaiting the tx.
             :post ["201" "200" "204"]
             :delete ["200" "204"]))

          _ (assert status "Status cannot be nil, badly formed response object in OpenAPI")

          ;;          _ (log/tracef "status is %s, response is %s" status response)

          req (assoc req :ring.response/status (Integer/parseInt status))

          resource-state (:data result)

          representation
          (conneg/pick-representation
           req (for [[media-type media-type-object] (get response "content")]
                 (assoc media-type-object ::http/content-type media-type)))

          ;;          _ (log/tracef "representation: %s" representation)

          body (entity-body
                (assoc req ::site/selected-representation representation)
                (-> resource-state
                    (assoc :description (get response "description" "no-description"))))]

      (cond-> req
        body (assoc :ring.response/body body)
        new-uri (update :ring.response/headers (fnil assoc {}) "location" new-uri)))))
