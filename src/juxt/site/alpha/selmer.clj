;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.selmer
  (:require
   [integrant.core :as ig]
   [selmer.parser :as selmer]))

(defmethod ig/init-key ::cache [_ {:keys [cache?]}]
  (reset! selmer/cache? cache?))
