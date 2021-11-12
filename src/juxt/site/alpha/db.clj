;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [xtdb.api :as xtdb]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::xt-node [_ xtdb-opts]
  (log/info "Starting XT node")
  (xtdb/start-node xtdb-opts))

(defmethod ig/halt-key! ::xt-node [_ node]
  (.close node)
  (log/info "Closed XT node"))
