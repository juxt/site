(ns kanban.resolvers
  (:require [juxt.site.alpha.graphql :refer [protected-lookup prepare-mutation-entity put-objects!]]
            [xtdb.api :as xt]))

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
        source (lookup
                (ffirst
                 (xt/q db {:find ['e]
                           :where [['e type-k "WorkflowState"]
                                   ['e :cardIds card-id]]})))

        updated-destination-cards (insert-into (vec cardIds) new-position card-id)
        updated-source-cards (remove (fn [id] (= id card-id)) (:cardIds source))
        updated-destination (assoc destination :cardIds
                                   (vec updated-destination-cards))
        updated-source (assoc source :cardIds
                              (vec updated-source-cards))
        prepare (fn [tx] (prepare-mutation-entity opts tx nil))
        new-source-tx (prepare updated-source)
        new-destination-tx (prepare updated-destination)
        _validate (do
                    (validate card "card")
                    (validate source "current-state")
                    (validate destination "new-state")
                    (validate updated-destination "updated dest")
                    (validate updated-source "updated source")
                    (when (= (:xt/id source)
                             (:xt/id destination))
                      (throw
                       (ex-info
                        "Can't move card to same state"
                        {:from source
                         :to destination}))))]
    (prn (put-objects! xt-node [new-source-tx new-destination-tx]))
    card))
