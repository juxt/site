;; Copyright © 2022, JUXT LTD.

(ns juxt.site.form-based-auth-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [malli.core :as malli]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.resources :as resources]
   [juxt.test.util :refer [*handler* with-resources with-system-xt with-handler]]))

(use-fixtures :each with-system-xt with-handler)

(deftest login-with-form-test
  (with-resources
    ^{:dependency-graphs
      #{(resources/load-dependency-graph "juxt/site/session-scope.edn")
        (resources/load-dependency-graph "juxt/site/user.edn")
        (resources/load-dependency-graph "juxt/site/form-based-auth.edn")
        (resources/load-dependency-graph "juxt/site/example-users.edn")}}
    #{"https://example.org/login"
      "https://example.org/session-scopes/default"
      "https://example.org/users/alice"
      "https://example.org/permissions/system/put-user-identity"
      "https://example.org/user-identities/alice"}

    (let [result (login/login-with-form!
                  *handler*
                  :juxt.site/uri "https://example.org/login"
                  "username" "ALICE"
                  "password" "garden")]
      (is (malli/validate [:map [:juxt.site/session-token :string]] result)))))
