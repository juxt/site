;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.basic-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [clojure.java.io :as io]
   [juxt.book :as book]
   [juxt.site.alpha.graphql.graphql-compiler :as gcompiler]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :as init]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [juxt.site.alpha.graphql.graphql-query-processor :as gqp]))

;; TODO: Dedupe between this test ns and juxt.book-test

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(defn call-handler [request]
  (*handler* (book/with-body request (::body-bytes request))))

(use-fixtures :each with-system-xt with-handler)

(defmacro with-fixtures [& body]
  `((t/join-fixtures [with-system-xt with-handler])
    (fn [] ~@body)))

(defn create-action-register-patient! []
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/register-patient"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]]})))))

(defn grant-permission-to-invoke-action-register-patient! []
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/register-patient"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/register-patient"
       :juxt.pass.alpha/purpose nil})))))

(defn create-action-get-patients! []
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-patients"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn create-action-read-vitals! []
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/read-vitals"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]
          ]]})))))

(defn register-patient! [args]
  (throw (ex-info "TODO: register patient" {:args args}))
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/register-patient"
      {:xt/id "https://example.org/patients/001"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]
          ]]})))))

(def dependency-graph
  {"https://example.org/actions/register-patient"
   {:create #'create-action-register-patient!
    :deps #{::init/system}}
   "https://site.test/permissions/system/register-patient"
   {:create #'grant-permission-to-invoke-action-register-patient!
    :deps #{::init/system}}
   "https://example.org/actions/get-patients"
   {:create #'create-action-get-patients!
    :deps #{::init/system}}
   "https://example.org/patients/{pid}"
   {:create #'register-patient!
    :deps #{::init/system
            "https://example.org/actions/register-patient"
            "https://site.test/permissions/system/register-patient"}}
   "https://example.org/actions/read-vitals"
   {:create #'create-action-read-vitals!
    :deps #{::init/system}}})

(defmacro with-resources [resources & body]
  `(do
     (init/converge!
      ~(conj resources ::init/system)
      (init/substitute-actual-base-uri
       (merge init/dependency-graph book/dependency-graph dependency-graph)))
     ~@body))

(with-fixtures
  (with-resources
    #{::init/system
      "https://site.test/graphql"
      "https://site.test/actions/get-patients"
      #_"https://site.test/actions/read-vitals"
      "https://site.test/actions/register-patient"
      "https://site.test/permissions/system/register-patient"
      "https://site.test/patients/001"

      ;; Add some users
      ;; Alice can read patients
      ;; Carlos cannot patients


      }

    (let [db (xt/db *xt-node*)
          compiled-schema
          (->
           "juxt/site/graphql/basic.graphql"
           io/resource
           slurp
           gcompiler/compile-schema)

          ]

      (gqp/graphql-query->xtdb-query
       "query { patients { name heartRate } }"
       compiled-schema
       db))))
