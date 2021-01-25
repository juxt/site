;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.payload
  (:require [juxt.spin.alpha :as spin]))

(defmulti generate-representation-body
  (fn [representation db] (::spin/bytes-generator representation)))
