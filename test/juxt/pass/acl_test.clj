;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.acl-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]
   [jsonista.core :as json]
   [juxt.jinx.alpha.api :refer [schema validate]]
   [clojure.java.io :as io]
   [juxt.jinx.alpha :as jinx]
   [xtdb.api :as xt]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-xt with-handler)

(defn fail [ex-data] (throw (ex-info "FAIL" ex-data)))

(deftest scenario-1-test
  (submit-and-await!
   [
    ;; Establish a resource
    [::xt/put
     {:xt/id "https://example.org/index"
      ::http/methods #{:get}
      ::http/content-type "text/html;charset=utf-8"
      ::http/content "Hello World!"
      ;; We'll define this lower down
      ::pass/ruleset "https://example.org/ruleset"}]

    ;; Establish a session. This is effectively the subject, Alice
    [::xt/put
     {:xt/id "urn:site:session:alice"
      :juxt.pass.jwt/sub "alice"
      ::pass/scope "read:index"}]

    [::xt/put
     {:xt/id "urn:site:session:alice-without-scope"
      :juxt.pass.jwt/sub "alice"}]

    ;; Bob's access will be via his 'manager' role

    [::xt/put
     {:xt/id "urn:site:session:bob"
      :juxt.pass.jwt/sub "bob"
      ::pass/scope "read:index"}]

    [::xt/put
     {:xt/id "https://example.org/roles/manager"
      ::site/type "Role"}]

    [::xt/put
     {:xt/id "https://example.org/roles/bob-is-manager"
      ::site/type "ACL"
      :juxt.pass.jwt/sub "bob"
      ::pass/role "https://example.org/roles/manager"}]

    ;; Carl isn't a manager

    [::xt/put
     {:xt/id "urn:site:session:carl"
      :juxt.pass.jwt/sub "carl"
      ::pass/scope "read:index"}]

    ;; A note on cacheing - each token can cache the resources it has access
    ;; to, keyed by action and transaction time. If a resource is updated, the
    ;; cache will fail. If an ACL is revoked, such that read access would no
    ;; longer be possible, the cache can still be used (avoiding the need to
    ;; detect changes to ACLs). See 'new enemy'
    ;; problem. https://duckduckgo.com/?t=ffab&q=authorization+%22new+enemy%22&ia=web

    [::xt/put
     {:xt/id "https://example.org/grants/alice-can-access-index"
      ::site/description "Alice is granted access to /index"
      ::site/type "ACL"

      :juxt.pass.jwt/sub "alice"

      ;; A resource can be any XT document, a superset of web resources. Common
      ;; authorization terminology uses the term 'resource' for anything that
      ;; can be protected.
      ::pass/resource "https://example.org/index"
      ::pass/action "read"
      ::pass/scope "read:index"}]

    ;; TODO: Resource 'sets'

    [::xt/put
     {:xt/id "https://example.org/grants/managers-can-access-index"
      ::site/description "Managers are granted access to /index"
      ::site/type "ACL"

      ::pass/role "https://example.org/roles/manager"

      ;; A resource can be any XT document, a superset of web resources. Common
      ;; authorization terminology uses the term 'resource' for anything that
      ;; can be protected.
      ::pass/resource "https://example.org/index"
      ::pass/action "read"
      ::pass/scope "read:index"}]

    [::xt/put
     {:xt/id "https://example.org/rules/1"
      ::site/description "Allow read access of resources to granted subjects"
      ::pass/rule-content
      (pr-str '[[(check acl subject action resource)
                 [acl ::pass/resource resource]
                 (granted acl subject)
                 [acl ::pass/action action]

                 ;; A subject may be constrained to a scope. In this case, only
                 ;; matching ACLs are literally 'in scope'.
                 [acl ::pass/scope scope]
                 [subject ::pass/scope scope]]

                [(granted acl subject)
                 [acl :juxt.pass.jwt/sub sub]
                 [subject :juxt.pass.jwt/sub sub]]

                [(granted acl subject)
                 [acl ::pass/role role]
                 [subject :juxt.pass.jwt/sub sub]

                 [role ::site/type "Role"]
                 [role-membership ::site/type "ACL"]
                 [role-membership :juxt.pass.jwt/sub sub]
                 [role-membership ::pass/role role]
                 ]])}]


    ;; We can now define the ruleset
    [::xt/put
     {:xt/id "https://example.org/ruleset"
      ::pass/rules ["https://example.org/rules/1"]}]])

  ;; Is subject allowed to do action to resource?
  ;; ACLs involved will include any limitations on actions

  ;; Which resources is subject allowed to do action on?
  ;; e.g. list of documents
  ;; This might be a solution to the n+1 problem in our graphql

  (let [db (xt/db *xt-node*)]

    ;; Check rules
    (when (not= (count (authz/rules db "https://example.org/index")) 3)
      (fail {:rules (authz/rules db "https://example.org/index")}))

    (let [check
          (fn [subject action resource expected-count]
            (let [acls (authz/acls db subject action resource)]
              (is (= expected-count (count acls)))
              (when-not (= expected-count (count acls))
                (fail {:subject subject
                       :action action
                       :resource resource
                       :expected-count expected-count
                       :actual-count (count acls)}))))]

      (check "urn:site:session:alice" "read" "https://example.org/index" 1)
      (check "urn:site:session:alice-without-scope" "read" "https://example.org/index" 0)

      ;; Fuzz each of the parameters to check that the ACL fails
      (check nil "read" "https://example.org/index" 0)
      (check "urn:site:session:alice" "read" "https://example.org/other-page" 0)
      (check "urn:site:session:alice" "write" "https://example.org/index" 0)

      ;; Bob can read index
      (check "urn:site:session:bob" "read" "https://example.org/index" 1)

      ;; But Carl cannot
      (check "urn:site:session:carl" "read" "https://example.org/index" 0)

      ;; Which resources can Alice access?

      {:status :ok :message "All tests passed"})))

((t/join-fixtures [with-xt with-handler])
 (fn []
   (submit-and-await!
    [
     ;; Establish a resource.
     [::xt/put
      {:xt/id "https://example.org/index"
       ::http/methods #{:get}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Hello World!"
       ;; We'll define this lower down
       ::pass/ruleset "https://example.org/ruleset"}]

     [::xt/put
      {:xt/id "https://example.org/~alice/index"
       ::http/methods #{:get}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Alice's page"
       ::pass/ruleset "https://example.org/ruleset"}]

     ;; Establish a session. This is effectively the subject, Alice.
     [::xt/put
      {:xt/id "urn:site:session:alice"
       :juxt.pass.jwt/sub "alice"
       ::pass/scope "read:index"}]

     ;; An access-token
     [::xt/put
      {:xt/id "urn:site:access-token:alice-without-read-index-scope"
       :juxt.pass.jwt/sub "alice"}]

     ;; This is Bob.
     [::xt/put
      {:xt/id "urn:site:session:bob"
       :juxt.pass.jwt/sub "bob"
       ::pass/scope "read:index"}]

     [::xt/put
      {:xt/id "https://example.org/roles/manager"
       ::site/type "Role"}]

     ;; Bob's access will be via his 'manager' role.
     [::xt/put
      {:xt/id "https://example.org/roles/bob-is-manager"
       ::site/type "ACL"
       :juxt.pass.jwt/sub "bob"
       ::pass/role "https://example.org/roles/manager"}]

     ;; Carl isn't a manager.

     [::xt/put
      {:xt/id "urn:site:session:carl"
       :juxt.pass.jwt/sub "carl"
       ::pass/scope "read:index"}]

     ;; A note on cacheing - each token can cache the resources it has access
     ;; to, keyed by action and transaction time. If a resource is updated, the
     ;; cache will fail. If an ACL is revoked, such that read access would no
     ;; longer be possible, the cache can still be used (avoiding the need to
     ;; detect changes to ACLs). See 'new enemy'
     ;; problem. https://duckduckgo.com/?t=ffab&q=authorization+%22new+enemy%22&ia=web

     [::xt/put
      {:xt/id "https://example.org/grants/alice-can-access-index"
       ::site/description "Alice is granted access to some resources"
       ::site/type "ACL"

       :juxt.pass.jwt/sub "alice"

       ;; A resource can be any XT document, a superset of web resources. Common
       ;; authorization terminology uses the term 'resource' for anything that
       ;; can be protected.
       ::pass/resource #{"https://example.org/index" "https://example.org/~alice/index"}
       ::pass/action "read"
       ::pass/scope "read:index"}]

     ;; TODO: Resource 'sets'

     [::xt/put
      {:xt/id "https://example.org/grants/managers-can-access-index"
       ::site/description "Managers are granted access to /index"
       ::site/type "ACL"

       ::pass/role "https://example.org/roles/manager"

       ;; A resource can be any XT document, a superset of web resources. Common
       ;; authorization terminology uses the term 'resource' for anything that
       ;; can be protected.
       ::pass/resource "https://example.org/index"
       ::pass/action "read"
       ::pass/scope "read:index"}]

     [::xt/put
      {:xt/id "https://example.org/rules/1"
       ::site/description "Allow read access of resources to granted subjects"
       ::pass/rule-content
       (pr-str '[[(check acl subject action resource)
                  [acl ::pass/resource resource]
                  (granted acl subject)
                  [acl ::pass/action action]

                  ;; A subject may be constrained to a scope. In this case, only
                  ;; matching ACLs are literally 'in scope'.
                  [acl ::pass/scope scope]
                  [subject ::pass/scope scope]]

                 [(granted acl subject)
                  [acl :juxt.pass.jwt/sub sub]
                  [subject :juxt.pass.jwt/sub sub]]

                 [(granted acl subject)
                  [acl ::pass/role role]
                  [subject :juxt.pass.jwt/sub sub]

                  [role ::site/type "Role"]
                  [role-membership ::site/type "ACL"]
                  [role-membership :juxt.pass.jwt/sub sub]
                  [role-membership ::pass/role role]]

                 [(list-resources acl subject action)
                  [acl ::pass/resource resource]
                  ;; Any acl, in scope, that references a resource (or set of
                  ;; resources)
                  [acl ::pass/scope scope]
                  [subject ::pass/scope scope]
                  [acl ::pass/action action]
                  (granted acl subject)]])}]

     ;; We can now define the ruleset
     [::xt/put
      {:xt/id "https://example.org/ruleset"
       ::pass/rules ["https://example.org/rules/1"]}]])

   ;; Is subject allowed to do action to resource?
   ;; ACLs involved will include any limitations on actions

   ;; Which resources is subject allowed to do action on?
   ;; e.g. list of documents
   ;; This might be a solution to the n+1 problem in our graphql

   (let [db (xt/db *xt-node*) check
         (fn [subject action resource expected-count]
           (let [acls (authz/acls db subject action resource)]
             (when-not (= expected-count (count acls))
               (fail {:subject subject
                      :action action
                      :resource resource
                      :expected-count expected-count
                      :actual-count (count acls)}))))

         list-resources
         (fn [subject action ruleset expected-resources]
           (let [acls (authz/list-resources db subject action ruleset)
                 actual-resources (set (mapcat ::pass/resource acls))]
             (when-not (= expected-resources actual-resources)
               (fail {:subject subject
                      :action action
                      :expected-resources expected-resources
                      :actual-resources actual-resources}))))]

     (check "urn:site:session:alice" "read" "https://example.org/index" 1)
     (check "urn:site:access-token:alice-without-read-index-scope" "read" "https://example.org/index" 0)

     ;; Fuzz each of the parameters to check that the ACL fails
     (check nil "read" "https://example.org/index" 0)
     (check "urn:site:session:alice" "read" "https://example.org/other-page" 0)
     (check "urn:site:session:alice" "write" "https://example.org/index" 0)

     ;; Bob can read index
     (check "urn:site:session:bob" "read" "https://example.org/index" 1)

     ;; But Carl cannot
     (check "urn:site:session:carl" "read" "https://example.org/index" 0)

     {:status :ok :message "All tests passed"}

     ;; Which resources can Alice access?
     (list-resources
      "urn:site:session:alice" "read" "https://example.org/ruleset"
      #{"https://example.org/~alice/index" "https://example.org/index"}
      )


     )))

;; Scenario 2 - INTERNAL classification


;; Scenario 3 - User content

;; Scenario 4 - Consent-based access control
