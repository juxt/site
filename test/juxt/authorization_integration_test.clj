;; Copyright © 2022, JUXT LTD.

(ns juxt.authorization-integration-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [juxt.test.util :refer [*xt-node* *handler*] :as tutil]
             [juxt.site.alpha :as-alias site]
             [juxt.http.alpha :as-alias http]
             [juxt.site.alpha.repl :as repl]
             [clojure.string :as str]
             [xtdb.api :as xt]
             [juxt.pass.alpha.application :as application]))

(def site-prefix "https://test.example.com")

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri site-prefix
              ::site/uri-prefix site-prefix})]
    (f)))

(defn make-user
  [user-id]
  {:xt/id (str site-prefix "/users/" user-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/user"})

(defn make-identity
  [user-id]
  {:xt/id (str site-prefix "/identities/" user-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
   :juxt.pass.alpha/user (str site-prefix "/users/" user-id)})

(defn make-subject
  [user-id subject-id]
  {:xt/id (str site-prefix "/subjects/" subject-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
   :juxt.pass.alpha/identity (str site-prefix "/identities/" user-id)})

(defn make-permission
  [action-id]
  {:xt/id (str site-prefix "/permissions/" action-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
   :juxt.pass.alpha/action (str site-prefix "/actions/" action-id)
   :juxt.pass.alpha/purpose nil})

(defn with-site-helpers
  [f]
  (repl/put! (merge (make-user "host") {:name "Test Host User"}))
  (repl/put! (make-identity "host"))
  (repl/put! (make-subject "host" "host-test"))
  (repl/put! (merge (make-permission "create-action") { :juxt.pass.alpha/user (str site-prefix "/users/host") }))
  (f))

(def fixtures [tutil/with-system-xt with-handler with-site-helpers])

(apply (partial t/use-fixtures :each) fixtures)

(def test-host-subject (str site-prefix "/subjects/host-test"))

(defn make-action
  [action-id]
  {:xt/id (str site-prefix "/actions/" action-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
   :juxt.pass.alpha/scope "read:resource"
   :juxt.pass.alpha/rules
   [['(allowed? permission subject action resource)
     ['permission :xt/id]]]})

(defn add-not-found-resource
  []
  (repl/put!
   {:xt/id "urn:site:resources:not-found"
    ::http/methods
    {:get {:juxt.pass.alpha/actions #{(str site-prefix "/actions/get-not-found")}}}}))

(defn make-resource
  [path]
  {:xt/id (str site-prefix path)
   ::http/methods {:get {:juxt.pass.alpha/actions #{(str site-prefix "/actions/get-public-resource")}}}
   :juxt.pass.alpha/rules
   [['(allowed? permission subject action resource)
     ['permission :xt/id]]]})

(defn make-access-token
  [subject-id]
  (merge
   (application/make-access-token-doc {
                                       :prefix (str site-prefix "/tokens/")
                                       :subject (str site-prefix "/subjects/" subject-id)
                                       :expires-in-seconds 160})
   {:juxt.site.alpha/type "https://meta.juxt.site/pass/access-token"}))


;;;; TESTS START ;;;;

(deftest ok-test
  (repl/put! (make-action "get-public-resource"))
  (repl/put! (make-permission "get-public-resource"))
  (testing "200 OK - When a resource can be retrieved from the uri 200 status is returned"
    (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 200 (:ring.response/status resp)))
      (is (= "Hello World" (:ring.response/body resp)))))

  (testing "200 OK - When a resource can be retrieved from a templated uri 200 status is returned"
    (repl/put! (merge (make-resource "/hello/{id}/world") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" :juxt.site.alpha/uri-template true }))
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello/1234/world"})]
      (is (= 200 (:ring.response/status resp)))
      (is (= "Hello World" (:ring.response/body resp))))))

(deftest unauthorized-test
  (repl/put! (merge (make-action "get-public-resource") {:juxt.pass.alpha/rules
                                                          '[[(allowed? permission subject action resource)
                                                             [permission :juxt.pass.alpha/user user]
                                                             [subject :juxt.pass.alpha/identity ident]
                                                             [ident :juxt.pass.alpha/user user]]]}))
  (repl/put! (merge (make-permission "get-public-resource") {:juxt.pass.alpha/user (str site-prefix "/users/unauthorized-user")}))
  (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
  (testing "401 Unauthorized - When a resource requires authentication credentials that are not available 401 status is returned"
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 401 (:ring.response/status resp))))))

(deftest forbidden-test
  (repl/put! (merge (make-action "get-public-resource") {:juxt.pass.alpha/rules
                                                     '[[(allowed? permission subject action resource)
                                                        [permission :xt/id]
                                                        [subject :juxt.pass.alpha/identity ident]
                                                        [ident :juxt.pass.alpha/user "https://test.example.com/users/alice"]]]}))
  (repl/put! (merge (make-permission "get-public-resource")))
  (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
  (repl/put! (make-user "bob"))
  (repl/put! (make-identity "bob"))
  (repl/put! (make-subject "bob" "bob-subj"))
  (let [access-token-doc (make-access-token "bob-subj")
        access-token (:juxt.pass.alpha/token access-token-doc)]
    (repl/put! access-token-doc)

    (testing "403 Forbidden - When a user is authenticated but not authorized to access the resource 403 status is returned"
      (let [resp (*handler* {:ring.request/method :get
                             :ring.request/headers {"authorization" (str "Bearer " access-token)}
                             :ring.request/path "/hello"})]
        (is (= 403 (:ring.response/status resp)))))))

(deftest method-not-allowed-test
  (repl/put! (make-action "get-public-resource"))
  (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
  (repl/put! (make-permission "get-public-resource"))
  (testing "405 Method Not Allowed - When a resource does not support the provided method 405 status is returned"
    (let [resp (*handler* {:ring.request/method :post
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))
    (let [resp (*handler* {:ring.request/method :put
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))))

(deftest not-found-test
  (testing "404 Not Found - When no resource exists at the uri 404 status is returned"
    (repl/put! (make-action "get-not-found"))
    (add-not-found-resource)
    (repl/put! (make-permission "get-not-found"))
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 404 (:ring.response/status resp))))))


(deftest issue-bearer-token-test
  (repl/put! (merge (make-action "get-public-resource") {:juxt.pass.alpha/rules
                                                          '[[(allowed? permission subject action resource)
                                                             [permission :xt/id]
                                                             [subject :juxt.pass.alpha/identity ident]
                                                             [ident :juxt.pass.alpha/user "https://test.example.com/users/alice"]]]}))
  (repl/put! (merge (make-permission "get-public-resource")))
  (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
  (repl/put! (make-user "alice"))
  (repl/put! (make-identity "alice"))
  (repl/put! (make-subject "alice" "alice-subj"))
  (repl/put! (make-user "bob"))
  (repl/put! (make-identity "bob"))
  (repl/put! (make-subject "bob" "bob-subj"))

  (testing "Access to a resource which requires authorization is denied when no bearer token provided"
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 401 (:ring.response/status resp)))))

  (let [access-token-doc (make-access-token "alice-subj")
        access-token (:juxt.pass.alpha/token access-token-doc)]
    (repl/put! access-token-doc)

    (testing "Access to a resource which requires authorization is permitted when a valid bearer token is provided"
      (let [resp (*handler* {:ring.request/method :get
                             :ring.request/headers {"authorization" (str "Bearer " access-token)}
                             :ring.request/path "/hello"})]
        (is (= 200 (:ring.response/status resp)))))

    (testing "Access to a resource which requires authorization is denied when an invalid bearer token is provided"
      (let [resp (*handler* {:ring.request/method :get
                             :ring.request/headers {"authorization" (str "Bearer " "invalid-access-token")}
                             :ring.request/path "/hello"})]
        (is (= 401 (:ring.response/status resp))))))
  (let [access-token-doc (make-access-token "bob-subj")
        access-token (:juxt.pass.alpha/token access-token-doc)]
    (repl/put! access-token-doc)

    (testing "Access to a resource which requires authorization is denied when a valid bearer token is provided but for a user without permission"
      (let [resp (*handler* {:ring.request/method :get
                             :ring.request/headers {"authorization" (str "Bearer " access-token)}
                             :ring.request/path "/hello"})]
        (is (= 403 (:ring.response/status resp)))))))

;;;; TESTS END ;;;;

(comment

  ((t/join-fixtures fixtures)
   (fn []

     (repl/put!(merge (make-action "get-public-resource") {:juxt.pass.alpha/rules
                                                             '[[(allowed? permission subject action resource)
                                                                [permission :xt/id]
                                                                [subject :juxt.pass.alpha/identity ident]]]}))
     (repl/put! (merge (make-permission "get-public-resource") {:juxt.pass.alpha/user (str site-prefix "/users/unauthorized-user")}))
     (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
     (repl/put! (make-user "alice"))
     (repl/put! (make-identity "alice"))
     (repl/put! (make-subject "alice" "alice-subj"))
     (let [access-token-doc (make-access-token "alice-subj")
        access-token (:juxt.pass.alpha/token access-token-doc)]
       (repl/put! access-token-doc)
       (juxt.pass.alpha.authentication/lookup-subject-from-bearer (xt/db *xt-node*) access-token))
     #_(juxt.pass.alpha.authorization/check-permissions (xt/db *xt-node*) #{"https://test.example.com/actions/get-public-resource"} {:resource "https://test.example.com/hello" :subject "https://test.example.com/subjects/alice-subj"})
     ))
)
