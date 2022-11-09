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
   [sci.core :as sci]

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

(defn make-repl-request-context [subject action edn-arg]
  (let [xt-node (xt-node)]
    (cond->
        {::site/xt-node xt-node
         ::site/db (xt/db xt-node)
         ::pass/subject subject
         ::pass/action action
         ::site/base-uri (base-uri)}
      edn-arg (merge {::site/received-representation
                      {::http/content-type "application/edn"
                       ::http/body (.getBytes (pr-str edn-arg))}}))))

(defn do-action
  ([subject-id action-id]
   (do-action subject-id action-id nil))
  ([subject-id action-id edn-arg]

   (assert (or (nil? subject-id) (string? subject-id)) "Subject must a string or nil")

   (let [xt-node (xt-node)
         db (xt/db xt-node)
         subject (when subject-id (xt/entity db subject-id))]
     (::pass/action-result
      (actions/do-action
       (make-repl-request-context subject action-id edn-arg))))))

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

#_(defn openid-provider-configuration-url
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
#_(comment
  (=
   (openid-provider-configuration-url "https://juxt.eu.auth0.com")
   (openid-provider-configuration-url "https://juxt.eu.auth0.com/")))

#_(defn install-openid-provider! [xt-node issuer-id]
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

#_(comment
  (json/read-value (slurp (openid-provider-configuration-url "https://juxt.eu.auth0.com"))))

#_(defn install-openid-resources!
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

(defn lookup [g id]
  (or
   (when-let [v (get g id)] (assoc v :id id))
   (some (fn [[k v]]
;;           (when-not (string? id) (throw (ex-info "DEBUG" {:id id})))
           (when-let [matches (re-matches (to-regex k) id)]
             (assoc v
                    :id id
                    :params
                    (zipmap
                     (map second (re-seq #"\{(\p{Alpha}+)\}" k))
                     (next matches)))))
         g)))

(def dependency-graph-malli-schema
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
       [:set [:or :string :keyword]]]]]]])

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [ids graph {:keys [dry-run? recreate?]}]
  {:pre [(malli/validate [:or [:set [:or :string :keyword]] [:sequential [:or :string :keyword]]] ids)
         ;;(malli/validate dependency-graph-malli-schema graph)
         ]
   :post [(malli/validate
           [:sequential
            [:map
             [:id :string]
             [:status :keyword]
             [:error {:optional true} :any]]] %)]}

  (let [db (xt/db (xt-node))]

    (when-not (malli/validate dependency-graph-malli-schema graph)
      (throw
       (ex-info
        "Graph failed to validate"
        {:graph graph
         :explain (malli/explain dependency-graph-malli-schema graph)})))

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
            (if (or (not (xt/entity db id)) recreate?)
              (do
                (when-not create (throw (ex-info (format "No creator for %s" id) {:id id})))
                (if-not dry-run?
                  (conj acc (try
                              (let [{::pass/keys [puts] :as result} (create v)]
                                (when (and puts (not (contains? (set puts) id)))
                                  (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                                {:id id :status :created :result result})
                              (catch Throwable cause
                                (throw (ex-info (format "Failed to converge %s" id) {:id id} cause))
                                ;;{:id id :status :error :error cause}
                                )))
                  ;; Dry run
                  (conj acc {:id id :status :dry-run})
                  ))
              acc))
          []))))
