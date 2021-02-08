;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.pass.jinx-test
  (:require
   [crypto.password.bcrypt :as password]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [jsonista.core :as json]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value process-transformations]]))

(defn load-json [path]
  (-> path
      io/resource
      slurp
      edn/read-string
      json/write-value-as-string
      json/read-value))

(deftest convert-json-to-crux-entity-test
  (let [doc (load-json "juxt/site/alpha/pass/openapi.edn")
        schema (-> doc
                   (get-in
                    ["paths" "/users/{id}" "get"
                     "responses" "200" "content"
                     "application/json" "schema"])
                   (jinx.api/schema))
        instance (->
                  (jinx.api/validate
                   schema
                   {"id" "/_crux/pass/users/juxtmal"
                    "username" "juxtmal"
                    "email" "mal@juxt.pro"
                    "password" "FoolishTiger"
                    "userGroup" "/_crux/pass/user-groups/owners"}
                   {:base-document doc})
                  process-transformations
                  ::jinx/instance)]
    (is
     (=
      {"id" (java.net.URI. "/_crux/pass/users/juxtmal")
       "username" "juxtmal"
       "email" "mal@juxt.pro"
       "userGroup" (java.net.URI. "/_crux/pass/user-groups/owners")}
      (select-keys instance ["id" "username" "email" "userGroup"])))
    (is (string? (get instance "password")))
    (is (> (count (get instance "password")) 30))
    (is (password/check "FoolishTiger" (get instance "password")))))
