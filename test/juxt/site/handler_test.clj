;; Copyright © 2021, JUXT LTD.

(ns juxt.site.handler-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [clojure.tools.logging :as log]
   [xtdb.api :as x]
   [jsonista.core :as json]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           install-test-resources!]]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.site.alpha.locator :as locator]
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [xtdb.api :as xt])
  (:import
   (java.io ByteArrayInputStream)))

(t/use-fixtures :each with-xt with-handler)

(deftest put-test
  (install-test-resources!)
  (submit-and-await!
   [
    [::xt/put
     {:xt/id "https://example.org/_site/actions/put-things"
      ::site/type "Action"
      :juxt.pass.alpha.malli/args-schema
      [:tuple
       [:map
        [:a/name :string]]]

      :juxt.pass.alpha/process
      [
       [:juxt.pass.alpha.malli/validate]
       [:xtdb.api/put]]

      ::pass/rules
      '[
        [(allowed? permission subject action resource)
         [permission ::pass/subject subject]
         [(nil? resource)]]]}]

    [::xt/put
     {:xt/id "https://example.org/_site/permissions/put-things"
      ::site/type "Permission"
      ::pass/subject :tester
      ::pass/action "https://example.org/_site/actions/put-things"
      ::pass/purpose nil}]])

  (submit-and-await!
    [
     [:xtdb.api/put
      {:xt/id "https://example.org/_site/apis/test/openapi.json"
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
                        "minLength" 2}}}}}}
           "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}}}}]])

  (let [body (json/write-value-as-string {"name" "foo"})
        _ (*handler*
           {::pass/subject :tester
            :ring.request/method :put
            :ring.request/path "/things/foo"
            :ring.request/body (ByteArrayInputStream. (.getBytes body))
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/json"}})
        db (x/db *xt-node*)]

    (is (= {:a/name "foo", :xt/id "https://example.org/things/foo"}
           (->
            (x/entity db "https://example.org/things/foo")
            (dissoc ::site/request))))))

#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (install-test-resources!)

   (submit-and-await!
    [
     [::xt/put
      {:xt/id "https://example.org/_site/actions/put-things"
       ::site/type "Action"
       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:a/name :string]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       ::pass/rules
       '[
         [(allowed? permission subject action resource)
          [permission ::pass/subject subject]
          [(nil? resource)]]]}]

     [::xt/put
      {:xt/id "https://example.org/_site/permissions/put-things"
       ::site/type "Permission"
       ::pass/subject :tester
       ::pass/action "https://example.org/_site/actions/put-things"
       ::pass/purpose nil}]])

   (submit-and-await!
    [
     [:xtdb.api/put
      {:xt/id "https://example.org/_site/apis/test/openapi.json"
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
                        "minLength" 2}}}}}}
           "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}}}}]

     ])

   #_(let [body (json/write-value-as-string {"name" "foo"})
         req {::site/xt-node *xt-node*
              ::site/db (xt/db *xt-node*)
              ::site/base-uri "https://example.org"
              ::site/uri-prefix "https://example.org"
              ::site/uri "https://example.org/things/foo"
              ::pass/subject :tester
              :ring.request/method :put
              :ring.request/body (ByteArrayInputStream. (.getBytes body))
              :ring.request/headers
              {"content-length" (str (count body))
               "content-type" "application/json"}
              }]
     (locator/locate-resource req))

   #_(authz/check-permissions
    (xt/db *xt-node*)
    #{"https://example.org/_site/actions/put-things"}
    (cond-> {:subject :tester}
      ;; When the resource is in the database, we can add it to the
      ;; permission checking in case there's a specific permission for
      ;; this resource.
      ;;(:xt/id resource) (assoc :resource (:xt/id resource))
      ))

   #_(xt/entity (xt/db *xt-node*) "https://example.org/_site/actions/put-things")

   (let [body (json/write-value-as-string {"name" "foo"})
         response
         (*handler*
          {::pass/subject :tester
           :ring.request/method :put
           :ring.request/path "/things/foo"
           :ring.request/body (ByteArrayInputStream. (.getBytes body))
           :ring.request/headers
           {"content-length" (str (count body))
            "content-type" "application/json"}})]

     response

     (x/entity (xt/db *xt-node*) "https://example.org/things/foo")

     (is (= {:a/name "foo", :xt/id "https://example.org/things/foo"}
              (->
               (x/entity db "https://example.org/things/foo")
               (dissoc ::site/request)))))

   #_(let [body "Hello"]
       (*handler*
        {:ring.request/method :put
         :ring.request/body (ByteArrayInputStream. (.getBytes body))
         :ring.request/headers
         {"content-length" (str (count body))
          "content-type" "application/json"
          "if-match" "*"}
         :ring.request/path "/test.png"}))))

;; Evoke "Throwing Multiple API paths match"

(deftest two-path-parameter-path-preferred-test
  (install-test-resources!)
  (submit-and-await!
   [
    [::xt/put
     {:xt/id "https://example.org/_site/actions/put-things"
      ::site/type "Action"
      :juxt.pass.alpha.malli/args-schema
      [:tuple [:map]]

      :juxt.pass.alpha/process
      [
       [:juxt.pass.alpha.malli/validate]
       [:xtdb.api/put]]

      ::pass/rules
      '[
        [(allowed? permission subject action resource)
         [permission ::pass/subject subject]
         [(nil? resource)]]]}]

    [::xt/put
     {:xt/id "https://example.org/_site/permissions/put-things"
      ::site/type "Permission"
      ::pass/subject :tester
      ::pass/action "https://example.org/_site/actions/put-things"
      ::pass/purpose nil}]])

  (submit-and-await!
   [[:xtdb.api/put
     {:xt/id "https://example.org/_site/apis/test/openapi.json"
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
              {"name" {"type" "string" "minLength" 1}}}}}}
          "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}

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
              {"name" {"type" "string" "minLength" 1}}}}}}
          "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}}}}]])
  (let [body (json/write-value-as-string {"name" "foo"})
        r (*handler*
           {::pass/subject :tester
            :ring.request/method :put
            ;; Matches both {a} and {b}
            :ring.request/path "/things/foo/bar"
            :ring.request/body (ByteArrayInputStream. (.getBytes body))
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/json"}})
        db (x/db *xt-node*)]
    (is (= "putAB"
           (get-in r [::site/resource ::apex/operation "operationId"])))))

(deftest inject-path-parameter-with-forward-slash-test
  ;; PUT a project code of ABC/DEF (with Swagger) and ensure the / is
  ;; preserved. This test tests an edge case where we want a path parameter to contain a /.
  (install-test-resources!)
  (submit-and-await!
   [
    [::xt/put
     {:xt/id "https://example.org/_site/actions/put-things"
      ::site/type "Action"
      :juxt.pass.alpha.malli/args-schema
      [:tuple [:map]]

      :juxt.pass.alpha/process
      [
       [:juxt.pass.alpha.malli/validate]
       [:xtdb.api/put]]

      ::pass/rules
      '[
        [(allowed? permission subject action resource)
         [permission ::pass/subject subject]
         [(nil? resource)]]]}]

    [::xt/put
     {:xt/id "https://example.org/_site/permissions/put-things"
      ::site/type "Permission"
      ::pass/subject :tester
      ::pass/action "https://example.org/_site/actions/put-things"
      ::pass/purpose nil}]])

  (submit-and-await!
   [[:xtdb.api/put
     {:xt/id "https://example.org/_site/apis/test/openapi.json"
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
              {"name" {"type" "string" "minLength" 1}}}}}}
          "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}

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
              {"name" {"type" "string" "minLength" 1}}}}}}
          "x-juxt-site-action" "https://example.org/_site/actions/put-things"}}}}}]])

  (let [path (str"/things/" (java.net.URLEncoder/encode "ABC/DEF"))
        body (json/write-value-as-string {"name" "zip"})
        r (*handler*
           {::pass/subject :tester
            :ring.request/method :put
            :ring.request/path path
            :ring.request/body (ByteArrayInputStream. (.getBytes body))
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/json"}})
        db (x/db *xt-node*)]
    (is (= "/things/{a}" (get-in r [::site/resource :juxt.apex.alpha/openapi-path])))
    (is (= {:name "zip",
            :juxt/code "ABC/DEF",
            :xt/id "https://example.org/things/ABC%2FDEF"}
           (->
            (x/entity db (str "https://example.org" path))
            (dissoc ::site/request))))))

(deftest if-modified-since-test
  (submit-and-await!
   [[:xtdb.api/put
     {:xt/id "https://example.org/test.png"
      ::http/last-modified #inst "2020-03-01"
      ::http/content-type "image/png"
      ::http/methods {:get {}}}]])
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
   [[:xtdb.api/put
     {:xt/id "https://example.org/test.png"
      ::http/etag "\"abc\""
      ::http/content-type "image/png"
      ::http/methods {:get {} :head {} :options {}}}]])
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
  (install-test-resources!)
  (is (= 412
         (:ring.response/status
          (let [body "Hello"]
            (*handler*
             {::pass/subject :tester
              :ring.request/method :put
              :ring.request/body (ByteArrayInputStream. (.getBytes body))
              :ring.request/headers
              {"content-length" (str (count body))
               "content-type" "application/json"
               "if-match" "*"}
              :ring.request/path "/test.png"}))))))

#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (install-test-resources!)
   ;; Install a VirtualResource for any top-level png files
   (let [body "Hello"
         req {::pass/subject :tester
              :ring.request/method :put
              :ring.request/body (ByteArrayInputStream. (.getBytes body))
              :ring.request/headers
              {"content-length" (str (count body))
               "content-type" "application/json"
               "if-match" "*"}
              ::site/base-uri "https://example.org"
              ::site/uri "https://example.org/test.png"
              ::site/db (xt/db *xt-node*)
              :ring.request/path "/test.png"}]
     (locator/locate-resource req)
     #_(*handler*
      req
      ))
   )
 )

(defn if-match-run [if-match]
  (install-test-resources!)
  (submit-and-await!
   [[:xtdb.api/put
     {:xt/id "https://example.org/test.png"
      ::site/type "StaticRepresentation"
      ::http/etag "\"abc\""
      ::http/content-type "image/png"
      ::http/methods {:put {::pass/actions #{:test}}}
      }]])
  (:ring.response/status
   (let [body "Hello"]
     (*handler*
      {::pass/subject :tester
       :ring.request/method :put
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
    [[:xtdb.api/put
      {:xt/id "https://example.org/"
       ::site/type "Redirect"
       ::site/location "/test.html"}]])

   (let [response (*handler*
                   {:ring.request/method :get
                    :ring.request/path "/"})]
     (is (= 302 (:ring.response/status response)))
     (is (= "/test.html" (get-in response [:ring.response/headers "location"])))))




(deftest content-negotiation-test
  (install-test-resources!)
  (submit-and-await!
   [[:xtdb.api/put
     {:xt/id "https://example.org/report"
      ::http/methods {:get {::pass/actions #{:test}} :head {} :options {}}
      ::http/representations
      #{{::http/content-type "text/html;charset=utf-8"
         ::http/content "<h1>Latest sales figures</h1>"}
        {::http/content-type "text/plain;charset=utf-8"
         ::http/content "Latest sales figures"}}}]])

  (let [response
        (*handler*
         {::pass/subject :tester
          :ring.request/method :get
          :ring.request/path "/report"
          :ring.request/headers {"accept" "text/html"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "<h1>Latest sales figures</h1>" (:ring.response/body response))))

  (let [response
        (*handler*
         {::pass/subject :tester
          :ring.request/method :get
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
   [[:xtdb.api/put
     {:xt/id "https://example.org/report"
      ::http/methods {:get {} :head {} :options {}}}]

    [:xtdb.api/put
     {:xt/id "https://example.org/report.html"
      ::site/variant-of "https://example.org/report"
      ::http/content-type "text/html;charset=utf-8"
      ::http/content "<h1>Latest sales figures</h1>"}]

    [:xtdb.api/put
     {:xt/id "https://example.org/report.txt"
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

#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (submit-and-await!
    [[:xtdb.api/put
      {:xt/id "https://example.org/sensitive-report.html"
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Latest sales figures"
       ::http/methods {:get {} :head {} :options {}}}]

     [:xtdb.api/put
      {:xt/id "https://example.org/401.html"
       ::site/type "ErrorRepresentation"
       ::http/status #{401 403}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "<h1>Unauthorized or Forbidden</h1>"}]

     [:xtdb.api/put
      {:xt/id "https://example.org/401.txt"
       ::site/type "ErrorRepresentation"
       ::http/status #{401}
       ::http/content-type "text/plain;charset=utf-8"
       ::http/content "Unauthorized"}]

     [:xtdb.api/put
      {:xt/id "https://example.org/406.html"
       ::site/type "ErrorRepresentation"
       ::http/status #{406}
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "<h1>Unacceptable</h1>"
       ::http/methods {:get {} :head {} :options {}}}]])

   (let [db (x/db *xt-node*)]
     (x/q db '{:find [er]
               :where [[er ::site/type "ErrorRepresentation"]
                       [er ::http/status 403]]}))

   (*handler*
    {:ring.request/method :get
     :ring.request/path "/sensitive-report.html"
     :ring.request/headers {"accept" "text/html"}})))

#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (submit-and-await!
    [[:xtdb.api/put
      {:xt/id "https://example.org/report.html"
       ::http/content-type "text/html;charset=utf-8"
       ::http/content "Latest figures"
       ::http/methods {:get {} :head {} :options {}}
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
   [[:xtdb.api/put
     {:xt/id "https://example.org/view/index.html"
      ::http/methods {:get {}}
      ::http/content-type "text/html;charset=utf-8"
      ::http/content "<h1>Hello!</h1>"}]])

  (let [response
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/view/index.html"
          :ring.request/headers {"accept" "text/html"}})]
    (is (= 200 (:ring.response/status response)))
    (is (= "<h1>Hello!</h1>" (:ring.response/body response)))))


#_((t/join-fixtures [with-xt with-handler])
 (fn []
   (install-test-resources!)

   (let [db (xt/db *xt-node*)]
     (xt/entity db :tester)

     (authz/actions->rules db #{:test})
     (authz/check-permissions
        (xt/db *xt-node*)
        #{:test}
        (cond-> {:subject :tester}
          ;; When the resource is in the database, we can add it to the
          ;; permission checking in case there's a specific permission for
          ;; this resource.
          ;;(:xt/id resource) (assoc :resource (:xt/id resource))
          )))

   (submit-and-await!
    [
     [:xtdb.api/put
      {:xt/id "https://example.org/report"
       ::http/methods {:get {::pass/actions #{:test}}
                       :head {}
                       :options {}}
       ::http/representations
       #{{::http/content-type "text/html;charset=utf-8"
          ::http/content "<h1>Latest sales figures</h1>"}
         {::http/content-type "text/plain;charset=utf-8"
          ::http/content "Latest sales figures"}}}]])
   (*handler*
    {:ring.request/method :get
     :ring.request/path "/report"
     :ring.request/headers {"accept" "text/html"}})))
