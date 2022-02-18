(ns kanban.resolvers
  (:require [juxt.site.alpha.graphql :refer [protected-lookup prepare-mutation-entity put-objects!]]
            [xtdb.api :as xt]
            [clojure.string :as str]))

(defn insert-into
  [coll idx item]
  (concat (subvec coll 0 idx)
          [item]
          (subvec coll idx)))

(defn remove-by-idx
  [coll idx]
  (concat (subvec coll 0 idx)
          (subvec coll (inc idx))))

(comment
  (let [coll ["a" "b" "c" "d" "e"]
        new-item "new"]
    [(insert-into coll 2 new-item)
     (remove-by-idx coll 2)]))

(defn move-card
  [{:keys [argument-values db juxt.pass.alpha/subject type-k xt-node] :as opts}]
  (let [lookup (fn [id] (protected-lookup id subject db))
        validate (fn [item name]
                   (when-not (:xt/id item)
                     (throw
                      (ex-info
                       "Entity does not exist in the DB, or you do not have access to it"
                       {:args argument-values
                        :name name
                        :item item}))))
        card-id (get argument-values "cardId")
        card (lookup card-id)
        {:keys [cardIds]
         :as destination} (lookup (get argument-values "workflowStateId"))
        new-position (let [prev (get argument-values "previousCard")]
                       (cond
                         (= "start" prev)
                         0
                         (or (= "end" prev) (nil? prev))
                         (count cardIds)
                         :else
                         (inc (.indexOf cardIds prev))))

        ;; we handle a collection of sources just in case there are duplicates
        sources (map lookup
                 (map first
                  (xt/q db {:find ['e]
                            :where [['e type-k "WorkflowState"]
                                    ['e :cardIds card-id]]})))

        updated-destination-cards (distinct (insert-into (vec cardIds) new-position card-id))
        updated-source-cards (into {}
                                   (map (fn [source]
                                          [(:xt/id source)
                                           (distinct (remove (fn [id] (= id card-id))
                                                             (:cardIds source)))])
                                        sources))
        updated-destination (assoc destination :cardIds
                                   (vec updated-destination-cards))
        updated-sources (map (fn [source] (assoc source :cardIds (vec (get updated-source-cards (:xt/id source))))) sources)
        prepare (fn [tx] (prepare-mutation-entity opts tx nil))
        new-sources-txes (map prepare updated-sources)
        new-destination-tx (prepare updated-destination)
        _validate (do
                    (validate card "card")
                    (map (fn [source] (validate source "current-state")) sources)
                    (validate destination "new-state")
                    (validate updated-destination "updated dest")
                    (map (fn [updated-source] (validate updated-source "updated source")) updated-sources))]
    (prn (put-objects! xt-node (conj new-sources-txes new-destination-tx)))
    card))
