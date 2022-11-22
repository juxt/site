;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.user
  (:require
   [clojure.string :as str]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]))

(defn create-action-put-user! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site.alpha/received-representation
                                 :juxt.http.alpha/content-type)
                body (-> *ctx*
                         :juxt.site.alpha/received-representation
                         :juxt.http.alpha/body)]
            (case content-type
              "application/edn"
              (some->
               body
               (String.)
               clojure.edn/read-string
               juxt.site.malli/validate-input
               (assoc
                :juxt.site.alpha/methods
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                 :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                 :options {}})))))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission) ; <5>
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-user! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-user![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-user"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-user"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-user![]
     ))))

(defn create-action-put-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user-identity"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]
        [:juxt.pass.alpha/user [:re "https://example.org/.+"]]

        ;; Required by basic-user-identity
        [:juxt.pass.alpha/username [:re "[A-Za-z0-9]{2,}"]]
        ;; NOTE: Can put in some password rules here
        [:juxt.pass.alpha/password [:string {:min 6}]]
        ;;[:juxt.pass.jwt.claims/iss {:optional true} [:re "https://.+"]]
        ;;[:juxt.pass.jwt.claims/sub {:optional true} [:string {:min 1}]]
        ]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program

        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site.alpha/received-representation
                                 :juxt.http.alpha/content-type)
                body (-> *ctx*
                         :juxt.site.alpha/received-representation
                         :juxt.http.alpha/body)]
            (case content-type
              "application/edn"
              (let [input
                    (some->
                     body
                     (String.)
                     clojure.edn/read-string
                     juxt.site.malli/validate-input
                     (assoc
                      :juxt.site.alpha/type #{"https://meta.juxt.site/pass/user-identity"
                                              "https://meta.juxt.site/pass/basic-user-identity"}
                      :juxt.site.alpha/methods
                      {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user-identity"}}
                       :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user-identity"}}
                       :options {}}))]
                (-> input
                    ;; We want usernames to be case-insensitive, as per OWASP
                    ;; guidelines.
                    (update :juxt.pass.alpha/username clojure.string/lower-case)
                    ;; Hash the password
                    (assoc :juxt.pass.alpha/password-hash (crypto.password.bcrypt/encrypt (:juxt.pass.alpha/password input)))
                    ;; Remove clear-text password from input, so it doesn't go into
                    ;; the database.
                    (dissoc :juxt.pass.alpha/password))))))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-user-identity"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-user-identity"
       :juxt.pass.alpha/purpose nil})))))

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

(defn put-user-identity! [& {:juxt.pass.alpha/keys [username password realm]}]
  (assert username)
  (assert password)
  (assert realm)
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-user-identity")
   (substitute-actual-base-uri
    {:xt/id (format "https://example.org/user-identities/%s/basic" (str/lower-case username))
     :juxt.pass.alpha/user (format "https://example.org/users/%s" (str/lower-case username))
     ;; Perhaps all user identities need this?
     :juxt.pass.alpha/canonical-root-uri "https://example.org"
     :juxt.pass.alpha/realm realm
     ;; Basic auth will only work if these are present
     :juxt.pass.alpha/username username
     ;; This will be encrypted
     :juxt.pass.alpha/password password})))

;; TODO: Could hashes be used to indicate which resources need to be installed
;; versus which ones are already up-to-date.  This would help performance but
;; all give the user confidence when a change is made to a resource that _only_
;; that change is going to applied.
