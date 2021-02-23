(ns juxt.site.alpha.function
  (:require [juxt.site.alpha :as site]))

(defmulti invoke-service-function ::site/service-function)
