;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.nrepl
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [nrepl.server :refer [start-server stop-server]]))

(alias 'site (create-ns 'juxt.site.alpha))

(defmethod ig/init-key ::server [_ {::site/keys [host port] :as opts}]
  (log/infof "Starting nREPL server on %s:%d" host port)
  (if (nil? host)
    (start-server :port port)
    (start-server :bind host :port port)))

(defmethod ig/halt-key! ::server [_ server]
  (when server
    (log/info "Stopping nREPL server")
    (stop-server server)))
