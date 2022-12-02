;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql-scenario-test
  (:require
   [juxt.site.logging :refer [with-logging]]
   [jsonista.core :as json]
   [clojure.test :refer [deftest is use-fixtures]]
   [juxt.site.resources.oauth :as oauth]
   [juxt.site.resources.session-scope :as session-scope]
   [juxt.site.resources.user :as user]
   [juxt.site.resources.form-based-auth :as form-based-auth]
   [juxt.site.resources.protection-space :as protection-space]
   [juxt.site.resources.example-users :as example-users]
   [juxt.site.resources.example-applications :as example-applications]
   [juxt.site.resources.example-protection-spaces :as example-protection-spaces]
   [juxt.site.init :as init :refer [do-action]]
   [juxt.site.repl :as repl]
   [juxt.test.util :refer [with-system-xt with-resources with-fixtures *handler*
                           with-resources with-handler
                           with-request-body]]))

(use-fixtures :each with-system-xt with-handler)

(def dependency-graph
  {"https://example.org/actions/install-graphql-type"
   {:deps #{::init/system}
    :create (fn [{:keys [id]}]
              (do-action
               "https://example.org/subjects/system"
               "https://example.org/actions/create-action"
               {:xt/id id

                :juxt.site/transact
                {:juxt.site.sci/program
                 (pr-str
                  '(do (throw (ex-info "TODO" {}))))}

                :juxt.site/rules
                '[
                  [(allowed? subject resource permission)
                   [subject :juxt.site/user-identity id]
                   [id :juxt.site/user user]
                   [permission :juxt.site/user user]]]}))}

   "https://example.org/permissions/{username}/install-graphql-type"
   {:deps #{::init/system
            "https://example.org/actions/install-graphql-type"}
    :create (fn [{:keys [id params]}]
              (do-action
               "https://example.org/subjects/system"
               "https://example.org/actions/grant-permission"
               {:xt/id id
                :juxt.site/action "https://example.org/actions/install-graphql-type"
                :juxt.site/purpose nil
                :juxt.site/user (format "https://example.org/users/%s" (get params "username"))}))}

   "https://example.org/graphql/_types/{typename}"
   {:deps #{::init/system
            "https://example.org/protection-spaces/bearer"}
    :create (fn [{:keys [id]}]
              (init/put! ;; TODO: install remotely
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/uri-template true
                 :juxt.site/methods
                 {:put {:juxt.site/actions #{"https://example.org/actions/install-graphql-type"}}}
                 :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}})))}

   #_"https://example.org/graphql"
   #_{:deps #{::init/system
              "https://example.org/actions/query-with-graphql"
              "https://example.org/protection-spaces/bearer"}
      :create (fn [{:keys [id]}]
                (init/put! ;; install-graphql-endpoint
                 (init/substitute-actual-base-uri
                  {:xt/id id
                   :juxt.site/methods
                   {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                   :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}})))}

   })

(with-fixtures
  (with-resources
    ^{:dependency-graphs
      #{session-scope/dependency-graph
        user/dependency-graph
        form-based-auth/dependency-graph
        oauth/dependency-graph
        protection-space/dependency-graph
        example-users/dependency-graph
        example-applications/dependency-graph
        example-protection-spaces/dependency-graph
        dependency-graph}}
    #{"https://site.test/user-identities/alice" ; Alice
      "https://site.test/login"         ; a way Alice can identity herself
      "https://site.test/applications/test-app" ; an app
      ::oauth/authorization-server              ; a way of authorizing the app
      "https://site.test/permissions/alice-can-authorize" ; which Alice can use

      "https://site.test/actions/install-graphql-type"
      "https://site.test/permissions/alice/install-graphql-type"

      "https://site.test/graphql/_types/{typename}"}

    (let [login-result
          (form-based-auth/login-with-form!
           *handler*
           "username" "alice"
           "password" "garden"
           :juxt.site/uri "https://site.test/login")

          session-token (:juxt.site/session-token login-result)
          _ (assert session-token)

          {access-token "access_token"}
          (oauth/authorize!
           {:juxt.site/session-token session-token
            "client_id" "test-app"})]

      (assert access-token)

      (repl/e "https://site.test/graphql/_types/{typename}")

      (*handler*
       (with-request-body
         {:ring.request/path "/graphql/_types/Customer"
          :ring.request/method :put
          :ring.request/headers
          {"authorization" (format "Bearer %s" access-token)}}
         (.getBytes (pr-str {})))))))
