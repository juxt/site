;; Copyright © 2022, JUXT LTD.

(ns juxt.site.form-based-auth-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [malli.core :as malli]
   [juxt.site.resources.form-based-auth :as form-based-auth]
   [juxt.site.resources.example-users :as example-users]
   [juxt.site.resources.session-scope :as session-scope]
   [juxt.site.resources.user :as user]
   [juxt.test.util :refer [*handler* with-resources with-system-xt with-handler]]))

(use-fixtures :each with-system-xt with-handler)

(deftest login-with-form-test
  (with-resources
    ^{:dependency-graphs
      #{session-scope/dependency-graph
        form-based-auth/dependency-graph
        example-users/dependency-graph
        user/dependency-graph}}
    #{"https://example.org/login"
      "https://example.org/session-scopes/default"
      "https://example.org/users/alice"
      "https://example.org/permissions/system/put-user-identity"
      "https://example.org/user-identities/alice"}

    (let [result (form-based-auth/login-with-form!
                  *handler*
                  :juxt.site/uri "https://example.org/login"
                  "username" "ALICE"
                  "password" "garden")]
      (is (malli/validate [:map [:juxt.site/session-token :string]] result)))))
