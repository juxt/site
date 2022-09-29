;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
   [jsonista.core :as json]
   [malli.core :as malli]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha.locator :refer [to-regex]]
   [juxt.pass.alpha.actions :as actions]
   [juxt.flip.alpha.core :as f]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.main :as main]
   [xtdb.api :as xt]

   ;; These are kept around just for the commented code sections
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
))

(defn system [] main/*system*)

(defn xt-node []
  (:juxt.site.alpha.db/xt-node (system)))

(defn put! [& ms]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [m ms]
      (let [vt (:xtdb.api/valid-time m)]
        [:xtdb.api/put (dissoc m :xtdb.api/valid-time) vt])))
   (xt/await-tx (xt-node))))

(defn config []
  (main/config))

(defn base-uri []
  (::site/base-uri (config)))

(defn substitute-actual-base-uri [form]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://example.org" (base-uri))))
   form))

(defn install-system-subject! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-system-subject![]
      {:xt/id "https://example.org/subjects/system"
       :juxt.site.alpha/description "The system subject"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}
      ;; end::install-system-subject![]
      )))))

(defn install-create-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-create-action![]
      {:xt/id "https://example.org/actions/create-action"
       :juxt.site.alpha/description "The action to create all other actions"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"

       :juxt.pass.alpha/rules
       '[
         ;; Creating actions should only be available to the most trusted
         ;; subjects. Actions can write directly to the database, if they wish.
         [(allowed? subject resource permission) ; <1>
          [permission :juxt.pass.alpha/subject subject]]]

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [juxt.site.alpha/request-body-as-edn

                (site/validate
                 [:map                  ; <2>
                  [:xt/id [:re "https://example.org/actions/(.+)"]]
                  [:juxt.pass.alpha/rules [:vector [:vector :any]]]])

                (site/set-type "https://meta.juxt.site/pass/action") ; <3>

                xtdb.api/put]))]))}}
      ;; end::install-create-action![]
      )))))

(defn install-system-permissions! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-system-permissions![]
      {:xt/id "https://example.org/permissions/system/bootstrap"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/permission" ; <1>
       :juxt.pass.alpha/action #{"https://example.org/actions/create-action"
                                 "https://example.org/actions/grant-permission"} ; <2>
       :juxt.pass.alpha/purpose nil      ; <3>
       :juxt.pass.alpha/subject "https://example.org/subjects/system" ; <4>
       }
      ;; end::install-system-permissions![]
      )))))

(defn install-do-action-fn! [_]
  (put! (actions/install-do-action-fn (base-uri))))

(defn make-repl-request-context [subject action edn-arg]
  (let [xt-node (xt-node)]
    {::site/xt-node xt-node
     ::site/db (xt/db xt-node)
     ::pass/subject subject
     ::pass/action action
     ::pass/action-input edn-arg
     ::site/base-uri (base-uri)
     ::site/received-representation
     {::http/content-type "application/edn"
      ::http/body (.getBytes (pr-str edn-arg))}}))

(defn do-action [subject-id action-id edn-arg]
  (assert (or (nil? subject-id) (string? subject-id)) "Subject must a string or nil")
  (let [xt-node (xt-node)
        db (xt/db xt-node)
        subject (when subject-id (xt/entity db subject-id))]
    (::pass/action-result
     (actions/do-action
      (make-repl-request-context subject action-id edn-arg)))))

(defn create-grant-permission-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-grant-permission-action![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/grant-permission"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         ;; This might be overly powerful, as a general way of granting anyone a
         ;; permission on any action! Let's comment for now
         #_[(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [juxt.site.alpha/request-body-as-edn
                (juxt.site.alpha/validate
                 [:map
                  [:xt/id [:re "https://example.org/permissions/(.+)"]]
                  [:juxt.pass.alpha/action [:re "https://example.org/actions/(.+)"]]
                  [:juxt.pass.alpha/purpose [:maybe :string]]])
                (f/set-at (f/dip ["https://meta.juxt.site/pass/permission" :juxt.site.alpha/type]))
                xtdb.api/put]))]))}})
     ;; end::create-grant-permission-action![]
     ))))

(defn create-action-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-not-found"
       :juxt.pass.alpha/rules
       [
        ['(allowed? subject resource permission)
         ['permission :xt/id]]]})))))

(defn create-action-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-not-found"
       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [site/request-body-as-edn
                (site/validate
                 [:map
                  [:xt/id [:re "https://example.org/.*"]]])
                (site/set-type "https://meta.juxt.site/not-found")
                (site/set-methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-not-found"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-not-found"}}})
                xtdb.api/put]))]))}
       :juxt.pass.alpha/rules
       [
        ['(allowed? subject resource permission)
         '[permission :juxt.pass.alpha/subject subject]]]})))))

(defn grant-permission-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-not-found"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-not-found"
       :juxt.pass.alpha/purpose nil})))))

(defn install-not-found-resource! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/install-not-found"
        {:xt/id "https://example.org/_site/not-found"}))))))

(defn grant-permission-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/grant-permission"
        {:xt/id "https://example.org/permissions/get-not-found"
         :juxt.pass.alpha/action "https://example.org/actions/get-not-found"
         :juxt.pass.alpha/purpose nil}))))))

#_(defn ^{:deprecated "This is the original function prior to adding the dependency graph approach."}
  install-not-found
  "Install an action to perform on '404'."
  []
  (create-action-get-not-found!)
  (create-action-install-not-found!)
  (grant-permission-install-not-found!)
  (install-not-found-resource!)
  (grant-permission-get-not-found!))

;; TODO: In the context of an application, rename 'put' to 'register'
(defn create-action-register-application!
  "Install an action to register an application"
  [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/register-application"

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [site/request-body-as-edn
                (site/validate
                 [:map
                  [::pass/client-id [:re "[a-z-]{3,}"]]
                  [::pass/redirect-uri [:re "https://"]]])

                (site/set-type "https://meta.juxt.site/pass/application")
                (f/set-at (f/dip [(pass/as-hex-str (pass/random-bytes 20)) ::pass/client-secret]))

                (f/set-at
                 (f/keep
                  [(f/of ::pass/client-id) "/applications/" f/str (f/env ::site/base-uri) f/str
                   :xt/id]))

                xtdb.api/put]))]))}

       :juxt.pass.alpha/rules
       '[[(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/user-identity id]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-register-application! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/register-application"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/register-application"
       :juxt.pass.alpha/purpose nil})))))

#_(defn put-graphql-schema-endpoint!
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

#_(defn put-graphql-operations!
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

#_(defn put-request-template!
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

#_(defn put-site-openapi!
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

#_(defn put-site-api! [xt-node {::site/keys [base-uri] :as config}]
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

(def ^{::malli/schema
       [:map-of [:or :string :keyword]
        [:map
         [:create {:optional true} :any]
         [:deps {:optional true}
          ;; :deps can be a set, but also, where necessary a function.
          [:or
           [:set [:or :string :keyword]]
           [:=> [:cat
                 [:map-of :string :string]
                 [:map {::site/base-uri :string}]]
            [:set [:or :string :keyword]]]]]]]}
  dependency-graph
  {"https://example.org/_site/do-action"
   {:create install-do-action-fn!}

   "https://example.org/subjects/system"
   {:create install-system-subject!}

   "https://example.org/permissions/system/bootstrap"
   {:create install-system-permissions!}

   "https://example.org/permissions/get-not-found"
   {:create grant-permission-get-not-found!}

   "https://example.org/actions/create-action"
   {:create install-create-action!}

   "https://example.org/actions/grant-permission"
   {:create create-grant-permission-action!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"
            "https://example.org/permissions/system/bootstrap"}}

   "https://example.org/actions/get-not-found"
   {:create create-action-get-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/actions/install-not-found"
   {:create create-action-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/permissions/system/install-not-found"
   {:create grant-permission-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/grant-permission"}}

   "https://example.org/_site/not-found"
   {:create install-not-found-resource!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/install-not-found"}}

   ::system {:deps #{"https://example.org/_site/do-action"
                     "https://example.org/_site/not-found"

                     "https://example.org/subjects/system"

                     "https://example.org/actions/create-action"
                     "https://example.org/actions/grant-permission"
                     "https://example.org/actions/install-not-found"
                     "https://example.org/actions/get-not-found"

                     "https://example.org/permissions/system/bootstrap"
                     "https://example.org/permissions/system/install-not-found"
                     "https://example.org/permissions/get-not-found"}}

   "https://example.org/actions/register-application"
   {:create create-action-register-application!
    :deps #{::system}}

   "https://example.org/permissions/system/register-application"
   {:create grant-permission-to-invoke-action-register-application!
    :deps #{::system}}})


(defn lookup [g id]
  (or
   (when-let [v (get g id)] (assoc v :id id))
   (some (fn [[k v]]
           (when-let [matches (re-matches (to-regex k) id)]
             (assoc v
                    :id id
                    :params
                    (zipmap
                     (map second (re-seq #"\{(\p{Alpha}+)\}" k))
                     (next matches)))))
         g)))

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [ids graph]
  {:pre [(malli/validate [:or [:set [:or :string :keyword]] [:sequential [:or :string :keyword]]] ids)
         (malli/validate (::malli/schema (meta #'dependency-graph)) graph)]
   :post [(malli/validate
           [:sequential
            [:map
             [:id :string]
             [:status :keyword]
             [:error {:optional true} :any]]] %)]}
  (->> ids
       (mapcat (fn [id]
                 (->> id
                      (tree-seq some?
                                (fn [id]
                                  (let [{:keys [deps params]} (lookup graph id)]
                                    (cond
                                      (nil? deps) nil
                                      (fn? deps) (deps params {::site/base-uri (base-uri)})
                                      (set? deps) deps
                                      :else (throw (ex-info "Unexpected deps type" {:deps deps}))))))
                      (keep (fn [id]
                              (if-let [v (lookup graph id)]
                                (when-not (keyword? id) [id v])
                                (throw (ex-info (format "No dependency graph entry for %s" id) {:id id}))))))))
       reverse distinct
       (reduce
        (fn [acc [id {:keys [create] :as v}]]
          (when-not create (throw (ex-info (format "No creator for %s" id) {:id id})))
          (conj acc (try
                      (let [{::pass/keys [puts] :as result} (create v)]
                        (when (and puts (not (contains? (set puts) id)))
                          (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                        {:id id :status :created :result result})
                      (catch Throwable cause
                        (throw (ex-info (format "Failed to converge %s" id) {:id id} cause))
                        ;;{:id id :status :error :error cause}
                        ))))
        [])))

(defn bootstrap! []
  (converge! #{::system} (substitute-actual-base-uri dependency-graph)))
