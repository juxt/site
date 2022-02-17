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

(defn fail [m]
  (throw (ex-info "FAIL" m)))

((t/join-fixtures [with-xt with-handler])
 (fn []
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

     ;; Establish a session. This is effectively the subject, Bob
     [::xt/put
      {:xt/id "urn:site:session:bob"
       :juxt.pass.jwt/sub "bob"
       ::pass/scope "read:index"}]

     [::xt/put
      {:xt/id "urn:site:session:bob-without-scope"
       :juxt.pass.jwt/sub "bob"}]

     [::xt/put
      {:xt/id "https://example.org/grants/bob-can-access-index"
       ::site/description "Bob is granted access to /index"
       ::site/type "ACL"
       :juxt.pass.jwt/sub "bob"
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
                  [subject :juxt.pass.jwt/sub sub]]])}]


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
     (when (not= (count (authz/rules db "https://example.org/index")) 2)
       (fail {:rules (authz/rules db "https://example.org/index")}))

     (let [check
           (fn [subject action resource expected-count]
             (let [acls (authz/acls db subject action resource)]
               (when-not (= expected-count (count acls))
                 (fail {:subject subject
                        :action action
                        :resource resource
                        :expected-count expected-count
                        :actual-count (count acls)}))))]

       (check "urn:site:session:bob" "read" "https://example.org/index" 1)
       (check "urn:site:session:bob-without-scope" "read" "https://example.org/index" 0)

       ;; Fuzz each of the parameters to check that the ACL fails
       (check nil "read" "https://example.org/index" 0)
       (check "urn:site:session:bob" "read" "https://example.org/other-page" 0)
       (check "urn:site:session:bob" "write" "https://example.org/index" 0)

       )

     )))
