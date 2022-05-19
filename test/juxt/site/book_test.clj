;; Copyright © 2022, JUXT LTD.

(ns juxt.site.book-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db* *handler*] :as tutil]
   [xtdb.api :as xt]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [clojure.string :as str]
   [xtdb.api :as x]))

(defn install-not-found []
  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-not-found"
    :juxt.pass.alpha/scope "read:resource"
    :juxt.pass.alpha/rules
    [
     ['(allowed? permission subject action resource)
      ['permission :xt/id]]]})

  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/get-not-found"
    :juxt.pass.alpha/action "https://site.test/actions/get-not-found"
    :juxt.pass.alpha/purpose nil})

  (repl/put!
   {:xt/id "urn:site:resources:not-found"
    ::http/methods
    {:get {::pass/actions #{"https://site.test/actions/get-not-found"}}}}))

(defn preliminaries! []
  (book/book-put-user!)
  (book/book-put-user-identity!)
  (book/book-put-subject!)
  (book/book-install-create-action!)
  (book/book-install-do-action-fn!)
  (book/book-permit-create-action!)
  (book/book-create-grant-permission-action!)
  (book/book-permit-grant-permission-action!)
  (book/book-create-action-put-user!)
  (book/book-create-action-put-user-identity!)
  (book/book-create-action-put-subject!)
  (book/book-grant-permission-to-invoke-action-put-subject!)
  (book/book-create-action-put-application!)
  (book/book-grant-permission-to-invoke-action-put-application!!)
  (book/book-create-action-authorize-application!)
  (book/book-grant-permission-to-invoke-action-authorize-application!)
  (book/book-create-action-issue-access-token!)
  (book/book-grant-permission-to-invoke-action-issue-access-token!)
  ;; This tackles the '404' problem.
  (install-not-found))

(defn setup-hello-world! []
  (book/book-create-action-put-immutable-public-resource!)
  (book/book-grant-permission-to-invoke-action-put-immutable-public-resource!)
  (book/book-create-action-get-public-resource!)
  (book/book-grant-permission-to-invoke-get-public-resource!)
  (book/book-create-hello-world-resource!)
  )

(defn setup-protected-resource! []
  (book/book-create-action-put-immutable-private-resource!)
  (book/book-grant-permission-to-put-immutable-private-resource!)
  (book/book-create-action-get-private-resource!)
  (book/book-grant-permission-to-get-private-resource!)
  (book/book-create-immutable-private-resource!))

(defn setup-application! []
  (book/book-invoke-put-application!)
  (book/book-invoke-authorize-application!)
  (book/book-create-test-subject!)
  (book/book-invoke-issue-access-token!))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-handler)

(deftest public-resource-test
  (preliminaries!)
  (setup-hello-world!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")) ;; Assert the entity exists in the db
  (is (not (xt/entity (xt/db *xt-node*) "https://site.test/not-hello"))) ;; Assert that out 404 entity is not in the db

  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}]

    (testing "Can retrieve a public immutable resource"
      (let [{:ring.response/keys [status body]} (*handler* req)]
        (is (= 200 status))
        (is (= "Hello World!\r\n" body))))

    (testing "Receive 405 when method not allowed"
      (let [invalid-req (assoc req :ring.request/method :put)
            {:ring.response/keys [status]} (*handler* invalid-req)]
        (is (= 405 status))))

    (testing "Receive 404 when resource does not exist"
      (let [invalid-req (assoc req :ring.request/path "/not-hello")
            {:ring.response/keys [status]} (*handler* invalid-req)]
        (is (= 404 status))))))

(deftest private-resource-test
  (preliminaries!)
  (setup-protected-resource!)
  (setup-application!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/private.html"))

  (let [request {:ring.request/method :get
                 :ring.request/path "/private.html"}]

    (testing "Cannot be accessed without a bearer token"
      (is (= 401 (:ring.response/status (*handler* request)))))

    (testing "Can be accessed with a valid bearer token"
      (is (= 200 (:ring.response/status
                  (*handler*
                   (assoc
                    request
                    :ring.request/headers
                    {"authorization" "Bearer test-access-token"}))))))

    (testing "Cannot be accessed with an invalid bearer token"
      (is (= 401 (:ring.response/status
                  (*handler*
                   (assoc
                    request
                    :ring.request/headers
                    {"authorization" "Bearer not-test-access-token"}))))))))

(deftest not-found-test
  (preliminaries!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))


(deftest user-directory-test
  (preliminaries!)
  (setup-protected-resource!)
  (setup-application!)
  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-user-owned-content"
    :juxt.pass.alpha/scope "write:user-content"
    :juxt.pass.alpha/rules
    [
     '[(allowed? permission subject action resource)
       [permission ::pass/user user]
       [subject ::pass/user-identity id]
       [id ::pass/user user]
       [resource :owner user]]]})

  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/put-user-owned-content"
    :juxt.pass.alpha/action "https://site.test/actions/put-user-owned-content"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.pass.alpha/purpose nil})

  ;; Both bob and alice have user directories
  (doseq [user #{"bob" "alice"}]
    (repl/put!
     {:xt/id (format "https://site.test/~%s/{path}" user)
      ;; This needs to return a resource 'owned' by the user, then the action can
      ;; unify on the subject's user and the resource's owner.
      :owner (format "https://site.test/users/%s" user)
      ::site/uri-template true
      ::http/methods
      {:get {::pass/actions #{"https://site.test/actions/get-not-found"}}
       :put {::pass/actions #{"https://site.test/actions/put-user-owned-content"}}}}))

  ;; 404 on GET, doesn't exist yet!
  (let [req {:ring.request/method :get
             :ring.request/path "/~bob/index.html"}]
    (is (= 404 (:ring.response/status (*handler* req)))))

  ;; Alice can't write to Bob's area
  (let [req {:ring.request/method :put
             :ring.request/path "/~bob/index.html"
             :ring.request/headers
             {"authorization" "Bearer test-access-token"}}]
    (is (= 403 (:ring.response/status (*handler* req)))))

  ;; When Alice writing to Alice's user directory, we get through security.
  (let [req {:ring.request/method :put
             :ring.request/path "/~alice/index.html"
             :ring.request/headers
             {"authorization" "Bearer test-access-token"}}]
    (is (= 411 (:ring.response/status (*handler* req))))))

#_((t/join-fixtures [with-system-xt with-site-book-setup with-handler])
 (fn []
))
