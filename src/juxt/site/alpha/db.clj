;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [xtdb.api :as xt]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::xtdb-node [_ crux-opts]
  (log/info "Starting Crux node")
  (crux/start-node crux-opts))

(defmethod ig/halt-key! ::xtdb-node [_ node]
  (.close node)
  (log/info "Closed Crux node"))
