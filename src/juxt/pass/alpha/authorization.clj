;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.authorization
  (:require
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn resource-access-rules [db]
  (->> '{:find [(pull rule [::pass/rule])]
         :where [[rule ::site/type "ResourceAccessRule"]]}
       (xt/q db)
       (map first)
       (mapv ::pass/rule)))

(defn resource-access-acls
  "Return ACLs. The session argument can be nil, the resource argument must not
  be."
  [db session resource]
  ;; Session can be nil, resource cannot be
  (assert resource)
  (let [rules (resource-access-rules db)
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

(defn authorize-resource [{::site/keys [db uri]
                           ::pass/keys [session] :as req}]
  (let [acls (resource-access-acls db (:xt/id session) uri)]
    (log/tracef "acls are %s" acls)
    (cond-> req
      (seq acls) (assoc-in [::site/resource ::pass/authorization ::pass/acls] acls)
      true (assoc-in [::site/resource ::pass/authorization] {:debug true}))))
