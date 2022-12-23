;; Copyright © 2022, JUXT LTD.

(ns juxt.site.form-based-auth-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [malli.core :as malli]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.resource-package :as pkg]
   [juxt.test.util :refer [*handler*
                           with-fixtures
                           with-system-xt with-handler]]))

(use-fixtures :each with-system-xt with-handler)

(deftest login-with-form-test
  (let [common-uri-map
        {"https://example.org" "https://example.test"
         "https://core.example.org" "https://example.test"}
        install! (fn [path]
                   (pkg/install-package-from-filesystem!
                    path
                    common-uri-map))]

    (install! "packages/bootstrap")
    (install! "packages/sessions")
    (install! "packages/login-form")
    (install! "packages/user-database")
    (install! "packages/example-users")

    (let [result (login/login-with-form!
                  *handler*
                  :juxt.site/uri "https://example.test/login"
                  "username" "ALICE"
                  "password" "garden")]
      (is (malli/validate [:map [:juxt.site/session-token :string]] result)))))
