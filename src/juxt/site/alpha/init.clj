;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.tools.logging :as log]
   [crux.api :as x]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha.util :as util])
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

(defn put-superuser-role!
  "Create the superuser role."
  [crux-node {::site/keys [base-uri]}]
  (log/info "Creating superuser role")
  (let [role (str base-uri "/_site/roles/superuser")]
    (put!
     crux-node
     {:crux.db/id role
      ::site/type "Role"
      :name "superuser"
      :description "Superuser"}

     ;; Add rule that allows superusers to do everything.
     {:crux.db/id (str base-uri "/_site/rules/superuser-allow-all")
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
  [crux-node username password fullname email {::site/keys [base-uri]}]
  (let [user (str base-uri "/_site/users/" username)]
    (put!
     crux-node
     {:crux.db/id user
      ::site/type "User"
      ::pass/username username
      :name fullname
      :email email}

     {:crux.db/id (str user "/password")
      ::site/type "Password"
      ::pass/user user
      ::pass/password-hash (password/encrypt password)
      ::pass/classification "RESTRICTED"}

     {:crux.db/id (format "%s/_site/roles/%s/users/%s" base-uri "superuser" username)
      ::site/type "UserRoleMapping"
      ::pass/assignee (format "%s/_site/users/%s" base-uri username)
      ::pass/role (str base-uri "/_site/roles/superuser")})))

(defn allow-public-access-to-public-resources!
  "Resources classified as PUBLIC should be readable (but not writable). For
  example, a login page needs to be a PUBLIC resource."
  [crux-node {::site/keys [base-uri]}]
  (put!
   crux-node
   {:crux.db/id (str base-uri "/_site/rules/public-resources")
    ::site/type "Rule"
    ::site/description "PUBLIC resources are accessible to GET"
    ::pass/target '[[request :ring.request/method #{:get :head :options}]
                    [resource ::pass/classification "PUBLIC"]]
    ::pass/effect ::pass/allow}))

(defn restict-access-to-restricted-resources!
  "Resources classified as RESTRICTED should never be accessed, unless another
  policy explicitly authorizes access."
  [crux-node {::site/keys [base-uri]}]
  (put!
   crux-node
   {:crux.db/id (str base-uri "/_site/rules/restricted-resources")
    ::site/type "Rule"
    ::site/description "RESTRICTED access is denied by default"
    ::pass/target '[[resource ::pass/classification "RESTRICTED"]]
    ::pass/effect ::pass/deny}))

(defn put-site-api!
  "Add the Site API"
  [crux-node json {::site/keys [base-uri]}]
  (log/info "Installing Site API")
  (let [openapi (json/read-value json)
        body (.getBytes json "UTF-8")]
    (put!
     crux-node
     {:crux.db/id (str base-uri "/_site/apis/site/openapi.json")
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

(defn put-openid-token-endpoint! [crux-node {::site/keys [base-uri]}]
  (log/info "Installing OpenID Connect token endpoint")
  (let [token-endpoint (str base-uri "/_site/token")
        grant-types #{"client_credentials"}]
    (put!
     crux-node
     {:crux.db/id token-endpoint
      ::http/methods #{:post :options}
      ::http/acceptable "application/x-www-form-urlencoded"
      ::site/purpose ::site/acquire-token
      ::site/access-control-allow-origins
      {"http://localhost:8000"
       {::site/access-control-allow-methods #{:post}
        ::site/access-control-allow-headers #{"authorization" "content-type"}}}
      ::pass/expires-in (* 60 60 1)}

     {:crux.db/id (str base-uri "/_site/rules/anyone-can-ask-for-a-token")
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
       {:crux.db/id (str base-uri "/.well-known/openid-configuration")
        ;; OpenID Connect Discovery documents are publically available
        ::pass/classification "PUBLIC"
        ::http/methods #{:get :head :options}
        ::http/content-type "application/json"
        ::http/last-modified (Date.)
        ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
        ::http/content content}))))

(defn put-login-endpoint! [crux-node {::site/keys [base-uri]}]
  (log/info "Installing login endpoint")
  ;; Allow anyone to login
  (put!
   crux-node
   {:crux.db/id (str base-uri "/_site/login")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/login
    ::pass/expires-in (* 3600 24 30)}

   {:crux.db/id (str base-uri "/_site/rules/anyone-can-post-login-credentials")
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/login]]
    ::pass/effect ::pass/allow}))

(defn put-logout-endpoint! [crux-node {::site/keys [base-uri]}]
  (log/info "Installing logout endpoint")
  ;; Allow anyone to login
  (put!
   crux-node
   {:crux.db/id (str base-uri "/_site/logout")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/logout}

   {:crux.db/id (str base-uri "/_site/rules/anyone-can-post-logout-credentials")
    ::site/type "Rule"
    ::site/description "The logout POST handler must be accessible by all"
    ::pass/target '[[request :ring.request/method #{:post}]
                    [resource ::site/purpose ::site/logout]]
    ::pass/effect ::pass/allow}))

;; Currently awaiting a fix to https://github.com/juxt/crux/issues/1480 because
;; these can be used.
(defn put-site-txfns! [crux-node {::site/keys [base-uri]}]
  (x/submit-tx
   crux-node
   [[:crux.tx/put
     {:crux.db/id (str base-uri "/_site/tx_fns/put_if_match_wildcard")
      ::site/description "Use this function for an If-Match header value of '*'"
      :crux.db/fn
      '(fn [ctx uri new-rep]
         (let [db (crux.api/db ctx)]
           (if (crux.api/entity db uri)
             [[:crux.tx/put new-rep]]
             false)))
      :http/content-type "application/clojure"}]])

  (x/submit-tx
   crux-node
   [[:crux.tx/put
     {:crux.db/id (str base-uri "/_site/tx_fns/put_if_match_etags")
      :crux.db/fn
      '(fn [ctx uri header-field new-rep if-match?]
         (let [db (crux.api/db ctx)
               selected-representation (crux.api/entity db uri)
               txes [[:crux.tx/put new-rep]]]
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

(defn do-action [crux-node subject action & args]
  (apply authz/do-action crux-node {::pass/subject subject} action args))

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

(comment
  (openid-provider-configuration-url "dev-14bkigf7.us.auth0.com"))

(defn install-openid-provider! [crux-node issuer-id]
  (let [;; https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.4
        ;; tells us we rely on the configuration information being available at
        ;; the path <issuer-id>/.well-known/openid-configuration.
        config-uri (openid-provider-configuration-url issuer-id)
        _ (printf "Loading OpenID configuration from %s\n" config-uri)
        config (json/read-value (slurp config-uri))]
    (printf "Issuer added: %s\n" (get config "issuer"))
    (put!
     crux-node
     {:crux.db/id issuer-id
      :juxt.pass.alpha/openid-configuration config})))

(comment
  (require 'sc.api)
  (sc.api/defsc 85))

(defn install-openid-resources!
  [crux-node {::site/keys [base-uri] :as config}
   & {:keys [name issuer-id client-id client-secret]}]

  (assert name)
  #_(install-put-immutable-public-resource-action! crux-node config)
  (let [client (format "%s/_site/openid/%s/client" base-uri name)
        login (format "%s/_site/openid/%s/login" base-uri name)
        callback (format "%s/_site/openid/%s/callback" base-uri name)]

    (put!
     crux-node
     {:crux.db/id client
      :juxt.pass.alpha/openid-issuer-id issuer-id
      :juxt.pass.alpha/oauth-client-id client-id
      :juxt.pass.alpha/oauth-client-secret client-secret
      :juxt.pass.alpha/redirect-uri callback}

     {:crux.db/id login
      :juxt.http.alpha/methods #{:head :get :options}
      :juxt.pass.alpha/classification "PUBLIC"
      :juxt.http.alpha/content-type "text/plain"
      :juxt.site.alpha/get-fn 'juxt.pass.alpha.openid-connect/login
      :juxt.pass.alpha/oauth-client client}

     {:crux.db/id callback
      :juxt.http.alpha/methods #{:head :get :options}
      :juxt.pass.alpha/classification "PUBLIC"
      :juxt.http.alpha/content-type "text/plain"
      :juxt.site.alpha/get-fn 'juxt.pass.alpha.openid-connect/callback
      :juxt.pass.alpha/oauth-client client})

    {:login-uri login
     :callback-uri callback}))
