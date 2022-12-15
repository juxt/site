;; Copyright © 2022, JUXT LTD.

(ns juxt.site.whoami-test
  (:require
   [jsonista.core :as json]
   [clojure.test :refer [deftest is use-fixtures]]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.test-helpers.oauth :as oauth]
   [juxt.site.resource-package :as pkg]
   [juxt.test.util
    :refer [with-system-xt
            with-session-token with-bearer-token
            with-bootstrapped-resources
            with-fixtures *handler* with-handler]]))

(use-fixtures :each with-system-xt with-handler with-bootstrapped-resources)

(deftest get-subject-test
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/core"))
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/example-users"))
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/oauth2-auth-server"))
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/example-oauth-resources"))
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/whoami"))
  (pkg/install-resources!
   #{"https://example.org/login"
     "https://example.org/permissions/alice/whoami"})

  (let [login-result
        (login/login-with-form!
         *handler*
         "username" "alice"
         "password" "garden"
         :juxt.site/uri "https://example.org/login")

        session-token (:juxt.site/session-token login-result)
        _ (assert session-token)

        {access-token "access_token"}
        (with-session-token
          session-token
          (oauth/authorize!
           {"client_id" "test-app"}))]

    (assert access-token)

    (with-bearer-token access-token
      (let [{:ring.response/keys [headers body]}
            (*handler*
             {:juxt.site/uri "https://example.org/whoami"
              :ring.request/method :get
              :ring.request/headers
              {"accept" "application/json"}})]

        (is (= "Alice"
               (-> body
                   json/read-value
                   (get-in ["subject"
                            "juxt.site/user-identity"
                            "juxt.site/user"
                            "name"]))))
        (is (= "application/json" (get headers "content-type")))
        (is (= "https://example.org/whoami.json" (get headers "content-location"))))

      (let [{:ring.response/keys [status headers]}
            (*handler*
             {:juxt.site/uri "https://example.org/whoami.html"
              :ring.request/method :get
              :ring.request/headers
              {"authorization" (format "Bearer %s" access-token)
               }})]
        (is (= 200 status))
        (is (= "text/html;charset=utf-8" (get headers "content-type")))))))
