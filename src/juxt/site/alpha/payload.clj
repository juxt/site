;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.payload)

(alias 'site (create-ns 'juxt.site.alpha))

(defmulti generate-representation-body
  (fn [request resource representation db authorization subject] (::site/body-generator representation)))
