;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.graphql :as graphql]
   [selmer.parser :as selmer]
   [xtdb.api :as xt]))

(defn put! [xt-node & ms]
  (->>
   (xt/submit-tx
    xt-node
    (for [m ms]
      [:xtdb.api/put m]))
   (xt/await-tx xt-node)))

(defn put-superuser-role!
  "Create the superuser role."
  [xt-node {::site/keys [base-uri]}]
  (log/info "Creating superuser role")
  (let [role (str base-uri "/_site/roles/superuser")]
    (put!
     xt-node
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
  [xt-node {:keys [username password fullname email]} {::site/keys [base-uri]}]
  (assert username)
  (assert password)
  (assert fullname)
  (let [user (str base-uri "/_site/users/" username)]
    (put!
     xt-node
     (merge
      {:xt/id user
       ::site/type "User"
       ::pass/username username
       :name fullname}
      (when email {:email email}))

     {:xt/id (str user "/password")
      ::site/type "Password"
      ::pass/user user
      ::pass/password-hash (password/encrypt password)
      ::pass/classification "RESTRICTED"}

     {:xt/id (format "%s/_site/roles/%s/users/%s" base-uri "superuser" username)
      ::site/type "UserRoleMapping"
      ::pass/assignee (format "%s/_site/users/%s" base-uri username)
      ::pass/role (str base-uri "/_site/roles/superuser")})))

(defn allow-public-access-to-public-resources!
  "Resources classified as PUBLIC should be readable (but not writable). For
  example, a login page needs to be a PUBLIC resource."
  [xt-node {::site/keys [base-uri]}]
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/rules/public-resources")
    ::site/type "Rule"
    ::site/description "PUBLIC resources are accessible to GET"
    ::pass/target '[[request :ring.request/method #{:get :head :options}]
                    [resource ::pass/classification "PUBLIC"]]
    ::pass/effect ::pass/allow}))

(defn restict-access-to-restricted-resources!
  "Resources classified as RESTRICTED should never be accessed, unless another
  policy explicitly authorizes access."
  [xt-node {::site/keys [base-uri]}]
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/rules/restricted-resources")
    ::site/type "Rule"
    ::site/description "RESTRICTED access is denied by default"
    ::pass/target '[[resource ::pass/classification "RESTRICTED"]]
    ::pass/effect ::pass/deny}))

(defn put-graphql-schema-endpoint!
  "Initialise the resource that will host Site's GraphQL schema, as well as the
  endpoint to post queries pertaining to the schema."
  ;; TODO: The 'and' above indicates this resource is conflating two concerns.
  [xt-node {::site/keys [base-uri]}]
  (log/info "Initialising GraphQL schema endpoint")
  ;; TODO: Establish some consistency with readers specified at various
  ;; call-sites!
  (put! xt-node

        (as-> (io/resource "juxt/site/alpha/graphql-resources.edn") %
          (slurp %)
          (edn/read-string {:readers {'regex re-pattern}} %)
          (postwalk
           (fn [x] (cond-> x
                     (string? x)
                     ;; expand {{base-uri}}
                     (selmer/render {:base-uri base-uri})))
           %)
          ;; TODO:
          (graphql/schema-resource % (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))))))

(defn put-graphql-operations!
  "Add GraphQL operations that provide idiomatic requests to Site's GraphQL endpoint."
  [xt-node {::site/keys [base-uri]}]
  (let [schema-id (str base-uri "/_site/graphql")
        schema (-> (xt/db xt-node) (xt/entity schema-id) ::site/graphql-compiled-schema)]
    (put!
     xt-node
     (into
      {:xt/id (str base-uri "/_site/graphql/requests/operations.graphql")
       ::site/methods
       {:get {}
        :head {}
        :post {::site/acceptable {"accept" "application/x-www-form-urlencoded"}}
        :options {}}
       ::site/graphql-schema schema-id
       ::site/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}
      (graphql/stored-document-resource-map
       (slurp (io/resource "juxt/site/alpha/operations.graphql"))
       schema))))
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/graphql/requests/operations.graphql.txt")
    :juxt.site.alpha/variant-of (str base-uri "/_site/graphql/requests/operations.graphql")
    :juxt.http.alpha/methods {:get {} :head {} :options {}}
    :juxt.http.alpha/content-type "text/plain;charset=utf-8"
    :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/text-plain-representation-body}))

(defn put-request-template!
  "Add the default request template, useful for debugging."
  [xt-node {::site/keys [base-uri]}]
  (log/info "Installing default request template")
  (let [body (-> "juxt/site/alpha/request-template.html"
                 io/resource
                 slurp
                 (.getBytes "UTF-8"))]
    (put!
     xt-node
     {:xt/id (str base-uri "/_site/templates/request.html")
      ::site/type "StaticRepresentation"
      ::http/methods {:get {} :head {} :options {} :put {}}
      ::http/content-type "text/html;charset=utf-8"
      ::http/content-length (count body)
      ::http/body body})))

(defn put-site-openapi!
  "Add the Site API"
  [xt-node json {::site/keys [base-uri]}]
  (log/info "Installing Site API")
  (let [openapi (json/read-value json)
        body (.getBytes json "UTF-8")]
    (put!
     xt-node
     {:xt/id (str base-uri "/_site/apis/site/openapi.json")
      ::site/type "OpenAPI"
      ::http/methods {:get {} :head {} :options {} :put {}}
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

(defn put-site-api! [xt-node {::site/keys [base-uri] :as config}]
  ;; Site API's dependencies need to be established in advance.
  (put-graphql-schema-endpoint! xt-node config)
  (put-graphql-operations! xt-node config)
  (put-request-template! xt-node config)

  (put-site-openapi!
   xt-node
   (as-> "juxt/site/alpha/openapi.edn" %
     (io/resource %)
     (slurp %)
     (selmer/render % {:base-uri base-uri})
     (edn/read-string
      {:readers
       ;; Forms marked as #edn need to be encoded into a string for transfer
       ;; as JSON and then decoded back into EDN. This is to preserve
       ;; necessary EDN features such as symbols.
       {'juxt.site.alpha/as-str pr-str}} %)
     (json/write-value-as-string %))
   config))

#_(defn permit-create-action! [xt-node {::site/keys [base-uri] :as config}]
  (put!
   xt-node
   {:xt/id (str base-uri "/permissions/repl/create-action")
    ::site/type "Permission"
    ::pass/identity (repl-identity)
    ::pass/action (str base-uri "/actions/create-action")
    ::pass/purpose nil}))

(defn do-action [ctx]
  (::pass/action-result (authz/do-action ctx)))

#_(defn do-action-with-purpose [xt-node action purpose & args]
  (apply
   authz/do-action
   xt-node
   {::pass/subject (repl-subject)
    ::pass/purpose purpose}
   action args))

(defn create-action! [xt-node {::site/keys [base-uri] :as config} action]
  (do-action
   xt-node
   (str base-uri "/actions/create-action")
   action))

#_(defn install-grant-permission-action! [xt-node {::site/keys [base-uri] :as config}]
    (create-action!
   xt-node
   config
   {:xt/id (str base-uri "/actions/grant-permission")
    ::site/type "Action"
    ::pass/scope "write:admin"          ; make configurable?

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [::site/type [:= "Permission"]]
      ]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0] 'merge {::site/type "Permission"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    ::pass/rules
    '[
      ;; See related comment above
      [(allowed? subject resource permission)
       [permission ::pass/identity i]
       [subject ::pass/identity i]]]}))

;; As a bootstrap, we need to grant the REPL permission to grant permissions!
;; This should be the last time we need to explicitly put anything in XTDB.
#_(defn permit-grant-permission-action!
  [xt-node {::site/keys [base-uri] :as config}]
  (put!
   xt-node
   {:xt/id (str base-uri "/permissions/repl/grant-permission")
    ::site/type "Permission"
    ::pass/identity (repl-identity)
    ::pass/action (str base-uri "/actions/grant-permission")
    ::pass/purpose nil}))

#_(defn grant-permission! [xt-node {::site/keys [base-uri]} permission]
  (do-action
   xt-node
   (str base-uri "/actions/grant-permission")
   permission))

#_(defn install-admin-app! [xt-node {::site/keys [base-uri] :as config}]
    (let [id (str base-uri "/_site/apps/admin")]
      (put!
       xt-node
       {:xt/id id
        :name "Admin App"              ; make this possible to configure somehow
        ::pass/client-secret (make-nonce 16)
        ::pass/scope #{"read:admin" "write:admin"}})))

#_(defn create-admin-access-token! [xt-node subject-id {::site/keys [base-uri] :as config}]
  (put!
   xt-node
   (at/make-access-token-doc subject-id (str base-uri "/_site/apps/admin"))))

;; This is deprecated because there are no longer any users/passwords
#_(defn ^:deprecated put-openid-token-endpoint! [xt-node {::site/keys [base-uri]}]
    (log/info "Installing OpenID Connect token endpoint")
    (let [token-endpoint (str base-uri "/_site/token")
          grant-types #{"client_credentials"}]
      (put!
       xt-node
       {:xt/id token-endpoint
        ::http/methods {:post {} :options {}}
        ::http/acceptable "application/x-www-form-urlencoded"
        ::site/post-fn `authn/token-response
        ::site/access-control-allow-origins
        {"http://localhost:8000"
         {::site/access-control-allow-methods #{:post}
          ::site/access-control-allow-headers #{"authorization" "content-type"}}}
        ::pass/expires-in (* 60 60 1)}

       {:xt/id (str base-uri "/_site/rules/anyone-can-ask-for-a-token")
        ::site/type "Rule"
        ::site/description "The token_endpoint must be accessible"
        ::pass/target [['request ::site/uri token-endpoint]
                       ['request :ring.request/method #{:post}]]
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
         xt-node
         {:xt/id (str base-uri "/.well-known/openid-configuration")
          ;; OpenID Connect Discovery documents are publically available
          ::pass/classification "PUBLIC"
          ::http/methods {:get {} :head {} :options {}}
          ::http/content-type "application/json"
          ::http/last-modified (Date.)
          ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
          ::http/content content}))))

#_(defn put-login-endpoint! [xt-node {::site/keys [base-uri]}]
  (log/info "Installing login endpoint")
  ;; Allow anyone to login
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/login")
    ::http/methods {:post {}}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/post-fn `authn/login-response
    ::pass/expires-in (* 3600 24 30)}

   {:xt/id (str base-uri "/_site/rules/anyone-can-post-login-credentials")
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target [['request :ring.request/method #{:post}]
                   ['request ::site/uri (str base-uri "/_site/login")]]
    ::pass/effect ::pass/allow}))

#_(defn put-logout-endpoint! [xt-node {::site/keys [base-uri]}]
  (log/info "Installing logout endpoint")
  ;; Allow anyone to login
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/logout")
    ::http/methods {:post {}}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/post-fn `authn/logout-response}

   {:xt/id (str base-uri "/_site/rules/anyone-can-post-logout-credentials")
    ::site/type "Rule"
    ::site/description "The logout POST handler must be accessible by all"
    ::pass/target [['request :ring.request/method #{:post}]
                   ['request ::site/uri (str base-uri "/_site/logout")]]
    ::pass/effect ::pass/allow}))

;; Currently awaiting a fix to https://github.com/juxt/xtdb/issues/1480 because
;; these can be used.
(defn put-site-txfns! [xt-node {::site/keys [base-uri]}]
  (xt/submit-tx
   xt-node
   [[:xtdb.api/put
     {:xt/id (str base-uri "/_site/tx_fns/put_if_match_wildcard")
      ::site/description "Use this function for an If-Match header value of '*'"
      :xt/fn
      '(fn [ctx uri new-rep]
         (let [db (xtdb.api/db ctx)]
           (if (xtdb.api/entity db uri)
             [[:xtdb.api/put new-rep]]
             false)))
      :http/content-type "application/clojure"}]])

  (xt/submit-tx
   xt-node
   [[:xtdb.api/put
     {:xt/id (str base-uri "/_site/tx_fns/put_if_match_etags")
      :xt/fn
      '(fn [ctx uri header-field new-rep if-match?]
         (let [db (xtdb.api/db ctx)
               selected-representation (xtdb.api/entity db uri)
               txes [[:xtdb.api/put new-rep]]]
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

#_(defn install-put-immutable-public-resource-action!
  [xt-node {::site/keys [base-uri] :as config}]
  (create-action!
   xt-node
   config
   {:xt/id (str base-uri "/actions/put-immutable-public-resource")
    :juxt.pass.alpha/scope "write:resource"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re (str base-uri "/.*")]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in
      [0] 'merge
      {::http/methods
       {:get {::pass/actions #{(str base-uri "/actions/get-public-resource")}}
        :head {::pass/actions #{(str base-uri "/actions/get-public-resource")}}
        :options {::pass/actions #{(str base-uri "/actions/get-options")}}}}]

     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/identity i]
       [subject :juxt.pass.alpha/identity i]]]})

  (grant-permission!
   xt-node
   config
   {:xt/id (str base-uri "/permissions/repl/put-immutable-public-resource")
    :juxt.pass.alpha/subject "urn:site:subjects:repl"
    :juxt.pass.alpha/action #{(str base-uri "/actions/put-immutable-public-resource")}
    :juxt.pass.alpha/purpose nil})

  ;; Create the action in order to read the resource
  (create-action!
   xt-node
   config
   {:xt/id (str base-uri "/actions/get-public-resource")
    :juxt.pass.alpha/scope "read:resource"

    :juxt.pass.alpha/rules
    [
     ['(allowed? subject resource permission)
      ['permission :xt/id (str base-uri "/permissions/public-resources-to-all")]]]})

  ;; All actions must be granted a permission. This permission allows anyone to
  ;; call get-public-resource
  (grant-permission!
   xt-node
   config
   {:xt/id (str base-uri "/permissions/public-resources-to-all")
    :juxt.pass.alpha/action #{(str base-uri "/actions/get-public-resource")}
    :juxt.pass.alpha/purpose nil}))

#_(defn install-put-immutable-private-resource-action!
  [xt-node {::site/keys [base-uri] :as config}]
  (create-action!
   xt-node
   config
   {:xt/id (str base-uri "/actions/put-immutable-private-resource")
    :juxt.pass.alpha/scope "write:resource"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re (str base-uri "/.*")]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in
      [0] 'merge
      {::http/methods
       {:get {::pass/actions #{(str base-uri "/actions/get-private-resource")}}
        :head {::pass/actions #{(str base-uri "/actions/get-private-resource")}}
        :options {::pass/actions #{(str base-uri "/actions/get-options")}}}}]

     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]})

  (grant-permission!
   xt-node
   config
   {:xt/id (str base-uri "/permissions/repl/put-immutable-private-resource")
    :juxt.pass.alpha/subject "urn:site:subjects:repl"
    :juxt.pass.alpha/action #{(str base-uri "/actions/put-immutable-private-resource")}
    :juxt.pass.alpha/purpose nil})

  ;; Create the action in order to read the resource
  (create-action!
   xt-node
   config
   {:xt/id (str base-uri "/actions/get-private-resource")
    :juxt.pass.alpha/scope "read:resource"

    :juxt.pass.alpha/rules
    [
     ['(allowed? subject resource permission)
      '[permission :juxt.pass.alpha/resource resource]
      ['permission :juxt.pass.alpha/action (str base-uri "/actions/get-private-resource")]
      ['subject :xt/id]]]}))

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

;; Should be true
(comment
  (=
   (openid-provider-configuration-url "https://juxt.eu.auth0.com")
   (openid-provider-configuration-url "https://juxt.eu.auth0.com/")))

(defn install-openid-provider! [xt-node issuer-id]
  (let [;; https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.4
        ;; tells us we rely on the configuration information being available at
        ;; the path <issuer-id>/.well-known/openid-configuration.
        config-uri (openid-provider-configuration-url issuer-id)
        _ (printf "Loading OpenID configuration from %s\n" config-uri)
        config (json/read-value (slurp config-uri))]
    (printf "Issuer added: %s\n" (get config "issuer"))
    (put!
     xt-node
     {:xt/id issuer-id
      :juxt.pass.alpha/openid-configuration config})))

(defn install-openid-resources!
  [xt-node {::site/keys [base-uri] :as config}
   & {:keys [name issuer-id client-id client-secret]}]

  (assert name)
  #_(install-put-immutable-public-resource-action! xt-node config)

  (let [client (format "%s/_site/openid/%s/client" base-uri name)
        login (format "%s/_site/openid/%s/login" base-uri name)
        callback (format "%s/_site/openid/%s/callback" base-uri name)
        put-immutable-public-resource
        (fn [doc]
          (do-action
           xt-node
           (str base-uri "/actions/put-immutable-public-resource")
           doc))]

    (put!
     xt-node
     {:xt/id client
      :juxt.pass.alpha/openid-issuer-id issuer-id
      :juxt.pass.alpha/oauth-client-id client-id
      :juxt.pass.alpha/oauth-client-secret client-secret
      :juxt.pass.alpha/redirect-uri callback})

    (let [login
          (put-immutable-public-resource
           {:xt/id login
            :juxt.http.alpha/content-type "text/plain"
            :juxt.site.alpha/methods {:get {:handler 'juxt.pass.alpha.openid-connect/login}}
            :juxt.pass.alpha/oauth-client client})

          callback
          (put-immutable-public-resource
           {:xt/id callback
            :juxt.http.alpha/content-type "text/plain"
            :juxt.site.alpha/methods {:get {:handler 'juxt.pass.alpha.openid-connect/callback}}
            :juxt.pass.alpha/oauth-client client})
          ]
      {:login-uri (get-in login [::pass/puts 0])
       :callback-uri (get-in callback [::pass/puts 0])})))
