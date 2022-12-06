;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.user
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]))

(defn create-action-put-user! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/put-user"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (with-out-str
       (pprint
        '(let [content-type (-> *ctx*
                                :juxt.site/received-representation
                                :juxt.http/content-type)
               body (-> *ctx*
                        :juxt.site/received-representation
                        :juxt.http/body)]
           (case content-type
             "application/edn"
             (some->
              body
              (String.)
              clojure.edn/read-string
              juxt.site.malli/validate-input
              (assoc
               :juxt.site/methods
               {:get {:juxt.site/actions #{"https://example.org/actions/get-user"}}
                :head {:juxt.site/actions #{"https://example.org/actions/get-user"}}
                :options {}}))))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]

      [(allowed? subject resource permission) ; <5>
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [user :role role]
       [permission :role role]]]}})

(defn grant-permission-to-invoke-action-put-user! [_]
  ;; tag::grant-permission-to-invoke-action-put-user![]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/system/put-user"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/put-user"
    :juxt.site/purpose nil}}
  ;; end::grant-permission-to-invoke-action-put-user![]
  )

(defn create-action-put-user-identity! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/put-user-identity"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/.*"]]
     [:juxt.site/user [:re "https://example.org/.+"]]

     ;; Required by basic-user-identity
     [:juxt.site/username [:re "[A-Za-z0-9]{2,}"]]
     ;; NOTE: Can put in some password rules here
     [:juxt.site/password [:string {:min 6}]]
     ;;[:juxt.site.jwt.claims/iss {:optional true} [:re "https://.+"]]
     ;;[:juxt.site.jwt.claims/sub {:optional true} [:string {:min 1}]]
     ]

    :juxt.site/prepare
    {:juxt.site.sci/program

     (with-out-str
       (pprint
        '(let [content-type (-> *ctx*
                                :juxt.site/received-representation
                                :juxt.http/content-type)
               body (-> *ctx*
                        :juxt.site/received-representation
                        :juxt.http/body)]
           (case content-type
             "application/edn"
             (let [input
                   (some->
                    body
                    (String.)
                    clojure.edn/read-string
                    juxt.site.malli/validate-input
                    (assoc
                     :juxt.site/type #{"https://meta.juxt.site/site/user-identity"
                                       "https://meta.juxt.site/site/basic-user-identity"}
                     :juxt.site/methods
                     {:get {:juxt.site/actions #{"https://example.org/actions/get-user-identity"}}
                      :head {:juxt.site/actions #{"https://example.org/actions/get-user-identity"}}
                      :options {}}))]
               (-> input
                   ;; We want usernames to be case-insensitive, as per OWASP
                   ;; guidelines.
                   (update :juxt.site/username clojure.string/lower-case)
                   ;; Hash the password
                   (assoc :juxt.site/password-hash (crypto.password.bcrypt/encrypt (:juxt.site/password input)))
                   ;; Remove clear-text password from input, so it doesn't go into
                   ;; the database.
                   (dissoc :juxt.site/password)))))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]

      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [user :role role]
       [permission :role role]]]}})

(defn grant-permission-to-invoke-action-put-user-identity! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/system/put-user-identity"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/put-user-identity"
    :juxt.site/purpose nil}})

;; TODO: Make this ns independent of openid. All the openid actions/permissions
;; should be demoted to the openid ns.
(def dependency-graph
  {"https://example.org/actions/put-user"
   {:create #'create-action-put-user!
    :deps #{:juxt.site.init/system}}

   "https://example.org/permissions/system/put-user"
   {:create #'grant-permission-to-invoke-action-put-user!
    :deps #{:juxt.site.init/system}}

   "https://example.org/actions/put-user-identity"
   {:create #'create-action-put-user-identity!
    :deps #{:juxt.site.init/system}}

   "https://example.org/permissions/system/put-user-identity"
   {:create #'grant-permission-to-invoke-action-put-user-identity!
    :deps #{:juxt.site.init/system}}})

(defn put-user! [& {:keys [id username name]}]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/put-user"
   :juxt.site/input
   {:xt/id (or id (format "https://example.org/users/%s" username))
    :name name}})

(defn put-user-identity! [& {:juxt.site/keys [username password realm]}]
  (assert username)
  (assert password)
  (assert realm)
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/put-user-identity"
   :juxt.site/input
   {:xt/id (format "https://example.org/user-identities/%s/basic" (str/lower-case username))
    :juxt.site/user (format "https://example.org/users/%s" (str/lower-case username))
    ;; Perhaps all user identities need this?
    :juxt.site/canonical-root-uri "https://example.org"
    :juxt.site/realm realm
    ;; Basic auth will only work if these are present
    :juxt.site/username username
    ;; This will be encrypted
    :juxt.site/password password}})

;; TODO: Could hashes be used to indicate which resources need to be installed
;; versus which ones are already up-to-date.  This would help performance but
;; all give the user confidence when a change is made to a resource that _only_
;; that change is going to applied.
