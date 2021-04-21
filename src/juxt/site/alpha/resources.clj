;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.resources
  (:require
   [clojure.edn :as edn]
   [crux.api :as x]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn post-resource
  "Post a new resource, or overwrite an existing one, in the database."
  [{::site/keys [db crux-node]
    ::apex/keys [request-instance] :as req}]

  (let [resource (edn/read-string (slurp (::http/body request-instance)))
        exists? (x/entity db (:crux.db/id resource))]
    (case (::http/content-type request-instance)
      "application/edn"
      (do
        (->>
         (x/submit-tx crux-node [[:crux.tx/put resource]])
         (x/await-tx crux-node))

        (-> req
            (assoc :ring.response/status (if-not exists? 201 204))
            (update :ring.response/headers assoc "location" (:crux.db/id resource)))))))
