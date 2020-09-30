;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.put
  (:require
   [juxt.spin.alpha :as spin]))

(defmulti put-representation
  (fn [request resource new-representation old-representation crux-node]
    (::spin/content-type new-representation)))

(defmethod put-representation :default [_ _ _]
  {:status 400
   :body (.getBytes "Bad Request\r\n")
   ;; Add :body to describe the new resource
   })
