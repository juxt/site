;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.example-users
  (:require
   [clojure.string :as str]
   [juxt.site.init :as init :refer [do-action]]
   [juxt.site.resources.user :as user]
   [malli.core :as malli]))

;; These are example users that are useful for testing

(defn grant-permission-to-authorize!
  [{:keys [username]}]
  ;;{:pre [(malli/validate [:map [:username [:string]]] args)]}
  ;; Grant user permission to perform /actions/oauth/authorize
  (do-action
   "https://example.org/subjects/system"
   "https://example.org/actions/grant-permission"
   {:xt/id (format "https://example.org/permissions/%s-can-authorize" (str/lower-case username))
    :juxt.site/action "https://example.org/actions/oauth/authorize"
    :juxt.site/user (format "https://example.org/users/%s" (str/lower-case username))
    :juxt.site/purpose nil}))

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
    :deps (fn [{:strs [username]} {:juxt.site/keys [base-uri]}]
            #{::init/system
              (format "%s/actions/oauth/authorize" base-uri)
              (format "%s/users/%s" base-uri username)})}

   "https://example.org/users/{username}"
   {:deps #{::init/system
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
   {:deps (fn [params _]
            #{::init/system
              (format "https://example.org/users/%s" (get params "username"))
              "https://example.org/actions/put-user-identity"
              "https://example.org/permissions/system/put-user-identity"
              })
    :create (fn [{:keys [id params]}]
              ;; TODO: Make this data rather than calling a function! (The
              ;; intention here is to demote this graphs to data;
              (let [username (get params "username")]
                (do-action
                 "https://example.org/subjects/system"
                 "https://example.org/actions/put-user-identity"
                 (let [user-details (username->user-details username)
                       user-id (format "https://example.org/users/%s" username)]
                   {:xt/id id
                    :juxt.site/user user-id
                    :juxt.site/username username
                    :juxt.site/password (:password user-details)}))))}})
