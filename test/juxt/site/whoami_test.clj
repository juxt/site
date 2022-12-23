;; Copyright © 2022, JUXT LTD.

(ns juxt.site.whoami-test
  (:require
   [jsonista.core :as json]
   [juxt.site.logging :refer [with-logging]]
   [clojure.test :refer [deftest is use-fixtures]]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.test-helpers.oauth :as oauth]
   [juxt.site.resource-package :as pkg]
   [juxt.test.util
    :refer [with-system-xt
            with-session-token with-bearer-token
            with-fixtures *handler* with-handler]]))

(use-fixtures :each with-system-xt with-handler)

(deftest get-subject-test

  ;; Build the authorization server (https://auth.example.test)

  (pkg/install-package-from-filesystem!
   "resources/bootstrap"
   {"https://example.org" "https://auth.example.test"})

  ;; TODO: Question: shouldn't the system subject be installed on
  ;; auth.example.test, and therefore the whole bootstrap?

  (pkg/install-package-from-filesystem!
   "resources/sessions"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/oauth2-auth-server"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  ;; Now we need some mechanism to authenticate with the authorization server in
  ;; order to authorize applications and acquire tokens.

  (pkg/install-package-from-filesystem!
   "resources/login-form"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/user-database"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/example-users"
   {#{"https://example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/example-oauth-resources"
   {#{"https://auth.example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/protection-spaces"
   {#{"https://auth.example.org" "https://core.example.org"} "https://auth.example.test"})

  (pkg/install-package-from-filesystem!
   "resources/whoami"
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
      (let [{:ring.response/keys [headers body] :as response}
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
