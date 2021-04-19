;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating)

(alias 'site (create-ns 'juxt.site.alpha))

(defmulti render-template
  (fn [_ template] (::site/template-engine template)) :default :selmer)
