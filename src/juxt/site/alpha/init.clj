;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [xtdb.api :as x]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.pass.alpha.authentication :as authn]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.regex :as re]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.site.alpha.util :as util]
   [selmer.parser :as selmer]
   [xtdb.api :as xt])
  (:import
   (java.util Date)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn put! [xt-node & ms]
  (->>
   (x/submit-tx
    xt-node
    (for [m ms]
      [:xtdb.api/put m]))
   (x/await-tx xt-node)))

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
       ::http/methods #{:get :head :post :options}
       ::http/acceptable-on-post {"accept" "application/x-www-form-urlencoded"}
       ::site/graphql-schema schema-id
       ::site/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}
      (graphql/stored-document-resource-map
       (slurp (io/resource "juxt/site/alpha/operations.graphql"))
       schema))))
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/graphql/requests/operations.graphql.txt")
    :juxt.site.alpha/variant-of (str base-uri "/_site/graphql/requests/operations.graphql")
    :juxt.http.alpha/methods #{:get :head :options}
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
      ::http/methods #{:get :head :options :put}
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

(defn put-openid-token-endpoint! [xt-node {::site/keys [base-uri]}]
  (log/info "Installing OpenID Connect token endpoint")
  (let [token-endpoint (str base-uri "/_site/token")
        grant-types #{"client_credentials"}]
    (put!
     xt-node
     {:xt/id token-endpoint
      ::http/methods #{:post :options}
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
        ::http/methods #{:get :head :options}
        ::http/content-type "application/json"
        ::http/last-modified (Date.)
        ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
        ::http/content content}))))

(defn put-login-endpoint! [xt-node {::site/keys [base-uri]}]
  (log/info "Installing login endpoint")
  ;; Allow anyone to login
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/login")
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/post-fn `authn/login-response
    ::pass/expires-in (* 3600 24 30)}

   {:xt/id (str base-uri "/_site/rules/anyone-can-post-login-credentials")
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target [['request :ring.request/method #{:post}]
                   ['request ::site/uri (str base-uri "/_site/login")]]
    ::pass/effect ::pass/allow}))

(defn put-logout-endpoint! [xt-node {::site/keys [base-uri]}]
  (log/info "Installing logout endpoint")
  ;; Allow anyone to login
  (put!
   xt-node
   {:xt/id (str base-uri "/_site/logout")
    ::http/methods #{:post}
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
  (x/submit-tx
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

  (x/submit-tx
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
