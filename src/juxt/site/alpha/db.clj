;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [clojure.java.io :as io]
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.pass.alpha :as pass]
   [juxt.spin.alpha :as spin]
   [juxt.site.alpha.entity :as entity]
   [juxt.site.alpha.util :as util])
  (:import
   (java.util Date UUID)))

(defn seed-database! [crux-node]
  (crux/submit-tx
   crux-node
   ;; The crux/admin user - in the future, the password will be provided when
   ;; the Crux instance is provisioned.
   (entity/user-entity "crux/admin" "FunkyForest"))

  (crux/sync crux-node))

(defmethod ig/init-key ::crux-node [_ crux-opts]
  (println "Starting Crux node")
  (crux/start-node crux-opts))

(defmethod ig/halt-key! ::crux-node [_ node]
  (.close node)
  (println "Closed Crux node"))
