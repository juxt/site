;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.flip.alpha.core :as f]
   [juxt.flip.alpha.hiccup :as hc]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha.init :as init]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler* with-fixtures with-resources with-handler] :as tutil]
   [ring.util.codec :as codec]
   [xtdb.api :as xt])
  (:import (clojure.lang ExceptionInfo)))

(defn finalize-request [request]
  (book/with-body request (::body-bytes request)))

(use-fixtures :each with-system-xt with-handler)

(defn encode-basic-authorization [user password]
  (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" user password))))))

;; Tests
(deftest not-found-test
  (with-resources #{::init/system}
    (let [req {:ring.request/method :get
               :ring.request/path "/hello"}
          invalid-req (assoc req :ring.request/path "/not-hello")]
      (is (= 404 (:ring.response/status (*handler* (finalize-request invalid-req))))))))

(deftest public-resource-test
  (with-resources #{::init/system
                    "https://site.test/hello"}

    (testing "The hello entity exists in the database"
      (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")))

    (testing "404 as expected"
      (is (not (xt/entity (xt/db *xt-node*) "https://site.test/not-hello")))) ;;

    (let [req {:ring.request/method :get
               :ring.request/path "/hello"}]

      (testing "Can retrieve a public immutable resource"
        (let [{:ring.response/keys [status body]} (*handler* (finalize-request req))]
          (is (= 200 status))
          (is (= "Hello World!\r\n" (String. body)))))

      (testing "Receive 405 when method not allowed"
        (let [invalid-req (assoc req :ring.request/method :put)
              {:ring.response/keys [status]} (*handler* invalid-req)]
          (is (= 405 status))))

      (testing "Receive 404 when resource does not exist"
        (let [invalid-req (assoc req :ring.request/path "/not-hello")
              {:ring.response/keys [status]} (*handler* invalid-req)]
          (is (= 404 status)))))))

(deftest user-directory-test
  (with-resources
    #{"https://site.test/actions/put-user-owned-content"
      "https://site.test/permissions/alice/put-user-owned-content"

      "https://site.test/oauth/authorize"
      "https://site.test/session-scopes/oauth"
      "https://site.test/login"
      "https://site.test/user-identities/alice/basic"
      "https://site.test/permissions/alice-can-authorize"
      "https://site.test/applications/local-terminal"
      }

    ;; Both Bob and Alice have user directories
    (doseq [user #{"bob" "alice"}]
      (juxt.site.alpha.init/put!
       {:xt/id (format "https://site.test/~%s/{path}" user)
        ;; This needs to return a resource 'owned' by the user, then the action can
        ;; unify on the subject's user and the resource's owner.
        :owner (format "https://site.test/users/%s" user)
        ::site/uri-template true
        ::site/methods
        {:get {::pass/actions #{"https://site.test/actions/get-not-found"}}
         :put {::pass/actions #{"https://site.test/actions/put-user-owned-content"}}}}))

    (testing "404 on GET, doesn't exist yet"
      (let [req {:ring.request/method :get
                 :ring.request/path "/~bob/index.html"}]
        (is (= 404 (:ring.response/status (*handler* req))))))

    (let [session-id (book/login-with-form! {"username" "alice" "password" "garden"})
          _ (is session-id)

          {access-token "access_token"
           error "error"}
          (book/authorize!
           :session-id session-id
           "client_id" "local-terminal")

          _ (is (nil? error) (format "OAuth2 grant error: %s" error))]

      (testing "Alice should not be allowed to write to Bob's area"
        (let [request {:ring.request/method :put
                       :ring.request/path "/~bob/index.html"
                       :ring.request/headers
                       {"authorization" (format "Bearer %s" access-token)}}
              response (*handler* request)]
          (is (= 403 (:ring.response/status response)))))

      (testing "Alice should be allowed to write to her own area"
        (let [request {:ring.request/method :put
                       :ring.request/path "/~alice/index.html"
                       :ring.request/headers
                       {"authorization" (format "Bearer %s" access-token)}}
              response (*handler* request)]
          (is (= 411 (:ring.response/status response))))))))

;; TODO: Actions should eventually be promoted to 'site'.

;; TODO: Test all branches of implicit grant, especially cases where quotations should throw exceptions

(deftest protected-resource-not-publicly-accessible
  (with-resources
    #{"https://site.test/private/internal.html"}

    ;; Try to access /private/internal.html - there is no protection space so no
    ;; WWW-Authenticate header. We simply can't perform
    ;; /actions/get-protected-resource anonymously.
    (is (= 403
           (:ring.response/status
            (*handler*
             {:ring.request/method :get
              :ring.request/path "/private/internal.html"}))))))

(deftest protected-resource-prompt-for-bearer-auth
  (with-resources #{"https://site.test/private/internal.html"
                    "https://site.test/protection-spaces/bearer"}
    (testing "Protected resource under Bearer protection space prompts Bearer authentication"
      (let [response (*handler*
                      {:ring.request/method :get
                       :ring.request/path "/private/internal.html"})]
        (is (= 401 (:ring.response/status response)))
        ;; TODO: Fix reap to avoid the trailing space
        (is (= "Bearer " (get-in response [:ring.response/headers "www-authenticate"])))))))

(deftest protected-resource-prompt-for-basic-auth
  (with-resources #{"https://site.test/private/internal.html"
                    "https://site.test/protection-spaces/basic"}
    (testing "Protected resource under Basic protection space prompts Basic authentication"
      (let [response (*handler*
                      {:ring.request/method :get
                       :ring.request/path "/private/internal.html"})]
        (is (= 401 (:ring.response/status response)))
        (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))))))

(deftest protected-resource-with-basic-auth
  (with-resources #{"https://site.test/private/internal.html"
                    "https://site.test/protection-spaces/basic"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice/private/internal.html"}

    (testing "No credentials"
      (let [response
            (*handler*
             {:ring.request/method :get
              :ring.request/path "/private/internal.html"
              :ring.request/headers {}})]
        (is (= 401 (:ring.response/status response)))
        (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))))

    (testing "Bad credentials"
      (let [response
            (*handler*
             {:ring.request/method :get
              :ring.request/path "/private/internal.html"
              :ring.request/headers
              {"authorization"
               (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" "alice" "bad-password")))))}})]
        (is (= 401 (:ring.response/status response)))
        (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))))

    (testing "Good credentials"
      (let [response
            (*handler*
             {:ring.request/method :get
              :ring.request/path "/private/internal.html"
              :ring.request/headers
              {"authorization"
               (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" "alice" "garden")))))}})]
        (is (= 200 (:ring.response/status response)))
        (is (nil? (get-in response [:ring.response/headers "www-authenticate"])))))))

(deftest session-scope-test
  (with-resources #{"https://site.test/protected-by-session-scope/document.html"
                    "https://site.test/session-scopes/example"}
    (let [uri (some :juxt.pass.alpha/login-uri
                    (session-scope/session-scopes (xt/db *xt-node*) "https://site.test/protected-by-session-scope/document.html"))]
      (is (string? uri)))

    (let [request {:ring.request/method :get
                   :ring.request/path "/protected-by-session-scope/document.html"}]
      (testing "Redirect"
        (let [response (*handler* request)]
          response
          (is (= 302 (:ring.response/status response)))
          (is (.startsWith
               (get-in response [:ring.response/headers "location"])
               "https://site.test/login?return-to=")))))))

(deftest login-with-form-test
  (with-resources #{"https://site.test/login"
                    "https://site.test/user-identities/alice/basic"}
    (is (book/login-with-form! {"username" "alice" "password" "garden"}))
    (is (book/login-with-form! {"username" "ALICE" "password" "garden"}))
    (is (book/login-with-form! {"username" "ALiCe" "password" "garden"}))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"Login failed"
         (book/login-with-form! {"username" "ALICE" "password" "badpassword"})))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"Login failed"
         (book/login-with-form! {"username" "bob" "password" "garden"})))
    (try
      (book/login-with-form! {"username" "bob" "password" "walrus"})
      (catch clojure.lang.ExceptionInfo e
        (is (= 400 (-> e ex-data :response :ring.response/status)))))))

(deftest authorization-server-anonymous-access-forbidden
  (with-resources #{"https://site.test/oauth/authorize"}
    (testing "Anonymous access to authorization server forbidden"
      (let [response (*handler*
                      {:ring.request/method :get
                       :ring.request/path "/oauth/authorize"})]
        (is (= 403 (:ring.response/status response)))))))

(deftest authorization-server-anonymous-access-via-session-scope-redirects
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"}
    (testing "Anonymous access to authorization server redirects to login"
      (let [response (*handler*
                      {:ring.request/method :get
                       :ring.request/path "/oauth/authorize"})
            location (get-in response [:ring.response/headers "location"])
            [_ rel] (re-matches #"https://site.test(.*?)\?(.*)" location)]
        (is (= "/login" rel))
        (is (= 302 (:ring.response/status response)))))))

(deftest authorization-server-forbidden-if-no-permission
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"}
    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]
      (is session-id)
      (is
       (thrown-with-msg?
        ExceptionInfo #"Forbidden to authorize"
        (book/authorize! :session-id session-id "client_id" "local-terminal"))))))

(deftest authorization-server-implicit-grant-with-unknown-app-client
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]
      (is session-id)

      (let [state (make-nonce 10)
            request {:ring.request/method :get
                     :ring.request/path "/oauth/authorize"
                     :ring.request/headers {"cookie" (format "id=%s" session-id)}
                     :ring.request/query
                     (codec/form-encode
                      {"response_type" "token"
                       "client_id" "missing-app"
                       "state" state})}
            response (*handler* request)]
        (is (= 400 (:ring.response/status response)))
        ;; TODO: Show better errors
        ))))

(deftest authorization-server-invalid-scope
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"
                    "https://site.test/oauth/scope/graphql/administer"
                    "https://site.test/oauth/scope/graphql/develop"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]

      (testing "valid scope"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize!
               :session-id session-id
               "client_id" "local-terminal"
               "scope" ["https://site.test/oauth/scope/graphql/administer"
                        "https://site.test/oauth/scope/graphql/develop"])]
          (is access-token)
          (is (nil? error))))

      (testing "invalid scope"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize!
               :session-id session-id
               "client_id" "local-terminal"
               "scope" ["https://site.test/oauth/scope/graphql/administer"
                        "https://site.test/oauth/scope/graphql/bad-scope"])]
          (is (nil? access-token))
          (is (= "invalid_scope" error))))

      (testing "no scope"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize!
               :session-id session-id
               "client_id" "local-terminal")]
          (is access-token)
          (is (nil? error)))))))

(deftest authorization-server-implicit-grant-invalid-response-type
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
          initial-state (make-nonce 10)
          request {:ring.request/method :get
                   :ring.request/path "/oauth/authorize"
                   :ring.request/headers {"cookie" (format "id=%s" session-id)}}

          make-request
          (fn [form]
            (let [request (assoc request :ring.request/query (codec/form-encode form))
                  response (*handler* request)
                  location-header (-> response :ring.response/headers (get "location"))
                  [_ encoded]
                  (when location-header
                    (re-matches
                     #"https://site.test/terminal/callback#(.*)" location-header))]
              (codec/form-decode encoded)))]

      (testing "No response_type provided"
        ;; response_type is REQUIRED
        (let [{error "error"
               error-description "error_description"
               state "state"}
              (make-request
               {"client_id" "local-terminal"
                "state" initial-state})]
          (is (= initial-state state))
          (is (= "invalid_request" error))
          (is (= "A response_type parameter is required" error-description))))

      (testing "The response_type parameter is provided more than once"
        (let [{error "error"
               error-description "error_description"
               state "state"}
              (make-request
               {"client_id" "local-terminal"
                "state" initial-state
                "response_type" ["one" "two"]})]
          (is (= initial-state state))
          (is (= "invalid_request" error))
          (is (= "The response_type parameter is provided more than once" error-description))))

      (testing "Unsupported response_type"
        (let [{error "error"
               error-description "error_description"
               state "state"}
              (make-request
               {"client_id" "local-terminal"
                "state" initial-state
                "response_type" "bad"})]
          (is (= initial-state state))
          (is (= "unsupported_response_type" error))
          (is (= "Only a response type of 'token' is currently supported" error-description)))))))

;; With no client_id we have no way of determining the redirect URI. In this
;; case, we return a 400.
(deftest authorization-server-implicit-grant-no-client-id
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
          state (make-nonce 10)
          request {:ring.request/method :get
                   :ring.request/path "/oauth/authorize"
                   :ring.request/headers {"cookie" (format "id=%s" session-id)}
                   :ring.request/query
                   (codec/form-encode
                    (cond->
                        {"response_type" "token"
                         "state" state}))}
          response (*handler* request)]
      (is (= 400 (:ring.response/status response))))))

(deftest authorization-server-implicit-grant
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
          {access-token "access_token"}
          (book/authorize!
           :session-id session-id
           "client_id" "local-terminal")]
      (is access-token)
      (is (= 32 (.length access-token))))))

(deftest access-to-protected-resource-with-bearer-token-but-no-permission
  (with-resources #{"https://site.test/private/internal.html"
                    "https://site.test/protection-spaces/bearer"
                    "https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"})
  (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
        {access-token "access_token"} (book/authorize! :session-id session-id "client_id" "local-terminal")
        _ (is access-token)
        response
        (*handler*
         {:ring.request/method :get
          :ring.request/headers {"authorization" (format "Bearer %s" access-token)}
          :ring.request/path "/private/internal.html"})]
    (is (= 403 (:ring.response/status response)))))

(deftest access-to-protected-resource-with-bearer-token
  (with-resources #{"https://site.test/private/internal.html"
                    "https://site.test/protection-spaces/bearer"
                    "https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"
                    "https://site.test/permissions/alice/private/internal.html"})
  (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
        {access-token "access_token"} (book/authorize! :session-id session-id "client_id" "local-terminal")
        response (*handler*
                    {:ring.request/method :get
                     :ring.request/headers {"authorization" (format "Bearer %s" access-token)}
                     :ring.request/path "/private/internal.html"})]
    (is (= 200 (:ring.response/status response)))))

(deftest install-graphql-schema-endpoint
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"

                    "https://site.test/applications/local-terminal"

                    "https://site.test/actions/install-graphql-endpoint"
                    "https://site.test/permissions/alice/install-graphql-endpoint"
                    "https://site.test/oauth/scope/graphql/administer"
                    "https://site.test/oauth/scope/graphql/develop"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]

      (testing "Installation with insufficient scope"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize! :session-id session-id
                               "client_id" "local-terminal"
                               "scope" ["https://site.test/oauth/scope/graphql/develop"])

              _ (is (nil? error) (format "OAuth2 grant error: %s" error))

              request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/edn"}
               ::body-bytes (.getBytes (pr-str {:xt/id "https://site.test/graphql"}))}
              response (*handler* (finalize-request request))]
          (is (= 403 (:ring.response/status response)))))

      (testing "Installation at wrong endpoint denied"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize! :session-id session-id
                               "client_id" "local-terminal"
                               "scope" ["https://site.test/oauth/scope/graphql/administer"])

              _ (is (nil? error) (format "OAuth2 grant error: %s" error))

              request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/edn"}
               ::body-bytes (.getBytes (pr-str {:xt/id "https://site.test/wrong-graphql"}))}
              response (*handler* (finalize-request request))]
          (is (= 403 (:ring.response/status response)))))

      (testing "Installation successful"
        (let [{access-token "access_token"
               error "error"}
              (book/authorize! :session-id session-id
                               "client_id" "local-terminal"
                               "scope" ["https://site.test/oauth/scope/graphql/administer"])

              _ (is (nil? error) (format "OAuth2 grant error: %s" error))

              request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/edn"}
               ::body-bytes (.getBytes (pr-str {:xt/id "https://site.test/graphql"}))}
              response (*handler* (finalize-request request))]
          (is (= 201 (:ring.response/status response))))))))

(deftest install-graphql-schema-with-wrong-user
  (with-resources #{"https://site.test/graphql"
                    "https://site.test/user-identities/bob/basic"
                    "https://site.test/permissions/bob-can-authorize"}
    (let [session-id (book/login-with-form! {"username" "bob" "password" "walrus"})
          {access-token "access-token"
           error "error"}
          (book/authorize! :session-id session-id
                           "client_id" "local-terminal"
                           "scope" ["https://site.test/oauth/scope/graphql/develop"])
          _ (is (nil? error) (format "OAuth2 grant error: %s" error))
          request
          (-> {:ring.request/method :put
               :ring.request/path "/graphql"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/graphql"}
               ::body-bytes (.getBytes "schema { }")})

          response (*handler* (finalize-request request))]
      (testing "Attempting for an unauthorized user to PUT a graphql schema"
        (is (= 403 (:ring.response/status response)))))))

;; An experiment to produce HTML via flip
(comment
  (require '[juxt.flip.alpha.hiccup :as-alias hc])

  (f/eval-quotation
   [{:title "My title"
     :fruits [{:name "apple" :count 10}
              {:name "banana" :count 12}
              {:name "kiwi" :count 5}]}]
   `(
     (hc/html
      (f/eval-embedded-quotations
       [:html
        [:body
         [:div
          (
           (juxt.pass.alpha/make-nonce 12)
           )]
         [:div#foo.bar.baz
          [:h1
           (
            ((f/of :title))
            )]
          [:p "Text"]
          [:table
           (
            (f/map
             (f/of :fruits)
             [(f/eval-embedded-quotations
               [:tr
                [:td ((f/of :name))]
                [:td ((f/of :count) f/number->string)]])]))]]]]))
     f/nip)
   {}))

(deftest put-graphql-schema
  (with-resources
    #{"https://site.test/graphql"
      "https://site.test/permissions/alice/put-graphql-schema"
      "https://site.test/permissions/alice/get-graphql-schema"}
    (let [session-id (book/login-with-form! {"username" "alice" "password" "garden"})
          {access-token "access_token"
           error "error"}
          (book/authorize!
           :session-id session-id
           "client_id" "local-terminal"
           ;; The graphql/develop scope is going to let us perform
           ;; put-graphql-schema and get-graphql-schema.
           "scope" ["https://site.test/oauth/scope/graphql/develop"])
          _ (is (nil? error) (format "OAuth2 grant error: %s" error))

          get-request
          (-> {:ring.request/method :get
               :ring.request/path "/graphql"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)}})

          get-response (*handler* get-request)
          ;; Confirm there are no representations for /graphql before attempting
          ;; to PUT one
          _ (is (= 404 (:ring.response/status get-response)))

          put-response-bad-content
          (*handler*
           (finalize-request
            {:ring.request/method :put
             :ring.request/path "/graphql"
             :ring.request/headers
             {"authorization" (format "Bearer %s" access-token)
              "content-type" "text/csv"}
             ::body-bytes (.getBytes "schema { }")}))

          _ (is (= 415 (:ring.response/status put-response-bad-content)))

          put-request
          {:ring.request/method :put
           :ring.request/path "/graphql"
           :ring.request/headers
           {"authorization" (format "Bearer %s" access-token)
            "content-type" "application/graphql"}
           ::body-bytes (.getBytes "type Query { myName: String }")}

          put-response (*handler* (finalize-request put-request))

          _ (is (= 201 (:ring.response/status put-response)))
          _ (is (nil? (get-in put-response [:ring.response/headers "location"])))

          second-put-response (*handler* (finalize-request put-request))
          _ (is (= 200 (:ring.response/status second-put-response)))

          get-response
          (*handler*
           (finalize-request
            {:ring.request/method :get
             :ring.request/path "/graphql"
             :ring.request/headers
             {"authorization" (format "Bearer %s" access-token)}}))
          _ (is (= 200 (:ring.response/status get-response)))
          _ (is (= "type Query { myName: String }" (String. (:ring.response/body get-response))))
          _ (is (= "application/graphql" (get-in get-response [:ring.response/headers "content-type"])))

          get-response-for-edn
          (*handler*
           (finalize-request
            {:ring.request/method :get
             :ring.request/path "/graphql"
             :ring.request/headers
             {"authorization" (format "Bearer %s" access-token)
              "accept" "application/edn"}}))
          _ (is (= 200 (:ring.response/status get-response-for-edn)))

          errors (:errors (edn/read-string (String. (:ring.response/body get-response))))
          _ (is (empty? errors))
          _ (is (= "application/edn" (get-in get-response-for-edn [:ring.response/headers "content-type"])))

          get-response-for-json
          (*handler*
           (finalize-request
            {:ring.request/method :get
             :ring.request/path "/graphql"
             :ring.request/headers
             {"authorization" (format "Bearer %s" access-token)
              "accept" "application/json"}}))

          _ (is (= 406 (:ring.response/status get-response-for-json)))

          ]

      ;; What if there are errors?  How to communicate these? - for now, via the
      ;; link to the request which should be generated as part of the error
      ;; output. Possibly this can be Selmer templated in the future.
      )))

(defn deploy-schema [session-id ^String schema]
  (let [{access-token "access_token"
         error "error"}
        (book/authorize!
         :session-id session-id
         "client_id" "local-terminal"
         "scope" ["https://site.test/oauth/scope/graphql/develop"])
        _ (is (nil? error) (format "OAuth2 grant error: %s" error))
        put-request
        {:ring.request/method :put
         :ring.request/path "/graphql"
         :ring.request/headers
         {"authorization" (format "Bearer %s" access-token)
          "content-type" "application/graphql"}
         ::body-bytes (.getBytes schema)}
        put-response (*handler* (finalize-request put-request))]
    (is (= 201 (:ring.response/status put-response)))
    put-response))

(defn graphql-query [^String access-token ^String query]
  (let [post-response
        (*handler*
         (finalize-request
          {:ring.request/method :post
           :ring.request/path "/graphql"
           :ring.request/headers
           {"authorization" (format "Bearer %s" access-token)
            "content-type" "application/graphql"}
           ::body-bytes (.getBytes query)}))

        _ (is (= 200 (:ring.response/status post-response)))]
    (:ring.response/body post-response)))

(deftest post-graphql-request
  (with-resources
    #{"https://site.test/graphql"
      "https://site.test/permissions/alice/put-graphql-schema"
      "https://site.test/permissions/alice/get-graphql-schema"}

    (let [session-id (book/login-with-form! {"username" "alice" "password" "garden"})

          response (deploy-schema session-id "type Query { myName: String }")

          #_{access-token "access_token" error "error"}
          #_(book/authorize!
           :session-id session-id
           "client_id" "local-terminal"
           "scope" ["https://site.test/oauth/scope/graphql/query"])
          ;;_ (is (nil? error) (format "OAuth2 grant error: %s" error))
          ]

      ;; Ready for implementation
      ;;(is (graphql-query access-token "query { myName }"))

      response
      ;;error
      )))

;; A post to /graphql selects the action relating to 'query', 'mutation' or
;; 'subscription'

;; Do we need to rethink scopes? No, but the /graphql POST handler needs to
;; programmatically check the query is of the correct scope before
;; proceeding. This is an exceptional example where custom code is required to
;; meet a requirement which should not be met by extending Site's design surface
;; to cover such cases.

(comment
  (doseq [tap @(deref #'clojure.core/tapset)]
    (remove-tap tap)))

(comment
  (deref (deref #'clojure.core/tapset)))

;; Wrapping in a tap
#_(try
        (portal.api/tap)
        (->
         (*handler*
          (finalize-request
           {:ring.request/method :get
            :ring.request/path "/graphql"
            :ring.request/headers
            {"authorization" (format "Bearer %s" access-token)}}))
         (select-keys [:ring.response/status :ring.response/headers :ring.response/body])

         )
        (finally
          (doseq [tap @(deref #'clojure.core/tapset)]
             (remove-tap tap))))

;; Keep this as an example of how to set up a database, initialize a request and
;; eval a quotation.
#_(with-fixtures
  (with-resources
    #{"https://site.test/graphql"
      "https://site.test/actions/put-graphql-schema"
      "https://site.test/permissions/alice/put-graphql-schema"
      "https://site.test/actions/get-graphql-schema"
      "https://site.test/permissions/alice/get-graphql-schema"}
    (f/eval-quotation
     []
     `(
       (f/define ^{:f/stack-effect '[ctx -- ctx]} extract-input
         [(f/set-at
           (f/dip
            [site/request-body-as-string
             :input]))])

       (f/define ^{:f/stack-effect '[ctx -- ctx]} compile-input-to-schema
         [(f/set-at
           (f/keep
            [(f/of :input)
             ;; TODO: Catch errors and create an effect which binds them to the
             ;; request context, along with a 400 status code.
             graphql-flip/compile-schema
             :compiled-schema]))])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} update-base-resource
         [(f/dip
           [(site/entity (f/env ::site/resource))
            (f/set-at (f/dip [f/dup (f/of :input) ::http/content]))
            (f/set-at (f/dip ["application/graphql" ::http/content-type]))
            (f/set-at (f/dip [f/dup (f/of :compiled-schema) ::site/graphql-compiled-schema]))])
          f/rot
          f/set-at])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} create-edn-resource
         [(f/dip
           ;; Perhaps could we use a template with eval-embedded-quotations?
           [{}
            (f/set-at (f/dip ["application/edn" ::http/content-type]))
            (f/set-at (f/dip [(f/env ::site/resource) ".edn" f/swap f/str :xt/id]))
            (f/set-at (f/dip [(f/env ::site/resource) ::site/variant-of]))
            (f/set-at (f/dip [f/dup (f/of :compiled-schema) f/pr-str ::http/content]))])
          f/rot
          f/set-at])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} push-resource
         [(f/push-at
           (xtdb.api/put
            f/dupd
            f/of
            (f/unless* [(f/throw-exception (f/ex-info "No object to push as an fx" {}))]))
           ::site/fx
           f/rot)])

       (f/define ^{:f/stack-effect '[ctx -- ctx]} determine-status
         [(f/of (site/entity (f/env ::site/resource)) ::http/content)
          [200 :status f/rot f/set-at]
          [201 :status f/rot f/set-at]
          f/if])

       (site/with-fx-acc ;;-with-checks - adding -with-checks somehow messes things up! :(
         [
          ;; The following can be done in advance of the fx-fn.
          extract-input
          compile-input-to-schema

          ;; The remainder would need to be done in the tx-fn because it looks
          ;; up the /graphql resource in order to update it.
          (update-base-resource :new-resource)
          (push-resource :new-resource)

          ;; The application/edn resource serves the compiled version
          (create-edn-resource :edn-resource)
          (push-resource :edn-resource)

          ;; Return a 201 if there is no existing schema, 200 otherwise
          determine-status
          (site/set-status f/dup (f/of :status))
          f/swap
          site/push-fx
          ]))
     {::site/db (xt/db *xt-node*)
      ::site/received-representation
      {::http/body (.getBytes "type Query { myName String }")
       }})))


;; Portal

(comment
  (require '[portal.api :as p])
  (def p (p/open)))

(comment
  (p/tap))


(comment
  (p/register! #'repl/e))

#_(with-resources #{::init/system})
