;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [clojure.java.io :as io]
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.spin.alpha :as spin]
   [juxt.site.alpha.entity :as entity]
   [juxt.site.alpha.util :as util])
  (:import
   (java.net URI)
   (java.util Date UUID)))

(defn seed-database! [crux-node]
  (crux/submit-tx
   crux-node
   (concat
    ;; The crux/admin user - in the future, the password will be provided when
    ;; the Crux instance is provisioned.
    (entity/user-entity "crux/admin" "FunkyForest")
    ;; TODO: Policies
))

  (crux/sync crux-node))

;;(password/encrypt "password")

(defmethod ig/init-key ::crux [_ _]
  (println "Starting Crux node")
  (let [node (crux/start-node {})]
    (seed-database! node)
    node))

(defmethod ig/halt-key! ::crux [_ node]
  (.close node)
  (println "Closed Crux node"))
