;; Copyright © 2022, JUXT LTD.

(ns juxt.example-users
  (:require
   [clojure.string :as str]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.pass.user :as user]
   [malli.core :as malli]))

;; These are example users that are useful for testing

(defn grant-permission-to-authorize!
  [{:keys [username]}]
  ;;{:pre [(malli/validate [:map [:username [:string]]] args)]}
  ;; Grant user permission to perform /actions/oauth/authorize
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id (format "https://example.org/permissions/%s-can-authorize" ~(str/lower-case username))
       :juxt.pass.alpha/action "https://example.org/actions/oauth/authorize"
       :juxt.pass.alpha/user (format "https://example.org/users/%s" ~(str/lower-case username))
       :juxt.pass.alpha/purpose nil}))))

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
    :deps (fn [{:strs [username]} {:juxt.site.alpha/keys [base-uri]}]
            #{::init/system
              (format "%s/actions/oauth/authorize" base-uri)
              (format "%s/users/%s" base-uri username)})}

   "https://example.org/users/{username}"
   {:deps #{::init/system
            "https://example.org/actions/put-user"
            ;;"https://example.org/actions/put-user-identity"
            ;;"https://example.org/actions/put-openid-user-identity"
            "https://example.org/permissions/system/put-user"
            ;;"https://example.org/permissions/system/put-basic-user-identity"
            ;;"https://example.org/permissions/system/put-openid-user-identity"
            }
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
              (format "%s/users/%s" base-uri (get params "username"))
              ;; TODO: Why do we have to substitute here and not in the other
              ;; deps? Is it because this is a function?
              (substitute-actual-base-uri "https://example.org/actions/put-user-identity")
              (substitute-actual-base-uri "https://example.org/permissions/system/put-user-identity")
              })
    :create (fn [{:keys [id params]}]
              ;; TODO: Make this data rather than calling a function! (The
              ;; intention here is to demote this graphs to data;
              (let [username (get params "username")]
                (init/do-action
                 (init/substitute-actual-base-uri "https://example.org/subjects/system")
                 (init/substitute-actual-base-uri "https://example.org/actions/put-user-identity")
                 (let [user-details (username->user-details username)
                       user-id (format "https://example.org/users/%s" username)]
                   (init/substitute-actual-base-uri
                    {:xt/id id
                     :juxt.pass.alpha/user user-id
                     :juxt.pass.alpha/username username
                     :juxt.pass.alpha/password (:password user-details)})))))}})
