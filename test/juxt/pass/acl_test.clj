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
      {:xt/id "urn:site:session:123"
       :juxt.pass.jwt/sub "bob"}]

     ;; Grants

     [::xt/put
      {:xt/id "https://example.org/grants/bob-can-access-index"
       ::site/description "Bob is granted access to /index"
       ::site/type "ACL"
       :juxt.pass.jwt/sub "bob"
       ::pass/resource "https://example.org/index"
       }]

     ;; Rules

     [::xt/put
      {:xt/id "https://example.org/rules/1"
       ::site/description "Allow access to those directly granted"
       ::pass/rule-content
       (pr-str '[(check acl subject resource)
                 [acl ::pass/resource resource]
                 [acl :juxt.pass.jwt/sub sub]
                 [subject :juxt.pass.jwt/sub sub]])}]

     ;; We can now
     [::xt/put
      {:xt/id "https://example.org/ruleset"
       ::pass/rules ["https://example.org/rules/1"]}]

     ])

   ;; Is subject allowed to do action to resource?
   ;; ACLs involved will include any limitations on actions

   ;; Which resources is subject allowed to do action on?
   ;; e.g. list of documents
   ;; This might be a solution to the n+1 problem in our graphql

   (let [db (xt/db *xt-node*)

         {:ring.response/keys [status] :as response}
         (*handler*
          {:ring.request/method :get
           :ring.request/path "/index"})]

     ;; Check rules
     (when (not= (count (authz/rules db "https://example.org/index")) 1)
       (throw (ex-info "FAIL" {})))

     (authz/acls db "urn:site:session:123" "https://example.org/index")

     ;;(when (not= 200 status) (throw (ex-info "FAIL" {:response response})))
     ;;(is (= 200 status))
     )))
