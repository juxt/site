;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.return)

(alias 'site (create-ns 'juxt.site.alpha))

(defn return [req status msg ex-data & args]
  (throw
   (ex-info
    (apply format msg args)
    (assoc
     ex-data
     ::site/request-context
     (cond-> req
       true (into (assoc (::site/request-context ex-data) :ring.response/status status))
       (:ring.response/headers ex-data) (update :ring.response/headers (fnil into {}) (:ring.response/headers ex-data)))))))
