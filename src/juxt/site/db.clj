;; Copyright © 2021, JUXT LTD.

(ns juxt.site.db
  (:require
   [xtdb.api :as xt]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::xt-node [_ xtdb-opts]
  (log/info "Starting XT node")
  (xt/start-node xtdb-opts))

(defmethod ig/halt-key! ::xt-node [_ node]
  (.close node)
  (log/info "Closed XT node"))
