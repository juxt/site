;; Copyright © 2021, JUXT LTD.

(ns juxt.site.nrepl
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [nrepl.server :refer [start-server stop-server]]
   [juxt.site :as-alias site]))

(defmethod ig/init-key ::server [_ {::site/keys [port]}]
  (log/infof "Starting nREPL server on port %d" port)
  (start-server :port port))

(defmethod ig/halt-key! ::server [_ server]
  (when server
    (log/info "Stopping nREPL server")
    (stop-server server)))
