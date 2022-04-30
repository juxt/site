;; Copyright © 2022, JUXT LTD.

;; Deprecated, kept because of make-access-token-doc and others

(ns juxt.pass.alpha.deprecated.authorization-2
  (:require
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [juxt.site.alpha.util :refer [sha random-bytes as-hex-str as-b64-str uuid-bytes]]
   [malli.core :as m]
   [xtdb.api :as xt]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'pass.malli (create-ns 'juxt.pass.alpha.malli))
(alias 'site (create-ns 'juxt.site.alpha))

(defn lookup->subject [id-token db]
  (let [iss (get-in id-token [:claims "iss"])
        sub (get-in id-token [:claims "sub"])]
    (ffirst
     (xt/q db '{:find [(pull subject [*])]
                :where [[identity ::pass/subject subject]
                        [identity :juxt.pass.jwt/iss iss]
                        [identity :juxt.pass.jwt/sub sub]]
                :in [iss sub]}
           iss sub))))

(defn make-oauth-client-doc
  "Return an XT doc representing an OAuth2 client (with a random client-id if not
  given) and random client-secret. This must be added to the database."
  ([{::site/keys [base-uri]} client-id]
   (let [client-secret (as-b64-str (random-bytes 24))]
     {:xt/id (str base-uri "/_site/apps/" client-id)
      ::pass/client-id client-id
      ::pass/client-secret client-secret}))
  ([ctx]
   (make-oauth-client-doc ctx (subs (as-hex-str (sha (uuid-bytes))) 0 10))))

(comment
  (make-oauth-client-doc {::site/base-uri "https://example.org"}))

(defn token-id->xt-id [token-id]
  (format "urn:site:access-token:%s" token-id))

(defn make-access-token-doc
  "Returns an XT doc representing an access token. Can be augmented
  with :juxt.pass.alpha/scope and other entries."
  ([subject-id client-id]
   (let [token-id (as-hex-str (random-bytes 20))]
     {:xt/id (token-id->xt-id token-id)
      ::pass/subject subject-id
      ;; TODO: We may harmonize these keywords with openid_connect if we decide
      ;; OAuth2 is the standard default.
      ::pass/client client-id}))
  ([subject-id client-id scope]
   (-> (make-access-token-doc subject-id client-id)
       (assoc ::pass/scope scope))))

(defn rules
  "Construct rules from a ruleset id"
  [db ruleset]
  (assert (string? ruleset))
  (->>
   (xt/q
    db
    '{:find [rule-content]
      :where [[ruleset ::pass/rules rule]
              [rule ::pass/rule-content rule-content]]
      :in [ruleset]}
    ruleset)
   (map (comp read-string first))
   (mapcat seq)
   vec))

(defn access-token-effective-scope
  "Return a set representing the scope of the access-token. The scope of an
  access-token defaults to the scope of application client it applies
  to. However, access-tokens may be issued with more restrictive scope."
  [access-token client]
  (assert (map? access-token))
  (assert (map? client))
  (if-let [access-token-scope (::pass/scope access-token)]
    (set/intersection access-token-scope (::pass/scope client))
    (::pass/scope client)))

#_(defn check-scope [access-token-effective-scope action]
  (assert (set? access-token-effective-scope))
  (assert (string? action))
  ;; First, an easy check to see if the action is allowed with respect to the
  ;; scope on the application client and, if applicable, any scope on the
  ;; access-token itself.
  (when-not (contains? access-token-effective-scope action)
    (throw
     (ex-info
      (format "Scope of access-token does not allow %s" action)
      {:action action
       :access-token-effective-scope access-token-effective-scope}))))

(defn check-acls
  [db {::site/keys [uri]
       ::pass/keys [subject ruleset access-token-effective-scope]}
   effect]

  (assert db)
  (assert subject)
  (assert (string? subject))
  (assert (string? ruleset))
  (assert (string? effect))

  (let [rules (rules db ruleset)]

    (when (seq rules)
      (let [query
            {:find ['(pull acl [*])]
             :where '[
                      ;; Site enforced
                      [acl ::site/type "ACL"]
                      [effect ::site/type "Effect"]
                      [acl ::pass/effect effect]

                      ;; Scope
                      [effect ::pass/scope scope]
                      [(contains? access-token-effective-scope scope)]

                      ;; These rules must be satisfied provided by the domain
                      (acl-applies-to-subject? acl subject)
                      ;; Some effects are limited by resource
                      (effect-applies-to-resource? effect resource)
                      ;; Some ACLs are limited by resource
                      (acl-applies-to-resource? acl resource)]

             :rules rules
             :in '[subject effect resource access-token-effective-scope]}]
        (seq (map first (xt/q db query
                              subject effect uri access-token-effective-scope)))))))

(defmulti apply-processor (fn [processor m arg-def] (first processor)))

(defmethod apply-processor :default [[kw] m _]
  (log/warnf "No processor for %s" kw)
  m)

(defmethod apply-processor ::pass/merge [[_ m-to-merge] val _]
  (merge val m-to-merge))

(defmethod apply-processor ::pass.malli/validate [[_ form] val {::pass.malli/keys [schema]}]
  (assert schema)
  (when-not (m/validate schema val)
    (throw
     (ex-info
      "Failed validation check"
      (m/explain schema val))))
  val)

(defn process-arg [arg arg-def]
  (reduce
   (fn [acc processor]
     (apply-processor processor acc arg-def))
   arg
   (::pass/process arg-def)))

(defn authorizing-put-fn [db {::pass/keys [ruleset] :as auth} effect-id & args]
  (assert ruleset)

  (try
    (let [acls (check-acls db auth effect-id)
          effect (xt/entity db effect-id)

          _ (when-not effect
              (throw
               (ex-info
                (format "No such effect: %s" effect-id)
                {:effect effect-id})))

          effect-args (::pass/effect-args effect)]

      (when (not= (count args) (count effect-args))
        (throw
         (ex-info
          (format "Arity error on effect: %s" effect-id)
          {:effect effect-id
           :args-given (count args)
           :args-expected (count effect-args)})))

      (when (nil? acls)
        (let [msg (format "Effect '%s' denied as no ACLs found that approve it." effect-id)]
          ;; Depending on the effect, we may want to log and alert
          (when false (log/warnf msg))
          ;; TODO: Run some diagnostics to determine the reason
          (throw (ex-info msg {}))))

      ;; This may be a process step
      #_(when-not (.startsWith (:xt/id doc) (::site/uri auth))
          (let [msg ":xt/id of new document must be a sub-resource of ::site/uri"]
            (log/warnf msg)
            (throw (ex-info msg {:new-doc-id (:xt/id doc)
                                 ::site/uri auth}))))

      (if acls
        (mapv (fn [arg arg-def]
                [::xt/put
                 (cond-> arg
                   ;; Critically, the new doc inherits the ruleset of the auth
                   ;; context. This prevents documents from escaping their authorization
                   ;; scheme into another.
                   true (assoc ::pass/ruleset ruleset)
                   (::pass/process arg-def) (process-arg arg-def)
                   )])
              args
              effect-args)
        []))

    (catch Throwable e
      (log/error e "Failed authorization check")
      (throw e))))
