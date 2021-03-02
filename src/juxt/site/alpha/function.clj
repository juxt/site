(ns juxt.site.alpha.function)

(alias 'site 'juxt.site.alpha)

(defmulti invoke-service-function ::site/service-function)
