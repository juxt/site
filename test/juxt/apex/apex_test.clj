;; Copyright © 2022, JUXT LTD.

(ns juxt.apex.apex-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]
   [jsonista.core :as json]
   [juxt.jinx.alpha.api :refer [schema validate]]
   [clojure.java.io :as io]
   [juxt.jinx.alpha :as jinx]))

(alias 'jinx (create-ns 'juxt.jinx.alpha))

#_(json/write-value-as-string
           {:openapi "3.0.2"
            :paths {"/foo" {:post {:summary "Post some data"}}}})

#_(json/read-value (io/resource "juxt/apex/schema.json"))

#_(let [schema (schema (json/read-value (io/resource "juxt/apex/schema.json")))
      doc {"openapi" "3.0.2"
           "info" {"title" "Foo" "version" "1.0.0"}
           "paths" {"\\/" {"get" {}}}}
      validation (validate schema doc)
      ]

  doc

  (if (::jinx/valid? validation)
    true
    validation))


#_(t/use-fixtures :each with-xt with-handler)

;; POST method operations

#_(do
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
      [
       ;; Establish a session
       ;; Establish a developer role
       ;; Establish a scope write:api
       ;; Establish a rule
       #_[:xtdb.api/put access-all-areas]

       ])
     ;; Install API (with validation)
     (install-api
      {:openapi "3.0.2"
       :paths {"/foo" {:post {:summary "Post some data"}}}})

     )
   ))


#_(defn post-method-operations []
  )
