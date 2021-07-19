;; Copyright © 2021, JUXT LTD.

(ns juxt.site.handler-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [clojure.tools.logging :as log]
   [crux.api :as x]
   [jsonista.core :as json]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.mail.alpha.mail :as mailer]
   [juxt.test.util :refer [with-crux with-handler submit-and-await!
                           *crux-node* *handler*
                           access-all-areas access-all-apis]])
  (:import
   (java.io ByteArrayInputStream)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-crux with-handler)

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
           (->
            (x/entity db "https://example.org/things/foo")
            (dissoc ::site/request))))))

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
           (get-in r [::site/resource ::apex/operation "operationId"])))))


(deftest inject-path-parameter-with-forward-slash-test
  ;; PUT a project code of ABC/DEF (with Swagger) and ensure the / is
  ;; preserved. This test tests an edge case where we want a path parameter to contain a /.
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
           ;; TODO: Why doesn't this pattern allow a / ? It should!
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
           (->
            (x/entity db (str "https://example.org" path))
            (dissoc ::site/request))))))

(deftest triggers-test
  (log/trace "TESTING")
  (submit-and-await!
   [[:crux.tx/put access-all-apis]

    [:crux.tx/put {:crux.db/id "https://example.org/users/sue"
                   ::site/type "User"
                   ::site/description "Sue should receive an email on every alert"
                   :email "sue@example.org"
                   ::email? true}]
    [:crux.tx/put {:crux.db/id "https://example.org/users/brian"
                   ::site/type "User"
                   ::site/description "Brian doesn't want emails"
                   :email "brian@example.org"
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
              {"juxt.site.alpha/type" {"type" "string"}}}}}}}}}}}]

    [:crux.tx/put
     {:crux.db/id "https://example.org/templates/alert-notification.html"
      ::http/content "<h1>Alert</h1><p>There has been an alert. See {{ :href }}</p>"}]

    [:crux.tx/put
     {:crux.db/id "https://example.org/templates/alert-notification.txt"
      ::http/content "There has been an alert. See {{ :href }}"}]

    [:crux.tx/put
     {:crux.db/id "https://example.org/triggers/alert-notification"
      ::site/type "Trigger"
      ::site/query
      '{:find [(pull user [:email]) alert asset-type customer]
        :keys [user href asset-type customer]
        :where [[request :ring.request/method :put]
                [request ::site/uri alert]
                [alert ::site/type "Alert"]
                [alert :asset-type asset-type]
                [alert :customer customer]
                [user ::site/type "User"]
                [mapping ::role "https://example.org/roles/service-manager"]
                [mapping ::user user]]}

      ::site/action ::mail/send-emails
      ::mail/from "notifications@example.org"
      ::mail/subject "{{:asset-type}} Alert!"
      ::mail/html-template "https://example.org/templates/alert-notification.html"
      ::mail/text-template "https://example.org/templates/alert-notification.txt"}]])

  (let [path "/alerts/123"
        body (json/write-value-as-string
              {"id" "123"
               "juxt.site.alpha/type" "Alert"
               "state" "unprocessed"
               "asset-type" "Heart Monitor"
               "customer" "Mountain Ridge Hospital"})
        emails (atom [])]

    (with-redefs
      [mailer/send-mail!
       (fn [_ from to subject _ _]
         (swap! emails conj {:from from :to to :subject subject}))]

      (*handler*
       {:ring.request/method :put
        :ring.request/path path
        :ring.request/body (ByteArrayInputStream. (.getBytes body))
        :ring.request/protocol "HTTP/1.1"
        :ring.request/headers
        {"content-length" (str (count body))
         "content-type" "application/json"}}))

    (is (= "123" (:id (x/entity (x/db *crux-node*) "https://example.org/alerts/123"))))
    (is (= [{:from "notifications@example.org"
             :to "brian@example.org"
             :subject "Heart Monitor Alert!"}
            {:from "notifications@example.org"
             :to "sue@example.org"
             :subject "Heart Monitor Alert!"}] @emails))))

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

;; TODO: If-Unmodified-Since

;; 3.1: If-Match
(deftest if-match-wildcard-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]])
  (is (= 412
         (:ring.response/status
          (let [body "Hello"]
            (*handler*
             {:ring.request/method :put
              :ring.request/body (ByteArrayInputStream. (.getBytes body))
              :ring.request/headers
              {"content-length" (str (count body))
               "content-type" "application/json"
               "if-match" "*"}
              :ring.request/path "/test.png"}))))))

(defn if-match-run [if-match]
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
        "content-type" "image/png"
        "if-match" if-match}
       :ring.request/path "/test.png"}))))

(deftest if-match-1-test
  (is (= 204 (if-match-run "\"abc\""))))

(deftest if-match-2-test
  (is (= 204 (if-match-run "\"abc\", \"def\""))))

(deftest if-match-3-test
  (is (= 412 (if-match-run "\"def\", \"ghi\""))))

(deftest redirect-test
  (submit-and-await!
    [[:crux.tx/put
      {:crux.db/id "https://example.org/"
       ::site/type "Redirect"
       ::site/location "/test.html"}]])

   (let [response (*handler*
                   {:ring.request/method :get
                    :ring.request/path "/"})]
     (is (= 302 (:ring.response/status response)))
     (is (= "/test.html" (get-in response [:ring.response/headers "location"])))))

(deftest content-negotiation-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]

    [:crux.tx/put
     {:crux.db/id "https://example.org/report"
      ::http/methods #{:get :head :options}
      ::http/representations
      #{{::http/content-type "text/html;charset=utf-8"
         ::http/content "<h1>Latest sales figures</h1>"}
        {::http/content-type "text/plain;charset=utf-8"
         ::http/content "Latest sales figures"}}}]])

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "text/html"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "<h1>Latest sales figures</h1>" (:ring.response/body response))))

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "text/plain"}})]

    (is (= 200 (:ring.response/status response)))
    (is (= "Latest sales figures" (:ring.response/body response))))

  ;; TODO: Enable test when 406 is re-instated
  #_(let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "image/png"}})]
    (is (= 406 (:ring.response/status response)))))

(deftest variants-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]

    [:crux.tx/put
     {:crux.db/id "https://example.org/report"
      ::http/methods #{:get :head :options}}]

    [:crux.tx/put
     {:crux.db/id "https://example.org/report.html"
      ::site/variant-of "https://example.org/report"
      ::http/content-type "text/html;charset=utf-8"
      ::http/content "<h1>Latest sales figures</h1>"}]

    [:crux.tx/put
     {:crux.db/id "https://example.org/report.txt"
      ::site/variant-of "https://example.org/report"
      ::http/content-type "text/plain;charset=utf-8"
      ::http/content "Latest sales figures"}]])

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "text/html"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "<h1>Latest sales figures</h1>" (:ring.response/body response))))

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "text/plain"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "Latest sales figures" (:ring.response/body response))))

  ;; TODO: Enable test when 406 is re-instated
  #_(let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "image/png"}})]
    (is (= 406 (:ring.response/status response)))))

#_((t/join-fixtures [with-crux with-handler])
 (fn []
   (submit-and-await!
    [ ;;[:crux.tx/put access-all-areas]
     [:crux.tx/put
      {:crux.db/id "https://example.org/sensitive-report.html"
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Latest sales figures"
       ::http/methods #{:get :head :options}}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/401.html"
       ::site/type "ErrorRepresentation"
       ::http/status #{401 403}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "<h1>Unauthorized or Forbidden</h1>"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/401.txt"
       ::site/type "ErrorRepresentation"
       ::http/status #{401}
       ::http/content-type "text/plain;charset=utf-8"
       ::http/content "Unauthorized"}]

     [:crux.tx/put
      {:crux.db/id "https://example.org/406.html"
       ::site/type "ErrorRepresentation"
       ::http/status #{406}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "<h1>Unacceptable</h1>"
       ::http/methods #{:get :head :options}}]])

   (let [db (x/db *crux-node*)]
     (x/q db '{:find [er]
               :where [[er ::site/type "ErrorRepresentation"]
                       [er ::http/status 403]]}))

   (*handler*
    {:ring.request/method :get
     :ring.request/path "/sensitive-report.html"
     :ring.request/headers {"accept" "text/html"}})))

#_((t/join-fixtures [with-crux with-handler])
 (fn []
   (submit-and-await!
    [[:crux.tx/put access-all-areas]
     [:crux.tx/put
      {:crux.db/id "https://example.org/report.html"
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Latest figures"
       ::http/methods #{:get :head :options}
       ::http/cache-directives #{:no-store}
       }]])

   (:ring.response/headers
    (*handler*
     {:ring.request/method :get
      :ring.request/path "/report.html"}))))

;; TODO:
      ;; "The server generating a 304 response MUST generate any of the following
      ;; header fields that would have been sent in a 200 (OK) response to the
      ;; same request: Cache-Control, Content-Location, Date, ETag, Expires, and
      ;; Vary." -- Section 4.1, RFC 7232

;; TODO: Call eval-conditional-requests on put/post
;; TODO: eval-conditional-requests in a transaction function to avoid race-conditions
;; TODO: Security headers - read latest OWASP and similar

#_(deftest app-test
  (submit-and-await!
   [[:crux.tx/put access-all-areas]

    [:crux.tx/put
     {:crux.db/id "https://example.org/view/index.html"
      ::http/methods #{:get}
      ::http/content-type "text/html;charset=utf-8"
      ::http/content "<h1>Hello!</h1>"}]])

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/view/index.html"
          :ring.request/headers {"accept" "text/html"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "<h1>Hello!</h1>" (:ring.response/body response)))))
