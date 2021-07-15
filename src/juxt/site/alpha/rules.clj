;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.rules
  (:require
   [clojure.edn :as edn]
   [crux.api :as x]
   [juxt.site.alpha.util :as util]
   [jsonista.core :as json]
   [clojure.tools.logging :as log]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn match-targets [db rules request-context]
  (let [temp-id-map (reduce-kv
                     ;; Preserve any existing crux.db/id - e.g. the resource will have one
                     (fn [acc k v] (assoc acc k (merge {:crux.db/id (java.util.UUID/randomUUID)} v)))
                     {} request-context)
        ;; Speculatively put each entry of the request context into the
        ;; database. This new database is only in scope for this authorization.
        db (x/with-tx db (->> temp-id-map
                              vals
                              (map util/->freezeable)
                              ;; TODO: Use map rather than mapv?
                              (mapv (fn [e] [:crux.tx/put e]))))

        evaluated-rules
        (try
          (keep
           (fn [rule-id]
             (let [rule (x/entity db rule-id)]
               ;; Arguably ::pass/target, ::pass/effect and ::pass/matched? should
               ;; be re-namespaced.
               (when-let [target (::pass/target rule)]
                 (let [q {:find ['success]
                          :where (into '[[(identity true) success]] target)
                          :in (vec (keys temp-id-map))}
                       match-results (apply x/q db q (map :crux.db/id (vals temp-id-map)))]
                   (assoc rule ::pass/matched? (pos? (count match-results)))))))
           rules)
          (catch Exception e
            (log/error e "Failed to evaluate rules")
            ;; Return empty vector of rules to recover
            []))
        ;;_ (log/debugf "Result of rule matching: %s" (pr-str evaluated-rules))
        ]
    (filter ::pass/matched? evaluated-rules)))

(defn eval-triggers [db triggers request-context]
  (let [temp-id-map (reduce-kv
                     ;; Preserve any existing crux.db/id - e.g. the resource will have one
                     (fn [acc k v] (assoc acc k (merge {:crux.db/id (java.util.UUID/randomUUID)} v)))
                     {} request-context)
        ;; Speculatively put each entry of the request context into the
        ;; database. This new database is only in scope for this authorization.
        db (x/with-tx db (mapv (fn [e] [:crux.tx/put e]) (vals temp-id-map)))]

    (keep
     (fn [trigger-id]
       (let [trigger (x/entity db trigger-id)
             q (merge (::site/query trigger)
                      {:in (vec (keys temp-id-map))})
             action-data (apply x/q db q (map :crux.db/id (vals temp-id-map)))]
         (when (seq action-data)
           {:trigger trigger
            :action-data action-data})))
     triggers)))
