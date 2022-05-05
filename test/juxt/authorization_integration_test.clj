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
  [user-id additional-info]
  (merge
   {:xt/id (str site-prefix "/users/" user-id)
    :juxt.site.alpha/type "https://meta.juxt.site/pass/user"
    :juxtcode user-id}
   additional-info))

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
  ([action-id]
   {:xt/id (str site-prefix "/permissions/" action-id)
          :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
    :juxt.pass.alpha/action (str site-prefix "/actions/" action-id)
    :juxt.pass.alpha/purpose nil})
  ([action-id additional-info]
   (merge (make-permission action-id) additional-info)))

(defn with-site-helpers
  [f]
  (repl/install-do-action-fn!)
  (repl/put! (make-create-action-helper))
  (repl/put! (make-user "host" {:name "Test Host User"}))
  (repl/put! (make-identity "host"))
  (repl/put! (make-subject "host" "host-test"))
  (repl/put! (make-permission "create-action" { :juxt.pass.alpha/user (str site-prefix "/users/host") }))
  (f))

(def fixtures [tutil/with-system-xt with-handler with-site-helpers])

(apply (partial t/use-fixtures :each) fixtures)

(def test-host-subject (str site-prefix "/subjects/host-test"))

(defn add-action
  [action-id]
  (repl/do-action
   test-host-subject
   (str site-prefix "/actions/create-action")
   {:xt/id (str site-prefix "/actions/" action-id)
    :juxt.pass.alpha/scope "read:resource"
    :juxt.pass.alpha/rules
    [['(allowed? permission subject action resource)
      ['permission :xt/id]]]}))

(defn add-not-found-resource
  []
  (repl/put!
   {:xt/id "urn:site:resources:not-found"
    ::http/methods
    {:get {:juxt.pass.alpha/actions #{(str site-prefix "/actions/get-not-found")}}}}))

(defn add-public-resource
  [path additional-info]
  (repl/put! (merge {:xt/id (str site-prefix path)
                     ::http/methods {:get {:juxt.pass.alpha/actions #{(str site-prefix "/actions/get-public-resource")}}}
                     :juxt.pass.alpha/rules
                     [['(allowed? permission subject action resource)
                       ['permission :xt/id]]]} additional-info)))

;;;; TESTS START ;;;;

(deftest ok-test
  (add-action "get-public-resource")
  (repl/put! (make-permission "get-public-resource"))
  (testing "200 OK - When a resource can be retrieved from the uri 200 status is returned"
    (add-public-resource "/hello" { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" })
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello"})]
      (is (= 200 (:ring.response/status resp)))
      (is (= "Hello World" (:ring.response/body resp)))))

  (testing "200 OK - When a resource can be retrieved from a templated uri 200 status is returned"
    (add-public-resource "/hello/{id}/world" { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" :juxt.site.alpha/uri-template true })
    (let [resp (*handler* {:ring.request/method :get
                           :ring.request/path "/hello/1234/world"})]
      (is (= 200 (:ring.response/status resp)))
      (is (= "Hello World" (:ring.response/body resp))))))

(deftest method-not-allowed-test
  (testing "405 Method Not Allowed - When a resource does not support the provided method 405 status is returned"
    (add-action "get-public-resource")
    (add-public-resource "/hello" { :juxt.http.alpha/content-type "text/plain" :juxt.http.alpha/content "Hello World" })
    (repl/put! (make-permission "get-public-resource"))
    (let [resp (*handler* {:ring.request/method :post
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))
    (let [resp (*handler* {:ring.request/method :put
                           :ring.request/path "/hello"})]
      (is (= 405 (:ring.response/status resp))))))

(deftest not-found-test
  (testing "404 Not Found - When no resource exists at the uri 404 status is returned"
    (add-action "get-not-found")
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
