;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locate
  (:require
   [juxt.spin.alpha :as spin]))

(defmulti locate-resource (fn [^java.net.URI uri db]))

(defmethod locate-resource :default [_ _]
  ;; Return an 'empty' resource
  {::spin/methods #{:get :head :options}
   ::spin/representations []})
