;; Copyright © 2022, JUXT LTD.

(ns juxt.apex.alpha.authorization
  (:require
   [clojure.tools.logging :as log]
   [clojure.set :as set]
   [xtdb.api :as xt]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn session-scope-rules [db]
  (->> '{:find [(pull rule [::apex/rule])]
         :where [[rule ::site/type "SessionScopeRule"]]}
       (xt/q db)
       (map first)
       (mapv ::apex/rule)))

(defn session-scope
  "Return the scope (as a set) that is accessible to the given session (id)."
  [db session]
  (set
   (map first
        (xt/q
         db
         {:find ['scope]
          :where
          '[
            (check-scope grant subject)
            [grant :juxt.site.alpha/type "ScopeGrant"]
            [grant :juxt.apex.alpha/scope scope]]
          :rules (session-scope-rules db)
          :in '[session]}
         session))))

(defn request-scope
  "Return the scope of the request. If there's a session with claims in the
  request, then query rules for scopes. If there's a Bearer token, the scope is
  the intersection of the set of scopes mentioned in the access token and the
  set of scopes the user has access to."
  [{::site/keys [db] ::pass/keys [session]}]
  (let [bearer-token
        ;; TODO: Get bearer token from Authorization header
        nil]
    (cond
      bearer-token (throw (ex-info "TODO:bearer token" {}))
      session (session-scope db (:xt/id session)))))

(defn authorize
  "Add an authorization to a resource, given the request."
  [resource req]
  (let [required-scope (:juxt.apex.alpha/required-scope resource)]
    (cond-> resource
      (and
       ;; Deny by default, see
       ;; https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html#deny-by-default
       required-scope
       (set/superset?
        (request-scope req)
        required-scope))
      (assoc ::pass/authorization {}))))
