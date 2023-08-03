;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha.util :as util])
  (:import
   (java.util Date)
   (com.google.common.net InternetDomainName)
   (org.apache.http.client.utils URIBuilder)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn put! [xtdb-node & ms]
  (->>
   (xt/submit-tx
    xtdb-node
    (for [m ms]
      [::xt/put m]))
   (xt/await-tx xtdb-node)))

(defn put-superuser-role!
  "Create the superuser role."
  [xtdb-node {::site/keys [base-uri]}]
  (log/info "Creating superuser role")
  (let [role (str base-uri "/_site/roles/superuser")]
    (put!
     xtdb-node
     {:xt/id role
      ::site/type "Role"
      :name "superuser"
      :description "Superuser"}

     ;; Add rule that allows superusers to do everything.
     {:xt/id (str base-uri "/_site/rules/superuser-allow-all")
      :description "Superusers can do everything"
      ::site/type "Rule"
      ::pass/target [['subject :juxt.pass.alpha/user 'user]
                     ['mapping :juxt.site.alpha/type "UserRoleMapping"]
                     ['mapping :juxt.pass.alpha/role role]
                     ['mapping :juxt.pass.alpha/assignee 'user]]
      ::pass/effect ::pass/allow
      ::http/max-content-length (Math/pow 2 40)})))

(defn put-superuser!
  "Create a superuser."
  [xtdb-node username password fullname email {::site/keys [base-uri] :as config}]
  (let [user (str base-uri "/_site/users/" username)]
    (put!
     xtdb-node
     {:xt/id user
      ::site/type "User"
      ::pass/username username
      :name fullname
      :email email}

     {:xt/id (str user "/password")
      ::site/type "Password"
      ::pass/user user
      ::pass/password-hash (password/encrypt password)
      ::pass/classification "RESTRICTED"}

     {:xt/id (str user "/oauth-credentials")
      ::site/type "OAuthCredentials"
      ::pass/user user
      :juxt.pass.jwt/iss (-> config :openid :issuer-id)
      :juxt.pass.jwt/sub (-> config :openid :superuser-sub)}

     {:xt/id (format "%s/_site/roles/%s/users/%s" base-uri "superuser" username)
      ::site/type "UserRoleMapping"
      ::pass/assignee (format "%s/_site/users/%s" base-uri username)
      ::pass/role (str base-uri "/_site/roles/superuser")})))

(defn allow-public-access-to-public-resources!
  "Resources classified as PUBLIC should be readable (but not writable). For
  example, a login page needs to be a PUBLIC resource."
  [xtdb-node {::site/keys [base-uri]}]
  (put!
   xtdb-node
   {:xt/id (str base-uri "/_site/rules/public-resources")
    ::site/type "Rule"
    ::site/description "PUBLIC resources are accessible to GET"
    ::pass/target '[[request :ring.request/method #{:get :head :options}]
                    [resource ::pass/classification "PUBLIC"]]
    ::pass/effect ::pass/allow}))

(defn allow-authenticated-users-access-to-user-info!
  "Authenticated users should be able to access their own user details"
  [xtdb-node {::site/keys [base-uri]}]
  (put!
   xtdb-node
   {:xt/id (str base-uri "/_site/rules/any-authenticated-allow-user-info")
    ::site/type "Rule"
    ::site/description "Allow authenticated users to get their user details"
    ::pass/target '[[subject ::pass/user user]
                    [user ::site/type "User"]
                    [request :ring.request/method #{:get :options}]
                    [request :ring.request/path "/_site/user"]]
    ::pass/effect ::pass/allow
    ::http/max-content-length (Math/pow 2 40)}))

(defn restict-access-to-restricted-resources!
  "Resources classified as RESTRICTED should never be accessed, unless another
  policy explicitly authorizes access."
  [xtdb-node {::site/keys [base-uri]}]
  (put!
   xtdb-node
   {:xt/id (str base-uri "/_site/rules/restricted-resources")
    ::site/type "Rule"
    ::site/description "RESTRICTED access is denied by default"
    ::pass/target '[[resource ::pass/classification "RESTRICTED"]]
    ::pass/effect ::pass/deny}))

(defn put-site-api!
  "Add the Site API"
  [xtdb-node json {::site/keys [base-uri]}]
  (log/info "Installing Site API")
  (let [openapi (json/read-value json)
        body (.getBytes json "UTF-8")]
    (put!
     xtdb-node
     {:xt/id (str base-uri "/_site/apis/site/openapi.json")
      ::site/type "OpenAPI"
      ::http/methods #{:get :head :options :put}
      ::http/content-type "application/vnd.oai.openapi+json;version=3.0.2"
      ;; TODO: Get last modified from resource - check JDK javadocs
      ;;::http/last-modified (Date. (.lastModified f))
      ::http/content-length (count body)
      ::http/body body
      ::apex/openapi openapi
      ;; Duplicated from openapi.clj - TODO: remove duplication
      :title (get-in openapi ["info" "title"])
      :version (get-in openapi ["info" "version"])
      :description (get-in openapi ["info" "description"])})))

(defn put-openid-token-endpoint! [xtdb-node {::site/keys [base-uri]}]
  (log/info "Installing OpenID Connect token endpoint")
  (let [token-endpoint (str base-uri "/_site/token")
        grant-types #{"client_credentials"}]
    (put!
     xtdb-node
     {:xt/id token-endpoint
      ::http/methods #{:post :options}
      ::http/acceptable "application/x-www-form-urlencoded"
      ::site/purpose ::site/acquire-token
      ::site/access-control-allow-origins
      {"http://localhost:8000"
       {::site/access-control-allow-methods #{:post}
        ::site/access-control-allow-headers #{"authorization" "content-type"}}}
      ::pass/expires-in (* 3600 24 7)}

     {:xt/id (str base-uri "/_site/rules/anyone-can-ask-for-a-token")
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
       xtdb-node
       {:xt/id (str base-uri "/.well-known/openid-configuration")
        ;; OpenID Connect Discovery documents are publically available
        ::pass/classification "PUBLIC"
        ::http/methods #{:get :head :options}
        ::http/content-type "application/json"
        ::http/last-modified (Date.)
        ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
        ::http/content content}))))

(defn put-login-endpoint! [xtdb-node {::site/keys [base-uri]}]
  (log/info "Installing login endpoint")
  ;; Allow anyone to login
  (put!
   xtdb-node
   {:xt/id (str base-uri "/_site/login")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/login
    ::pass/expires-in (* 3600 24 7)}

   {:xt/id (str base-uri "/_site/rules/anyone-can-post-login-credentials")
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/login]]
    ::pass/effect ::pass/allow}))

(defn put-logout-endpoint! [xtdb-node {::site/keys [base-uri]}]
  (log/info "Installing logout endpoint")
  ;; Allow anyone to login
  (put!
   xtdb-node
   {:xt/id (str base-uri "/_site/logout")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/logout}

   {:xt/id (str base-uri "/_site/rules/anyone-can-post-logout-credentials")
    ::site/type "Rule"
    ::site/description "The logout POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/logout]]
    ::pass/effect ::pass/allow}))

;; Currently awaiting a fix to https://github.com/xtdb/xtdb/issues/1480 because
;; these can be used.
(defn put-site-txfns! [xtdb-node {::site/keys [base-uri]}]
  (xt/submit-tx
   xtdb-node
   [[::xt/put
     {:xt/id (str base-uri "/_site/tx_fns/put_if_match_wildcard")
      ::site/description "Use this function for an If-Match header value of '*'"
      :xt/fn
      '(fn [ctx uri new-rep]
         (let [db (xtdb.api/db ctx)]
           (if (xtdb.api/entity db uri)
             [[::xt/put new-rep]]
             false)))
      :http/content-type "application/clojure"}]])

  (xt/submit-tx
   xtdb-node
   [[::xt/put
     {:xt/id (str base-uri "/_site/tx_fns/put_if_match_etags")
      :xt/fn
      '(fn [ctx uri header-field new-rep if-match?]
         (let [db (xtdb.api/db ctx)
               selected-representation (xtdb.api/entity db uri)
               txes [[::xt/put new-rep]]]
           (if-let [rep-unparsed-etag (some-> (get selected-representation ::http/etag))]
             (if (if-match? header-field rep-unparsed-etag)
               txes ; success, we matched
               false)
             false)))

      :http/content-type "application/clojure"}]]))

(def host-parser (rfc7230.decoders/host {}))

(def base-uri-parser
  (p/complete
   (p/into {}
           (p/sequence-group
            (p/pattern-parser #"(?<scheme>https?)://" {:group {:juxt.reap.alpha.rfc7230/scheme "scheme"}})
            host-parser))))

(defn openid-provider-configuration-url
  "Returns the URL of the OpenID Provider Configuration Information."
  [issuer-id]
  ;; See https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.4.1
  ;;
  ;; "If the Issuer value contains a path component, any terminating / MUST be
  ;; removed before appending /.well-known/openid-configuration."
  ;;
  ;; This uses a reluctant regex qualifier.
  (str (second (re-matches #"(.*?)/?" issuer-id)) "/.well-known/openid-configuration"))

(defn install-openid-provider! [xtdb-node issuer-id]
  (let [;; https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.4
        ;; tells us we rely on the configuration information being available at
        ;; the path <issuer-id>/.well-known/openid-configuration.
        config-uri (openid-provider-configuration-url issuer-id)
        _ (log/info "Loading OpenID configuration from" config-uri)
        config (json/read-value (slurp config-uri))
        auth-uri (new java.net.URI (get config "authorization_endpoint"))
        app-tld (-> (InternetDomainName/from (.getHost auth-uri))
                    (.topPrivateDomain)
                    (.toString))
        config (cond-> config
                 (= app-tld "amazoncognito.com")
                 (assoc "authorization_endpoint"
                        (-> (new URIBuilder auth-uri)
                            (.setPath "/logout")
                            (.toString))))]
    (log/info "Issuer added:" (get config "issuer"))
    (put!
     xtdb-node
     {:xt/id issuer-id
      :juxt.pass.alpha/openid-configuration config})))

(defn install-openid-resources!
  [xtdb-node {::site/keys [base-uri]
              {:keys [name issuer-id client-id client-secret]} :openid
              :as config}]
  (assert name)

  (let [client (format "%s/_site/openid/%s/client" base-uri name)
        login (format "%s/_site/openid/%s/login" base-uri name)
        callback (format "%s/_site/openid/%s/callback" base-uri name)]

    (put!
     xtdb-node
     {:xt/id client
      :juxt.pass.alpha/openid-issuer-id issuer-id
      :juxt.pass.alpha/oauth-client-id client-id
      :juxt.pass.alpha/oauth-client-secret client-secret
      :juxt.pass.alpha/redirect-uri callback}

     {:xt/id login
      :juxt.http.alpha/methods #{:head :get :options}
      :juxt.pass.alpha/classification "PUBLIC"
      :juxt.http.alpha/content-type "text/plain"
      :juxt.site.alpha/get-fn 'juxt.pass.alpha.openid-connect/login
      :juxt.pass.alpha/oauth-client client}

     {:xt/id callback
      :juxt.http.alpha/methods #{:head :get :options}
      :juxt.pass.alpha/classification "PUBLIC"
      :juxt.http.alpha/content-type "text/plain"
      :juxt.site.alpha/get-fn 'juxt.pass.alpha.openid-connect/callback
      :juxt.pass.alpha/oauth-client client})

    {:login-uri login
     :callback-uri callback}))
