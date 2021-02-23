;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [crux.api :as crux]
   [integrant.core :as ig]))

(defmethod ig/init-key ::crux-node [_ crux-opts]
  (println "Starting Crux node")
  (crux/start-node crux-opts))

(defmethod ig/halt-key! ::crux-node [_ node]
  (.close node)
  (println "Closed Crux node"))
