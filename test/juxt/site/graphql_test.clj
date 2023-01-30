;; Copyright © 2021, JUXT LTD.

(ns juxt.site.graphql-test
  (:require
   [xtdb.api :as x]
   [jsonista.core :as json]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.site.alpha.init :as init]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.grab.alpha.parser :as parser]
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [xtdb.api :as xt]
   [clojure.string :as str]
   [juxt.jinx.alpha.jsonpointer :refer [json-pointer]])
  (:import (java.io ByteArrayInputStream)))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'g (create-ns 'juxt.grab.alpha.graphql))

(t/use-fixtures :each with-xt with-handler)

(defn graphql []
  (init/put-site-api! *xt-node* {::site/base-uri "https://example.org"})
  (submit-and-await!
    [[:xtdb.api/put access-all-areas]

     [:xtdb.api/put
      {:xt/id "https://example.org/_site/users/mal"
       :juxt.site.alpha/type "User"
       :juxt.pass.alpha/username "mal"
       :email "mal@juxt.pro"
       :name "Malcolm Sparks"}]

     [:xtdb.api/put
      {:xt/id "https://example.org/_site/users/alx"
       :juxt.site.alpha/type "User"
       :juxt.pass.alpha/username "alx"
       :email "alx@juxt.pro"
       :name "Alex Davis"}]])

  ;; GraphQL query (direct, for EDN)
  (let [query "{ allUsers { id } }"
        json (json/write-value-as-string {:query query
                                          :operationName nil})
        bytes (.getBytes json)

        r (*handler*
            {:ring.request/method :post
             :ring.request/path "/_site/graphql"
             :ring.request/headers {"content-length" (str (count bytes))
                                    "content-type" "application/json"}
             :ring.request/body (ByteArrayInputStream. bytes)})]

    (is (= 200 (:ring.response/status r)))
    (is (= {"data"
            {"allUsers"
             [{"id" "https://example.org/_site/users/alx"}
              {"id" "https://example.org/_site/users/mal"}]}} (json/read-value (:ring.response/body r))))))

(deftest graphql-test
  (graphql))

#_((t/join-fixtures [with-xt with-handler])
   (fn []
     (submit-and-await!
       [[:xtdb.api/put
         {:xt/id "https://example.org/_site/users/mal",
          :juxt.site.alpha/type "User",
          :juxt.pass.alpha/username "mal",
          :email "mal@juxt.pro",
          :name "Malcolm Sparks"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/users/alx",
          :juxt.site.alpha/type "User",
          :juxt.pass.alpha/username "alx",
          :email "alx@juxt.pro",
          :name "Alex Davis"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/users/joa",
          :juxt.site.alpha/type "User",
          :juxt.pass.alpha/username "joa",
          :email "joa@juxt.pro",
          :name "Johanna Antonelli"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/roles/superuser",
          :juxt.site.alpha/type "Role",
          :name "superuser",
          :description "Superuser"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/roles/developer",
          :juxt.site.alpha/type "Role",
          :name "developer",
          :description "Developer"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/roles/superuser/users/mal",
          ::site/type "UserRoleMapping",
          ::pass/assignee "https://example.org/_site/users/mal",
          ::pass/role "https://example.org/_site/roles/superuser"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/roles/developer/users/mal",
          ::site/type "UserRoleMapping",
          ::pass/assignee "https://example.org/_site/users/mal",
          ::pass/role "https://example.org/_site/roles/developer"}]

        [:xtdb.api/put
         {:xt/id "https://example.org/_site/roles/superuser/users/joa",
          ::site/type "UserRoleMapping",
          ::pass/assignee "https://example.org/_site/users/joa",
          ::pass/role "https://example.org/_site/roles/developer"}]

        ])

     (let [schema-str (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))
           schema (schema/compile-schema (parser/parse schema-str))
           query "{ allUsers { id  name email roles { name } } }"
           document (document/compile-document (parser/parse query) schema)]

       (graphql/query schema document nil {} {::site/db (x/db *xt-node*)}))))


(defn add-body [m s ct]
  (let [bytes (.getBytes s)]
    (-> m
        (assoc-in [:ring.request/headers "content-length"] (str (count bytes)))
        (assoc-in [:ring.request/headers "content-type"] ct)
        (assoc :ring.request/body (java.io.ByteArrayInputStream. bytes)))))

(defn stored-query []
  (let [query "query { persons { name }}"]

    (submit-and-await!
      [[:xtdb.api/put access-all-areas]

       [:xtdb.api/put
        {:xt/id "https://example.org/alice"
         :type "Person"
         :name "Alice"}]

       [:xtdb.api/put
        {:xt/id "https://example.org/bob"
         :type "Person"
         :name "Bob"}]

       [:xtdb.api/put
        {:xt/id "https://example.org/graphql"
         :doc "A GraphQL endpoint"
         :juxt.http.alpha/methods #{:post :put :options}
         :juxt.http.alpha/acceptable "application/graphql"
         :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
         :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]

       [:xtdb.api/put
        {:xt/id "https://example.org/get-persons"
         :doc "A GraphQL stored query"
         :juxt.http.alpha/methods #{:put :post}
         :juxt.http.alpha/acceptable #{"application/graphql" "application/json"}
         :juxt.site.alpha/graphql-schema "https://example.org/graphql"
         :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/stored-document-put-handler
         :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}]

       ;; Install variants to have CSV output
       ])

    ;; Install a GraphQL schema at /graphql
    (let [schema "
type Query { persons: [Person] @site(q: { find: [e] where: [[e {keyword: \"type\"} \"Person\"]]})}
type Person { name: String @site(a: \"name\")}"
          response (*handler*
                     (-> {:ring.request/method :put
                          :ring.request/path "/graphql"}
                         (add-body schema "application/graphql")))]
      (is (= 204 (:ring.response/status response))))

    ;; POST a query to that schema
    (let [response
          (*handler*
            (-> {:ring.request/method :post
                 :ring.request/path "/graphql"}
                (add-body query "application/graphql")))
          body (json/read-value (:ring.response/body response))]

      (is (= 200 (:ring.response/status response)))
      (is (= {"data" {"persons" [{"name" "Bob"} {"name" "Alice"}]}} body)))

    ;; PUT a stored query
    (*handler*
      (-> {:ring.request/method :put
           :ring.request/path "/get-persons"}
          (add-body query "application/graphql")))

    ;; POST to a stored query
    #_(*handler*
        (-> {:ring.request/method :post
             :ring.request/path "/get-persons"}
            (add-body (json/write-value-as-string {}) "application/json")))))

#_((t/join-fixtures [with-xt with-handler])
   stored-query
   )

(defn stored-query-edn []
  (let [query "query { personsByKey(name: \"Bob\", key: \"name\") { name }}"]

    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/alice"
        :type "Person"
        :name "Alice"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/bob"
        :type "Person"
        :name "Bob"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/graphql"
        :doc "A GraphQL endpoint"
        :juxt.http.alpha/methods #{:post :put :options}
        :juxt.http.alpha/acceptable "application/graphql"
        :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
        :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]

      [:xtdb.api/put
       {:xt/id "https://example.org/get-persons"
        :doc "A GraphQL stored query"
        :juxt.http.alpha/methods #{:put :post}
        :juxt.http.alpha/acceptable #{"application/graphql" "application/json"}
        :juxt.site.alpha/graphql-schema "https://example.org/graphql"
        :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/stored-document-put-handler
        :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}]

       ;; Install variants to have CSV output
      ])

    ;; Install a GraphQL schema at /graphql
    (let [schema "
type Query { personsByKey(name: String, key: String): [Person] @site(q: {edn: \"\"\"{:find [e] :where [[e :type \"Person\"] [e :{{args.key}} \"{{args.name}}\"]]}\"\"\"})}
type Person { name: String @site(a: \"name\")}"
          response (*handler*
                    (-> {:ring.request/method :put
                         :ring.request/path "/graphql"}
                        (add-body schema "application/graphql")))]
      (is (= 204 (:ring.response/status response))))

    ;; POST a query to that schema
    (let [response
          (*handler*
           (-> {:ring.request/method :post
                :ring.request/path "/graphql"}
               (add-body query "application/graphql")))
          body (json/read-value (:ring.response/body response))]

      (is (= 200 (:ring.response/status response)))
      (is (= {"data" {"personsByKey" [{"name" "Bob"}]}} body)))

    ;; PUT a stored query
    (*handler*
     (-> {:ring.request/method :put
          :ring.request/path "/get-persons"}
         (add-body query "application/graphql")))

    ;; POST to a stored query
    #_(*handler*
       (-> {:ring.request/method :post
            :ring.request/path "/get-persons"}
           (add-body (json/write-value-as-string {}) "application/json")))))

(deftest stored-query-test
  (stored-query))


(deftest stored-query-edn-test
  (stored-query-edn))

;; For CSV output
(comment
  (json-pointer {"person" {"name" ["Alice" "Bob"]}} "/person/name"))

;; Schema stitching
(comment
  (schema/compile-schema (parser/parse "extend schema @site(import: \"/graphql\") type Query { person: Person } type Person { name: String }")))

(def straight-mutation-schema
  "
schema { query: Query mutation: Mutation }
type Query { person: Person }
type Person { id: ID @site(a: \"xt/id\") name: String }
type Mutation {
  addPerson(id: ID @site(a: \"xt/id\") name: String): Person
}
")

(def straight-query
  "
mutation {
  addPerson(
      id: \"https://example.org/persons/mal\"
      name: \"Malcolm Sparks\"
  ) { id name }
}
")

(def query-with-input
  "
mutation {
  addPerson(person: {
      id: \"https://example.org/persons/mal\"
      name: \"Malcolm Sparks\"})
  { id name }
}
")

(def valid-mutation-schema
  "
schema { query: Query mutation: Mutation }
type Query { person: Person }
type Person { id: ID @site(a: \"xt/id\") name: String }
input PersonInput { name: String! id: ID }

type Mutation {
  addPerson(
    person: PersonInput
  ): Person @site(
    validation: {
      person: \"[:map [:id {:optional true} :any] [:name [:string {:min 1 :max 20}]]]\"
    }
  )
}
")

(def invalid-mutation-schema
  "
schema { query: Query mutation: Mutation }
type Query { person: Person }
  type Person { id: ID @site(a: \"xt/id\") name: String }
input PersonInput { name: String! id: ID }

type Mutation {
  addPerson(
    person: PersonInput
  ): Person @site(
    validation: {
      person: \"[:map [:id {:optional true} :any] [:name [:string {:min 1 :max 5}]]]\"
    }
  )
}
")

(defn put-schema [schema]
  (*handler*
   (-> {:ring.request/method :put
        :ring.request/path "/graphql"}
       (add-body schema "application/graphql"))))

(defn post-mutation [query]
  (*handler*
   (-> {:ring.request/method :post
        :ring.request/path "/graphql"}
       (add-body query "application/graphql"))))

(defn mutation-base [schema query expected-response]
  (submit-and-await!
   [[:xtdb.api/put access-all-areas]

    [:xtdb.api/put
     {:xt/id "https://example.org/graphql"
      :doc "A GraphQL endpoint"
      :juxt.http.alpha/methods #{:post :put :options}
      :juxt.http.alpha/acceptable "application/graphql"
      :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
      :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]])

  ;; Install a GraphQL schema at /graphql
  (is (= 204 (:ring.response/status (put-schema schema))))

  ;; POST a mutation to that endpoint
  (let [response (post-mutation query)]

    (when (= (get-in response [:ring.response/status]) 500)
      (throw (ex-info "Unexpected error" {:response response})))

    (is (= 200 (:ring.response/status response)))

    response

    (when-not (= (get-in response [:ring.response/headers "content-type"])
                 "application/json")
      (throw (ex-info "Unexpected content-type"
                      {:content-type (get-in response [:ring.response/headers "content-type"])})))

    ;; Ensure mutation worked
    (let [db (xt/db *xt-node*)]
      (is (= {:xt/id "https://example.org/persons/mal"
              :juxt.site/type "Person"
              :name "Malcolm Sparks"}
             (-> (xt/entity db "https://example.org/persons/mal")
                 (dissoc :_siteCreatedAt)))))

    (let [body (json/read-value (:ring.response/body response))]
      (is (= expected-response body)))))

#_((t/join-fixtures [with-xt with-handler])
   mutation
 )

(deftest mutation-test
  (mutation-base

   ;; schema
   straight-mutation-schema

   ;; query
   straight-query

   ;; expected-response
   {"data"
    {"addPerson"
     {"id" "https://example.org/persons/mal"
      "name" "Malcolm Sparks"}}}))

(deftest mutation-with-validation-directive-test
  (mutation-base

   ;; schema
   valid-mutation-schema

   ;; query
   query-with-input

   ;; expected-response
   {"data"
    {"addPerson"
     {"id" "https://example.org/persons/mal"
      "name" "Malcolm Sparks"}}}))

(defn invalid-mutation-with-validation-directive []
  (let [schema invalid-mutation-schema
        query query-with-input]

    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/graphql"
        :doc "A GraphQL endpoint"
        :juxt.http.alpha/methods #{:post :put :options}
        :juxt.http.alpha/acceptable "application/graphql"
        :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
        :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]])

    ;; Install a GraphQL schema at /graphql
    (let [response (*handler*
                    (-> {:ring.request/method :put
                         :ring.request/path "/graphql"}
                        (add-body schema "application/graphql")))]
      (is (= 204 (:ring.response/status response))))

    ;; POST a mutation to that endpoint
    (let [response
          (*handler*
           (-> {:ring.request/method :post
                :ring.request/path "/graphql"}
               (add-body query "application/graphql")))]

      (when (= (get-in response [:ring.response/status]) 500)
        (throw (ex-info "Unexpected error" {:response response})))

      (is (= 200 (:ring.response/status response)))

      response

      (when-not (= (get-in response [:ring.response/headers "content-type"])
                   "application/json")
        (throw (ex-info "Unexpected content-type"
                        {:content-type (get-in response [:ring.response/headers "content-type"])})))

      ;; Ensure mutation worked
      (let [db (xt/db *xt-node*)]
        (is (= nil
               (xt/entity db "https://example.org/persons/mal"))))

      (let [error-message (-> response
                              :ring.response/body
                              json/read-value
                              (get "errors")
                              first
                              (get "message"))]

        (is (= error-message
               "({:name [\"should be between 1 and 5 characters\"]})"))))))

(deftest invalid-mutation-with-validation-directive-test
  (invalid-mutation-with-validation-directive))


#_(parser/parse "mutation { addPerson { name }}")

#_(schema/compile-schema
    (parser/parse
      "
schema { query: Query mutation: Mutation }
type Query { person: Person }
enum WorkerStatus { EMPLOYEE CONTIGENT }
type Person { id: ID name: String status: WorkerStatus }
scalar Date
type Holiday {
  beginning: Date!
  ending: Date!
  description: String
}
type Mutation {
  addPerson(id: ID! name: String! status: WorkerStatus! = EMPLOYEE): Person
  addHoliday(person: ID beginning: Date! ending: Date! description: String): Holiday
}
"
      ))

(def cascade-mutation-test-data
  "Data for evict-cascade and delete-cascade tests"
  [[:xtdb.api/put access-all-areas]

   [:xtdb.api/put
    {:xt/id "https://example.org/graphql"
     :doc "A GraphQL endpoint"
     :juxt.http.alpha/methods #{:post :put :options}
     :juxt.http.alpha/acceptable "application/graphql"
     :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
     :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]

   [:xtdb.api/put
    {:xt/id "https://example.org/persons/ts1"
     :type "Person"
     :name "Testuser 1"}]

   [:xtdb.api/put
    {:xt/id "https://example.org/persons/ts2"
     :type "Person"
     :name "Testuser 2"}]

   [:xtdb.api/put
    {:xt/id "https://example.org/house/hh1"
     :type "House"
     :name "House 1"
     :ownerId "https://example.org/persons/ts2"
     }]

   [:xtdb.api/put
    {:xt/id "https://example.org/house/hh2"
     :type "House"
     :name "House 2"
     :ownerId "https://example.org/persons/ts2"
     }]

   [:xtdb.api/put
    {:xt/id "https://example.org/house/hh3"
     :type "House"
     :name "House 3"
     :ownerId "https://example.org/persons/ts1"
     }]
   ])

(def cascade-schema
  "Schema for cascade-evict and cascade-delete tests"
  "schema {
             query: Query
             mutation: Mutation
           }
           type Query {
                 persons: [Person] @site(q: { find: [e] where: [[e {keyword: \"type\"} \"Person\"]]})
                 person( id: ID! ): Person
           }
           type Mutation {
                 CascadeEvictPerson(id: ID!): Person
                    @site(mutation: \"evict-cascade\"
                          cascadeKey: \"ownerId\")
                 CascadeDeletePerson(id: ID!): Person
                    @site(mutation: \"delete-cascade\"
                          cascadeKey: \"ownerId\")
           }
           type Person {
                 id: ID!
                 name: String @site(a: \"name\")
                 house: [House] @site(ref: \"houseId\")
           }
           type House {
                 id: ID!
                 name: String @site(a: \"name\")
                 owner: Person @site(ref: \"ownerId\")
           }")


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Delete cascade test ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def straight-delete-cascade-query
  "
mutation {
  CascadeDeletePerson(id: \"https://example.org/persons/ts2\"
              cascadeKey: \"https://example.org/persons/ts2\"
) { id }
}
")

(defn testing-delete-cascade []
  ;; Put data into DB
  (submit-and-await! cascade-mutation-test-data)
  ;; Install a GraphQL schema at /graphql
  (is (= 204 (:ring.response/status (put-schema cascade-schema))))

  ;; POST a query to that schema
  (comment
    (let [response (post-mutation "{ persons { id name } }")
         body (json/read-value (:ring.response/body response))]
     (is (= 200 (:ring.response/status response)))
     (is (= 2 (-> body (get-in ["data" "persons"]) count)))))

  ;; Testing that db contains all data
  (is (= [(x/q (x/db *xt-node*) '{:find [e] :where [[e :type "Person"]]})
          (x/q (x/db *xt-node*) '{:find [e]  :where [[e :type "House"]]})]
         [#{["https://example.org/persons/ts2"] ["https://example.org/persons/ts1"]}
          #{["https://example.org/house/hh2"] ["https://example.org/house/hh1"] ["https://example.org/house/hh3"]}]))

  ;; Delete entity xt/id: "https://example.org/persons/ts2" and all entities with ownerId: https://example.org/pesons/ts2
  (is (= 200 (:ring.response/status (post-mutation straight-delete-cascade-query))))

  ;; Testing that entity has been deleted (but entity history is preserved since it's not a complete data eviction)
  (let [entity "https://example.org/persons/ts2"]
    (is (and (nil? ((x/q (x/db *xt-node*) '{:find [e] :where [[e :type "Person"]]}) entity))
             (seq (x/entity-history (x/db *xt-node*) entity :desc)))))

  ;; Testing that linked entities have been deleted
  (let [linked-entities ["https://example.org/house/hh2" "https://example.org/house/hh3"]]
    (is (and (->> linked-entities
                    (map (comp (x/q (x/db *xt-node*) '{:find [e] :where [[e :type "House"]]}) vec))
                    (every? nil?))
             (->> linked-entities
                    (map #(x/entity-history (x/db *xt-node*) % :desc))
                    (every? seq))))))

(deftest delete-cascade-test
  (testing-delete-cascade))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Evict cascade test ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def straight-evict-cascade-query
  "
mutation {
  CascadeEvictPerson(id: \"https://example.org/persons/ts2\"
              cascadeKey: \"https://example.org/persons/ts2\"
) { id }
}
")

(defn testing-evict-cascade []
  ;; Put data into DB
  (submit-and-await! cascade-mutation-test-data)
  ;; Install a GraphQL schema at /graphql
  (is (= 204 (:ring.response/status (put-schema cascade-schema))))

  ;; Testing that db contains all data
  (is (= [(x/q (x/db *xt-node*) '{:find [e] :where [[e :type "Person"]]})
          (x/q (x/db *xt-node*) '{:find [e]  :where [[e :type "House"]]})]
         [#{["https://example.org/persons/ts2"] ["https://example.org/persons/ts1"]}
          #{["https://example.org/house/hh2"] ["https://example.org/house/hh1"] ["https://example.org/house/hh3"]}]))

  ;; Delete entity xt/id: "https://example.org/persons/ts2" and all entities with ownerId: https://example.org/pesons/ts2
    (is (= 200 (:ring.response/status (post-mutation straight-evict-cascade-query))))

  ;; Testing that entity and linked entities has been completely evicted, including all historical records
  (is (and (empty? (x/entity-history (x/db *xt-node*) "https://example.org/persons/ts2" :desc))
           (empty? (x/entity-history (x/db *xt-node*) "https://example.org/house/hh2" :desc))
           (empty? (x/entity-history (x/db *xt-node*) "https://example.org/house/hh1" :desc)))))

(deftest evict-cascade-test
  (testing-evict-cascade))

