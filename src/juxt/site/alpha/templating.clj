;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating)

(alias 'site (create-ns 'juxt.site.alpha))


(defmulti render-template
  "Methods should return a modified request (first arg), typically associated
  a :ring.response/body entry."
  (fn [_ template] (::site/template-engine template)) :default :selmer)
