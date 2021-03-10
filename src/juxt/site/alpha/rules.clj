;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.rules
  (:require
   [crux.api :as x]
   [clojure.tools.logging :as log]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn post-rule [{::site/keys [crux-node uri request-locals] :as req}]

  (let [request-instance (get request-locals ::apex/request-instance)

        location
        (str uri (hash (select-keys request-instance [::pass/target ::pass/effect])))]

    (->> (x/submit-tx
          crux-node
          [[:crux.tx/put (merge {:crux.db/id location} request-instance)]])
         (x/await-tx crux-node))

    (into req {:ring.response/status 201
               :ring.response/headers {"location" location}})))
