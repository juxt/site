;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [malli.core :as m]
   [malli.error :a me]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [juxt.site.alpha :as-alias site]))

(defn actions->rules
  "Determine rules for the given action ids. Each rule is bound to the given
  action."
  [db actions]

  (vec (for [action actions
             :let [e (xt/entity db action)]
             rule (::pass/rules e)]
         (conj rule ['action :xt/id action]))))

(defn check-permissions
  "Given a subject, possible actions and resource, return all related pairs of permissions and actions."
  [db actions {:keys [subject resource purpose] :as pass-ctx}]

  (log/debugf "CHECK PERMS: %s" (pr-str (assoc pass-ctx :actions actions)))

  (let [rules (actions->rules db actions)]
    (when (seq rules)
      (let [permissions
            (xt/q
             db
             {:find '[(pull permission [*]) (pull action [*])]
              :keys '[permission action]
              :where
              '[
                [action ::site/type "https://meta.juxt.site/pass/action"]

                ;; Only consider given actions
                [(contains? actions action)]

                ;; Only consider a permitted action
                [permission ::site/type "https://meta.juxt.site/pass/permission"]
                [permission ::pass/action action]
                (allowed? permission subject action resource)

                ;; Only permissions that match our purpose
                [permission ::pass/purpose purpose]
                ]

              :rules rules

              :in '[subject actions resource purpose]}

             subject actions resource purpose)]
        (log/debugf "Returning %d permissions" (count permissions))
        permissions))))

(defn allowed-resources
  "Given a set of possible actions, and possibly a subject and purpose, which
  resources are allowed?"
  [db actions {::pass/keys [subject purpose]}]
  (let [rules (actions->rules db actions)]
    (xt/q
     db
     {:find '[resource]
      :where
      '[
        [action ::site/type "https://meta.juxt.site/pass/action"]

        ;; Only consider given actions
        [(contains? actions action)]

        ;; Only consider a permitted action
        [permission ::site/type "https://meta.juxt.site/pass/permission"]
        [permission ::pass/action action]
        (allowed? permission subject action resource)

        ;; Only permissions that match our purpose
        [permission ::pass/purpose purpose]]

      :rules rules

      :in '[subject actions purpose]}

      subject actions purpose)))

;; TODO: How is this call protected from unauthorized use? Must call this with
;; access-token to verify subject.
(defn allowed-subjects
  "Given a resource and a set of actions, which subjects can access and via which
  actions?"
  [db resource actions {:keys [purpose]}]
  (let [rules (actions->rules db actions)]
    (->> (xt/q
          db
          {:find '[subject action]
           :keys '[subject action]
           :where
           '[
             [action ::site/type "https://meta.juxt.site/pass/action"]

             ;; Only consider given actions
             [(contains? actions action)]

             ;; Only consider a permitted action
             [permission ::site/type "https://meta.juxt.site/pass/permission"]
             [permission ::pass/action action]
             (allowed? permission subject action resource)

             ;; Only permissions that match our purpose
             [permission ::pass/purpose purpose]

             #_[access-token ::pass/subject subject]]

           :rules rules

           :in '[resource actions purpose]}

          resource actions purpose))))

;; TODO: This subject should be folded into the pass-ctx as it's optional (could
;; be an anonymous action)
(defn pull-allowed-resource
  "Given a subject, a set of possible actions and a resource, pull the allowed
  attributes."
  [db actions resource {::pass/keys [subject purpose]}]
  (let [check-result
        (check-permissions
         db
         actions
         {:subject subject
          :purpose purpose
          :resource resource})

        pull-expr (vec (mapcat
                        (fn [{:keys [action]}]
                          (::pass/pull action))
                        check-result))]
    (xt/pull db pull-expr resource)))

(defn pull-allowed-resources
  "Given a subject and a set of possible actions, which resources are allowed, and
  get me the documents. If resources-in-scope is given, only consider resources
  in that set."
  [db actions {::pass/keys [subject purpose include-rules resources-in-scope]}]
  (let [rules (actions->rules db actions)
        _ (when-not (seq rules)
            (throw (ex-info "No rules found for actions" {:actions actions})))
        results
        (xt/q
         db
         {:find '[resource (pull action [::xt/id ::pass/pull]) purpose permission]
          :keys '[resource action purpose permission]
          :where
          (cond-> '[
                    ;; Only consider given actions
                    [action ::site/type "https://meta.juxt.site/pass/action"]
                    [(contains? actions action)]

                    ;; Only consider allowed permssions
                    [permission ::site/type "https://meta.juxt.site/pass/permission"]
                    [permission ::pass/action action]
                    (allowed? permission subject action resource)

                    ;; Only permissions that match our purpose
                    [permission ::pass/purpose purpose]]

            include-rules
            (conj '(include? subject action resource))

            resources-in-scope
            (conj '[(contains? resources-in-scope resource)]
                  ))

          :rules (vec (concat rules include-rules))

          :in '[subject actions purpose resources-in-scope]}

         subject actions purpose (or resources-in-scope #{}))

        pull-expr (vec (mapcat (comp ::pass/pull :action) results))]

    (->> results
         (map :resource)
         (xt/pull-many db pull-expr))))

(defn join-with-pull-allowed-resources
  "Join collection on given join-key with another pull of allowed-resources with
  given actions and options."
  [db coll join-key actions options]
  (let [idx (->>
             (assoc options ::pass/resources-in-scope (set (map join-key coll)))
             (pull-allowed-resources db actions)
             (group-by :xt/id))]
    (map #(update % join-key (comp first idx)) coll)))

(defn resolve-with-ctx [form ctx]
  (postwalk
   (fn [x]
     (if (and (vector? x) (= (first x) ::pass/resolve))
       (ctx (second x))
       x))
   form))

(defmulti apply-processor (fn [processor action acc] (first processor)))

(defmethod apply-processor :default [[kw] action acc]
  (throw (ex-info (format "No processor for %s" kw) {:kw kw :action action})))

(defmethod apply-processor :juxt.pass.alpha.process/update-in [[kw ks f-sym & update-in-args] action acc]
  (assert (vector? (:args acc)))
  (let [f (case f-sym 'merge merge nil)]
    (when-not f
      (throw (ex-info "Unsupported update-in function" {:f f-sym})))
    (apply update acc :args update-in ks f (resolve-with-ctx update-in-args (:ctx acc)))))

(defmethod apply-processor ::xt/put [[kw ks] action acc]
  (update acc :args (fn [args] (mapv (fn [arg] [::xt/put arg]) args))))

(defmethod apply-processor ::pass.malli/validate [_ {::pass.malli/keys [args-schema]} acc]
  (let [resolved-args-schema (resolve-with-ctx args-schema (:ctx acc))]
    (when-not (m/validate resolved-args-schema (:args acc))
      (throw
       (ex-info
        "Failed validation check"
        ;; Not sure why Malli throws this error here: No implementation of
        ;; method: :-form of protocol: #'malli.core/Schema found for class: clojure.lang.PersistentVector
        ;;
        ;; Workaround is to pr-str and read-string
        (read-string (pr-str (m/explain resolved-args-schema (:args acc))))))))
  acc)

(defmethod apply-processor :gen-hex-string [[_ k size] action acc]
  (update acc :ctx assoc k (as-hex-str (random-bytes size))))

(defmethod apply-processor :add-prefix [[_ k prefix] action acc]
  (update acc :ctx update k (fn [old] (str prefix old))))

(defn process-args [pass-ctx action args]
  (:args
   (reduce
    (fn [acc processor]
      (apply-processor processor action acc))
    {:args args
     :ctx pass-ctx}
    (::pass/process action))))

(defn do-action*
  [xt-ctx
   {resource ::pass/resource
    purpose ::pass/purpose
    subject ::pass/subject
    action ::pass/action
    :as pass-ctx}
    args]
  (assert (vector? args))
  (let [db (xt/db xt-ctx)
        tx (xt/indexing-tx xt-ctx)]
    (try
      ;; Check that we /can/ call the action
      (let [check-permissions-result
            (check-permissions
             db
             #{action}
             {:subject subject
              :resource resource
              :purpose purpose})

            action-doc (xt/entity db action)
            _ (when-not action-doc
                (throw
                 (ex-info
                  (format "Action '%s' not found in db" action)
                  {:action action})))]

        (when-not (seq check-permissions-result)
          (throw
           (ex-info
            "Action denied"
            {:subject subject
             :action action
             :resource resource
             :purpose purpose})))

        (let [processed-args (process-args pass-ctx action-doc args)]
          (doseq [arg processed-args]
            (when-not (and (vector? arg) (keyword? (first arg)))
              (throw (ex-info "Every arg must be processed to return a tx op" {:action action :arg arg}))))

          (conj
           processed-args
           [::xt/put
            (into
             {:xt/id (format "urn:site:action-log:%s" (::xt/tx-id tx))
              ::site/type "ActionLogEntry"
              ::pass/subject subject
              ::pass/action action
              ::pass/purpose purpose
              ::pass/puts (vec
                           (keep
                            (fn [[tx-op {id :xt/id}]]
                              (when (= tx-op ::xt/put) id))
                            processed-args))
              ::pass/deletes (vec
                              (keep
                               (fn [[tx-op {id :xt/id}]]
                                 (when (= tx-op ::xt/delete) id))
                               processed-args))}
             tx)])))

      (catch Exception e
        (log/errorf e "Error when doing action: %s %s" action (format "urn:site:action-log:%s" (::xt/tx-id tx)))
        [[::xt/put
          {:xt/id (format "urn:site:action-log:%s" (::xt/tx-id tx))
           ::site/type "ActionLogEntry"
           ::site/error {:message (.getMessage e)
                         :ex-data (ex-data e)}}]]))))

(defn install-do-action-fn []
  {:xt/id "urn:site:tx-fns:do-action"
   :xt/fn '(fn [xt-ctx pass-ctx args]
             (juxt.pass.alpha.authorization/do-action* xt-ctx pass-ctx (vec args)))})

(defn do-action [xt-node pass-ctx action & args]
  (assert (xt/entity (xt/db xt-node) "urn:site:tx-fns:do-action"))
  (let [tx (xt/submit-tx
            xt-node
            [[::xt/fn
              "urn:site:tx-fns:do-action"
              (assoc pass-ctx ::pass/action action)
              args]])

        {::xt/keys [tx-id]} (xt/await-tx xt-node tx)]

    ;; Throw a nicer error
    (when-not (xt/tx-committed? xt-node tx)
      (throw
       (ex-info
        "Transaction failed to be committed"
        {::xt/tx-id tx-id
         ::pass/action action})))

    (xt/entity
     (xt/db xt-node)
     (format "urn:site:action-log:%s" tx-id))))
