;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.helpers
  (:require
   [clojure.tools.logging :as log]
   [juxt.apex.alpha.openapi :as openapi]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn post-request-body [{::apex/keys [new-resource-state]
                          ::site/keys [uri resource] :as req}]
  (let [;; Operation can contain extra config, such as uri-templates
        operation (::apex/operation resource)
        ;; TODO: This is just a hack to get a demo working - the method of
        ;; determining the identifier for the new resource needs to be more
        ;; configurable!
        id (str uri "/" (java.util.UUID/randomUUID))]

    (openapi/put-resource-state
     req (assoc new-resource-state :crux.db/id id))))
