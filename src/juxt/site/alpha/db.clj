;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.pprint :refer [pprint]]
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.dave.alpha :as dave]
   [juxt.pass.alpha :as pass]
   [juxt.spin.alpha :as spin]
   [juxt.site.alpha.entity :as entity]
   [juxt.site.alpha.payload :as payload]
   [juxt.site.alpha.util :as util])
  (:import
   (java.util Date UUID)))

(defmethod payload/generate-representation-body
  ::contact
  [request resource representation db authorization subject]
  (.getBytes
   (with-out-str
     (pprint
      (select-keys resource [:name :email :address :tel])))))

(def last-modified (java.util.Date.))

(def contacts
  [[:crux.tx/put
    {:crux.db/id "/contacts/roger.edn"
     :type "Contact"
     :name "Roger Farringdon"
     :email "roger@example.com"
     :address "13 Pickets Way, Colchester"
     :tel "01 232 7321"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/jeremy.edn"
     :type "Contact"
     :name "Jeremy Taylor"
     :email "jdt@juxt.pro"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/malcoml.edn"
     :type "Contact"
     :name "Jeremy Taylor"
     :email "jdt@juxt.pro"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/ben.edn"
     :type "Contact"
     :name "Ben Harvy"
     :email "ben@example.com"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/tim.edn"
     :type "Contact"
     :name "Tim Greene"
     :email "tim@example.com"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/chris.edn"
     :type "Contact"
     :name "Chris Roberts"
     :email "ben@example.com"
     ::spin/methods #{:get :head :options :put}
     ::spin/acceptable "application/edn"
     ::spin/representations
     [{::spin/content-type "application/edn"
       ::spin/last-modified last-modified
       ::spin/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/projects/internal.edn"
     :type "Project"
     :name "Internal projects"}]

   [:crux.tx/put
    {:crux.db/id "/contacts/"
     :description "A collection"
     ::spin/methods #{:get :head :options :propfind :mkcol}
     ::spin/options {"DAV" "1"}
     ::dave/query '[[e :type "Contact"]]}]])

(defn seed-database! [crux-node]
  (println "Seeding database")
  (try
    (crux/submit-tx
     crux-node
     contacts)

    (crux/sync crux-node)
    (catch Exception e
      (log/error e "Failed to seed database"))))

(defmethod ig/init-key ::crux-node [_ crux-opts]
  (println "Starting Crux node")
  (let [node (crux/start-node crux-opts)]
    (seed-database! node)
    node))

(defmethod ig/halt-key! ::crux-node [_ node]
  (.close node)
  (println "Closed Crux node"))
