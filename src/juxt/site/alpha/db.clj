;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [xtdb.api :as xt]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(defmethod ig/init-key ::xt-node [_ xtdb-opts]
  (log/info "Starting XT node...")
  (xt/start-node xtdb-opts)
  ;; we need to make sure the tx-ingester has caught up before
  ;; declaring the node up
  (xt/await-tx xtdb-opts
               (xt/submit-tx xtdb-opts [[::xt/put {:xt/id :tx-ingester-synced!}]]))
  (log/info "... XT node started!")
  )

(defmethod ig/halt-key! ::xt-node [_ node]
  (.close node)
  (log/info "Closed XT node"))
