;; Copyright © 2021, JUXT LTD.

(ns juxt.site.handler-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [clojure.tools.logging :as log]
   [crux.api :as x]
   [jsonista.core :as json]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
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
  (-> h/handler
      (h/wrap-responder)
      (h/wrap-error-handling)
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
   ::pass/target '[]
   ::pass/effect ::pass/allow})

(def access-all-apis
  {:crux.db/id "https://example.org/access-rule"
   ::site/description "A rule allowing access everything"
   ::site/type "Rule"
   ::pass/target '[[resource ::site/resource-provider :juxt.apex.alpha.openapi/openapi-path]]
   ::pass/effect ::pass/allow})

(deftest put-test
  (submit-and-await!
   [[:crux.tx/put access-all-apis]
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
   [[:crux.tx/put access-all-apis]
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


(deftest inject-path-parameter-with-forward-slash-test
  ;; PUT a project code of ABC/DEF (with Swagger) and ensure the / is
  ;; preserved. This test tests an edge case where we want a path parameter to contain a /.
  ((t/join-fixtures [with-crux with-handler])
   (fn []
     (log/trace "")
     (submit-and-await!
      [[:crux.tx/put access-all-apis]
       [:crux.tx/put
        {:crux.db/id "https://example.org/_site/apis/test/openapi.json"
         ::site/type "OpenAPI"
         :juxt.apex.alpha/openapi
         {"servers" [{"url" ""}]
          "paths"
          {"/things/{a}"
           {"parameters"
            [{"name" "a" "in" "path" "required" "true"
              "schema" {"type" "string" "pattern" "\\p{Alnum}+"}
              "x-juxt-site-inject-property" "juxt/code"}]
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
              "schema" {"type" "string" "pattern" "\\p{Alpha}+"}}
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

     (let [path (str"/things/" (java.net.URLEncoder/encode "ABC/DEF"))
           body (json/write-value-as-string {"name" "zip"})
           r (*handler*
              {:ring.request/method :put
               :ring.request/path path
               :ring.request/body (ByteArrayInputStream. (.getBytes body))
               :ring.request/headers
               {"content-length" (str (count body))
                "content-type" "application/json"}})
           db (x/db *crux-node*)]
       (is (= "/things/{a}" (get-in r [::site/resource :juxt.apex.alpha/openapi-path])))
       (is (= {:name "zip",
               :juxt/code "ABC/DEF",
               :crux.db/id "https://example.org/things/ABC%2FDEF"}
              (x/entity db (str "https://example.org" path))))))))

#_((t/join-fixtures [with-crux with-handler])
 (fn []
   (log/trace "")
   (submit-and-await!
    [[:crux.tx/put access-all-apis]
     [:crux.tx/put {:crux.db/id "https://example.org/users/sue"
                    ::site/type "User"
                    ::site/description "Sue should receive an email on every alert"
                    ::email "sue@example.org"
                    ::email? true}]
     [:crux.tx/put {:crux.db/id "https://example.org/users/brian"
                    ::site/type "User"
                    ::site/description "Brian doesn't want emails"
                    ::email "brian@example.org"
                    ::email? false}]
     [:crux.tx/put {:crux.db/id "https://example.org/roles/service-manager"
                    ::site/type "Role"
                    ::site/description "A service manager"}]
     [:crux.tx/put {:crux.db/id "https://example.org/users/sue-is-a-service-manager"
                    ::site/type "UserRoleMapping"
                    ::user "https://example.org/users/sue"
                    ::role "https://example.org/roles/service-manager"}]
     [:crux.tx/put {:crux.db/id "https://example.org/users/brian-is-a-service-manager"
                    ::site/type "UserRoleMapping"
                    ::user "https://example.org/users/brian"
                    ::role "https://example.org/roles/service-manager"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/_site/apis/test/openapi.json"
       ::site/type "OpenAPI"
       :juxt.apex.alpha/openapi
       {"servers" [{"url" ""}]
        "paths"
        {"/alerts/{id}"
         {"put"
          {"requestBody"
           {"content"
            {"application/json"
             {"schema"
              {"properties"
               {"juxt.site.alpha/type" {"type" "string"}}}}}}}}}}}]])

   (let [path "/alerts/123"
         body (json/write-value-as-string {"id" "123"
                                           "juxt.site.alpha/type" "Alert"
                                           "state" "unprocessed"})]

     ;; Put the first alert
     (log/trace "Logging alert!")
     (*handler*
      {:ring.request/method :put
       :ring.request/path path
       :ring.request/body (ByteArrayInputStream. (.getBytes body))
       :ring.request/headers
       {"content-length" (str (count body))
        "content-type" "application/json"}})

     (let [db (x/db *crux-node*)]
       (doseq [[recipient alert]
               (x/q db '{:find [(eql/project user [:crux.db/id ::email]) alert]
                         :where [[alert ::site/type "Alert"]
                                 [user ::site/type "User"]
                                 [user ::email? true]
                                 [mapping ::role "https://example.org/roles/service-manager"]
                                 [mapping ::user user]
                                 (not-join [user alert]
                                           [mr ::site/type "MailRecord"]
                                           [mr ::recipient user]
                                           [mr ::about alert])]})]

         ;; Send email to recipient (::email recipient)
         (log/tracef "Send email to %s (id=%s)" (::email recipient) (:crux.db/id recipient))

         (->>
          (x/submit-tx
           *crux-node*
           [[:crux.tx/put {:crux.db/id "https://example.org/maillogs/123"
                           ::site/type "MailRecord"
                           ::recipient (:crux.db/id recipient)
                           ::about alert}]])
          (x/await-tx *crux-node*))))

     ;; Put a repeat alert
     (log/trace "Logging repeat alert!")
     (*handler*
        {:ring.request/method :put
         :ring.request/path path
         :ring.request/body (ByteArrayInputStream. (.getBytes body))
         :ring.request/headers
         {"content-length" (str (count body))
          "content-type" "application/json"}})


     (let [db (x/db *crux-node*)]

       (x/q db '{:find [(eql/project mr [*])]
                 :where [[mr ::site/type "MailRecord"]
                         ]})

       (doseq [[recipient alert]
               (x/q db '{:find [(eql/project user [::email]) alert]
                         :where [[alert ::site/type "Alert"]
                                 [user ::site/type "User"]
                                 [user ::email? true]
                                 [mapping ::role "https://example.org/roles/service-manager"]
                                 [mapping ::user user]
                                 (not-join [user alert]
                                           [mr ::site/type "MailRecord"]
                                           [mr ::recipient user]
                                           [mr ::about alert])]})]

         ;; Send email to recipient (::email recipient)
         (log/tracef "Send email to %s" (::email recipient)))))))

(deftest if-modified-since-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]
    [:crux.tx/put
     {:crux.db/id "https://example.org/test.png"
      ::http/last-modified #inst "2020-03-01"
      ::http/content-type "image/png"
      ::http/methods #{:get}}]])
  (are [if-modified-since status]
      (= status
         (:ring.response/status
          (*handler*
           {:ring.request/method :get
            :ring.request/headers
            (if if-modified-since
              {"if-modified-since"
               (format-http-date if-modified-since)}
              {})
            :ring.request/path "/test.png"})))
      nil 200
      #inst "2020-02-29" 200
      #inst "2020-03-01" 304
      #inst "2020-03-02" 304))

(deftest if-none-match-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]
    [:crux.tx/put
     {:crux.db/id "https://example.org/test.png"
      ::http/etag "\"abc\""
      ::http/content-type "image/png"
      ::http/methods #{:get :head :options}}]])
  (are [if-none-match status]
      (= status
         (:ring.response/status
          (*handler*
           {:ring.request/method :get
            :ring.request/headers
            (if if-none-match {"if-none-match" if-none-match} {})
            :ring.request/path "/test.png"})))
      nil 200
      "" 200
      "def" 200
      "abc" 200
      "\"abc\"" 304))

;; TODO: If-Match, If-Unmodified-Since

;; 3.1: If-Match
((t/join-fixtures [with-crux with-handler])
 (fn []
   (submit-and-await!
    [[:crux.tx/put access-all-areas]
     [:crux.tx/put
      {:crux.db/id "https://example.org/test.png"
       ::site/type "StaticRepresentation"
       ::http/etag "\"abc\""
       ::http/content-type "image/png"
       ::http/methods #{:put}
       }]])
   (:ring.response/status
    (let [body "Hello"]
      (*handler*
       {:ring.request/method :put
        :ring.request/body (ByteArrayInputStream. (.getBytes body))
        :ring.request/headers
        {"content-length" (str (count body))
         "content-type" "application/json"
         "if-match" "\"def\""}
        :ring.request/path "/test.png"})))))

;; TODO:
      ;; "The server generating a 304 response MUST generate any of the following
      ;; header fields that would have been sent in a 200 (OK) response to the
      ;; same request: Cache-Control, Content-Location, Date, ETag, Expires, and
      ;; Vary." -- Section 4.1, RFC 7232

;; TODO: Call eval-conditional-requests on put/post

;; TODO: Try fix bug with DELETE (producing 415 not 204)

;; TODO: Error representations

;; TODO: eval-conditional-requests in a transaction function to avoid race-conditions

;; TODO: Security headers - read latest OWASP and similar

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
