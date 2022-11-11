;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.form-based-auth-test
  (:require
   [clojure.edn :as edn]
   [malli.core :as malli]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [juxt.pass.form-based-auth :as form-based-auth]
   [java-http-clj.core :as hc]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.openid :as openid]
   [juxt.pass.session-scope :as session-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.user :as user]
   [juxt.site.alpha.init :as init]
   [juxt.site.alpha.repl :as repl]
   [juxt.site.bootstrap :as bootstrap]
   [juxt.test.util :refer [*handler* *xt-node* with-fixtures with-resources with-system-xt with-handler]]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [juxt.reap.alpha.regex :as re]))

(use-fixtures :each with-system-xt with-handler)

(def username->user-details
  {"alice" {:name "Alice" :password "garden"}})

(def dependency-graph
  {"https://example.org/~{username}"
   ;; TODO: Note this predates example-users, so is now redundant
   {:deps #{::init/system
            ::user/all-actions
            ::user/default-permissions}
    :create (fn [{:keys [id params]}]
              (let [username (get params "username")]
                (user/put-user!
                 {:id id
                  :username username
                  :name (:name (or
                                (username->user-details username)
                                (throw
                                 (ex-info
                                  (format "No name for '%s'" username)
                                  {:username username}))))})))}

   "https://example.org/user-identities/{username}"
   {:deps (fn [params {:juxt.site.alpha/keys [base-uri]}]
            #{::init/system
              (str base-uri "/~" (get params "username"))})
    :create (fn [{:keys [id params]}]
              ;; TODO: Make this data rather than calling a function! (The
              ;; intention here is to demote this graphs to data;
              (init/do-action
               (init/substitute-actual-base-uri "https://example.org/subjects/system")
               (init/substitute-actual-base-uri "https://example.org/actions/put-basic-user-identity")
               (let [username (get params "username")
                     user (str "https://example.org/~" username)]
                 (init/substitute-actual-base-uri
                  {:xt/id id
                   :juxt.pass.alpha/user user
                   :juxt.pass.alpha/username username
                   :juxt.pass.alpha/password
                   (or (:password (username->user-details username))
                       (throw
                        (ex-info
                         (format "No password for '%s'" username)
                         {:username username})))}))))}})

(deftest login-with-form-test
  (with-resources
    ^{:dependency-graphs
      #{session-scope/dependency-graph
        form-based-auth/dependency-graph
        user/dependency-graph
        dependency-graph}}
    #{"https://site.test/login"
      "https://site.test/session-scopes/default"
      "https://site.test/~alice"
      "https://site.test/user-identities/alice"}

    (let [result (form-based-auth/login-with-form!
                  *handler*
                 :juxt.site.alpha/uri "https://site.test/login"
                 "username" "ALICE"
                 "password" "garden")]

      (is (malli/validate [:map [:juxt.pass.alpha/session-token :string]] result)))))
