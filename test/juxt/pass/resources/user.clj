;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.resources.user
  (:require
   [clojure.string :as str]
   [juxt.site.init :as init :refer [substitute-actual-base-uri]]))

(defn create-action-put-user! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
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
                {:get {:juxt.pass/actions #{"https://example.org/actions/get-user"}}
                 :head {:juxt.pass/actions #{"https://example.org/actions/get-user"}}
                 :options {}})))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass/subject subject]]

         [(allowed? subject resource permission) ; <5>
          [subject :juxt.pass/user-identity id]
          [id :juxt.pass/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-user! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-user![]
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-user"
       :juxt.pass/subject "https://example.org/subjects/system"
       :juxt.pass/action "https://example.org/actions/put-user"
       :juxt.pass/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-user![]
     ))))

(defn create-action-put-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user-identity"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]
        [:juxt.pass/user [:re "https://example.org/.+"]]

        ;; Required by basic-user-identity
        [:juxt.pass/username [:re "[A-Za-z0-9]{2,}"]]
        ;; NOTE: Can put in some password rules here
        [:juxt.pass/password [:string {:min 6}]]
        ;;[:juxt.pass.jwt.claims/iss {:optional true} [:re "https://.+"]]
        ;;[:juxt.pass.jwt.claims/sub {:optional true} [:string {:min 1}]]
        ]

       :juxt.site/prepare
       {:juxt.site.sci/program

        (pr-str
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
                      :juxt.site/type #{"https://meta.juxt.site/pass/user-identity"
                                              "https://meta.juxt.site/pass/basic-user-identity"}
                      :juxt.site/methods
                      {:get {:juxt.pass/actions #{"https://example.org/actions/get-user-identity"}}
                       :head {:juxt.pass/actions #{"https://example.org/actions/get-user-identity"}}
                       :options {}}))]
                (-> input
                    ;; We want usernames to be case-insensitive, as per OWASP
                    ;; guidelines.
                    (update :juxt.pass/username clojure.string/lower-case)
                    ;; Hash the password
                    (assoc :juxt.pass/password-hash (crypto.password.bcrypt/encrypt (:juxt.pass/password input)))
                    ;; Remove clear-text password from input, so it doesn't go into
                    ;; the database.
                    (dissoc :juxt.pass/password))))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass/user-identity id]
          [id :juxt.pass/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-user-identity"
       :juxt.pass/subject "https://example.org/subjects/system"
       :juxt.pass/action "https://example.org/actions/put-user-identity"
       :juxt.pass/purpose nil})))))

;; TODO: Make this ns independent of openid. All the openid actions/permissions
;; should be demoted to the openid ns.
(def dependency-graph
  {"https://example.org/actions/put-user"
   {:create #'create-action-put-user!
    :deps #{::init/system}}

   "https://example.org/permissions/system/put-user"
   {:create #'grant-permission-to-invoke-action-put-user!
    :deps #{::init/system}}

   "https://example.org/actions/put-user-identity"
   {:create #'create-action-put-user-identity!
    :deps #{::init/system}}

   "https://example.org/permissions/system/put-user-identity"
   {:create #'grant-permission-to-invoke-action-put-user-identity!
    :deps #{::init/system}}})

(defn put-user! [& {:keys [id username name]}]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-user")
   (substitute-actual-base-uri
    {:xt/id (or id (format "https://example.org/users/%s" username))
     :name name})))

(defn put-user-identity! [& {:juxt.pass/keys [username password realm]}]
  (assert username)
  (assert password)
  (assert realm)
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-user-identity")
   (substitute-actual-base-uri
    {:xt/id (format "https://example.org/user-identities/%s/basic" (str/lower-case username))
     :juxt.pass/user (format "https://example.org/users/%s" (str/lower-case username))
     ;; Perhaps all user identities need this?
     :juxt.pass/canonical-root-uri "https://example.org"
     :juxt.pass/realm realm
     ;; Basic auth will only work if these are present
     :juxt.pass/username username
     ;; This will be encrypted
     :juxt.pass/password password})))

;; TODO: Could hashes be used to indicate which resources need to be installed
;; versus which ones are already up-to-date.  This would help performance but
;; all give the user confidence when a change is made to a resource that _only_
;; that change is going to applied.
