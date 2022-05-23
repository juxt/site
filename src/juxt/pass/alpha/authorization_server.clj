;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization-server)

(defn authorize [req]
  (assoc req :ring.response/body "TODO: Authorization Server!"))
