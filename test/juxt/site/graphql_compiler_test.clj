;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql-compiler-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [juxt.site.graphql-eql-compiler :refer [graphql->eql-ast]]
   [juxt.grab.alpha.parser :as parser]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.schema :as schema]))

(deftest graphql->eql-test
  (let [schema (-> "juxt/site/schema.graphql"
                   io/resource
                   slurp
                   parser/parse
                   schema/compile-schema)]

    (testing "Basic GraphQL compilation"
      (is (= {:type :root
              :children
              [{:type :join
                :dispatch-key :doctors
                :key :doctors
                :params
                {:search "jack"
                 :juxt.pass/action "https://site.test/actions/get-doctor"}
                :children [{:type :prop
                            :dispatch-key :xt/id
                            :key :xt/id}]}]}

             (graphql->eql-ast
              schema
              (-> "query GetDoctors { doctors(search: \"jack\") { id } }"
                  parser/parse
                  (document/compile-document schema)
                  (document/get-operation "GetDoctors"))))))

    (testing "Nested selection set"
      (is (= {:type :root
              :children
              [{:type :join
                :dispatch-key :doctors
                :key :doctors
                :params
                {:search "jack"
                 :juxt.pass/action "https://site.test/actions/get-doctor"}
                :children
                [{:type :prop :dispatch-key :xt/id :key :xt/id}
                 {:type :prop :dispatch-key :name :key :name}
                 {:type :join
                  :dispatch-key :patients
                  :key :patients
                  :params
                  {:juxt.pass/action "https://site.test/actions/get-patient"}
                  :children
                  [{:type :prop :dispatch-key :xt/id :key :xt/id}
                   {:type :prop :dispatch-key :name :key :name}]}]}]}

             (graphql->eql-ast
              schema
              (-> "query GetDoctors { doctors(search: \"jack\") { id name patients { id name } } }"
                  parser/parse
                  (document/compile-document schema)
                  (document/get-operation "GetDoctors"))))))))
