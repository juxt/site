;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [malli.core :as m]
   [malli.error :a me]
   [ring.util.codec :as codec]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [juxt.pass.alpha.http-authentication :as http-authn]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha.process :as process]
   [juxt.flip.alpha.core :refer [eval-quotation]]
   [juxt.flip.alpha :as-alias flip]
   [xtdb.api :as xt]
   [clojure.walk :as walk]))

(defn actions->rules
  "Determine rules for the given action ids. Each rule is bound to the given
  action."
  [db actions]
  (vec (for [action actions
             :let [e (xt/entity db action)]
             rule (::pass/rules e)]
         (conj rule ['action :xt/id action]))))

;; This is broken out into its own function to assist debugging when
;; authorization is denied and we don't know why. A better authorization
;; debugger is definitely required.
(defn query-permissions [{:keys [db rules subject actions resource purpose] :as args}]
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

  (log/debugf "check-permissions: subject %s, resource: %s, purpose: %s, actions: %s, subject: %s" subject resource purpose actions
              (xt/pull db '[*] subject))

  (log/debugf "Actions: %s" actions)
  (log/debugf "Rules: %s" (actions->rules db actions))

  (let [rules (actions->rules db actions)]
    (when (seq rules)
      (let [permissions
            (query-permissions
             {:db db :rules rules
              :subject subject :actions actions :resource resource :purpose purpose})]
        (log/debugf "Returning %s permissions" (pr-str permissions))
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
            (conj '[(contains? resources-in-scope resource)]
                  ))

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
    resource ::site/resource
    purpose ::pass/purpose
    :as ctx} args]
  (let [db (xt/db xt-ctx)
        tx (xt/indexing-tx xt-ctx)]

    (try
      ;; Check that we /can/ call the action
      (let [check-permissions-result
            (check-permissions db #{action} ctx)

            action-doc (xt/entity db action)
            _ (when-not action-doc
                (throw
                 (ex-info
                  (format "Action '%s' not found in db" action)
                  {:action action})))]

        (when-not (seq check-permissions-result)
          (throw (ex-info "Action denied" ctx)))

        (let [ops
              (cond
                (::flip/quotation action-doc)
                (eval-quotation
                 (reverse args)         ; push the args to the stack
                 (::flip/quotation action-doc)
                 (assoc ctx ::site/db db))
                ;; There might be other strategies in the future (although the
                ;; fewer the better really)
                :else
                (throw (ex-info "All actions must have some processing steps"
                                {:action action-doc})))

              ;; Validate
              _ (doseq [op ops]
                  (when-not (and (vector? op) (keyword? (first op)))
                    (throw (ex-info "Invalid op" {::pass/action action :op op}))))

              xtdb-ops (filter (fn [[op]] (= (namespace op) "xtdb.api")) ops)
              apply-to-request-context-ops (filter (fn [[op]]  (= op :juxt.site.alpha/apply-to-request-context)) ops)

              result-ops
              (conj
               xtdb-ops
               ;; Add an action log entry for this transaction
               [:xtdb.api/put
                (into
                 (cond->
                     {:xt/id (format "urn:site:action-log:%s" (::xt/tx-id tx))
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
                     (seq apply-to-request-context-ops)
                     (assoc :juxt.site.alpha/apply-to-request-context-ops apply-to-request-context-ops)

                     ))])]

          ;; This isn't the best debugger :( - need a better one!
          ;;(log/tracef "XXXX Result is: %s" result-ops)

          result-ops))

      (catch Throwable e
        (log/errorf e "Error when doing action: %s %s" action (format "urn:site:action-log:%s" (::xt/tx-id tx)))
        [[::xt/put
          {:xt/id (format "urn:site:action-log:%s" (::xt/tx-id tx))
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
                               (:env ex-data) (dissoc :env)#_(update :env dissoc ::site/db ::site/xt-node)))}}]]))))

(defn install-do-action-fn []
  {:xt/id "urn:site:tx-fns:do-action"
   :xt/fn '(fn [xt-ctx ctx & args]
             (juxt.pass.alpha.authorization/do-action* xt-ctx ctx args))})

;; Remove anything in the ctx that will upset nippy. However, in the future
;; we'll definitely want to record all inputs to actions, so this is an
;; opportunity to decide which entries form the input 'record' and which are
;; only transitory for the purposes of responnding to the request.

(defn sanitize-ctx [ctx]
  (dissoc ctx ::site/xt-node ::site/db))

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

(defn do-action [{::site/keys [xt-node] ::pass/keys [action] :as ctx}]
  (assert (:juxt.site.alpha/xt-node ctx) "xt-node must be present")
  (assert (xt/entity (xt/db xt-node) "urn:site:tx-fns:do-action") "do-action must exist in database")
  (assert (xt/entity (xt/db xt-node) action) "action must exist in database")

  (let [tx (xt/submit-tx
            xt-node
            [[::xt/fn "urn:site:tx-fns:do-action" (sanitize-ctx ctx)]])
        {::xt/keys [tx-id]} (xt/await-tx xt-node tx)]

    (when-not (xt/tx-committed? xt-node tx)
      (throw
       (ex-info
        (format "Transaction failed to be committed for action %s" action)
        {::xt/tx-id tx-id
         ::pass/action action})))

    (let [result
          (xt/entity
           (xt/db xt-node)
           (format "urn:site:action-log:%s" tx-id))]
      (if-let [error (::site/error result)]
        (throw
         (ex-info
          (format "Transaction error performing action %s: %s" action (:message error))
          (merge
           (dissoc result ::site/type :xt/id ::site/error)
           (:ex-data error))))

        (let [
              apply-to-request-context-ops (:juxt.site.alpha/apply-to-request-context-ops result)]

          (cond-> ctx
            result (assoc ::pass/action-result result)

            ;; These ops are quotations that can be applied to the request
            ;; context.  The intention if for these quotations to set the
            ;; response status and add headers.
            (seq apply-to-request-context-ops)
            (apply-request-context-operations (reverse apply-to-request-context-ops))

            ))))))

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

    (let [actions (get-in resource [::site/methods method ::pass/actions])
          permitted-actions
          (check-permissions
           db
           actions
           (cond-> {::pass/subject (:xt/id subject)}
             ;; When the resource is in the database, we can add it to the
             ;; permission checking in case there's a specific permission for
             ;; this resource.
             (:xt/id resource) (assoc ::site/resource (:xt/id resource))))]

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
