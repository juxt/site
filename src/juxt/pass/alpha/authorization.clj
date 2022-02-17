;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn rules [db resource]
  (assert (string? resource))
  (->>
   (xt/q
    db
    '{:find [rule-content]
      :where [[resource ::pass/ruleset ruleset]
              [ruleset ::pass/rules rule]
              [rule ::pass/rule-content rule-content]]
      :in [resource]}
    resource)
   (map (comp read-string first))
   (mapcat seq)
   vec))

(defn acls
  "Return ACLs. The session argument can be nil, the resource argument must not
  be."
  [db subject action resource]
  ;; Subject can be nil, resource cannot be
  (assert (or (nil? subject) (string? subject)))
  (assert (string? action))
  (assert (string? resource))
  (let [rules (rules db resource)
        query {:find ['(pull acl [*])]
               :where '[[acl ::site/type "ACL"]
                        (check acl subject action resource)]
               :rules rules
               :in '[subject action resource]}]
    (if (seq rules)
      (map first (xt/q db query subject action resource))
      #{})))

(defn list-resources
  [db subject action ruleset]
  (assert (string? subject))
  (assert (string? action))
  (let [rules
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
         vec)

        query {:find ['(pull acl [*])]
               :where '[[acl ::site/type "ACL"]
                        (list-resources acl subject action)]
               :rules rules
               :in '[subject action]}]

    (if (seq rules)
      (map first (xt/q db query subject action))
      #{})))

(defn authorize-resource [{::site/keys [db uri]
                           :ring.request/keys [method]
                           ::pass/keys [session] :as req}]
  (let [acls (acls
              db
              (:xt/id session)    ; for now we treat the session as representing
                                        ; the subject

              ;; TODO: The subject needs to be in the database, accessible to Datalog, and for
              ;; historic/audit. Therefore, the subject needs to be established when a session
              ;; is created (access with a session id cookie), or when a Bearer token is
              ;; minted. Whatever the means of access, the same subject needs to be determined
              ;; (whether accessed via session id, bearer token or other means).

              (case method :get "read")
              uri)]
    (log/tracef "acls are %s" acls)
    (cond-> req
      (seq acls) (assoc-in [::site/resource ::pass/authorization ::pass/acls] acls)
      ;;true (assoc-in [::site/resource ::pass/authorization] {:debug true})
      )))
