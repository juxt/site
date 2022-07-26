;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.actions
  (:require
   [clojure.tools.logging :as log]
   [malli.error :a me]
   [ring.util.codec :as codec]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.http-authentication :as http-authn]
   [juxt.site.alpha :as-alias site]
   [juxt.flip.alpha.core :refer [eval-quotation]]
   [juxt.flip.alpha :as-alias flip]
   [xtdb.api :as xt]))

(defn actions->rules
  "Determine rules for the given action ids. Each rule is bound to the given
  action."
  [db actions]
  (mapv
   #(conj (second %) ['action :xt/id (first %)])
   (xt/q db {:find ['e 'rules]
             :where [['e :xt/id actions]
                     ['e ::pass/rules 'rules]]})))

;; This is broken out into its own function to assist debugging when
;; authorization is denied and we don't know why. A better authorization
;; debugger is definitely required.
(defn query-permissions [{:keys [db rules subject actions resource purpose] :as args}]
  (assert (or (nil? subject) (string? subject)))
  (assert (or (nil? resource) (string? resource)))
  (xt/q
   db
   {:find '[(pull permission [*]) (pull action [*])]
    :keys '[juxt.pass.alpha/permission juxt.pass.alpha/action]
    :where
    '[
      [action ::site/type "https://meta.juxt.site/pass/action"]

      ;; Only consider given actions
      [(contains? actions action)]

      ;; Only consider a permitted action
      [permission ::site/type "https://meta.juxt.site/pass/permission"]
      [permission ::pass/action action]
      (allowed? subject resource permission)

      ;; Only permissions that match our purpose
      [permission ::pass/purpose purpose]
      ]

    :rules rules

    :in '[subject actions resource purpose]}

   subject actions resource purpose)
  )

(defn check-permissions
  "Given a subject, possible actions and resource, return all related pairs of permissions and actions."
  [db actions {subject ::pass/subject resource ::site/resource purpose ::pass/purpose}]

  (assert (or (nil? subject) (string? subject)) "Subject expected to be a string, or null")
  (assert (or (nil? resource) (string? resource)) "Resource expected to be a string, or null")

  (log/debugf "check-permissions: resource: %s, purpose: %s, actions: %s, subject: %s" resource purpose actions subject)

  (log/debugf "Actions: %s" actions)
  (log/debugf "Rules: %s" (actions->rules db actions))

  (let [rules (actions->rules db actions)]
    (when (seq rules)
      (let [permissions
            (query-permissions
             {:db db
              :rules rules
              :subject subject
              :actions actions
              :resource resource
              :purpose purpose})]
        ;;(log/debugf "Returning permissions: %s" (pr-str permissions))
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
        (allowed? subject resource permission)

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
             (allowed? subject resource permission)

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
  [db actions resource {::pass/keys [subject purpose] :as pass-ctx}]
  (let [check-result
        (check-permissions
         db
         actions
         (assoc pass-ctx ::site/resource resource))

        pull-expr (vec (mapcat
                        (fn [{::pass/keys [action]}]
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
         {:find '[resource (pull action [:xt/id ::pass/pull]) purpose permission]
          :keys '[resource action purpose permission]
          :where
          (cond-> '[
                    ;; Only consider given actions
                    [action ::site/type "https://meta.juxt.site/pass/action"]
                    [(contains? actions action)]

                    ;; Only consider allowed permssions
                    [permission ::site/type "https://meta.juxt.site/pass/permission"]
                    [permission ::pass/action action]
                    (allowed? subject resource permission)

                    ;; Only permissions that match our purpose
                    [permission ::pass/purpose purpose]]

            include-rules
            (conj '(include? subject action resource))

            resources-in-scope
            (conj '[(contains? resources-in-scope resource)]))

          :rules (vec (concat rules include-rules))

          :in '[subject actions purpose resources-in-scope]}

         subject actions purpose (or resources-in-scope #{}))]

    ;; TODO: Too complex, extract this and unit test. The purpose here it to
    ;; apply the pull of each relevant action to each result, and merge the
    ;; results into a single map.

    (for [[resource resource-group] (group-by :resource results)]
      (apply merge
             (for [{:keys [action purpose permission]}
                   ;; TODO: Purpose and permission are useful metadata, how do
                   ;; we retain in the result? with-meta?
                   resource-group]
               (xt/pull db (::pass/pull action '[*]) resource))))))

(defn join-with-pull-allowed-resources
  "Join collection on given join-key with another pull of allowed-resources with
  given actions and options."
  [db coll join-key actions options]
  (let [idx (->>
             (assoc options ::pass/resources-in-scope (set (map join-key coll)))
             (pull-allowed-resources db actions)
             (group-by :xt/id))]
    (map #(update % join-key (comp first idx)) coll)))

(defn do-action*
  "This function is applied within a transaction function. It should be fast, but
  at least doesn't have to worry about the database being stale!"
  [xt-ctx
   {subject ::pass/subject
    action ::pass/action
    access-token ::pass/access-token
    resource ::site/resource
    purpose ::pass/purpose
    base-uri ::site/base-uri
    :as ctx} args]
  (let [db (xt/db xt-ctx)
        tx (xt/indexing-tx xt-ctx)]
    (try
      (assert base-uri "The base-uri must be provided")
      (assert (or (nil? subject) (string? subject)) "Subject to do-action* expected to be a string, or null")
      (assert (or (nil? resource) (string? resource)) "Resource to do-action* expected to be a string, or null")

      ;; Check that we /can/ call the action
      (let [check-permissions-result
            (check-permissions db #{action} ctx)

            {::pass/keys [scope] :as action-doc} (xt/entity db action)
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
                (when-not (contains? (set (::pass/scope access-token)) scope)
                  (throw
                   (ex-info
                    (format "Access token does not have sufficient scope (%s)" scope)
                    {:ring.response/status 403
                     :access-token access-token
                     :action action
                     :scope-granted (::pass/scope access-token)
                     :scope-required scope}))))]

        (when-not (seq check-permissions-result)
          (throw (ex-info "Action denied" ctx)))

        (let [env (assoc
                   ctx
                   ::site/db db
                   ::pass/permissions (map ::pass/permission check-permissions-result))
              [{::site/keys [fx]}]
              (cond
                (::flip/quotation action-doc)
                (eval-quotation
                 (reverse args)         ; push the args to the stack
                 (::flip/quotation action-doc)
                 env)

                ;; There might be other strategies in the future (although the
                ;; fewer the better really)
                :else
                (throw (ex-info "All actions must have some processing steps"
                                {:action action-doc})))

              _ (log/infof "FX are %s" (pr-str fx))

              ;; Validate
              _ (doseq [effect fx]
                  (when-not (and (vector? effect)
                                 (keyword? (first effect))
                                 (if (= :xtdb.api/put (first effect))
                                   (map? (second effect))
                                   true))
                    (throw (ex-info "Invalid effect" {::pass/action action :effect effect}))))

              xtdb-ops (filter (fn [[effect]] (= (namespace effect) "xtdb.api")) fx)
              apply-to-request-context-fx (filter (fn [[effect]]  (= effect :juxt.site.alpha/apply-to-request-context)) fx)

              _ (log/infof "xtdb ops is %s" (pr-str xtdb-ops))

              result-fx
              (conj
               xtdb-ops
               ;; Add an action log entry for this transaction
               [:xtdb.api/put
                (into
                 (cond->
                     {:xt/id (format "%s/_site/action-log/%s" base-uri (::xt/tx-id tx))
                      ::site/type "https://meta.juxt.site/site/action-log-entry"
                      ::pass/subject subject
                      ::pass/action action
                      ::pass/purpose purpose
                      ::pass/puts (vec
                                   (keep
                                    (fn [[tx-op {id :xt/id}]]
                                      (when (= tx-op ::xt/put) id))
                                    xtdb-ops))
                      ::pass/deletes (vec
                                      (keep
                                       (fn [[tx-op {id :xt/id}]]
                                         (when (= tx-op ::xt/delete) id))
                                       xtdb-ops))}
                     tx (into tx)

                     ;; Any quotations that we want to apply to the request context?
                     (seq apply-to-request-context-fx)
                     (assoc :juxt.site.alpha/apply-to-request-context-ops apply-to-request-context-fx)))])]

          ;; This isn't the best debugger :( - need a better one!
          ;;(log/tracef "XXXX Result is: %s" result-ops)

          result-fx))

      (catch Throwable e
        (let [action-log-entry-uri (format "%s/_site/action-log/%d" base-uri (::xt/tx-id tx))]
          (log/errorf e "Error when doing action: %s %s" action action-log-entry-uri)
          [[::xt/put
            {:xt/id action-log-entry-uri
             ::site/type "https://meta.juxt.site/site/action-log-entry"
             ::pass/subject subject
             ::pass/action action
             ::site/resource resource
             ::pass/purpose purpose
             ::site/error {:message (.getMessage e)
                           :ex-data
                           ;; The site db will not be nippyable
                           (dissoc (ex-data e) :env)
                           ;; TODO: Ideally we'd like the environment in the
                           ;; action log for debugging purposes. But the below
                           ;; stills fails with a nippy error, haven't
                           ;; investigated thorougly enough.
                           #_(let [ex-data (ex-data e)]
                               (cond-> ex-data
                                 (:env ex-data) (dissoc :env)#_(update :env dissoc ::site/db ::site/xt-node)))}}]])))))

(defn install-do-action-fn [uri]
  {:xt/id (str uri "/_site/do-action")
   :xt/fn '(fn [xt-ctx ctx & args]
             (juxt.pass.alpha.actions/do-action* xt-ctx ctx args))})

;; Remove anything in the ctx that will upset nippy. However, in the future
;; we'll definitely want to record all inputs to actions, so this is an
;; opportunity to decide which entries form the input 'record' and which are
;; only transitory for the purposes of responnding to the request.

(defn sanitize-ctx [ctx]
  (-> ctx
      (dissoc ::site/xt-node ::site/db)
      ;; We take the ids, because it saves on serialization cost and we only
      ;; need the ids in do-action*
      (update ::pass/subject :xt/id)
      (update ::site/resource :xt/id)))

(defn apply-request-context-operations [ctx ops]
  (let [res
        (reduce
         (fn [ctx [op & args]]
           (case op
             :juxt.site.alpha/apply-to-request-context
             (let [quotation (first args)]
               (first (eval-quotation (list ctx) quotation {})))))
         ctx
         ops)]
    res))

(defn do-action
  [{::site/keys [xt-node db base-uri resource]
    ::pass/keys [subject action]
    :as ctx}]
  (assert (::site/xt-node ctx) "xt-node must be present")
  (assert (::site/db ctx) "db must be present")
  (assert (xt/entity db action) (format "Action '%s' must exist in database" action))

  (when-not (or (nil? subject) (map? subject))
    (throw
     (ex-info
      "Subject to do-action expected to be a map, or null"
      {::site/request-context ctx :subject subject})))

  (when-not (or (nil? resource) (map? resource))
    (throw
     (ex-info
      "Resource to do-action expected to be a map, or null"
      {::site/request-context ctx :resource resource})))

  ;; The :juxt.pass.alpha/subject can be nil, if this action is being performed
  ;; by an anonymous user.
  (let [tx-fn (str base-uri "/_site/do-action")]
    (assert (xt/entity db tx-fn) "do-action must exist in database")
    (let [tx (xt/submit-tx
              xt-node
              [[::xt/fn tx-fn (sanitize-ctx ctx)]])
          {::xt/keys [tx-id]} (xt/await-tx xt-node tx)
          ctx (assoc ctx ::site/db (xt/db xt-node))]

      (when-not (xt/tx-committed? xt-node tx)
        (throw
         (ex-info
          (format "Transaction failed to be committed for action %s" action)
          {::xt/tx-id tx-id
           ::pass/action action
           ::site/request-context ctx})))

      (let [result
            (xt/entity
             (xt/db xt-node)
             (format "%s/_site/action-log/%d" base-uri tx-id))]
        (if-let [error (::site/error result)]
          (do
            (log/errorf "Transaction error: %s" (pr-str error))
            (let [status (:ring.response/status (:ex-data error))]
              (throw
               (ex-info
                (format "Transaction error performing action %s: %s" action (:message error))
                (into
                 {::site/request-context
                  (cond-> ctx
                    status (assoc :ring.response/status status))}
                 (merge
                  (dissoc result ::site/type :xt/id ::site/error)
                  (:ex-data error)))))))

          (let [apply-to-request-context-ops (:juxt.site.alpha/apply-to-request-context-ops result)]
            (cond-> ctx
              result (assoc ::pass/action-result result)

              ;; These ops are quotations that can be applied to the request
              ;; context.  The intention if for these quotations to set the
              ;; response status and add headers.
              (seq apply-to-request-context-ops)
              (apply-request-context-operations (reverse apply-to-request-context-ops)))))))))

;; TODO: Since it is possible that a permission is in the queue which might
;; grant or revoke an action, it is necessary to run this check 'head-of-line'
;; and submit a transaction function. This will avoid any non-determinism caused
;; by a race-condition and retain proper serialization of transactions.
;;
;; For a fuller discussion on determinism and its benefits, see
;; https://www.cs.umd.edu/~abadi/papers/abadi-cacm2018.pdf
(defn wrap-authorize-with-actions [h]
  (fn [{::pass/keys [subject]
        ::site/keys [db resource uri]
        :ring.request/keys [method]
        :as req}]

    (assert (or (nil? subject) (map? subject)))
    (assert (or (nil? resource) (map? resource)))

    (let [actions (get-in resource [::site/methods method ::pass/actions])

          _ (doseq [action actions]
              (when-not (xt/entity db action)
                (throw (ex-info (format "No such action: %s" action) {::site/request-context req
                                                                      :missing-action action}))))

          permitted-actions
          (check-permissions
           db
           actions
           (cond-> {::pass/subject (:xt/id subject)}
             ;; When the resource is in the database, we can add it to the
             ;; permission checking in case there's a specific permission for
             ;; this resource.
             resource (assoc ::site/resource (:xt/id resource))))]

      (if (seq permitted-actions)
        (h (assoc req ::pass/permitted-actions permitted-actions))

        (if subject
          (throw
           (ex-info
            (format "No permission for actions: %s" (pr-str actions))
            {::site/request-context
             (assoc req :ring.response/status 403)}))

          ;; No subject?
          (if-let [protection-spaces (::pass/protection-spaces req)]
            ;; We are in a protection space, so this is HTTP Authentication (401
            ;; + WWW-Authenticate header)
            (throw
             (ex-info
              (format "No anonymous permission for actions (try authenticating!): %s" (pr-str actions))
              {::site/request-context
               (assoc
                req
                :ring.response/status 401
                :ring.response/headers
                {"www-authenticate"
                 (http-authn/www-authenticate-header protection-spaces)})}))

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

            (if-let [session-scopes (::pass/session-scopes req)]
              (let [login-uri (some->> session-scopes (some :juxt.pass.alpha/login-uri))
                    redirect (str login-uri "?return-to=" (codec/url-encode uri))]
                ;; If we are in a session-scope that contains a login-uri, let's redirect to that
                (throw
                 (ex-info
                  (format "No anonymous permission for actions (try logging in!): %s" (pr-str actions))
                  {::site/request-context
                   (-> req
                       (assoc :ring.response/status 302)
                       (assoc-in [:ring.response/headers "location"] redirect))})))
              (throw
               (ex-info
                (format "No anonymous permission for actions: %s" (pr-str actions))
                {::site/request-context
                 (assoc req :ring.response/status 403)})))))))))
