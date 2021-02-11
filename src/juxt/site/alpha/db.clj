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
   (concat
    ;; The crux/admin user - in the future, the password will be provided when
    ;; the Crux instance is provisioned.
    (entity/user-entity "crux/admin" "FunkyForest")
    ;; TODO: Policies

    #_[
     [:crux.tx/put
      {:crux.db/id :rule1
       :type "Rule"
       ::pass/description "Paul can do anything :)"

       ::pass/target
       '[[request ::pass/username "rlwspaul"]]

       ::pass/effect ::pass/allow}]

     [:crux.tx/put
      {:crux.db/id :rule2
       :type "Rule"
       ::pass/description
       "People called Malcolm Sparks can do anything :)"

       ;; used to match the rule against the target
       ::pass/target
       '[
         [request ::pass/user user]
         [user :name "Malcolm Sparks"]]

       ::pass/effect ::pass/allow

       ;; the effect of the rule 'going forward'
       #_::pass/limiting-clauses
       #_'[(or
            [e :dealership dealership]
            [e :owner dealership])
           [(get context ::pass/role) role]
           [role :owner dealership]]}]

     ;; Ken shouldn't be able to see the RLWS org
     #_[:crux.tx/put
      {:crux.db/id :rule3
       :type "Rule"
       ::pass/description "Ken should be able to see his own org and those of his customers"

       ::pass/target
       '[[request ::pass/username "ken"]]

       ::pass/effect ::pass/allow}]]
    ))

  (crux/sync crux-node))

;;(password/encrypt "password")

(defmethod ig/init-key ::crux [_ crux-opts]
  (println "Starting Crux node")
  (let [node (crux/start-node crux-opts)]
    (seed-database! node)
    node))

(defmethod ig/halt-key! ::crux [_ node]
  (.close node)
  (println "Closed Crux node"))
