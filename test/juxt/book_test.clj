;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha.init :as init]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [juxt.site.alpha.repl :as repl]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [xtdb.api :as xt])
  (:import (clojure.lang ExceptionInfo)))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-handler)

;; This is useful when developing tests at the REPL.
(defmacro with-fixtures [& body]
  `((t/join-fixtures [with-system-xt with-handler])
    (fn [] ~@body)))

(defmacro with-resources [resources & body]
  `(do
     (init/converge!
      ~(conj resources ::init/system)
      (init/substitute-actual-base-uri
       (merge init/dependency-graph book/dependency-graph)))
     ~@body))

(defn encode-basic-authorization [user password]
  (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" user password))))))

;; Tests

(deftest not-found-test
  (with-resources #{::init/system}
    (let [req {:ring.request/method :get
               :ring.request/path "/hello"}
          invalid-req (assoc req :ring.request/path "/not-hello")]
      (is (= 404 (:ring.response/status (*handler* invalid-req)))))))

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
        (let [{:ring.response/keys [status body]} (*handler* req)]
          (is (= 200 status))
          (is (= "Hello World!\r\n" body))))

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
         (book/login-with-form! {"username" "bob" "password" "garden"})))))

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
                "content-type" "application/edn"}}
              response (-> request
                           (book/with-body (.getBytes (pr-str {:xt/id "https://site.test/graphql"})))
                           *handler*)]
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
                "content-type" "application/edn"}}
              response (*handler* (book/with-body request (.getBytes (pr-str {:xt/id "https://site.test/wrong-graphql"}))))]
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
                "content-type" "application/edn"}}
              response (*handler* (book/with-body request (.getBytes (pr-str {:xt/id "https://site.test/graphql"}))))]
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
                "content-type" "application/graphql"}})

          response (*handler*
                    (-> request
                        (book/with-body (.getBytes "schema { }"))))]
      (testing "Attempting for an unauthorized user to PUT a graphql schema"
        (is (= 403 (:ring.response/status response)))))))

;;with-fixtures
(deftest put-graphql-schema
  (with-resources #{"https://site.test/graphql"
                    "https://site.test/actions/put-graphql-schema"
                    "https://site.test/permissions/alice/put-graphql-schema"
                    "https://site.test/actions/get-graphql-schema"
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
           (-> {:ring.request/method :put
                :ring.request/path "/graphql"
                :ring.request/headers
                {"authorization" (format "Bearer %s" access-token)
                 "content-type" "text/csv"}}
               (book/with-body (.getBytes "schema { }"))))

          _ (is (= 415 (:ring.response/status put-response-bad-content)))

          put-request
          (-> {:ring.request/method :put
               :ring.request/path "/graphql"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/graphql"}}
              (book/with-body (.getBytes "type Query { myName: String }")))

          put-response (*handler* put-request)

          _ (is (= 201 (:ring.response/status put-response)))
          _ (is (nil? (get-in put-response [:ring.response/headers "location"])))



          #_#_put-response (*handler* (-> put-request
                                      (book/with-body (.getBytes "type Query { myName: String }"))))
          #_#__ (is (= 200 (:ring.response/status put-response)))
          ]


      put-response

      (repl/e "https://site.test/graphql")

      )))


;; Portal

(comment
  (require '[portal.api :as p])
  (def p (p/open)))

(comment
  (p/tap))

(comment
  (p/register! #'repl/e))

(comment
  (require '[juxt.flip.alpha.xml :as-alias x])

  (f/eval-quotation
   []
   `(

     (x/<contained-tag> "html" {}))
   {}
   )
  (require '[hiccup2.core :as h2]))
