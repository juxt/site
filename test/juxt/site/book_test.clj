;; Copyright © 2022, JUXT LTD.

(ns juxt.site.book-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db* *handler*] :as tutil]
   [juxt.site.alpha.repl :refer [put!]]
   [xtdb.api :as xt]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.pass.alpha.authentication :as authn]
   [clojure.string :as str]
   [xtdb.api :as x]))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-handler)

(deftest public-resource-test
  (book/preliminaries!)
  (book/setup-hello-world!)

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

(deftest protected-resource-with-http-basic-auth-test
  (book/preliminaries!)
  (book/setup-protected-resource!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected/document.html"))

  #_(let [request {:ring.request/method :get
                 :ring.request/path "/protected.html"}]

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

(deftest protected-resource-with-http-bearer-auth-test
  (book/preliminaries!)
  (book/setup-protected-resource!)
  (book/setup-application!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected/document.html"))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected/document.html"}]

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
  (book/preliminaries!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))


(deftest user-directory-test
  (book/preliminaries!)
  (book/setup-protected-resource!)
  (book/setup-application!)
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

;; TODO: Test for www-authenticate header

#_((t/join-fixtures [with-system-xt with-handler])
 (fn []
   (book/preliminaries!)
   (book/book-put-basic-auth-user-identity!)
   (book/book-put-basic-protection-space!)
   (book/setup-protected-resource!)

   (is (xt/entity (xt/db *xt-node*) "https://site.test/protected/document.html"))

   (is (= 1 (count (authn/protection-spaces (xt/db *xt-node*) "https://site.test/protected/document.html"))))

   #_(xt/entity (xt/db *xt-node*) "https://site.test/user-identities/alice/basic")
   #_(xt/entity (xt/db *xt-node*) "https://site.test/permissions/alice/protected-html")

   #_(xt/entity (xt/db *xt-node*) "https://site.test/actions/get-protected-resource")


   #_(xt/q (xt/db *xt-node*)
         '{:find [e]
           :where [[e ::site/type "https://meta.juxt.site/pass/permission"]]})

   (let [request {:ring.request/method :get
                  :ring.request/path "/protected/document.html"}]
     (*handler*
      request
      #_(assoc
       request
       :ring.request/headers
       {"authorization"
        (format
         "Basic %s"
         (String.
          (.encode
           (java.util.Base64/getEncoder)
           (.getBytes "alice:garden"))))})))))
