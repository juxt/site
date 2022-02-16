;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.acl-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
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

     [::xt/put
      {:xt/id "https://example.org/rules/valid-user"
       ::pass/rule []}]

     [::xt/put
      {:xt/id "https://example.org/ruleset"
       ::pass/rules ["https://example.org/rules/valid-user"]}]

     ;; Establish a resource
     [::xt/put
      {:xt/id "https://example.org/index"
       ::http/methods #{:get}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Hello World!"
       ::pass/ruleset "https://example.org/ruleset"}]])

   (let [{:ring.response/keys [status] :as response}
         (*handler*
          {:ring.request/method :get
           :ring.request/path "/index"})]
     (when (not= 200 status) (throw (ex-info "FAIL" {:response response})))
     (is (= 200 status)))))
