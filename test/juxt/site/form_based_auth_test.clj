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
                           with-bootstrapped-resources
                           with-system-xt with-handler]]))

(use-fixtures :each with-system-xt with-handler with-bootstrapped-resources)

(deftest login-with-form-test
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/core"))
  (pkg/install-package! (pkg/load-package-from-filesystem "resources/example-users"))
  (pkg/install-resources!
   #{"https://example.org/login"})

  (let [result (login/login-with-form!
                *handler*
                :juxt.site/uri "https://example.org/login"
                "username" "ALICE"
                "password" "garden")]
    (is (malli/validate [:map [:juxt.site/session-token :string]] result))))
