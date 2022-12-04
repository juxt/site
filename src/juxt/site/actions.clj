; Copyright © 2022, JUXT LTD.

(ns juxt.site.actions
  (:require
   [juxt.grab.alpha.parser :as graphql.parser]
   [juxt.grab.alpha.schema :as graphql.schema]
   [clojure.tools.logging :as log]
   [crypto.password.bcrypt :as bcrypt]
   [java-http-clj.core :as hc]
   [jsonista.core :as json]
   [juxt.site.http-authentication :as http-authn]
   [juxt.site.openid-connect :as openid-connect]
   [juxt.site.util :refer [make-nonce]]
   [malli.core :as malli]
   [ring.util.codec :as codec]
   [sci.core :as sci]
   [xtdb.api :as xt]
   juxt.site.schema))

(defn actions->rules
  "Determine rules for the given action ids. Each rule is bound to the given
  action."
  [db actions]
  ;; Old version
  (comment
    (vec (for [action actions
               :let [e (xt/entity db action)]
               rule (:juxt.site/rules e)]
           (conj rule ['action :xt/id action]))))
  (mapv
   #(conj (second %) ['action :xt/id (first %)])
   (xt/q db {:find ['e 'rules]
             :where [['e :xt/id (set actions)]
                     ['e :juxt.site/rules 'rules]]})))

;; This is broken out into its own function to assist debugging when
;; authorization is denied and we don't know why. A better authorization
;; debugger is definitely required.
(defn ^{:private true} query-permissions
  [{:keys [db rules subject actions resource purpose]}]
  (assert (or (nil? subject) (string? subject)))
  (assert (or (nil? resource) (string? resource)))
  (let [query {:find '[(pull permission [*]) (pull action [*])]
               :keys '[juxt.site/permission juxt.site/action]
               :where
               '[
                 [action :juxt.site/type "https://meta.juxt.site/site/action"]

                 ;; Only consider given actions
                 [(contains? actions action)]

                 ;; Only consider a permitted action
                 [permission :juxt.site/type "https://meta.juxt.site/site/permission"]
                 [permission :juxt.site/action action]
                 (allowed? subject resource permission)

                 ;; Only permissions that match our purpose
                 [permission :juxt.site/purpose purpose]]

               :rules rules

               :in '[subject actions resource purpose]}]
    (try
      (xt/q
       db
       query
       subject actions resource purpose)
      (catch Exception e
        (throw (ex-info "Failed to query permissions" {:query query} e))))))

(defn check-permissions
  "Given a subject, possible actions and resource, return all related pairs of permissions and actions."
  [db actions {subject :juxt.site/subject resource :juxt.site/resource purpose :juxt.site/purpose :as options}]

  (when (= (find options :juxt.site/subject) [:juxt.site/subject nil])
    (throw (ex-info "Nil subject passed!" {})))

  ;; TODO: These asserts have been replaced by Malli schema instrumentation
  (assert (or (nil? subject) (map? subject)) "Subject expected to be a map, or null")
  (assert (or (nil? resource) (map? resource)) "Resource expected to be a map, or null")

  (let [rules (actions->rules db actions)]
    (when (seq rules)
      (let [permissions
            (query-permissions
             {:db db
              :rules rules
              :subject (:xt/id subject)
              :actions actions
              :resource (:xt/id resource)
              :purpose purpose})]
        ;;(log/debugf "Returning permissions: %s" (pr-str permissions))
        permissions))))

(malli/=>
 check-permissions
 [:=> [:cat
       :any
       [:set :string]
       [:map
        [:juxt.site/subject {:optional true}]
        [:juxt.site/resource {:optional true}]
        [:juxt.site/purpose {:optional true}]]]
  :any])

(defn allowed-resources
  "Given a set of possible actions, and possibly a subject and purpose, which
  resources are allowed?"
  [db actions {:juxt.site/keys [subject purpose]}]
  (let [rules (actions->rules db actions)
        query {:find '[resource]
               :where
               '[
                 [action :juxt.site/type "https://meta.juxt.site/site/action"]

                 ;; Only consider given actions
                 [(contains? actions action)]

                 ;; Only consider a permitted action
                 [permission :juxt.site/type "https://meta.juxt.site/site/permission"]
                 [permission :juxt.site/action action]
                 (allowed? subject resource permission)

                 ;; Only permissions that match our purpose
                 [permission :juxt.site/purpose purpose]]

               :rules rules

               :in '[subject actions purpose]}]

    (try
      (xt/q db query (:xt/id subject) actions purpose)
      (catch Exception cause
        (throw
         (ex-info
          "Actions query failed"
          {:query query
           :rules rules
           :subject subject
           :actions actions
           :action-entities (doseq [a actions] (xt/entity db a))
           :purpose purpose}
          cause))))))

(malli/=>
 allowed-resources
 [:=> [:cat
       :any
       [:set {:min 0} :string]
       ;;[:set :string]
       [:map
        [:juxt.site/subject {:optional true}]
        [:juxt.site/purpose {:optional true}]]]
  :any])

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
             [action :juxt.site/type "https://meta.juxt.site/site/action"]

             ;; Only consider given actions
             [(contains? actions action)]

             ;; Only consider a permitted action
             [permission :juxt.site/type "https://meta.juxt.site/site/permission"]
             [permission :juxt.site/action action]
             (allowed? subject resource permission)

             ;; Only permissions that match our purpose
             [permission :juxt.site/purpose purpose]

             #_[access-token :juxt.site/subject subject]]

           :rules rules

           :in '[resource actions purpose]}

          resource actions purpose))))

(defn pull-allowed-resource
  "Given a subject, a set of possible actions and a resource, pull the allowed
  attributes."
  [db actions resource ctx]
  (let [check-result
        (check-permissions
         db
         actions
         (assoc ctx :juxt.site/resource resource))

        pull-expr (vec (mapcat
                        (fn [{:juxt.site/keys [action]}]
                          (:juxt.site/pull action))
                        check-result))]
    (xt/pull db pull-expr (:xt/id resource))))

(malli/=>
 pull-allowed-resource
 [:=> [:cat
       :any
       [:set :string]
       :juxt.site/resource
       [:map
        [:juxt.site/subject {:optional true}]
        [:juxt.site/purpose {:optional true}]]]
  :any])

(defn pull-allowed-resources
  "Given a subject and a set of possible actions, which resources are allowed, and
  get me the documents. If resources-in-scope is given, only consider resources
  in that set."
  [db actions {:juxt.site/keys [subject purpose include-rules resources-in-scope]}]
  (let [rules (actions->rules db actions)
        _ (when-not (seq rules)
            (throw (ex-info "No rules found for actions" {:actions actions})))
        results
        (xt/q
         db
         {:find '[resource (pull action [:xt/id :juxt.site/pull]) purpose permission]
          :keys '[resource action purpose permission]
          :where
          (cond-> '[
                    ;; Only consider given actions
                    [action :juxt.site/type "https://meta.juxt.site/site/action"]
                    [(contains? actions action)]

                    ;; Only consider allowed permssions
                    [permission :juxt.site/type "https://meta.juxt.site/site/permission"]
                    [permission :juxt.site/action action]
                    (allowed? subject resource permission)

                    ;; Only permissions that match our purpose
                    [permission :juxt.site/purpose purpose]]

            include-rules
            (conj '(include? subject action resource))

            resources-in-scope
            (conj '[(contains? resources-in-scope resource)]))

          :rules (vec (concat rules include-rules))

          :in '[subject actions purpose resources-in-scope]}

         (:xt/id subject) actions purpose (or resources-in-scope #{}))]

    ;; TODO: Too complex, extract this and unit test. The purpose here it to
    ;; apply the pull of each relevant action to each result, and merge the
    ;; results into a single map.
    (doall
     (for [[resource resource-group] (group-by :resource results)]
       (apply merge
              (for [{:keys [action]}
                    ;; TODO: Purpose and permission are useful metadata, how do
                    ;; we retain in the result? with-meta?
                    resource-group]
                (xt/pull db (:juxt.site/pull action '[*]) resource)))))))

(malli/=>
 pull-allowed-resources
 [:=> [:cat
        :any
        [:set :string]
        [:map
         [:juxt.site/subject {:optional true}]
         [:juxt.site/purpose {:optional true}]]]
   :any])

(defn join-with-pull-allowed-resources
  "Join collection on given join-key with another pull of allowed-resources with
  given actions and options."
  [db coll join-key actions options]
  (let [idx (->>
             (assoc options :juxt.site/resources-in-scope (set (map join-key coll)))
             (pull-allowed-resources db actions)
             (group-by :xt/id))]
    (map #(update % join-key (comp first idx)) coll)))

(defn common-sci-namespaces [action-doc]
  {
   'com.auth0.jwt.JWT
   {'decode (fn [x] (com.auth0.jwt.JWT/decode x))}

   'crypto.password.bcrypt {'encrypt bcrypt/encrypt}

   'java-http-clj.core
   {'send hc/send}

   'jsonista.core
   {'write-value-as-string json/write-value-as-string
    'read-value json/read-value}

   'juxt.site
   {'decode-id-token juxt.site.openid-connect/decode-id-token}

   'juxt.site.malli
   {'validate (fn [schema value] (malli/validate schema value))
    'explain (fn [schema value] (malli/explain schema value))
    'validate-input
    (fn [input]
      (let [schema (get-in action-doc [:juxt.site.malli/input-schema])
            valid? (malli/validate schema input)]
        (when-not valid?
          (throw
           (ex-info
            "Validation failed"
            {:error :validation-failed
             :input input
             :schema schema})))
        input))}

   'grab
   {'parse graphql.parser/parse
    'compile-schema graphql.schema/compile-schema*}

   'ring.util.codec
   {'form-encode codec/form-encode
    'form-decode codec/form-decode}})

(defn do-action-in-tx-fn
  "This function is applied within a transaction function. It should be fast, but
  at least doesn't have to worry about the database being stale!"
  [xt-ctx
   {subject :juxt.site/subject
    action :juxt.site/action
    access-token :juxt.site/access-token
    resource :juxt.site/resource
    purpose :juxt.site/purpose
    base-uri :juxt.site/base-uri
    prepare :juxt.site/prepare
    :as ctx}]
  (let [db (xt/db xt-ctx)
        tx (xt/indexing-tx xt-ctx)]
    (try
      (assert base-uri "The base-uri must be provided")
      (assert (or (nil? subject) (map? subject)) "Subject to do-action-in-tx-fn expected to be a string, or null")
      (assert (or (nil? resource) (map? resource)) "Resource to do-action-in-tx-fn expected to be a string, or null")

      ;; Check that we /can/ call the action
      (let [check-permissions-result
            (check-permissions db #{action} ctx)

            {:juxt.site/keys [scope] :as action-doc} (xt/entity db action)
            _ (when-not action-doc
                (throw
                 (ex-info
                  (format "Action '%s' not found in db" action)
                  {:action action})))

            _ (when scope
                (when-not access-token
                  (throw
                   (ex-info
                    (format "Action (%s) requires a scope (%s) but there is no access-token" action scope)
                    {:ring.response/status 403
                     :action action
                     :scope scope})))
                (when-not (contains? (set (:juxt.site/scope access-token)) scope)
                  (throw
                   (ex-info
                    (format "Access token does not have sufficient scope (%s)" scope)
                    {:ring.response/status 403
                     :access-token access-token
                     :action action
                     :scope-granted (:juxt.site/scope access-token)
                     :scope-required scope}))))]

        (when-not (seq check-permissions-result)
          (throw (ex-info "Action denied" ctx)))

        (let [fx
              (cond
                ;; Official: sci
                (-> action-doc :juxt.site/transact :juxt.site.sci/program)
                (try
                  (sci/eval-string
                   (-> action-doc :juxt.site/transact :juxt.site.sci/program)
                   {:namespaces
                    (merge-with
                     merge
                     {'user
                      {'*action* action-doc
                       '*resource* resource
                       '*prepare* prepare
                       '*ctx* ctx}
                      ;; Allowed to access the database
                      'xt
                      {'entity (fn [id] (xt/entity db id))
                       ;; Definitely can't do this! But we may be able to give access to an action that can do it.
                       ;;'q (fn [& args] (apply xt/q db args))
                       }

                      'juxt.site
                      {'match-identity
                       (fn [m]
                         (ffirst
                          (xt/q db {:find ['id]
                                    :where (into
                                            [['id :juxt.site/type "https://meta.juxt.site/site/user-identity"]]
                                            (for [[k v] m] ['id k v] ))})))

                       'match-identity-with-password
                       (fn [m password password-hash-key]
                         (ffirst
                          (xt/q db {:find ['id]
                                    :where (into
                                            [['id :juxt.site/type "https://meta.juxt.site/site/user-identity"]
                                             ['id password-hash-key 'password-hash]
                                             ['(crypto.password.bcrypt/check password password-hash)]
                                             ]
                                            (for [[k v] m] ['id k v]))
                                    :in ['password]} password)))

                       'lookup-application
                       (fn [client-id]
                         (let [results (xt/q
                                        db
                                        '{:find [(pull e [*])]
                                          :where [[e :juxt.site/type "https://meta.juxt.site/site/application"]
                                                  [e :juxt.site/client-id client-id]]
                                          :in [client-id]} client-id)]
                           (if (= 1 (count results))
                             (ffirst results)
                             (if (seq results)
                               (throw
                                (ex-info
                                 (format "Too many applications for client-id: %s" client-id)
                                 {:client-id client-id
                                  :applications results}))
                               (throw
                                (ex-info
                                 (format "No application with client-id: %s" client-id)
                                 {:client-id client-id}))))))

                       'lookup-scope
                       (fn [scope]
                         (let [results (xt/q
                                        db
                                        '{:find [(pull e [*])]
                                          :where [[e :juxt.site/type "https://meta.juxt.site/site/oauth-scope"]]})]

                           (if (= 1 (count results))
                             (ffirst results)
                             (if (seq results)
                               (throw
                                (ex-info
                                 (format "Multiple documents for scope: %s" scope)
                                 {:scope scope
                                  :documents (map :xt/id results)}))
                               (throw
                                (ex-info
                                 (format "No such scope: %s" scope)
                                 {:error "invalid_scope"}))))))}

                      'grab
                      {'parsed-types
                       (fn parsed-types [schema-id]
                         (map :juxt.grab/type-definition
                              (map first
                                   (xt/q db '{:find [(pull e [:juxt.grab/type-definition])]
                                              :where [[e :juxt.site/type "https://meta.juxt.site/site/graphql-type"]
                                                      [e :juxt.site/graphql-schema schema-id]]
                                              :in [schema-id]}
                                         schema-id))))}}

                     (common-sci-namespaces action-doc))

                    :classes
                    {'java.util.Date java.util.Date
                     'java.time.Instant java.time.Instant}

                    ;; We can't allow random numbers to be computed as they
                    ;; won't be the same on each node. If this is a problem, we
                    ;; can replace with a (non-secure) PRNG seeded from the
                    ;; tx-instant of the tx. Note that secure random numbers
                    ;; should not be generated this way anyway, since then it
                    ;; would then be possible to mount an attack based on
                    ;; knowledge of the current time. Instead, secure random
                    ;; numbers should be generated in the action's 'prepare'
                    ;; step.
                    :deny `[loop recur rand rand-int]})

                  (catch clojure.lang.ExceptionInfo e
                    ;; The sci.impl/callstack contains a volatile which isn't freezable.
                    ;; Also, we want to unwrap the original cause exception.
                    ;; Possibly, in future, we should get the callstack
                    (throw (ex-info (.getMessage e) (or (ex-data (.getCause e)) {}) (.getCause e)))))

                ;; There might be other strategies in the future (although the
                ;; fewer the better really)
                :else
                (throw
                 (ex-info
                  "Submitted actions should have a valid juxt.site/transact entry"
                  {:action action-doc})))

              _ (log/debugf "FX are %s" (pr-str fx))

              ;; Validate
              _ (doseq [effect fx]
                  (when-not (and (vector? effect)
                                 (keyword? (first effect))
                                 (if (= :xtdb.api/put (first effect))
                                   (map? (second effect))
                                   true))
                    (throw (ex-info "Invalid effect" {:juxt.site/action action :effect effect}))))

              xtdb-ops (filter (fn [[effect]] (= (namespace effect) "xtdb.api")) fx)

              ;; Deprecated
              apply-to-request-context-fx (filter (fn [[effect]] (= effect :juxt.site/apply-to-request-context)) fx)
              ;; Decisions we've made which don't update the database but should
              ;; be record and reflected in the response.
              other-response-fx
              (remove
               (fn [[kw]]
                 (or
                  (= (namespace kw) "xtdb.api")
                  (= kw :juxt.site/apply-to-request-context)))
               fx)

              result-fx
              (conj
               xtdb-ops
               ;; Add an action log entry for this transaction
               [:xtdb.api/put
                (into
                 (cond->
                     {:xt/id (format "%s/_site/events/%s" base-uri (::xt/tx-id tx))
                      :juxt.site/type "https://meta.juxt.site/site/event"
                      :juxt.site/subject-uri (:xt/id subject)
                      :juxt.site/action action
                      :juxt.site/purpose purpose
                      :juxt.site/puts (vec
                                       (keep
                                        (fn [[tx-op {id :xt/id}]]
                                          (when (= tx-op ::xt/put) id))
                                        xtdb-ops))
                      :juxt.site/deletes (vec
                                          (keep
                                           (fn [[tx-op {id :xt/id}]]
                                             (when (= tx-op ::xt/delete) id))
                                           xtdb-ops))}
                     tx (into tx)

                     ;; Any quotations that we want to apply to the request context?
                     ;; (deprecated)
                     (seq apply-to-request-context-fx)
                     (assoc :juxt.site/apply-to-request-context-ops apply-to-request-context-fx)

                     (seq other-response-fx)
                     (assoc :juxt.site/response-fx other-response-fx)

                     ))])]

          ;; This isn't the best debugger :( - need a better one!
          ;;(log/debugf "XXXX Result is: %s" result-ops)

          result-fx))

      (catch Throwable e
        (let [event-id (format "%s/_site/events/%d" base-uri (::xt/tx-id tx))]
          (log/errorf e "Error when performing action: %s %s" action event-id)

          [[::xt/put
            {:xt/id event-id
             :juxt.site/type "https://meta.juxt.site/site/event"
             :juxt.site/subject subject
             :juxt.site/action action
             :juxt.site/resource resource
             :juxt.site/purpose purpose
             :juxt.site/error {:message (.getMessage e)
                               ;; ex-data is just too problematic to put into the
                               ;; database as-is without some sanitization ensuring
                               ;; it's nippyable.
                               :ex-data (ex-data e)
                               ;; The site db will not be nippyable

                               #_(dissoc (ex-data e) :env)
                               ;; TODO: Ideally we'd like the environment in the
                               ;; action log for debugging purposes. But the below
                               ;; stills fails with a nippy error, haven't
                               ;; investigated thorougly enough.
                               #_(let [ex-data (ex-data e)]
                                   (cond-> ex-data
                                     (:env ex-data) (dissoc :env)#_(update :env dissoc :juxt.site/db :juxt.site/xt-node)))}}]])))))

(defn install-do-action-fn [uri]
  {:xt/id (str uri "/_site/do-action")
   :xt/fn '(fn [xt-ctx ctx & args]
             (juxt.site.actions/do-action-in-tx-fn xt-ctx ctx))})

;; Remove anything in the ctx that will upset nippy. However, in the future
;; we'll definitely want to record all inputs to actions, so this is an
;; opportunity to decide which entries form the input 'record' and which are
;; only transitory for the purposes of responnding to the request.

(defn sanitize-ctx [ctx]
  (dissoc ctx :juxt.site/xt-node :juxt.site/db))

(defn apply-response-fx [ctx fx]
  (reduce
   (fn [ctx [op & args]]
     (case op
       :ring.response/status (assoc ctx :ring.response/status (first args))
       :ring.response/headers (update ctx :ring.response/headers (fnil {} into) (first args))
       :ring.response/body (assoc ctx :ring.response/body (first args))
       (throw
        (ex-info
         (format "Op not recognized: %s" op)
         {:op op :args args}))))
   ctx fx))

(defn do-prepare [{:juxt.site/keys [db resource] :as ctx} action-doc]
  (when-let [prepare-program (some-> action-doc :juxt.site/prepare :juxt.site.sci/program)]
    (try
      (sci/eval-string
       prepare-program
       {:namespaces
        (merge
         {'user {'*action* action-doc
                 '*resource* resource
                 '*ctx* (sanitize-ctx ctx)
                 'logf (fn [& args] (eval `(log/debugf ~@args)))
                 'log (fn [& args] (eval `(log/debug ~@args)))}

          'xt
          { ;; Unsafe due to violation of strict serializability, hence marked as
           ;; entity*
           'entity*
           (fn [id] (xt/entity db id))}

          'juxt.site.util {'make-nonce make-nonce}}
         (common-sci-namespaces action-doc))
        :classes
        {'java.util.Date java.util.Date
         'java.time.Instant java.time.Instant
         'java.time.Duration java.time.Duration}})
      (catch clojure.lang.ExceptionInfo e
        (throw (ex-info "Failure during prepare" {:cause-ex-info (ex-data e)} e))))))

(defn do-action
  [;; TODO: Arguably action should passed as a map
   {:juxt.site/keys [xt-node db base-uri resource subject action] :as ctx}]
  (assert (:juxt.site/xt-node ctx) "xt-node must be present")
  (assert (:juxt.site/db ctx) "db must be present")

  (when-not (xt/entity db action)
    (throw (ex-info "No action found"
                    {:action action
                     :ls (->> (xt/q db '{:find [(pull e [:xt/id :juxt.site/type])]
                                      :where [[e :xt/id]]})
                              (map first)
                              (filter (fn [e]
                                        (not (#{"Request"
                                                "ActionLogEntry"
                                                "Session"
                                                "SessionToken"
                                                }
                                              (:juxt.site/type e)))))
                              (map :xt/id)
                              (sort-by str))}))
    )

  (assert (xt/entity db action) (format "Action '%s' must exist in database" action))


  (log/debugf "Doing action: %s" action)

  (when-not (or (nil? subject) (map? subject))
    (throw
     (ex-info
      "Subject to do-action expected to be a map, or null"
      {:juxt.site/request-context ctx :subject subject})))

  (when-not (or (nil? resource) (map? resource))
    (throw
     (ex-info
      "Resource to do-action expected to be a map, or null"
      {:juxt.site/request-context ctx :resource resource})))

  ;; Prepare the transaction - this work happens prior to the transaction, one a
  ;; single node, and may be wasted work if the transaction ultimately
  ;; fails. However, it is a good place to compute any secure random numbers
  ;; which can't be done in the transaction.

  ;; The :juxt.site/subject can be nil, if this action is being performed
  ;; by an anonymous user.
  (let [action-doc (xt/entity db action)
        _ (when-not action-doc
            (throw (ex-info (format "Action not found in the database: %s" action) {:action action})))
        prepare (do-prepare ctx action-doc)
        tx-fn (str base-uri "/_site/do-action")
        _ (assert (xt/entity db tx-fn) (format "do-action must exist in database: %s" tx-fn))
        tx-ctx (cond-> (sanitize-ctx ctx) prepare (assoc :juxt.site/prepare prepare))
        tx (xt/submit-tx xt-node [[::xt/fn tx-fn tx-ctx]])
        {::xt/keys [tx-id] :as tx} (xt/await-tx xt-node tx)
        ctx (assoc ctx :juxt.site/db (xt/db xt-node tx))]

    (when-not (xt/tx-committed? xt-node tx)
      (throw
       (ex-info
        (format "Transaction failed to be committed for action %s" action)
        {::xt/tx-id tx-id
         :juxt.site/action action
         :juxt.site/request-context ctx})))

    (let [result
          (xt/entity
           (xt/db xt-node)
           (format "%s/_site/events/%d" base-uri tx-id))]
      (if-let [error (:juxt.site/error result)]
        (do
          (log/errorf "Transaction error: %s" error)
          (let [status (:ring.response/status (:ex-data error))]
            (throw
             (ex-info
              (format
               "Transaction error performing action %s: %s%s"
               action
               (:message error)
               (if status (format "(status: %s)" status) ""))
              (into
               (cond-> {:juxt.site/request-context ctx}
                 status (assoc :ring.response/status status))
               (merge
                (dissoc result :juxt.site/type :xt/id :juxt.site/error)
                (:ex-data error)))))))

        (cond-> ctx
          result (assoc :juxt.site/action-result result)

          (seq (:juxt.site/response-fx result))
          (apply-response-fx (:juxt.site/response-fx result))

          )))))

;; TODO: Since it is possible that a permission is in the queue which might
;; grant or revoke an action, it is necessary to run this check 'head-of-line'
;; and submit a transaction function. This will avoid any non-determinism caused
;; by a race-condition and retain proper serialization of transactions.
;;
;; For a fuller discussion on determinism and its benefits, see
;; https://www.cs.umd.edu/~abadi/papers/abadi-cacm2018.pdf
(defn wrap-authorize-with-actions [h]
  (fn [{:juxt.site/keys [db resource uri subject]
        :ring.request/keys [method]
        :as req}]

    (assert (or (nil? subject) (map? subject)))
    (assert (or (nil? resource) (map? resource)))

    (let [actions (get-in resource [:juxt.site/methods method :juxt.site/actions])

          _ (assert actions (format "No actions for method %s" method))

          _ (doseq [action actions]
              (when-not (xt/entity db action)
                (throw (ex-info (format "No such action: %s" action) {:juxt.site/request-context req
                                                                      :missing-action action}))))

          _ (log/tracef "actions are %s" (pr-str {:actions actions}))

          permitted-actions
          (check-permissions
           db
           actions
           ;; TODO: Isn't this now superfluous - can't we pass through the req?
           (cond-> {}
             ;; When the resource is in the database, we can add it to the
             ;; permission checking in case there's a specific permission for
             ;; this resource.
             subject (assoc :juxt.site/subject subject)
             resource (assoc :juxt.site/resource resource)))]

      #_(log/debugf "Permitted actions: %s" (pr-str permitted-actions))
      (log/tracef "Subject is %s" (pr-str subject))

      (if (seq permitted-actions)
        (h (assoc req :juxt.site/permitted-actions permitted-actions))

        (if subject
          (throw
           (ex-info
            (format "No permission for actions: %s" (pr-str actions))
            {:ring.response/status 403
             :juxt.site/request-context req}))

          ;; No subject?
          (if-let [protection-spaces (:juxt.site/protection-space resource)]
            ;; We are in a protection space, so this is HTTP Authentication (401
            ;; + WWW-Authenticate header)
            (throw
             (ex-info
              (format "No anonymous permission for actions (try authenticating!): %s" (pr-str actions))
              {:ring.response/status 401
               :ring.response/headers
               {"www-authenticate" (http-authn/www-authenticate-header db protection-spaces)}
               :juxt.site/request-context req}))

            ;; We are outside a protection space, there is nothing we can do
            ;; except return a 403 status.

            ;; We MUST NOT return a 401 UNLESS we can
            ;; set a WWW-Authenticate header (which we can't, as there is no
            ;; protection space). 403 is the only option afforded by RFC 7231: "If
            ;; authentication credentials were provided in the request ... the
            ;; client MAY repeat the request with new or different credentials. "
            ;; -- Section 6.5.3, RFC 7231

            ;; TODO: But are we inside a session-scope ? If so, we can
            ;; respond with a redirect to a page that will establish (immediately
            ;; or eventually), the cookie.

            (if-let [session-scope (:juxt.site/session-scope req)]
              (let [login-uri (:juxt.site/login-uri session-scope)
                    redirect (str login-uri "?return-to=" (codec/url-encode uri))]
                ;; If we are in a session-scope that contains a login-uri, let's redirect to that
                (throw
                 (ex-info
                  (format "No anonymous permission for actions (try logging in!): %s" (pr-str actions))
                  {:ring.response/status 302
                   :ring.response/headers {"location" redirect}
                   :juxt.site/request-context req})))
              (throw
               (ex-info
                (format "No anonymous permission for actions: %s" (pr-str actions))
                {:ring.response/status 403
                 :juxt.site/request-context req})))))))))

(comment
  (sci/eval-string
   "(+ (clojure.core/rand) 10)"
   {:namespaces {'clojure.core {'rand (constantly 0.5)}}
    ;;:deny '[+]
    }
   ))
