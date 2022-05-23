;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
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

(defn encode-basic-authorization [user password]
  (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" user password))))))

(deftest protected-resource-with-http-basic-auth-test
  (book/preliminaries!)
  (book/setup-protected-resource!)
  (book/book-put-basic-auth-user-identity!)
  (book/book-create-action-put-protection-space!)
  (book/book-grant-permission-to-put-protection-space!)
  (book/book-put-basic-protection-space!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected/document.html"))

  (is (= 1 (count (authn/protection-spaces (xt/db *xt-node*) "https://site.test/protected/document.html"))))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected/document.html"}

        request-with-good-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "garden")})

        request-with-bad-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "gradne")})]

    (let [response (*handler* request)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-bad-creds)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-good-creds)]
      (is (= 200 (:ring.response/status response)))
      (is (nil? (get-in response [:ring.response/headers "www-authenticate"]))))))

(deftest protected-resource-with-http-bearer-auth-test
  (book/preliminaries!)
  (book/setup-protected-resource!)
  (book/applications-preliminaries!)
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
                    {"authorization" "Bearer not-test-access-token"})))))
      ;; Test WWW-Authenticate header and realm

      )))

(deftest not-found-test
  (book/preliminaries!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))


(deftest user-directory-test
  (book/preliminaries!)
  (book/setup-protected-resource!)
  (book/applications-preliminaries!)
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
