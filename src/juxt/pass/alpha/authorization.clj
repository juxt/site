;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn rules [db]
  (mapv :juxt.pass.alpha/rule
        (map first
             (xt/q db
                   '{:find [(pull rule [:juxt.pass.alpha/rule])]
                     :where [[rule :juxt.site.alpha/type "ResourceAccessRule"]]}))))

(defn acls
  "Return ACLs"
  [db session resource]
  (seq
   (xt/q db {:find ['(pull acl [*])]
             :where '[[acl ::site/type "ACL"]
                      (check acl subject resource)]

             :rules (rules db)

             :in '[session resource]}

         session resource)
   #_(xt/q
      db
      '{:find [(pull acl [*])]
        :where [[session :juxt.pass.openid/sub sub]
                [session :juxt.pass.openid/iss iss]
                ;; TODO: Can't reference home here
                [ident :juxt.home/issuer iss]
                [ident :juxt.home/subject-identifier sub]
                [ident :juxt.home/person-id subject]
                (check acl subject resource)
                ]

        :in [session resource]

        ;; Here are the rules, attached to /index.html, that say that for an
        ;; INTERNAL resource, those that have been granted access to internal, can
        ;; {GET,HEAD,OPTIONS} it.
        :rules [
                ;; Anyone who has the internal role can see resources classified as INTERNAL
                [(check acl subject resource)
                 [acl ::site/type "ACL"]
                 [acl :juxt.home/role "https://home.test/_home/roles/internal"]
                 [acl :juxt.home/person-id subject]
                 [resource :juxt.pass.alpha/classification "INTERNAL"]]

                ;; Role access (with ACL granting the role to the subject)
                [(check acl subject resource)
                 [acl ::site/type "ACL"]
                 [acl :juxt.home/person-id subject]
                 [acl :juxt.home/role role]
                 [role-access :juxt.home/type "RoleAccess"]
                 [role-access :juxt.home/role role]
                 [role-access :juxt.site/uri resource]]]}

      (:xt/id session)

      uri))
  )

(defn authorize [{::site/keys [db uri]
                  ::pass/keys [session] :as req}]

  (assert session "Shouldn't call this function without a session in context")

  (let [acls (acls db (:xt/id session) uri)]

    (log/tracef "acls are %s" acls)

    (cond-> req
      acls (assoc-in [::site/resource ::pass/authorization ::pass/acls] acls))))
