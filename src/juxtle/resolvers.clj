(ns juxtle.resolvers
  (:require [xtdb.api :as xt]
            [tick.core :as t]))

(defn resolve-game-time
  [{:keys [object-value variable-values db] :as args}]
  (let [tt (:timeTakenMillis object-value)]
    (or (let [game-id (or (:xt/id object-value)
                          (get variable-values "id")
                          (str (get variable-values "username") "wordleGame"
                               (t/day-of-month) "/01/2022")
                          (throw (ex-info "no id on object" {:object object-value
                                                             :vars variable-values
                                                             :arg-keys (keys args)})))
              game-history (xt/entity-history db game-id :asc {:with-docs? true})
              tx->time (fn [tx] (-> tx :xtdb.api/valid-time t/instant))
              start-time (some-> game-history first tx->time)
              last-game (or (first (filter (fn [{:keys [xtdb.api/doc]}]
                                             (= (last (:guesses doc))
                                                (:solution doc)))
                                           game-history))
                            (first (filter (fn [{:keys [xtdb.api/doc]}]
                                             (= 6 (count (:guesses doc))))
                                           game-history)))
              end-time (some-> last-game tx->time)]
          (when (and (< 1 (count game-history)) start-time end-time)
            (t/millis (t/duration {:tick/beginning start-time
                                   :tick/end end-time}))))
        (and tt (> tt 0) tt)
        0)))

(defn resolve-finished
  [{:keys [object-value]}]
  (let [{:keys [guesses solution]} object-value]
    (boolean
     (or (= (last guesses) solution) (= 6 (count guesses))))))
