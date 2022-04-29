;; Copyright © 2021, JUXT LTD.

(ns juxt.dave.webdav-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-xt with-handler submit-and-await! *handler*
                           access-all-areas access-all-apis]]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-xt with-handler)

#_(deftest get-test
  (submit-and-await!
   [[:xtdb.api/put access-all-apis]
    [:xtdb.api/put
     {:xt/id "https://example.org/test.txt"
      ::http/body "Hello World!\n"}]])

  (let [resp (*handler*
              {:ring.request/method :get
               :ring.request/path "/test.txt"
               })]))

#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (submit-and-await!
    [[:xtdb.api/put access-all-areas]
     [:xtdb.api/put
      {:xt/id "https://example.org/test.txt"
       ::http/content-type "text/plain;charset=utf-8"
       ::http/content "Hello World!\n"
       ::http/methods {:get {} :head {} :options {}}}]])

   (:ring.response/body
    (*handler*
     {:ring.request/method :get
      :ring.request/path "/test.txt"}))))
