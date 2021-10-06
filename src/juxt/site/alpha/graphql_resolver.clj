;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql-resolver
  (:require
   [juxt.site.alpha.repl :as repl]
   [crux.api :as xt]
   [clojure.set :as set]
   [clojure.java.shell :as sh]
   [juxt.site.alpha.main :as main]
   [clojure.string :as str]))

(defn config []
  (main/config))

(defn df [dir]
  (when-let [out (:out (sh/sh "df" "--output=avail" dir))]
    (Long/parseLong
     (str/trim
      (second (str/split out #"\n"))))))

(defn tx-log-avail []
  )

(defn git-sha []
  (let [{:keys [out err]} (sh/sh "git" "rev-parse" "HEAD")]
    (when (seq err)
      (throw (ex-info "Failed to get git sha1 version" {})))
    (str/trim out)))

(defn system [_]
  (let [system (repl/system)]
    {"configuration"
     (delay
       (let [config (repl/config)]
         {"baseUri" (:juxt.site.alpha/base-uri config)
          "unixPassPasswordPrefix"
          (:juxt.site.alpha.unix-pass/password-prefix config)
          "serverPortNumber"
          (get-in config [:ig/system
                          :juxt.site.alpha.server/server
                          :juxt.site.alpha/port])}))
     "database"
     (delay
       (let [node (:juxt.site.alpha.db/crux-node system)
             status (xt/status node)]
         (merge
          (set/rename-keys
           status
           {:crux.version/version "version"
            :crux.version/revision "revision"
            :crux.index/index-version "indexVersion"
            :crux.kv/kv-store "kvStore"
            :crux.kv/estimate-num-keys "estimateNumKeys"
            :crux.kv/size "kvSize"})
          {"attributeStats"
           (delay
             (for [[name frequency] (xt/attribute-stats node)]
               {"attribute" (str name) "frequency" frequency}))})))

     "version"
     {"gitSha" (delay (git-sha))}

     ;; TODO: Push these into JMX and pull JMX mbeans into GraphQL
     ;; TODO: Suggest to jms he does the same with XT
     "status"
     {"txLogAvail" (delay (df (get-in (config) [:ig/system :juxt.site.alpha.db/crux-node :crux/tx-log :kv-store :db-dir])))
      "docStoreAvail" (delay (df (get-in (config) [:ig/system :juxt.site.alpha.db/crux-node :crux/document-store :kv-store :db-dir])))
      "indexStoreAvail" (delay (df (get-in (config) [:ig/system :juxt.site.alpha.db/crux-node :crux/index-store :kv-store :db-dir])))}
     }))
