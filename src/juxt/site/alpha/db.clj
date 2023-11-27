;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [xtdb.api :as xtdb]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::xtdb-node [_ xtdb-opts]
  (log/info "Starting Crux node")
  (xtdb/start-node xtdb-opts))

(defmethod ig/halt-key! ::xtdb-node [_ node]
  (.close node)
  (log/info "Closed Crux node"))
