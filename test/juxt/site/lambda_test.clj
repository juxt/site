;; Copyright © 2021, JUXT LTD.

(ns juxt.site.lambda-test
  (:require [xtdb.api :as x]
            [jsonista.core :as json]
            [juxt.grab.alpha.schema :as schema]
            [juxt.grab.alpha.document :as document]
            [juxt.grab.alpha.execution :refer [execute-request]]
            [juxt.site.alpha.graphql :as graphql]
            [juxt.grab.alpha.parser :as parser]
            [clojure.test :refer [deftest is are testing] :as t]
            [juxt.test.util :refer [with-xt with-handler submit-and-await!
                                    *xt-node* *handler* access-all-areas]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [xtdb.api :as xt]
            [clojure.string :as str]
            [juxt.jinx.alpha.jsonpointer :refer [json-pointer]])
  (:import (java.io ByteArrayInputStream)))

(t/use-fixtures :each with-xt with-handler)

(defn- request [method body]
  (let [request-map {:ring.request/method method
                     :ring.request/path "/graphql"}
        content-type "application/graphql"
        as-bytes (.getBytes body)
        request-fn *handler*]
    (request-fn
     (-> request-map
         (assoc-in [:ring.request/headers "content-length"] (str (count as-bytes)))
         (assoc-in [:ring.request/headers "content-type"] content-type)
         (assoc :ring.request/body (java.io.ByteArrayInputStream. as-bytes))))))

(defn- post [body] (request :post body))
(defn- put [body] (request :put body))

(deftest stored-query-test
  (submit-and-await!
   [[:xtdb.api/put access-all-areas]
    [:xtdb.api/put {:xt/id "https://example.org/n"
                    :type "LambdaArg"
                    :name "n"
                    :lambdaArgType "long"}]
    [:xtdb.api/put {:xt/id "https://example.org/inc/1"
                    :type "LambdaInstance"
                    :lambdaInstanceType "SYNC"}]
    [:xtdb.api/put {:xt/id "https://example.org/inc"
                    :type "Lambda"
                    :name "inc"
                    :lambdaArgs ["https://example.org/n"]
                    :lambdaIntances ["https://example.org/inc/1"]
                    :lambdaContent "(fn [n] (inc n))"}]
    [:xtdb.api/put {:xt/id "https://example.org/graphql"
                    :doc "A GraphQL endpoint"
                    :juxt.http.alpha/methods #{:post :put :options}
                    :juxt.http.alpha/acceptable "application/graphql"
                    :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
                    :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler}]
    [:xtdb.api/put {:xt/id "https://example.org/lambdas"
                    :doc "A GraphQL stored query"
                    :juxt.http.alpha/methods #{:put :post}
                    :juxt.http.alpha/acceptable #{"application/graphql" "application/json"}
                    :juxt.site.alpha/graphql-schema "https://example.org/graphql"
                    :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/stored-document-put-handler
                    :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}]])
  (let [schema (slurp "opt/lambda/schema.graphql")]
    (is (= 204 (:ring.response/status (put schema)))))
  (let [response (post "query { lambdas { name }}")]
    (is (= 200 (:ring.response/status response)))
    (is (= {"data" {"lambdas" [{"name" "inc"}]}}
           (json/read-value (:ring.response/body response))))))

(defn test-resolve [m]
  (println "###" m))
