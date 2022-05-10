;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.response)

(defn redirect [req status location]
  (-> req
      (assoc :ring.response/status status)
      (update :ring.response/headers assoc "location" location)))
