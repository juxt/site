;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.payload
  (:require [juxt.spin.alpha :as spin]))

(defmulti generate-representation-body
  (fn [request resource representation db authorization subject] (::spin/bytes-generator representation)))
