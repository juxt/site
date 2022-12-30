;; Copyright © 2022, JUXT LTD.

(ns juxt.site.oauth-test
  (:require
   [jsonista.core :as json]
   [juxt.site.logging :refer [with-logging]]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.test-helpers.oauth :as oauth]
   [xtdb.api :as xt]
   [juxt.test.util
    :refer [with-system-xt
            with-session-token with-bearer-token
            with-fixtures *handler* *xt-node* with-handler
            install-package! install-resource-with-action!]]))

(use-fixtures :each with-system-xt with-handler)

(deftest register-client-test
  (let [uri-map {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"}]
    (install-package! "bootstrap" uri-map)
    (install-package! "sessions" uri-map)
    (install-package! "oauth-authorization-server" uri-map)

    (testing "Register client with generated client-id"
      (let [result
            (install-resource-with-action!
             {:juxt.site/subject-id "https://auth.example.test/subjects/system"
              :juxt.site/action-id "https://auth.example.test/actions/register-client"
              :juxt.site/input
              {:juxt.site/client-type "public"
               :juxt.site/redirect-uri "https://test-app.example.test/callback"}})
            doc-id (some-> result :juxt.site/puts first)
            doc (when doc-id (xt/entity (xt/db *xt-node*) doc-id))]
        (is doc)
        (is (nil? (:juxt.site/client-secret doc)))))

    (testing "Register client with generated client-id and client-secret"
      (let [result
            (install-resource-with-action!
             {:juxt.site/subject-id "https://auth.example.test/subjects/system"
              :juxt.site/action-id "https://auth.example.test/actions/register-client"
              :juxt.site/input
              {:juxt.site/client-type "confidential"
               :juxt.site/redirect-uri "https://test-app.example.test/callback"}})
            doc-id (some-> result :juxt.site/puts first)
            doc (when doc-id (xt/entity (xt/db *xt-node*) doc-id))]
        (is doc)
        (is (:juxt.site/client-secret doc))))

    (testing "Re-registering the same client-id will fail"
      (let [input {:juxt.site/subject-id "https://auth.example.test/subjects/system"
                   :juxt.site/action-id "https://auth.example.test/actions/register-client"
                   :juxt.site/input
                   {:juxt.site/client-id "test-app"
                    :juxt.site/client-type "public"
                    :juxt.site/redirect-uri "https://test-app.example.test/callback"}}]
        (install-resource-with-action! input)

        (is
         (=
          {:juxt.site/type "https://meta.juxt.site/types/client"
           :juxt.site/client-id "test-app"
           :juxt.site/client-type "public"
           :juxt.site/redirect-uri "https://test-app.example.test/callback"
           :xt/id "https://auth.example.test/clients/test-app"}
          (xt/entity (xt/db *xt-node*) "https://auth.example.test/clients/test-app")))

        (is
         (thrown?
          clojure.lang.ExceptionInfo
          (install-resource-with-action! input)))))))

(deftest get-subject-test

  ;; Build the authorization server (https://auth.example.test)
  (install-package!
   "bootstrap"
   {"https://example.org" "https://auth.example.test"})

  ;; TODO: Question: shouldn't the system subject be installed on
  ;; auth.example.test, and therefore the whole bootstrap?

  (install-package!
   "sessions"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "oauth-authorization-server"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  ;; Register an application
  ;; TODO: Only temporary while moving init below pkg
  (install-resource-with-action!
   {:juxt.site/subject-id "https://auth.example.test/subjects/system"
    :juxt.site/action-id "https://auth.example.test/actions/register-client"
    :juxt.site/input
    {:juxt.site/client-id "test-app"
     :juxt.site/client-type "confidential"
     :juxt.site/redirect-uri "https://test-app.example.test/callback"}})

  ;; Now we need some mechanism to authenticate with the authorization server in
  ;; order to authorize applications and acquire tokens.

  (install-package!
   "login-form"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "user-model"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "password-based-user-identity"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "example-users"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "protection-spaces"
   {#{"https://auth.example.org" "https://core.example.org"} "https://auth.example.test"})

  (install-package!
   "whoami"
   {"https://example.org" "https://example.test"
    #{"https://auth.example.org" "https://core.example.org"} "https://auth.example.test"})

  (let [login-result
        (login/login-with-form!
         *handler*
         "username" "alice"
         "password" "garden"
         :juxt.site/uri "https://auth.example.test/login")

        session-token (:juxt.site/session-token login-result)
        _ (assert session-token)

        {access-token "access_token"}
        (with-session-token
          session-token
          (oauth/authorize!
           "https://auth.example.test/oauth/authorize"
           {"client_id" "test-app"}))]

    (with-bearer-token access-token
      (let [{:ring.response/keys [headers body]}
            (*handler*
             {:juxt.site/uri "https://example.test/whoami"
              :ring.request/method :get
              :ring.request/headers
              {"accept" "application/json"}})]

        (is (= "Alice"
               (-> body
                   json/read-value
                   (get-in ["juxt.site/subject"
                            "juxt.site/user-identity"
                            "juxt.site/user"
                            "name"
                            ]))))
        (is (= "application/json" (get headers "content-type")))
        (is (= "https://example.test/whoami.json" (get headers "content-location"))))

      (let [{:ring.response/keys [status headers]}
            (*handler*
             {:juxt.site/uri "https://example.test/whoami.html"
              :ring.request/method :get
              :ring.request/headers
              {"authorization" (format "Bearer %s" access-token)
               }})]
        (is (= 200 status))
        (is (= "text/html;charset=utf-8" (get headers "content-type")))))))
