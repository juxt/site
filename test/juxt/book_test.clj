;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
  (:require
   [clojure.java.io :as io]
   [portal.api :as p]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.pass.alpha.actions :as actions]
   [juxt.pass.alpha.http-authentication :as authn]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :as init]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [malli.core :as malli]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [clojure.string :as str]
   [juxt.site.alpha.repl :as repl]
   [juxt.flip.alpha.core :as f]
   [clojure.tools.logging :as log]
   [portal.api :as portal])
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

;; TODO: These tests should use with-resources

(deftest not-found-test
  (init/bootstrap!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))

(deftest public-resource-test
  (init/bootstrap!)
  (book/setup-hello-world!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")) ;; Assert the entity exists in the db
  (is (not (xt/entity (xt/db *xt-node*) "https://site.test/not-hello"))) ;; Assert that out 404 entity is not in the db

  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}]

    (testing "Can retrieve a public immutable resource"
      (let [{:ring.response/keys [status body] :as response} (*handler* req)]
        (is (= 200 status))
        (is (= "Hello World!\r\n" body))))

    (testing "Receive 405 when method not allowed"
        (let [invalid-req (assoc req :ring.request/method :put)
              {:ring.response/keys [status]} (*handler* invalid-req)]
          (is (= 405 status))))

    (testing "Receive 404 when resource does not exist"
        (let [invalid-req (assoc req :ring.request/path "/not-hello")
              {:ring.response/keys [status]} (*handler* invalid-req)]
          (is (= 404 status))))))

(deftest protected-resource-with-http-basic-auth-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (book/protection-spaces-preliminaries!)

  (book/create-resource-protected-by-basic-auth!)
  (book/grant-permission-to-resource-protected-by-basic-auth!)
  (book/put-basic-protection-space!)

  (book/users-preliminaries!)
  (book/create-action-put-basic-user-identity!)
  (book/grant-permission-to-invoke-action-put-basic-user-identity!)
  (book/create-basic-user-identity! #::pass{:username "ALICE" :password "garden" :realm "Wonderland"})

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected-by-basic-auth/document.html"))

  (is (= 1 (count (authn/protection-spaces (xt/db *xt-node*) "https://site.test/protected-by-basic-auth/document.html"))))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-basic-auth/document.html"}

        request-with-good-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "garden")})

        request-with-bad-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "gradne")})]

    (let [response (*handler* request)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-bad-creds)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-good-creds)]
      (is (= 200 (:ring.response/status response)))
      (is (nil? (get-in response [:ring.response/headers "www-authenticate"]))))))

(deftest session-scope-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)

  (book/session-scopes-preliminaries!)

  (book/create-resource-protected-by-session-scope!)
  (book/grant-permission-to-resource-protected-by-session-scope!)
  (book/create-session-scope!)

  (let [uri (some :juxt.pass.alpha/login-uri
                  (session-scope/session-scopes (xt/db *xt-node*) "https://site.test/protected-by-session-scope/document.html"))]
    (is (string? uri)))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-session-scope/document.html"}]
    (testing "Redirect"
      (let [response (*handler* request)]
        (is (= 302 (:ring.response/status response)))
        (is (.startsWith
             (get-in response [:ring.response/headers "location"])
             "https://site.test/login?return-to="))))))

;; Reinstate when refactored setup-application
#_(deftest protected-resource-with-http-bearer-auth-test
    (init/bootstrap!)
    (book/protected-resource-preliminaries!)
    (book/protection-spaces-preliminaries!)

    #_(book/applications-preliminaries!)

    (let [log-entry (book/setup-application!)
          db (xt/db *xt-node*)
          lookup (fn [id] (xt/entity db id))
          bearer-token (-> log-entry ::pass/puts (get 0) lookup ::pass/token)]

      (book/create-resource-protected-by-bearer-auth!)
      (book/grant-permission-to-resource-protected-by-bearer-auth!)
      (book/put-bearer-protection-space!)

      (is (xt/entity (xt/db *xt-node*) "https://site.test/protected-by-bearer-auth/document.html"))

      (let [request {:ring.request/method :get
                     :ring.request/path "/protected-by-bearer-auth/document.html"}]

        (testing "Cannot be accessed without a bearer token"
          (let [response (*handler* request)]
            response
            (is (= 401 (:ring.response/status response)))
            (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))
            ))

        (testing "Can be accessed with a valid bearer token"
          (let [response (*handler*
                          (assoc
                           request
                           :ring.request/headers
                           {"authorization" (format "Bearer %s" bearer-token)}))]
            (is (= 200 (:ring.response/status response)))
            (is (nil? (get-in response [:ring.response/headers "www-authenticate"])))))

        (testing "Cannot be accessed with an invalid bearer token"
          (let [response (*handler*
                          (assoc
                           request
                           :ring.request/headers
                           {"authorization" "Bearer not-test-access-token"}))]
            (is (= 401 (:ring.response/status response)))
            (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))))))

;; Reinstate when refactored setup-application
#_(deftest user-directory-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (book/applications-preliminaries!)

  (let [log-entry (book/setup-application!)
        db (xt/db *xt-node*)
        lookup (fn [id] (xt/entity db id))
        bearer-token (-> log-entry ::pass/puts (get 0) lookup ::pass/token)]

    (do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/create-action"
     {:xt/id "https://site.test/actions/put-user-owned-content"
      :juxt.pass.alpha/scope "write:user-content"
      :juxt.pass.alpha/rules
      [
       '[(allowed? subject resource permission)
         [permission ::pass/user user]
         [subject ::pass/user-identity id]
         [id ::pass/user user]
         [resource :owner user]]]})

    (do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/grant-permission"
     {:xt/id "https://site.test/permissions/put-user-owned-content"
      :juxt.pass.alpha/action "https://site.test/actions/put-user-owned-content"
      :juxt.pass.alpha/user "https://site.test/users/alice"
      :juxt.pass.alpha/purpose nil})

    ;; Both bob and alice have user directories
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

    ;; 404 on GET, doesn't exist yet!
    (let [req {:ring.request/method :get
               :ring.request/path "/~bob/index.html"}]
      (is (= 404 (:ring.response/status (*handler* req)))))

    ;; Alice can't write to Bob's area
    (let [req {:ring.request/method :put
               :ring.request/path "/~bob/index.html"
               :ring.request/headers
               {"authorization" (format "Bearer %s" bearer-token)}}]
      (is (= 403 (:ring.response/status (*handler* req)))))

    ;; When Alice writing to Alice's user directory, we get through security.
    (let [req {:ring.request/method :put
               :ring.request/path "/~alice/index.html"
               :ring.request/headers
               {"authorization" (format "Bearer %s" bearer-token)}}]
      (is (= 411 (:ring.response/status (*handler* req)))))))

;; This is a test just to check that
;; https://site.test/actions/put-immutable-protected-resource functions
;; properly.
(deftest put-protected-resource-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (is (=
       {:juxt.pass.alpha/subject "https://site.test/subjects/system"
        :juxt.site.alpha/type "https://meta.juxt.site/site/action-log-entry"
        :juxt.pass.alpha/action
        "https://site.test/actions/put-immutable-protected-resource"
        :juxt.pass.alpha/puts
        ["https://site.test/protected-by-session-scope/document.html"]
        :juxt.pass.alpha/deletes []}
       (select-keys
        (book/create-resource-protected-by-session-scope!)
        [:juxt.pass.alpha/subject
         :juxt.site.alpha/type
         :juxt.pass.alpha/action
         :juxt.pass.alpha/puts
         :juxt.pass.alpha/deletes]))))

;; TODO: Actions should eventually be promoted to 'site'.

;; TODO: Test all branches of flip, especially cases where quotations should throw exceptions

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
    (let [response
          (*handler*
           {:ring.request/method :get
            :ring.request/path "/private/internal.html"
            :ring.request/headers
            {"authorization"
             (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" "alice" "garden")))))}})]
      (tap> (:ring.response/status response))
      (is (= 200 (:ring.response/status response))))))

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

(deftest authorization-server-implicit-grant
  (with-resources #{"https://site.test/oauth/authorize"
                    "https://site.test/session-scopes/oauth"
                    "https://site.test/login"
                    "https://site.test/user-identities/alice/basic"
                    "https://site.test/permissions/alice-can-authorize"
                    "https://site.test/applications/local-terminal"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]

      (testing "Missing response type"
        (let [
              state (make-nonce 10)
              request {:ring.request/method :get
                       :ring.request/path "/oauth/authorize"
                       :ring.request/headers {"cookie" (format "id=%s" session-id)}
                       :ring.request/query
                       (codec/form-encode
                        {"client_id" "local-terminal"
                         "state" state})}
              response (*handler* request)
              location-header (-> response :ring.response/headers (get "location"))
              [_ error returned-state]
              (when location-header
                (re-matches
                 #"https://site.test/terminal/callback#error=(.*?)\&state=(.*?)" location-header))]
          (is (= error "invalid_request"))
          (is (= state returned-state))))

      (testing "Access token"
        (let [access-token (book/authorize!
                            :session-id session-id
                            "client_id" "local-terminal")]
          (is access-token)
          (is (= 32 (.length access-token))))))))

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
        access-token (book/authorize! :session-id session-id "client_id" "local-terminal")
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
        access-token (book/authorize! :session-id session-id "client_id" "local-terminal")
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
                    "https://site.test/permissions/alice/install-graphql-endpoint"}

    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})]

      (testing "Installation at wrong endpoint denied"
        (let [request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" (book/authorize! :session-id session-id
                                                                "client_id" "local-terminal"
                                                                "scope" ["admin.graphql" "query.graphql"]))
                "content-type" "application/edn"}}
              response (*handler* (book/with-body request (.getBytes (pr-str {:xt/id "https://site.test/my-graphql"}))))]
          (is (= 403 (:ring.response/status response)))))

      (testing "Installation with insufficient scope"
        (let [request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" (book/authorize! :session-id session-id
                                                                "client_id" "local-terminal"
                                                                "scope" ["query.graphql"]))
                "content-type" "application/edn"
                }}
              response (*handler* (book/with-body request (.getBytes (pr-str {:xt/id "https://site.test/graphql"}))))]
          (is (= 403 (:ring.response/status response)))))


      (testing "Installation with sufficient scope"
        (let [request
              {:ring.request/method :post
               :ring.request/path "/actions/install-graphql-endpoint"
               :ring.request/headers
               {"authorization" (format "Bearer %s" (book/authorize! :session-id session-id
                                                                "client_id" "local-terminal"
                                                                "scope" ["admin.graphql" "query.graphql"]))
                "content-type" "application/edn"
                }}
              response (*handler* (book/with-body request (.getBytes (pr-str {:xt/id "https://site.test/graphql"}))))]
          (is (= 201 (:ring.response/status response))))))))

(deftest install-graphql-schema
  (with-resources #{"https://site.test/graphql"}
    (let [session-id (book/login-with-form! {"username" "ALICE" "password" "garden"})
          access-token (book/authorize! :session-id session-id
                                        "client_id" "local-terminal"
                                        "scope" ["query.graphql"])
          request
          (-> {:ring.request/method :put
               :ring.request/path "/graphql"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/graphql"}})

          response (*handler*
                    (-> request
                        (book/with-body (.getBytes "schema { }"))))]

      (is (= 200 (:ring.response/status response))))))

(deftest install-graphql-schema-with-wrong-user
  (with-resources #{"https://site.test/graphql"
                    "https://site.test/user-identities/bob/basic"
                    "https://site.test/permissions/bob-can-authorize"}
    (let [session-id (book/login-with-form! {"username" "bob" "password" "walrus"})
          access-token (book/authorize! :session-id session-id
                                   "client_id" "local-terminal"
                                   "scope" ["query.graphql"])
          request
          (-> {:ring.request/method :put
               :ring.request/path "/graphql"
               :ring.request/headers
               {"authorization" (format "Bearer %s" access-token)
                "content-type" "application/graphql"}})

          response (*handler*
                      (-> request
                          (book/with-body (.getBytes "schema { }"))))]
      (is (= 403 (:ring.response/status response))))))

(comment
  (def p (p/open)))

(comment
  (p/tap))

(comment
  (portal/register! #'repl/e))
