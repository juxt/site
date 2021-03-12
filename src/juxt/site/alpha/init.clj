;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [crux.api :as x]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.site.alpha.util :as util]
   [juxt.reap.alpha.regex :as re]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders])
  (:import
   (java.util Date)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn put! [crux-node & ms]
  (->>
   (x/submit-tx
    crux-node
    (for [m ms]
      [:crux.tx/put m]))
   (x/await-tx crux-node)))

(defn put-master-user!
  "Create the master user."
  [crux-node {::site/keys [canonical-host master-user master-password]}]
  (assert master-password)
  (put!
   crux-node
   {:crux.db/id (str "https://" canonical-host "/_site/users/" master-user)
    ::site/type "User"
    ::pass/username master-user
    ::http/methods #{:get :head :options}}

   {:crux.db/id (str "https://" canonical-host "/_site/users/" master-user "/password")
    ::site/type "Password"
    ::http/methods #{:post}
    ::pass/user (str "https://" canonical-host "/_site/users/" master-user)
    ::pass/password-hash (password/encrypt master-password)
    ::pass/classification "RESTRICTED"}

   ;; Add rule that allows the master user to do everything, at least during the
   ;; bootstrap phase of a deployment. This can be deleted after the initial
   ;; users/roles have been populated, if required.
   {:crux.db/id (str "https://" canonical-host "/_site/rules/master-user-allow-all")
    :description "The master user has access to everything"
    ::site/type "Rule"
    ::pass/target [['subject :juxt.pass.alpha/username master-user]]
    ::pass/effect ::pass/allow
    ::http/max-content-length (Math/pow 2 40)}))

(defn allow-public-access-to-public-resources!
  "Resources classified as PUBLIC should be readable (but not writable). For
  example, a login page needs to be a PUBLIC resource."
  [crux-node {::site/keys [canonical-host]}]
  (put!
   crux-node
   {:crux.db/id (str "https://" canonical-host "/_site/rules/public-resources")
    ::site/type "Rule"
    ::site/description "PUBLIC resources are accessible to GET"
    ::pass/target '[[request :ring.request/method #{:get :head :options}]
                    [resource ::pass/classification "PUBLIC"]]
    ::pass/effect ::pass/allow}))

(defn restict-access-to-restricted-resources!
  "Resources classified as RESTRICTED should never be accessed, unless another
  policy explicitly authorizes access."
  [crux-node {::site/keys [canonical-host]}]
  (put!
   crux-node
   {:crux.db/id (str "https://" canonical-host "/_site/rules/restricted-resources")
    ::site/type "Rule"
    ::site/description "RESTRICTED access is denied by default"
    ::pass/target '[[resource ::pass/classification "RESTRICTED"]]
    ::pass/effect ::pass/deny}))

(defn put-site-api!
  "Add the Site API"
  [crux-node json {::site/keys [canonical-host]}]
  (let [openapi (json/read-value json)
        body (.getBytes json "UTF-8")]
    (put!
     crux-node
     {:crux.db/id (str "https://" canonical-host "/_site/apis/site/openapi.json")
      ::site/type "OpenAPI"
      ::http/methods #{:get :head :options}
      ::http/content-type "application/vnd.oai.openapi+json;version=3.0.2"
      ;; TODO: Get last modified from resource - check JDK javadocs
      ;;::http/last-modified (Date. (.lastModified f))
      ::http/content-length (count body)
      ::http/body body
      ::apex/openapi openapi
      ;; Just for now, while we figure out how to set classifications
      ::pass/classification "PUBLIC"})))

(defn put-openid-token-endpoint! [crux-node {::site/keys [canonical-host]}]
  (let [token-endpoint (str "https://" canonical-host "/_site/token")
        grant-types #{"client_credentials"}]
    (put!
     crux-node
     {:crux.db/id token-endpoint
      ::http/methods #{:post}
      ::http/acceptable "application/x-www-form-urlencoded"
      ::site/purpose ::site/acquire-token
      ::pass/expires-in (* 60 60 1)}

     {:crux.db/id (str "https://" canonical-host "/_site/rules/anyone-can-ask-for-a-token")
      ::site/type "Rule"
      ::site/description "The token_endpoint must be accessible"
      ::pass/target '[[request :ring.request/method #{:post}]
                      [resource ::site/purpose ::site/acquire-token]]
      ::pass/effect ::pass/allow})

    (let [content
          (str
           (json/write-value-as-string
            {"issuer" "https://juxt.site" ; draft
             "token_endpoint" token-endpoint
             "token_endpoint_auth_methods_supported" ["client_secret_basic"]
             "grant_types_supported" (vec grant-types)}
            (json/object-mapper
             {:pretty true}))
           "\r\n")]
      (put!
       crux-node
       {:crux.db/id (str "https://" canonical-host "/.well-known/openid-configuration")
        ;; OpenID Connect Discovery documents are publically available
        ::pass/classification "PUBLIC"
        ::http/methods #{:get :head :options}
        ::http/content-type "application/json"
        ::http/last-modified (Date.)
        ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
        ::http/content content}))))

(defn put-login-endpoint! [crux-node {::site/keys [canonical-host]}]
  ;; Allow anyone to login
  (put!
   crux-node
   {:crux.db/id (str "https://" canonical-host "/_site/login")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/login
    ::pass/expires-in (* 3600 24 30)}

   {:crux.db/id (str "https://" canonical-host "/_site/rules/anyone-can-post-login-credentials")
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/login]]
    ::pass/effect ::pass/allow}))

(defn put-logout-endpoint! [crux-node {::site/keys [canonical-host]}]
  ;; Allow anyone to login
  (put!
   crux-node
   {:crux.db/id (str "https://" canonical-host "/_site/logout")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/logout}

   {:crux.db/id (str "https://" canonical-host "/_site/rules/anyone-can-post-logout-credentials")
    ::site/type "Rule"
    ::site/description "The logout POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/logout]]
    ::pass/effect ::pass/allow}))

(def host-parser (rfc7230.decoders/host {}))

(defn init-db!
  "Initialize the database. You usually call this as part of setting up a new Site
  instance. It's safe to call multiple times. No data is deleted."
  [crux-node {::site/keys [canonical-host
                           master-user
                           master-password]
              :as opts}]

  (let [db (x/db crux-node)
        site-settings (x/entity db ::site/init-settings)
        opts (merge site-settings opts)]

    (when site-settings                 ; existing database

      (when (and canonical-host
                 (not= canonical-host (::site/canonical-host site-settings)))
        (throw
         (ex-info
          "Canonical host is immutable once configured"
          {:existing-canonical-host (::site/canonical-host site-settings)
           :requested-canonical-host canonical-host})))

      (when master-user
        (throw
         (ex-info
          "Master user is immutable once configured"
          {:existing-master-user (::site/master-user site-settings)
           :requested-master-user master-user})))

      (when master-password (put-master-user! crux-node opts)))

    (when-not site-settings             ; new database

      (when-not canonical-host
        (throw
         (ex-info
          (format "Must provide a value for %s when initializing a new site database" ::site/canonical-host)
          {})))

      (try
        (host-parser (re/input canonical-host))
        (catch Exception e
          (throw (ex-info "Canonical host must be a hostname" {::site/canonical-host canonical-host} e))))

      (when-not master-password
        (throw
         (ex-info
          (format "Must provide a value for %s (for user %s), when initializing a new site database" ::site/master-password master-user)
          {})))

      (when-not (re-matches #"[\p{Punct}\p{Alnum}]{8,}" master-password)
        (throw
         (ex-info
          "Master password not sufficiently strong or contains invalid characters"
          {})))

      (put!
       crux-node
       (merge
        {:crux.db/id ::site/init-settings
         ::site/master-user "webmaster"}
        (dissoc opts ::site/master-password)))

      (put-master-user!
       crux-node (merge {::site/master-user (or master-user "webmaster")} opts)))

    ;; Initial access policies
    (allow-public-access-to-public-resources! crux-node opts)
    (restict-access-to-restricted-resources! crux-node opts)

    ;; Site API allows management of users
    (put-site-api!
     crux-node
     (-> "juxt/site/alpha/openapi.edn"
         io/resource
         slurp
         edn/read-string
         json/write-value-as-string)
     opts)

    ;; Authentication
    (put-openid-token-endpoint! crux-node opts)
    (put-login-endpoint! crux-node opts)
    (put-logout-endpoint! crux-node opts)))
