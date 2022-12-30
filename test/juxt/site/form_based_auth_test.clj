;; Copyright © 2022, JUXT LTD.

(ns juxt.site.form-based-auth-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [malli.core :as malli]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :as login]
   [juxt.test.util
    :refer [*handler* with-fixtures with-system-xt with-handler install-package!]]))

(use-fixtures :each with-system-xt with-handler)

(deftest login-with-form-test
  (let [common-uri-map
        {"https://example.org" "https://example.test"
         "https://core.example.org" "https://example.test"}]

    (install-package! "bootstrap" common-uri-map)
    (install-package! "sessions" common-uri-map)
    (install-package! "login-form" common-uri-map)
    (install-package! "user-model" common-uri-map)
    (install-package! "password-based-user-identity" common-uri-map)
    (install-package! "oauth-authorization-server" common-uri-map)
    (install-package! "example-users" common-uri-map)

    (let [result (login/login-with-form!
                  *handler*
                  :juxt.site/uri "https://example.test/login"
                  "username" "ALICE"
                  "password" "garden")]
      (is (malli/validate [:map [:juxt.site/session-token :string]] result)))))
