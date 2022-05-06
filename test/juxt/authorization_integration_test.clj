;; Copyright © 2022, JUXT LTD.

(ns juxt.authorization-integration-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [juxt.test.util :refer [*xt-node* *handler*] :as tutil]
             [juxt.site.alpha :as-alias site]
             [juxt.http.alpha :as-alias http]
             [juxt.site.alpha.repl :as repl]
             [clojure.string :as str]
             [xtdb.api :as xt]))

(def site-prefix "https://test.example.com")

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri site-prefix
              ::site/uri-prefix site-prefix})]
    (f)))

(defn make-create-action-helper
  []
  {:xt/id (str site-prefix "/actions/create-action")
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
   :juxt.pass.alpha/scope "write:admin"
   :juxt.pass.alpha.malli/args-schema
   [:tuple
    [:map
     [:xt/id [:re (str site-prefix "/actions/(.+)")]]
     [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/action"]]
     [:juxt.pass.alpha/rules [:vector [:vector :any]]]]]

   :juxt.pass.alpha/process
   [
    [:juxt.pass.alpha.process/update-in [0]
     'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/action"}]
    [:juxt.pass.alpha.malli/validate]
    [:xtdb.api/put]]

   :juxt.pass.alpha/rules
   '[
     [(allowed? permission subject action resource)
      [subject :juxt.pass.alpha/identity id]
      [id :juxt.pass.alpha/user user]
      [permission :juxt.pass.alpha/user user]]]})

(defn make-user
  [user-id]
  {:xt/id (str site-prefix "/users/" user-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/user"
   :juxtcode user-id})

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
  (repl/install-do-action-fn!)
  (repl/put! (make-create-action-helper))
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
   :juxt.pass.alpha/scope "read:resource"
   :juxt.pass.alpha/rules
   [['(allowed? permission subject action resource)
     ['permission :xt/id]]]})

(defn add-action
  [action]
  (repl/do-action
   test-host-subject
   (str site-prefix "/actions/create-action")
   action))

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


;;;; TESTS START ;;;;

(deftest ok-test
  (add-action (make-action "get-public-resource"))
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
  (add-action (merge (make-action "get-public-resource") {:juxt.pass.alpha/rules
                                                          '[[(allowed? permission subject action resource)
                                                             [permission :juxt.pass.alpha/user user]
                                                             [subject :juxt.pass.alpha/identity ident]
                                                             [ident :juxt.pass.alpha/user user]]]}))
  (repl/put! (merge (make-permission "get-public-resource") {:juxt.pass.alpha/user (str site-prefix "/users/unauthorized-user")}))
  (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
  (let [resp (*handler* {:ring.request/method :get
                         :ring.request/path "/hello"})]
    (is (= 401 (:ring.response/status resp)))))

(deftest method-not-allowed-test
  (testing "405 Method Not Allowed - When a resource does not support the provided method 405 status is returned"
    (add-action (make-action "get-public-resource"))
    (repl/put! (merge (make-resource "/hello") { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" }))
    (repl/put! (make-permission "get-public-resource"))
    (let [resp (*handler* {:ring.request/method :post
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))
    (let [resp (*handler* {:ring.request/method :put
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))))

(deftest not-found-test
  (testing "404 Not Found - When no resource exists at the uri 404 status is returned"
    (add-action (make-action "get-not-found"))
    (add-not-found-resource)
    (repl/put! (make-permission "get-not-found"))
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 404 (:ring.response/status resp))))))

;;;; TESTS END ;;;;

(comment

  #_((t/join-fixtures fixtures)
   (fn []

     (add-action "get-public-resource")
     (repl/put! (make-permission "get-public-resource"))
     (add-public-resource "/hello/{id}/world" { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" })
     (juxt.site.alpha.locator/match-uri-templated-uris (xt/db *xt-node*) (str site-prefix "/hello/{id}/world"))
     #_(juxt.pass.alpha.authorization/check-permissions (xt/db *xt-node*) #{"https://test.example.com/actions/get-public-resource"} {:resource "https://test.example.com/hello"})
     ))
)
