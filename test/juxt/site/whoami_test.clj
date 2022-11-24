;; Copyright © 2022, JUXT LTD.

(ns juxt.site.whoami-test
  (:require
   [clojure.edn :as edn]
   [juxt.site.logging :refer [with-logging]]
   [clojure.tools.logging :as log]
   [jsonista.core :as json]
   [edn-query-language.core :as eql]
   [juxt.site.eql-datalog-compiler :as eqlc]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [java-http-clj.core :as hc]
   [juxt.site.resources.openid :as openid]
   [juxt.site.resources.oauth :as oauth]
   [juxt.site.resources.session-scope :as session-scope]
   [juxt.site.resources.user :as user]
   [juxt.site.resources.form-based-auth :as form-based-auth]
   [juxt.site.resources.example-users :as example-users]
   [juxt.site.resources.example-applications :as example-applications]
   [juxt.site :as-alias site]
   [juxt.site.init :as init]
   [juxt.site.repl :as repl]
   [juxt.site.bootstrap :as bootstrap]
   [juxt.test.util :refer [with-system-xt with-resources *handler* *xt-node* with-fixtures with-resources assoc-session-token with-handler assoc-session-token lookup-session-details]]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [juxt.reap.alpha.regex :as re]))

(use-fixtures :each with-system-xt with-handler)

(def dependency-graph
  {"https://example.org/actions/whoami"
   {:deps #{::init/system}
    :create (fn [{:keys [id]}]
              (init/do-action
               (init/substitute-actual-base-uri "https://example.org/subjects/system")
               (init/substitute-actual-base-uri "https://example.org/actions/create-action")
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
                (juxt.site.init/do-action
                 (init/substitute-actual-base-uri "https://example.org/subjects/system")
                 (init/substitute-actual-base-uri "https://example.org/actions/grant-permission")
                 (let [user (format "https://example.org/users/%s" username)]
                   (init/substitute-actual-base-uri
                    {:xt/id id
                     :juxt.site/action "https://example.org/actions/whoami"
                     :juxt.site/purpose nil
                     :juxt.site/user user})))))}

   ;; TODO: Create an action for establishing a protection space
   "https://example.org/bearer-protection-space"
   {:deps #{::init/system}
    :create (fn [{:keys [id]}]
              (init/put!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/auth-scheme "Bearer"})))}

   "https://example.org/whoami"
   {:deps #{::init/system
            "https://example.org/actions/whoami"
            "https://example.org/bearer-protection-space"}
    :create (fn [{:keys [id]}]
              (init/put!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/methods
                 {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                 :juxt.site/protection-spaces #{"https://example.org/bearer-protection-space"}})))}

   "https://example.org/whoami.json"
   {:deps #{::init/system
            "https://example.org/actions/whoami"}
    :create (fn [{:keys [id]}]
              (init/put!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/methods
                 {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                 :juxt.site/variant-of "https://example.org/whoami"
                 :juxt.http/content-type "application/json"
                 ;; TODO: Rename to :juxt.site/respond
                 :juxt.http/respond
                 {:juxt.site.sci/program
                  (pr-str
                   '(let [content (jsonista.core/write-value-as-string *state*)]
                      (-> *ctx*
                          (assoc :ring.response/body content)
                          (update :ring.response/headers assoc "content-length" (count (.getBytes content)))
                          )))}
                 ;; TODO: protection space?
                 })))}

   "https://example.org/whoami.html"
   {:deps #{::init/system
            "https://example.org/actions/whoami"}
    :create (fn [{:keys [id]}]
              (init/put!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/methods
                 {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                 :juxt.site/variant-of "https://example.org/whoami"
                 :juxt.http/content-type "text/html;charset=utf-8"
                 :juxt.http/respond
                 {:juxt.site.sci/program
                  (pr-str
                   '(let [content (format "<h1>Hello World! state is %s</h1>\n" (pr-str *state*))]
                      (-> *ctx*
                          (assoc :ring.response/body content)
                          (update :ring.response/headers assoc "content-length" (count (.getBytes content)))
                          )))}})))}})

(deftest get-subject-test
  (with-resources
    ^{:dependency-graphs
      #{session-scope/dependency-graph
        user/dependency-graph
        form-based-auth/dependency-graph
        oauth/dependency-graph
        example-users/dependency-graph
        example-applications/dependency-graph
        dependency-graph}}
    #{"https://site.test/login"
      "https://site.test/user-identities/alice"
      "https://site.test/whoami"
      "https://site.test/whoami.json"
      "https://site.test/whoami.html"
      "https://site.test/permissions/alice/whoami"
      "https://site.test/applications/test-app"
      ::oauth/authorization-server
      "https://site.test/permissions/alice-can-authorize"}

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

      (when access-token
        (let [{:ring.response/keys [status body]}
              (*handler*
               {:juxt.site/uri "https://site.test/whoami"
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
                              "name"] body)))))))))

;; Note: If we try to login (with basic), we'll won't need to user 'put' (which will
;; lead to dangerously brittle tests if/when we change the structure of internal
;; documents like sessions and session-tokens).

;; TODO
;; Login alice with basic (ensuring session scope exists)
;; Passing the session-token as a cookie, call the /whoami resource.
;; Build the functionality of GET /whoami into the action (in the prepare part of the transaction)
