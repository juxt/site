;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [crux.api :as crux]))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn do-action [crux-node pass-ctx action & args]
  (assert (crux/entity (crux/db crux-node) "urn:site:tx-fns:do-action"))
  (let [tx (crux/submit-tx
            crux-node
            [[::crux/fn
              "urn:site:tx-fns:do-action"
              (assoc pass-ctx ::pass/action action)
              args]])

        {::crux/keys [tx-id]} (crux/await-tx crux-node tx)]

    ;; Throw a nicer error
    (when-not (crux/tx-committed? crux-node tx)
      (throw
       (ex-info
        "Transaction failed to be committed"
        {::crux/tx-id tx-id
         ::pass/action action})))

    (let [result
          (crux/entity
           (crux/db crux-node)
           (format "urn:site:action-log:%s" tx-id))]
      (if (::site/error result)
        (throw (ex-info "Failed to do action" (merge {:action action} pass-ctx (dissoc result ::site/type))))
        result))))
