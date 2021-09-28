;; Copyright © 2021, JUXT LTD.

(ns juxt.site.graphql-test
  (:require
   [crux.api :as x]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.grab.alpha.parser :as parser]
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-crux with-handler submit-and-await!
                           *crux-node* *handler*
                           access-all-areas access-all-apis]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [crux.api :as xt]
   [clojure.string :as str])
  (:import (java.io ByteArrayInputStream)))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(t/use-fixtures :each with-crux with-handler)

(deftest graphql-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]
    [:crux.tx/put
     (edn/read-string (str/replace
                       (slurp "opt/graphiql/resources.edn")
                       "{{base-uri}}" "https://example.org"))]])

  ;; Install schema (via HTTP, for accuracy)
  (let [schema (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))
        bytes (.getBytes schema)

        r (*handler*
           {:ring.request/method :put
            :ring.request/path "/graphql"
            :ring.request/headers {"content-length" (str (count bytes))}
            :ring.request/body (ByteArrayInputStream. bytes)
            })]
    (is (= 204 (:ring.response/status r))))

  ;; GraphQL query (direct, for EDN)
  (let [query "{ allUsers { id } }"
        bytes (.getBytes query)

        r (*handler*
           {:ring.request/method :post
            :ring.request/path "/graphql"
            :ring.request/headers {"content-length" (str (count bytes))}
            :ring.request/body (ByteArrayInputStream. bytes)})]

    (is (= 200 (:ring.response/status r)))))



((t/join-fixtures [with-crux with-handler])
 (fn []
   (submit-and-await!
    [[:crux.tx/put
      {:crux.db/id "https://example.org/_site/users/mal",
       :juxt.site.alpha/type "User",
       :juxt.pass.alpha/username "mal",
       :email "mal@juxt.pro",
       :name "Malcolm Sparks"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/users/alx",
       :juxt.site.alpha/type "User",
       :juxt.pass.alpha/username "alx",
       :email "alx@juxt.pro",
       :name "Alex Davis"}]])

   (let [schema-str (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))
         schema (schema/compile-schema (parser/parse schema-str))
         query "{ allUsers { id name } }"
         document (document/compile-document (parser/parse query) schema)]

     (graphql/query schema document nil (x/db *crux-node*)))))

((t/join-fixtures [with-crux with-handler])
 (fn []
   (submit-and-await!
    [[:crux.tx/put
      {:crux.db/id "https://example.org/_site/users/mal",
       :juxt.site.alpha/type "User",
       :juxt.pass.alpha/username "mal",
       :email "mal@juxt.pro",
       :name "Malcolm Sparks"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/users/alx",
       :juxt.site.alpha/type "User",
       :juxt.pass.alpha/username "alx",
       :email "alx@juxt.pro",
       :name "Alex Davis"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/users/joa",
       :juxt.site.alpha/type "User",
       :juxt.pass.alpha/username "joa",
       :email "joa@juxt.pro",
       :name "Johanna Antonelli"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/roles/superuser",
       :juxt.site.alpha/type "Role",
       :name "superuser",
       :description "Superuser"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/roles/developer",
       :juxt.site.alpha/type "Role",
       :name "developer",
       :description "Developer"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/roles/superuser/users/mal",
       ::site/type "UserRoleMapping",
       ::pass/assignee "https://example.org/_site/users/mal",
       ::pass/role "https://example.org/_site/roles/superuser"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/roles/developer/users/mal",
       ::site/type "UserRoleMapping",
       ::pass/assignee "https://example.org/_site/users/mal",
       ::pass/role "https://example.org/_site/roles/developer"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/roles/superuser/users/joa",
       ::site/type "UserRoleMapping",
       ::pass/assignee "https://example.org/_site/users/joa",
       ::pass/role "https://example.org/_site/roles/developer"}]

     ])

   (let [schema-str (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))
         schema (schema/compile-schema (parser/parse schema-str))
         query "{ allUsers { id  name email roles { name } } }"
         document (document/compile-document (parser/parse query) schema)]

     (graphql/query schema document nil (x/db *crux-node*)))))
