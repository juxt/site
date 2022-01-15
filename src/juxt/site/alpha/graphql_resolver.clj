;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql-resolver
  (:require
   [clojure.java.shell :as sh]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [juxt.site.alpha.cache :as cache]
   [juxt.site.alpha.main :as main]
   [juxt.site.alpha.repl :as repl]
   [ring.util.codec :refer [form-decode]]
   [xtdb.api :as xt]))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))

(defn config []
  (main/config))

(defn subject [{::pass/keys [subject] :keys [db]}]
  (let [user (xt/entity db (::pass/user subject))]
    {"user" user
     "authScheme" (::pass/auth-scheme subject)}))

(defn query-string [args]
  (some-> args :juxt.site.alpha/request-context :ring.request/query))

(defn- form [args]
  (let [form (some-> args :juxt.site.alpha/request-context :ring.request/query (form-decode "US-ASCII"))]
    (if (string? form)
      {form "true"}
      ;; Set empty strings to "true" rather than empty-string, to enable selmer
      ;; truthy on 'if'.
      (reduce-kv
       (fn [acc k v]
         (assoc acc k (if (str/blank? v) "true" v)))
       {}
       form))))

(defn query-parameters [args]
  (mapv (fn [[k v]] {"name" k "value" v}) (form args)))

(defn query-parameter [args]
  (get (form args) (-> args :argument-values (get "name"))))

(defn constant [args]
  (some-> args :argument-values (get "value")))

(defn make-string-transform [o] (String. o))

(defn df [dir]
  (when-let [out (:out (sh/sh "df" "--output=avail" dir))]
    (Long/parseLong
     (str/trim
      (second (str/split out #"\n"))))))

(defn tx-log-avail [])

(defn git-sha []
  (let [{:keys [out err]} (sh/sh "git" "rev-parse" "HEAD")]
    (when (seq err)
      (throw (ex-info "Failed to get git sha1 version" {})))
    (str/trim out)))


(defn paginate
  [items {:keys [argument-values]}]
  (let [limit (get argument-values "limit" 1000)
        offset (get argument-values "offset" 0)]
    (some->> (seq items)
             (drop offset)
             (take limit))))

(defn apis [{:keys [db]}]
  (let [openapis
        (for [[uri api] (xt/q
                         db '{:find [openapi-uri openapi]
                              :where [[openapi-uri :juxt.apex.alpha/openapi openapi]]})]
          {:xt/id uri
           :type "OPENAPI"
           :contents api})
        graphql-apis
        (map first (xt/q db '{:find [uri]
                              :where [[uri :juxt.site.alpha/graphql-compiled-schema]]}))
        graphqls
        (for [uri graphql-apis]
          {:xt/id uri
           :type "GRAPHQL"
           :contents (xt/entity db uri)})]
    (concat openapis graphqls)))

(defn request [args]
  (let [req (get cache/requests-cache (get-in args [:argument-values "id"]))]
    (assoc req :_detail req)))

(defn stack-trace-resolver [args]
  (some->> args :object-value :stack-trace (map bean)))

(defn requests [args]
  {"count"
   (fn [_] (.size cache/requests-cache))

   "summaries"
   (fn [_]
     (for [{:keys [xt/id]}
           (paginate (remove #(= "/_site/graphql" (:ring.request/path %))
                             cache/requests-cache) args)]
       (request {:argument-values {"id" id}})))})

(defn system [_]
  (let [system (repl/system)]
    {"configuration"
     (fn [_]
       (let [config (repl/config)]
         {"baseUri" (:juxt.site.alpha/base-uri config)
          "unixPassPasswordPrefix"
          (:juxt.site.alpha.unix-pass/password-prefix config)
          "serverPortNumber"
          (get-in config [:ig/system
                          :juxt.site.alpha.server/server
                          :juxt.site.alpha/port])}))
     "database"
     (fn [_]
       (let [node (:juxt.site.alpha.db/xt-node system)
             status (xt/status node)]
         (merge
          (set/rename-keys
           status
           {:xtdb.version/version "version"
            :xtdb.version/revision "revision"
            :xtdb.index/index-version "indexVersion"
            :xtdb.kv/kv-store "kvStore"
            :xtdb.kv/estimate-num-keys "estimateNumKeys"
            :xtdb.kv/size "kvSize"})
          {"attributeStats"
           (fn [_]
             (for [[name frequency] (xt/attribute-stats node)]
               {"attribute" (str name) "frequency" frequency}))})))

     "version"
     {"gitSha" (fn [_] (git-sha))}

     ;; TODO: Push these into JMX and pull JMX mbeans into GraphQL
     ;; TODO: Suggest to jms he does the same with XT
     "status"
     {"txLogAvail" (fn [_] (df (get-in (config) [:ig/system :juxt.site.alpha.db/xt-node :xtdb/tx-log :kv-store :db-dir])))
      "docStoreAvail" (fn [_] (df (get-in (config) [:ig/system :juxt.site.alpha.db/xt-node :xtdb/document-store :kv-store :db-dir])))
      "indexStoreAvail" (fn [_] (df (get-in (config) [:ig/system :juxt.site.alpha.db/xt-node :xtdb/index-store :kv-store :db-dir])))}

     }))

(defn extract-errors-resolver [args]
  (some->>
   args
   :object-value
   :juxt.site.alpha/errors
   (map (fn [error]
          (let [ex-data (:ex-data error)
                graphql-type-name (::site/graphql-type ex-data "SiteGeneralError")]
            (into
             (into error {::site/graphql-type graphql-type-name})
             (case graphql-type-name
               "SiteGraphqlExecutionError"
               (select-keys ex-data [::site/graphql-stored-query-resource-path
                                     ::site/graphql-operation-name
                                     ::site/graphql-variables
                                     ::grab/errors])
               {}
               )))))))

(defn graphql-errors [args]
  (for [error (some-> args :object-value ::grab/errors)]
    (-> error
        #_(assoc :stack-trace (get-in error [:extensions :stack-trace]))
        #_(update :extensions dissoc :stack-trace))))
