;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [xtdb.api :as xt]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [diehard.core :as dh])
  (:import
   java.time.Duration
   software.amazon.awssdk.services.s3.S3AsyncClient
   software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient))



(def s3-configurator
  (reify xtdb.s3.S3Configurator
    (makeClient [_this]
      (.. (S3AsyncClient/builder)
          (httpClientBuilder
            (.. (NettyNioAsyncHttpClient/builder)
                (connectionAcquisitionTimeout (Duration/ofSeconds 600))
                (maxConcurrency (Integer. 100))
                (maxPendingConnectionAcquires (Integer. 10000))))
          (build)))))

(defn- start-node
  [config]
  (dh/with-retry
    {:retry-if
     (fn [_ ex]
       (= "incomplete checkpoint restore"
          (ex-message ex)))
     :max-retries 3
     :on-failed-attempt
     (fn [_ ex]
       (log/warn ex "Couldn't complete checkpoint restore"))
     :on-failure
     (fn [_ ex]
       (log/error ex "Checkpoint restore failed"))}
    (xt/start-node config)))

(defmethod ig/init-key ::xt-node [_ xtdb-opts]
  (log/info "Starting XT node ...")

  (let [config (update-in xtdb-opts
                          [:xtdb/index-store :kv-store :checkpointer :store]
                          assoc :configurator (constantly s3-configurator))
        node (start-node config)]
    ;; we need to make sure the tx-ingester has caught up before
    ;; declaring the node up
    (->>
     (xt/submit-tx node [[::xt/put {:xt/id :tx-ingester-synced!}]])
     (xt/await-tx node))
    (log/info "... XT node started!")
    node))

(defmethod ig/halt-key! ::xt-node [_ node]
  (.close node)
  (log/info "Closed XT node"))
