;; Copyright © 2022, JUXT LTD.

(ns juxt.site.demo-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.demo :as demo]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db* *handler*] :as tutil]
   [xtdb.api :as xt]
   [juxt.site.alpha :as-alias site]))

(defn with-site-book-setup [f]
  (demo/demo-put-user!)
  (demo/demo-put-user-identity!)
  (demo/demo-put-subject!)
  (demo/demo-install-create-action!)
  (demo/demo-install-do-action-fn!)
  (demo/demo-permit-create-action!)
  (demo/demo-create-grant-permission-action!)
  (demo/demo-permit-grant-permission-action!)
  (demo/demo-create-action-put-user!)
  (demo/demo-create-action-put-identity!)
  (demo/demo-create-action-put-subject!)
  (demo/demo-grant-permission-to-invoke-action-put-subject!)
  (demo/demo-create-action-put-application!)
  (demo/demo-grant-permission-to-invoke-action-put-application!!)
  (demo/demo-create-action-authorize-application!)
  (demo/demo-grant-permission-to-invoke-action-authorize-application!)
  (demo/demo-create-action-issue-access-token!)
  (demo/demo-grant-permission-to-invoke-action-issue-access-token!)
  (demo/demo-create-action-put-immutable-public-resource!)
  (demo/demo-grant-permission-to-invoke-action-put-immutable-public-resource!)
  (demo/demo-create-action-get-public-resource!)
  (demo/demo-grant-permission-to-invoke-get-public-resource!)
  (demo/demo-create-hello-world-resource!)
  (demo/demo-create-action-put-immutable-private-resource!)
  (demo/demo-grant-permission-to-put-immutable-private-resource!)
  (demo/demo-create-action-get-private-resource!)
  (demo/demo-grant-permission-to-get-private-resource!)
  (demo/demo-create-immutable-private-resource!)
  ;;(demo/demo-invoke-put-application!)
  ;;(demo/demo-invoke-authorize-application!)
  (demo/demo-create-test-subject!)
  (demo/demo-invoke-issue-access-token!)
  (f))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-site-book-setup with-handler)

(deftest public-resource-test
  (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")) ;; Assert the entity exists in the db

  (testing "Can retrieve a public immutable resource"
    (let [{:ring.response/keys [status body]}
          (*handler* {:ring.request/method :get
                      :ring.request/path "/hello"})]
      (is (= 200 status))
      (is (= "Hello World!\r\n" body))))
  )

(deftest private-resource-test
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

#_((t/join-fixtures [with-system-xt with-site-book-setup with-handler with-db])
   (fn []
     (let [response
           (*handler* {:ring.request/method :get
                       :ring.request/path "/private.html"
                       :ring.request/headers {"authorization" "Bearer test-access-token"}})]
       (:ring.response/status response))))
