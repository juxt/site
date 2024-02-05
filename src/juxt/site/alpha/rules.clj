;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.rules
  (:require
   [xtdb.api :as xt]
   [juxt.site.alpha.util :as util]
   [clojure.tools.logging :as log]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn post-rule
  [{::site/keys [xtdb-node uri] ::apex/keys [request-instance] :as req}]

  (let [location
        (str uri (hash (select-keys request-instance [::pass/target ::pass/effect])))]

    (->> (xt/submit-tx
          xtdb-node
          [[:xtdb.api/put (merge {:xt/id location} request-instance)]])
         (xt/await-tx xtdb-node))

    (-> req
        (assoc :ring.response/status 201)
        (update :ring.response/headers assoc "location" location))))

(defn match-targets [db rules request-context]
  (let [temp-id-map (reduce-kv
                     ;; Preserve any existing xt/id - e.g. the resource will have one
                     (fn [acc k v] (assoc acc k (merge {:xt/id (java.util.UUID/randomUUID)} v)))
                     {} request-context)
        ;; Speculatively put each entry of the request context into the
        ;; database. This new database is only in scope for this authorization.
        db (xt/with-tx db (->> temp-id-map
                              vals
                              (map util/->freezeable)
                              ;; TODO: Use map rather than mapv?
                              (mapv (fn [e] [:xtdb.api/put e]))))

        evaluated-rules
        (try
          (doall
           (->> rules
                (pmap
                 (fn [rule-id]
                   (let [rule (xt/entity db rule-id)]
                     ;; Arguably ::pass/target, ::pass/effect and ::pass/matched? should
                     ;; be re-namespaced.
                     (when-let [target (::pass/target rule)]
                       (let [q {:find ['success]
                                :where (into '[[(identity true) success]] target)
                                :in (vec (keys temp-id-map))}
                             match-results (apply xt/q db q (map :xt/id (vals temp-id-map)))]
                         (assoc rule ::pass/matched? (pos? (count match-results))))))))
                (remove nil?)))
          (catch Exception e
            (log/error e "Failed to evaluate rules")
            ;; Return empty vector of rules to recover
            []))
        ;;_ (log/debugf "Result of rule matching: %s" (pr-str evaluated-rules))
        ]
    (filter ::pass/matched? evaluated-rules)))

(defn eval-triggers [db triggers request-context]
  (let [temp-id-map (reduce-kv
                     ;; Preserve any existing xt/id - e.g. the resource will have one
                     (fn [acc k v] (assoc acc k (merge {:xt/id (java.util.UUID/randomUUID)} v)))
                     {} request-context)
        ;; Speculatively put each entry of the request context into the
        ;; database. This new database is only in scope for this authorization.
        db (xt/with-tx db (mapv (fn [e] [:xtdb.api/put e]) (vals temp-id-map)))]

    (doall
     (keep
      (fn [trigger-id]
        (let [trigger (xt/entity db trigger-id)
              q (merge (::site/query trigger)
                       {:in (vec (keys temp-id-map))})
              action-data (try (apply xt/q db q (map :xt/id (vals temp-id-map)))
                               (catch Exception e
                                 (log/error "Failed trigger" e)
                                 (log/info "Failed trigger query:" {:q q
                                                                    :q-args (map :xt/id (vals temp-id-map))
                                                                    :trigger trigger})
                                 (log/info "Failed trigger request-context:" request-context)
                                 (log/info "Failed trigger data:"
                                           {:stage-report-creation
                                            (apply xt/q db
                                                   (merge '{:find [(pull sr [*])]
                                                            :where [[request :ring.request/method :put]
                                                                    [request :juxt.site.alpha/uri sr]
                                                                    [sr :juxt.site.alpha/type "PileStageReport"]
                                                                    (not [sr :edited-by])
                                                                    [sr :stages stages]
                                                                    [sr :wells wells]
                                                                    [sr :buckets buckets]
                                                                    ; Total - tons and lbs
                                                                    [sr :total-tons total-tons]
                                                                    [(clojure.edn/read-string total-tons) total-tons-float]
                                                                    #_[(int total-tons-float) total-tons-int]]}
                                                          {:in (vec (keys temp-id-map))}
                                                          )
                                                   (map :xt/id (vals temp-id-map)))
                                            :stage-report-correction
                                            (apply xt/q db
                                                   (merge
                                                     '{:find [(pull sr [*])]
                                                       :where [[request :ring.request/method :put]
                                                               [request :juxt.site.alpha/uri sr]
                                                               [sr :juxt.site.alpha/type "PileStageReport"]
                                                               [sr :edited-by edited-by]
                                                               [sr :stages stages]
                                                               [sr :wells wells]
                                                               [sr :buckets buckets]
                                                               ; Total - tons and lbs
                                                               [sr :total-tons total-tons]
                                                               [(clojure.edn/read-string total-tons) total-tons-float]
                                                               #_[(int total-tons-float) total-tons-int]]}
                                                     {:in (vec (keys temp-id-map))})
                                                   (map :xt/id (vals temp-id-map)))})
                                 (throw e)))]
          (when (seq action-data)
            {:trigger trigger
             :action-data action-data})))
      triggers))))
