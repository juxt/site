;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.triggers)

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(defmulti run-action! (fn [req action] (get-in action [:trigger ::site/action])))
