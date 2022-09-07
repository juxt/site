;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.init
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [jsonista.core :as json]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.alpha.graphql :as graphql]
   [selmer.parser :as selmer]

   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha :as-alias site]))

(defn put! [xt-node & ms]
  (->>
   (xt/submit-tx
    xt-node
    (for [m ms]
      [:xtdb.api/put m]))
   (xt/await-tx xt-node)))

(defn put-graphql-schema-endpoint!
  "Initialise the resource that will host Site's GraphQL schema, as well as the
  endpoint to post queries pertaining to the schema."
  ;; TODO: The 'and' above indicates this resource is conflating two concerns.
  [xt-node config]
  (log/info "Initialising GraphQL schema endpoint")
  ;; TODO: Establish some consistency with readers specified at various
  ;; call-sites!
  (put! xt-node

        (as-> (io/resource "juxt/site/alpha/graphql-resources.edn") %
          (slurp %)
          (edn/read-string {:readers {'regex re-pattern}} %)
          (graphql/schema-resource % (slurp (io/resource "juxt/site/alpha/site-schema.graphql"))))))

(defn put-graphql-operations!
  "Add GraphQL operations that provide idiomatic requests to Site's GraphQL endpoint."
  [xt-node config]
  (let [schema-id "/_site/graphql"
        schema (-> (xt/db xt-node) (xt/entity schema-id) ::site/graphql-compiled-schema)]
    (put!
     xt-node
     (into
      {:xt/id "/_site/graphql/requests/operations.graphql"
       ::http/methods #{:get :head :post :options}
       ::http/acceptable-on-post {"accept" "application/x-www-form-urlencoded"}
       ::site/graphql-schema schema-id
       ::site/post-fn 'juxt.site.alpha.graphql/stored-document-post-handler}
      (graphql/stored-document-resource-map
       (slurp (io/resource "juxt/site/alpha/operations.graphql"))
       schema))))
  (put!
   xt-node
   {:xt/id "/_site/graphql/requests/operations.graphql.txt"
    :juxt.site.alpha/variant-of "/_site/graphql/requests/operations.graphql"
    :juxt.http.alpha/methods #{:get :head :options}
    :juxt.http.alpha/content-type "text/plain;charset=utf-8"
    :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/text-plain-representation-body}))

(defn put-request-template!
  "Add the default request template, useful for debugging."
  [xt-node config]
  (log/info "Installing default request template")
  (let [body (-> "juxt/site/alpha/request-template.html"
                 io/resource
                 slurp
                 (.getBytes "UTF-8"))]
    (put!
     xt-node
     {:xt/id "/_site/templates/request.html"
      ::site/type "StaticRepresentation"
      ::http/methods #{:get :head :options :put}
      ::http/content-type "text/html;charset=utf-8"
      ::http/content-length (count body)
      ::http/body body})))

(defn put-site-openapi!
  "Add the Site API"
  [xt-node json config]
  (log/info "Installing Site API")
  (let [openapi (json/read-value json)
        body (.getBytes json "UTF-8")]
    (put!
     xt-node
     {:xt/id "/_site/apis/site/openapi.json"
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

(defn put-site-api! [xt-node config]
  ;; Site API's dependencies need to be established in advance.
  (put-graphql-schema-endpoint! xt-node config)
  (put-graphql-operations! xt-node config)
  (put-request-template! xt-node config)

  (put-site-openapi!
   xt-node
   (as-> "juxt/site/alpha/openapi.edn" %
     (io/resource %)
     (slurp %)
     (selmer/render % {:base-uri ""})
     (edn/read-string
      {:readers
       ;; Forms marked as #edn need to be encoded into a string for transfer
       ;; as JSON and then decoded back into EDN. This is to preserve
       ;; necessary EDN features such as symbols.
       {'juxt.site.alpha/as-str pr-str}} %)
     (json/write-value-as-string %))
   config))

;; Currently awaiting a fix to https://github.com/juxt/xtdb/issues/1480 because
;; these can be used.
(defn put-site-txfns! [xt-node config]
  (xt/submit-tx
   xt-node
   [[:xtdb.api/put
     {:xt/id "/_site/tx_fns/put_if_match_wildcard"
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
     {:xt/id "/_site/tx_fns/put_if_match_etags"
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

(defn insert-base-resources!
  [xt-node config]
  (put-site-api! xt-node config)
  (put-graphql-operations! xt-node config)
  (put-graphql-schema-endpoint! xt-node config)
  (put-request-template! xt-node config))
