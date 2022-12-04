;; Copyright © 2022, JUXT LTD.

(ns juxt.site.whoami-test
  (:require
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
   [juxt.test.util :refer [with-system-xt with-resources with-fixtures *handler* with-resources with-handler]]))

(use-fixtures :each with-system-xt with-handler)

(def dependency-graph
  {"https://example.org/actions/whoami"
   {:deps #{::init/system}
    :create (fn [{:keys [id]}]
              (do-action
               "https://example.org/subjects/system"
               "https://example.org/actions/create-action"
               {:xt/id id

                ;; NOTE: This means: Use the action to extract part of the
                ;; resource's state.  Actions are used to extract
                ;; protected data, particularly part of the state of a
                ;; resource.
                ;;
                ;; NOTE: Actions emit DATA, not form. It is the data that
                ;; an action is protecting and managing, not a particular
                ;; view of it.

                :juxt.site/state
                {:juxt.site.sci/program
                 (pr-str
                  '{:subject
                    (xt/pull
                     '[* {:juxt.site/user-identity [* {:juxt.site/user [*]}]}]
                     (:xt/id (:juxt.site/subject *ctx*)))})}

                :juxt.site/rules
                '[
                  [(allowed? subject resource permission)
                   [subject :juxt.site/user-identity id]
                   [id :juxt.site/user user]
                   [permission :juxt.site/user user]]]}))}

   "https://example.org/permissions/{username}/whoami"
   {:deps #{::init/system
            "https://example.org/actions/whoami"}
    :create (fn [{:keys [id params]}]
              (let [username (get params "username")]
                (do-action
                 "https://example.org/subjects/system"
                 "https://example.org/actions/grant-permission"
                 (let [user (format "https://example.org/users/%s" username)]
                   {:xt/id id
                    :juxt.site/action "https://example.org/actions/whoami"
                    :juxt.site/purpose nil
                    :juxt.site/user user}))))}

   "https://example.org/whoami"
   {:deps #{::init/system
            "https://example.org/actions/whoami"
            "https://example.org/protection-spaces/bearer"}
    :create (fn [{:keys [id]}]
              (init/put!
               {:xt/id id
                :juxt.site/methods
                {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}))}

   "https://example.org/whoami.json"
   {:deps #{::init/system
            "https://example.org/actions/whoami"
            "https://example.org/protection-spaces/bearer"}
    :create (fn [{:keys [id]}]
              (init/put!
               {:xt/id id
                :juxt.site/methods
                {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                :juxt.site/variant-of "https://example.org/whoami"
                :juxt.http/content-type "application/json"
                :juxt.site/respond
                {:juxt.site.sci/program
                 (pr-str
                  '(let [content (jsonista.core/write-value-as-string *state*)]
                     (-> *ctx*
                         (assoc :ring.response/body content)
                         (update :ring.response/headers assoc "content-length" (count (.getBytes content)))
                         )))}
                :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}))}

   "https://example.org/whoami.html"
   {:deps #{::init/system
            "https://example.org/actions/whoami"
            "https://example.org/protection-spaces/bearer"}
    :create (fn [{:keys [id]}]
              (init/put!
               {:xt/id id
                :juxt.site/methods
                {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                :juxt.site/variant-of "https://example.org/whoami"
                :juxt.http/content-type "text/html;charset=utf-8"
                :juxt.site/respond
                {:juxt.site.sci/program
                 (pr-str
                  '(let [content (format "<h1>Hello World! state is %s</h1>\n" (pr-str *state*))]
                     (-> *ctx*
                         (assoc :ring.response/body content)
                         (update :ring.response/headers assoc "content-length" (count (.getBytes content)))
                         )))}
                :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}))}})

;;deftest get-subject-test

(with-fixtures
  (with-resources
    ^{:dependency-graphs
      #{;;session-scope/dependency-graph
        ;;user/dependency-graph
        ;;form-based-auth/dependency-graph
        ;;oauth/dependency-graph
        ;;protection-space/dependency-graph
        ;;example-users/dependency-graph
        ;;example-applications/dependency-graph
        ;;example-protection-spaces/dependency-graph
        ;;dependency-graph
        }}
    #{;;"https://example.org/login"
      ;;"https://example.org/user-identities/alice"
      ;;"https://example.org/whoami"
      ;;"https://example.org/whoami.json"
      ;;"https://example.org/whoami.html"
      ;;"https://example.org/permissions/alice/whoami"
      ;;"https://example.org/applications/test-app"
      ;;::oauth/authorization-server
      ;;"https://example.org/permissions/alice-can-authorize"
      }

    #_(let [login-result
          (form-based-auth/login-with-form!
           *handler*
           "username" "alice"
           "password" "garden"
           :juxt.site/uri "https://example.org/login")

          session-token (:juxt.site/session-token login-result)
          _ (assert session-token)

          {access-token "access_token"}
          (oauth/authorize!
           {:juxt.site/session-token session-token
            "client_id" "test-app"})]

      (assert access-token)

      (let [{:ring.response/keys [status headers body]}
            (*handler*
             {:juxt.site/uri "https://example.org/whoami"
              :ring.request/method :get
              :ring.request/headers
              {"authorization" (format "Bearer %s" access-token)
               "accept" "application/json"}})]
        (is (= 200 status))
        (is (= "Alice"
               (-> body
                   json/read-value
                   (get-in ["subject"
                            "juxt.site/user-identity"
                            "juxt.site/user"
                            "name"] body))))
        (is (= "application/json" (get headers "content-type")))
        (is (= "https://example.org/whoami.json" (get headers "content-location"))))

      (let [{:ring.response/keys [status headers body] :as response}
            (*handler*
             {:juxt.site/uri "https://example.org/whoami.html"
              :ring.request/method :get
              :ring.request/headers
              {"authorization" (format "Bearer %s" access-token)
               }})]
        (is (= 200 status))
        (is (= "text/html;charset=utf-8" (get headers "content-type")))))))
