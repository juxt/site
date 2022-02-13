;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn rules [db]
  (->> '{:find [(pull rule [::pass/rule])]
         :where [[rule ::site/type "ResourceAccessRule"]]}
       (xt/q db)
       (map first)
       (mapv ::pass/rule)))

(defn acls
  "Return ACLs"
  [db session resource]
  (let [rules (rules db)
        query {:find ['(pull acl [*])]
               :where '[[acl ::site/type "ACL"]
                        (check acl subject resource)]
               :rules rules
               :in '[session resource]}]
    (if (seq rules)
      (do
        (log/tracef "Query %s" (pr-str query))
        (xt/q db query session resource))
      #{})))

(defn authorize [{::site/keys [db uri]
                  ::pass/keys [session] :as req}]
  (assert session "Shouldn't call this function without a session in context")
  (let [acls (acls db (:xt/id session) uri)]
    (log/tracef "acls are %s" acls)
    (cond-> req
      (seq acls) (assoc-in [::site/resource ::pass/authorization ::pass/acls] acls))))
