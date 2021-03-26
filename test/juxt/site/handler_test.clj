;; Copyright © 2021, JUXT LTD.

(ns juxt.site.handler-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [clojure.tools.logging :as log]
   [crux.api :as x]
   [jsonista.core :as json]
   [juxt.site.alpha.handler :as h])
  (:import
   (crux.api ICruxAPI)
   (java.io ByteArrayInputStream)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(def ^:dynamic *opts* {})
(def ^:dynamic ^ICruxAPI *crux-node*)
(def ^:dynamic *handler*)
(def ^:dynamic *db*)

(defn with-crux [f]
  (with-open [node (x/start-node *opts*)]
    (binding [*crux-node* node]
      (f))))

(defn submit-and-await! [transactions]
  (->>
   (x/submit-tx *crux-node* transactions)
   (x/await-tx *crux-node*)))

(defn make-handler [opts]
  (-> h/inner-handler
      (h/wrap-initialize-request opts)))

(defn with-handler [f]
  (binding [*handler* (make-handler
                       {::site/crux-node *crux-node*
                        ::site/base-uri "https://example.org"
                        ::site/uri-prefix "https://example.org"})]
    (f)))

(defn with-db [f]
  (binding [*db* (x/db *crux-node*)]
    (f)))

(t/use-fixtures :once with-crux with-handler)

(def access-all-areas
  {:crux.db/id "https://example.org/access-rule"
   ::site/description "A rule allowing access everything"
   ::site/type "Rule"
   ::pass/target '[[resource ::site/resource-provider :juxt.apex.alpha.openapi/openapi-path]]
   ::pass/effect ::pass/allow})

(deftest put-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]
    [:crux.tx/put
     {:crux.db/id "https://example.org/_site/apis/test/openapi.json"
      ::site/type "OpenAPI"
      :juxt.apex.alpha/openapi
      {"servers" [{"url" ""}]
       "paths"
       {"/things/foo"
        {"put"
         {"requestBody"
          {"content"
           {"application/json"
            {"schema"
             {"juxt.jinx.alpha/keyword-mappings"
              {"name" "a/name"}
              "properties"
              {"name" {"type" "string"
                       "minLength" 2}}}}}}}}}}}]])
  (let [body (json/write-value-as-string {"name" "foo"})
        _ (*handler*
           {:ring.request/method :put
            :ring.request/path "/things/foo"
            :ring.request/body (ByteArrayInputStream. (.getBytes body))
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/json"}})
        db (x/db *crux-node*)]

    (is (= {:a/name "foo", :crux.db/id "https://example.org/things/foo"}
           (x/entity db "https://example.org/things/foo")))))

;; Evoke "Throwing Multiple API paths match"

(deftest two-path-parameter-path-preferred-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]
    [:crux.tx/put
     {:crux.db/id "https://example.org/_site/apis/test/openapi.json"
      ::site/type "OpenAPI"
      :juxt.apex.alpha/openapi
      {"servers" [{"url" ""}]
       "paths"
       {"/things/{a}"
        {"parameters"
         [{"name" "a" "in" "path" "required" "true"
           "schema" {"type" "string" "pattern" "\\p{Alnum}+"}}]
         "put"
         {"operationId" "putA"
          "requestBody"
          {"content"
           {"application/json"
            {"schema"
             {"properties"
              {"name" {"type" "string" "minLength" 1}}}}}}}}

        "/things/{a}/{b}"
        {"parameters"
         [{"name" "a" "in" "path" "required" "true"
           "schema" {"type" "string" "pattern" "\\p{Alnum}+"}}
          {"name" "b" "in" "path" "required" "true"
           "schema" {"type" "string" "pattern" "\\p{Alnum}+"}}]
         "put"
         {"operationId" "putAB"
          "requestBody"
          {"content"
           {"application/json"
            {"schema"
             {"properties"
              {"name" {"type" "string" "minLength" 1}}}}}}}}}}}]])
  (let [body (json/write-value-as-string {"name" "foo"})
        r (*handler*
           {:ring.request/method :put
            ;; Matches both {a} and {b}
            :ring.request/path "/things/foo/bar"
            :ring.request/body (ByteArrayInputStream. (.getBytes body))
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/json"}})
        db (x/db *crux-node*)]
    (is (= "putAB"
           (get-in r [::site/resource ::site/request-locals ::apex/operation "operationId"])))))


;; Continue with trying to post a project code of ABC/DEF (with Swagger) and ensure the / is
;; preserved.
((t/join-fixtures [with-crux with-handler])
 (fn []
   (log/trace "")
   (submit-and-await!
    [[:crux.tx/put access-all-areas]
     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/apis/test/openapi.json"
       ::site/type "OpenAPI"
       :juxt.apex.alpha/openapi
       {"servers" [{"url" ""}]
        "paths"
        {"/things/{a}"
         {"parameters"
          [{"name" "a" "in" "path" "required" "true"
            "schema" {"type" "string" "pattern" "\\p{Alnum}+"}}]
          "put"
          {"operationId" "putA"
           "requestBody"
           {"content"
            {"application/json"
             {"schema"
              {"properties"
               {"name" {"type" "string" "minLength" 1}}}}}}}}

         "/things/{a}/{n}"
         {"parameters"
          [{"name" "a" "in" "path" "required" "true"
            "schema" {"type" "string" "pattern" "\\p{Alpha}+"}
            "x-juxt-site-inject-property" "juxt/a"}
           {"name" "n" "in" "path" "required" "true"
            "schema" {"type" "string"}}]
          "put"
          {"operationId" "putAB"
           "requestBody"
           {"content"
            {"application/json"
             {"schema"
              {"properties"
               {"name" {"type" "string" "minLength" 1}}}}}}}}}}}]])

   (let [path "/things/abc/123"
         body (json/write-value-as-string {"name" "foo"})
         r (*handler*
            {:ring.request/method :put
             ;; Matches both {a} and {b}
             :ring.request/path path
             :ring.request/body (ByteArrayInputStream. (.getBytes body))
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/json"}})
         db (x/db *crux-node*)]
     r
     (x/entity db (str "https://example.org" path))
     )))




;; First we need to 'fix' this error, and have a strategy for choosing the right
;; path.

;; Apparently path-level parameters are not yet supported - let's fix that
;; first. Update: they ARE detected and parsed at the resource-locator stage,
;; and put into :juxt.apex.alpha/openid-path-params. But what about later
;; stages?

;; Do some testing against various parameter forms, including url encoded
;; strings (which should arguably be url decoded before being parsed/validated).


#_(deftest get-with-if-none-match-test
    (let [{status1 :status
           {:strs [etag] :as headers1} :headers}
          (demo/handler
           {:uri "/en/index.html"
            :request-method :get})
          {status2 :status headers2 :headers}
          (demo/handler
           {:uri "/en/index.html"
            :request-method :get
            :headers {"if-none-match" etag}})]
      (is (= 200 status1))
      (is (= #{"date" "content-type" "etag" "last-modified" "content-language" "content-length"}
             (set (keys headers1))))
      (is (= 304 status2))
      ;; "The server generating a 304 response MUST generate any of the following
      ;; header fields that would have been sent in a 200 (OK) response to the
      ;; same request: Cache-Control, Content-Location, Date, ETag, Expires, and
      ;; Vary." -- Section 4.1, RFC 7232
      (is (= #{"date" "etag"} (set (keys headers2)))))

    ;; Check that content-location and vary are also generated, when they would be
    ;; for a 200.
    (let [{status1 :status
           headers1 :headers}
          (demo/handler
           {:uri "/index.html"
            :request-method :get})
          {status2 :status
           headers2 :headers}
          (demo/handler
           {:uri "/index.html"
            :request-method :get
            :headers {"if-none-match" "\"1465419893\""}})]

      (is (= 200 status1))
      (is (= #{"date" "etag" "vary" "content-location"
               "content-type" "content-length" "last-modified" "content-language"}
             (set (keys headers1))))
      (is (= 304 status2))
      (is (= #{"date" "etag" "vary" "content-location"} (set (keys headers2))))
      ;; TODO: test for cache-control & expires
      ))
