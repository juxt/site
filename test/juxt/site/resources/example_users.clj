;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.example-users
  (:require
   [clojure.string :as str]
   [juxt.site.resources.user :as user]
   [malli.core :as malli]))

;; These are example users that are useful for testing

(defn grant-permission-to-authorize!
  [{:keys [username]}]
  ;;{:pre [(malli/validate [:map [:username [:string]]] args)]}
  ;; Grant user permission to perform /actions/oauth/authorize
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id (format "https://example.org/permissions/%s-can-authorize" (str/lower-case username))
    :juxt.site/action "https://example.org/actions/oauth/authorize"
    :juxt.site/user (format "https://example.org/users/%s" (str/lower-case username))
    :juxt.site/purpose nil}})

(malli/=>
 grant-permission-to-authorize!
 [:=> [:cat
       [:map [:username [:string]]]]
  :any])

(def username->user-details
  {"alice" {:name "Alice" :password "garden"}
   "bob" {:name "Bob" :password "walrus"}
   "carlos" {:name "Carlos" :password "jackal"}})

(def dependency-graph
  {"https://example.org/permissions/{username}-can-authorize"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-authorize! {:username (get params "username")}))
    :deps (fn [{:keys [params]}]
            #{:juxt.site.init/system
              (format "https://example.org/actions/oauth/authorize")
              (format "https://example.org/users/%s" (get params "username"))})}

   "https://example.org/users/{username}"
   {:deps #{:juxt.site.init/system
            "https://example.org/actions/put-user"
            "https://example.org/permissions/system/put-user"}
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
   {:deps (fn [{:keys [params]}]
            #{:juxt.site.init/system
              (format "https://example.org/users/%s" (get params "username"))
              "https://example.org/actions/put-user-identity"
              "https://example.org/permissions/system/put-user-identity"})
    :create
    (fn [{:keys [id params]}]
      ;; TODO: Make this data rather than calling a function! (The
      ;; intention here is to demote this graphs to data;
      (let [username (get params "username")]
        {:juxt.site/subject-id               "https://example.org/subjects/system"
         :juxt.site/action-id "https://example.org/actions/put-user-identity"
         :juxt.site/input
         (let [user-details (username->user-details username)
               user-id (format "https://example.org/users/%s" username)]
           {:xt/id id
            :juxt.site/user user-id
            :juxt.site/username username
            :juxt.site/password (:password user-details)})}))}})
