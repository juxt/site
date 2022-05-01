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

  (f))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-site-book-setup with-handler with-db)

(deftest hello-test
  ;; First, as something that can easily fail, we test the hello resource exists
  ;; in the db
  (is (xt/entity *db* "https://site.test/hello"))

  (let [{:ring.response/keys [body]}
        (*handler* {:ring.request/method :get
                    :ring.request/path "/hello"})]
    ;; Can we see the body of the resource?
    (is (= "Hello World!\r\n" body))))

;; Test private


#_((t/join-fixtures [with-system-xt with-site-book-setup with-handler with-db])
 (fn []
   ;;(:xtdb.kv/estimate-num-keys (xt/status *xt-node*))
   ;;(xt/entity (xt/db *xt-node*) "https://site.test/users/mal")
   *db*
   (repl/ls)

   (*handler* {:ring.request/method :get
                     :ring.request/path "/hello"})

   ;; curl -i https://site.test/hello
   ))
