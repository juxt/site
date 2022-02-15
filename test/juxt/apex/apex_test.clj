;; Copyright © 2022, JUXT LTD.

(ns juxt.apex.apex-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]
   [jsonista.core :as json]
   [clojure.java.io :as io]))

(t/use-fixtures :each with-xt with-handler)

;; POST method operations

#_(json/write-value-as-bytes
 {:openapi "3.0.2"
  :paths {"/foo" {:post {:summary "Post some data"}}}})

(do
  (defn install-api [api]
    (let [body (json/write-value-as-bytes api)]
      (*handler*
       {:ring.request/method :put
        :ring.request/path "/things/foo"
        :ring.request/headers
        {"content-type" "application/vnd.oai.openapi+json;version=3.0.2"
         "content-length" (str (count body))}
        :ring.request/body (io/input-stream body)})))

  ((t/join-fixtures [with-xt with-handler])
   (fn []
     (submit-and-await!
      [[:xtdb.api/put access-all-areas]

       ])
     ;; Install API (with validation)
     (install-api
      {:openapi "3.0.2"
       :paths {"/foo" {:post {:summary "Post some data"}}}})

     )
   ))


(defn post-method-operations []
  )
