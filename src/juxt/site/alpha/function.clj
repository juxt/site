;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.function)

(alias 'site (create-ns 'juxt.site.alpha))

(defmulti invoke-service-function ::site/service-function)
