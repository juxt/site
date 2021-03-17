;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [crux.api :as crux]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::crux-node [_ crux-opts]
  (log/info "Starting Crux node")
  (crux/start-node crux-opts))

(defmethod ig/halt-key! ::crux-node [_ node]
  (.close node)
  (log/info "Closed Crux node"))
