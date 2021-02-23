;; Copyright © 2020-2021, JUXT LTD.

(ns juxt.spin.alpha.negotiation
  (:require
   [clojure.string :as str]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.spin.alpha :as spin]))

(defn negotiate-representation [request current-representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  (let [{selected-representation :juxt.pick.alpha/representation
         vary :juxt.pick.alpha/vary}
        (when (seq current-representations)
          ;; Call into pick which does that actual negotiation for us.
          (pick
           request
           (for [r current-representations]
             (assoc r :juxt.pick.alpha/representation-metadata (::spin/representation-metadata r)))
           {:juxt.pick.alpha/vary? true}))

        ;; Check for a 406 Not Acceptable
        _ (when (contains? #{:get :head} (:request-method request))
            (spin/check-not-acceptable! selected-representation))]

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc-in
                        [::spin/representation-metadata "vary"]
                        (str/join ", " vary)))))
