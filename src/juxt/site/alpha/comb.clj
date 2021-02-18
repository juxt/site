;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.comb
  (:require
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [comb.template :as template]))

;; Here is an integration of weavejester's comb.

(defmethod generate-representation-body ::template
  [request resource representation db authorization subject]
  (template/eval
   (str "TODO:" representation "\r\n")
   {:resource resource}))
