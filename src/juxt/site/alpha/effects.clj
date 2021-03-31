;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.effects
  (:require [clojure.tools.logging :as log]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(defmulti run-effect! (fn [req effect] (get-in effect [:effect ::site/effect])))
