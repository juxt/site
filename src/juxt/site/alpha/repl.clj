;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.repl
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :refer [postwalk]]
   [xtdb.api :as xt]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [clojure.java.shell :as sh]
   [io.aviso.ansi :as ansi]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.grab.alpha.schema :as graphql.schema]
   [juxt.grab.alpha.document :as graphql.document]
   [juxt.grab.alpha.parser :as graphql.parser]
   [selmer.parser :as selmer]
   [juxt.site.alpha.main :as main]
   [juxt.site.alpha.handler :as handler]
   [juxt.site.alpha.cache :as cache]
   [juxt.site.alpha.init :as init]
   [clojure.string :as str]
   [juxt.grab.alpha.parser :as parser]
   [clojure.tools.logging :as log]

   [juxt.dave.alpha :as-alias dave]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site])
  (:import (java.util Date)))

(defn base64-reader [form]
  {:pre [(string? form)]}
  (let [decoder (java.util.Base64/getDecoder)]
    (.decode decoder form)))

(def edn-readers
  {'juxt.site/base64 base64-reader
   'regex #(re-pattern %)})

(defn config []
  (main/config))

(defn system []
  main/system)

(defn help []
  (doseq [[_ v] (sort (ns-publics 'juxt.site.alpha.repl))
          :let [m (meta v)]]
    (println (format "%s %s: %s"
                     (:name m) (:arglists m) (:doc m))))
  :ok)

(defn xt-node []
  (:juxt.site.alpha.db/xt-node main/system))

(defn db []
  (xt/db (xt-node)))

(defn e [id]
  (postwalk
   (fn [x] (if (and (vector? x)
                    (#{::http/content ::http/body} (first x))
                    (> (count (second x)) 1024))

             [(first x)
              (cond
                (= ::http/content (first x)) (str (subs (second x) 0 80) "…")
                :else (format "(%d bytes)" (count (second x))))]
             x))
   (xt/entity (db) id)))

(defn hist [id]
  (xt/entity-history (db) id :asc {:with-docs? true}))

(defn valid-time [id] (:xtdb.api/valid-time (xt/entity-tx (db) id)))

(defn put! [& ms]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [m ms]
      (let [vt (:xtdb.api/valid-time m)]
        [:xtdb.api/put (dissoc m :xtdb.api/valid-time) vt])))
   (xt/await-tx (xt-node))))

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn rm! [& ids]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [id ids]
      [:xtdb.api/delete id]))
   (xt/await-tx (xt-node))))

(defn ids-for-type-k
  [type-k]
  (map
   first
   (xt/q (db) {:find ['e]
               :where [['e type-k]]
               :timeout (* 1 1000 60 10)})))

(defn count-type
  [type-k]
  (count (ids-for-type-k type-k)))

(defn evict! [& ids]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [id ids]
      [:xtdb.api/evict id]))
   (xt/await-tx (xt-node))))

(defn rm-r!
  "Does xtdb.api/delete on all entities containing an attribute with the given key"
  [type-k]
  (let [entities (ids-for-type-k type-k)]
    (apply evict! entities)))

(defn q [query & args]
  (apply xt/q (db) query args))

(defn t [t]
  (map
   first
   (xt/q (db) '{:find [e] :where [[e ::site/type t]] :in [t]} t)))

(defn t* [t]
  (map
   first
   (xt/q (db) '{:find [e] :where [[e :type t]] :in [t]} t)))

(defn types []
  (->> (q '{:find [t]
            :where [[_ ::site/type t]]})
       (map first)
       (sort)))

(defn ls
  "List Site resources"
  ([]
   (->> (q '{:find [(pull e [:xt/id ::site/type])]
             :where [[e :xt/id]]})
        (map first)
        (filter #(not= (::site/type %) "Request"))
        (map :xt/id)
        (sort-by str)))
  ([pat]
   (->> (q '{:find [e]
             :where [[e :xt/id]
                     [(str e) id]
                     [(re-seq pat id) match]
                     [(some? match)]]
             :in [pat]}
           (re-pattern pat))
        (map first)
        (sort-by str))))

(defn ls-type
  [t]
  (->> (q '{:find [e]
            :where [[e :xt/id]
                    [e ::site/type t]]
            :in [t]} t)
       (map first)
       (sort)))

(defn now-id []
  (.format
   (.withZone
    (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd-HHmmss")
    (java.time.ZoneId/systemDefault))
   (java.time.Instant/now)))

;; Start import at 00:35

(defn resources-from-stream [in]
  (let [record (try
                 (edn/read
                  {:eof :eof :readers edn-readers}
                  in)
                 (catch Exception e
                   (def in in)
                   (prn (.getMessage e))))]
    (cond
      (nil? record)
      (lazy-seq (resources-from-stream in))
      (not= record :eof)
      (cons record (lazy-seq (resources-from-stream in)))
      :else
      nil)))

(defn- submit-and-wait-tx
  [node tx]
  (let [tx-id (xt/submit-tx node tx)]
    (xt/await-tx node tx-id)))

(defn validate-resource-line [s]
  (edn/read-string
   {:eof :eof :readers edn-readers}
   s))

(defn get-zipped-output-stream []
  (let [zos (doto
             (-> (str (now-id) ".edn.zip")
                 io/file
                 io/output-stream
                 java.util.zip.ZipOutputStream.)
              (.putNextEntry (java.util.zip.ZipEntry. "resources.edn")))]
    (java.io.OutputStreamWriter. zos)))

(defn apply-uri-mappings [mapping]
  (fn [ent]
    ;; Create a regex pattern which detects anything as a mapping key
    (let [pat (re-pattern (str/join "|" (map #(format "\\Q%s\\E" %) (keys mapping))))]
      (postwalk
       (fn [s]
         (cond-> s
           (string? s)
           (str/replace pat (fn [x] (get mapping x)))))
       ent))))

(defn import-resources
  ([] (import-resources {:filename "import/resources.edn"
                         :uri-mapping
                         {"{{KG_URL_BASE}}" ""}}))
  ([{:keys [filename uri-mapping]}]
   (let [node (xt-node)
         in (java.io.PushbackReader. (io/reader (io/input-stream (io/file filename))))]
     (doseq [rec ((apply-uri-mappings uri-mapping) (resources-from-stream in))
             :when rec]
       (if (e (:xt/id rec))
         (println "Skipping existing resource: " (:xt/id rec))
         (do
           (submit-and-wait-tx node [[:xtdb.api/put rec]])
           (println "Imported resource: " (:xt/id rec))))))))

(defn validate-resource-line [s]
  (edn/read-string
   {:eof :eof :readers edn-readers}
   s))

(defn get-zipped-output-stream []
  (let [zos (doto
             (-> (str (now-id) ".edn.zip")
                 io/file
                 io/output-stream
                 java.util.zip.ZipOutputStream.)
              (.putNextEntry (java.util.zip.ZipEntry. "resources.edn")))]
    (java.io.OutputStreamWriter. zos)))

(comment
  (export-resources
   {:pred (fn [x] (or (= (:juxt.home/type x) "Person")))
    :filename "/home/mal/Sync/persons.edn"}))

(defn export-resources
  "Export all resources to a file."
  ([]
   (export-resources {}))
  ([{:keys [out pred filename]}]
   (let [out (or out
                 (when filename (io/output-stream (io/file filename)))
                 (get-zipped-output-stream))
         encoder (java.util.Base64/getEncoder)
         resources
         (cond->> (q '{:find [(pull e [*])]
                       :where [[e :xt/id]]
                       :timeout 10000000000})
           true (map first)
           true (sort-by :xt/id))]

     (defmethod print-method (type (byte-array [])) [x writer]
       (.write writer "#juxt.site/base64")
       (.write writer (str " \"" (String. (.encode encoder x)) "\"")))

     (with-open [w (io/writer out)]
       (doseq [batch (partition-all 100 (map vector (range) resources))]
         (doseq [[_ ent] batch]
           (let [line (pr-str ent)]
             ;; Test the line can be read
             #_(try
                 (validate-resource-line line)
                 (catch Exception e
                   (throw
                    (ex-info
                     (format "Serialization of entity '%s' will not be readable" (:xt/id ent))
                     {:xt/id (:xt/id ent)} e))))
             (.write w line)
             (.write w (System/lineSeparator))))
         (let [n (inc (first (last batch)))
               total (count resources)
               pct (float (/ (* 100 n) total))]
           (printf "Written %d/%d (%.2f%%) resources\n" n total pct))))

     (remove-method print-method (type (byte-array [])))
     (printf "Dumped %d resources\n" (count resources)))))

(defn cat-type
  [t]
  (->> (q '{:find [(pull e [*])]
            :where [[e :xt/id]
                    [e ::site/type t]]
            :in [t]} t)
       (map first)
       (sort-by str)))

(defn rules []
  (sort-by
   str
   (map first
        (q '{:find [(pull e [*])] :where [[e ::site/type "Rule"]]}))))

(defn uuid
  ([] (str (java.util.UUID/randomUUID)))
  ([s]
   (cond
     (string? s) (java.util.UUID/fromString s)
     (uuid? s) s)))

(defn req [s]
  (into
   (sorted-map)
   (cache/find
    cache/requests-cache
    (re-pattern (str "/_site/requests/" s)))))

(defn recent
  ([] (recent 5))
  ([n]
   (map (juxt ::site/request-id ::site/date ::site/uri :ring.request/method :ring.response/status)
        (cache/recent cache/requests-cache n))))

(defn requests-cache []
  cache/requests-cache)

(defn gc
  "Remove request data that is older than an hour."
  ([] (gc (* 1 60 60)))
  ([seconds]
   (let [records (map first
                      (q '{:find [e]
                           :where [[e ::site/type "Request"]
                                   [e ::site/end-date ended]
                                   [(< ended checkpoint)]]
                           :in [checkpoint]}
                         (Date. (- (.getTime (Date.)) (* seconds 1000)))))]
     (doseq [batch (partition-all 100 records)]
       (println "Evicting" (count batch) "records")
       (println (apply evict! batch))))))

(defn steps
  ([] (steps (config)))
  ([opts]
   (let [db (xt/db (xt-node))]
     [;; Awaiting a fix to https://github.com/juxt/xtdb/issues/1480
      #_{:complete? (and
                     (xt/entity db (str base-uri "/_site/tx_fns/put_if_match_wildcard"))
                     (xt/entity db (str base-uri "/_site/tx_fns/put_if_match_etags")))
         :happy-message "Site transaction functions installed."
         :sad-message "Site transaction functions not installed. "
         :fix "Enter (put-site-txfns!) to fix this."}

      {:complete? (xt/entity db "/_site/apis/site/openapi.json")
       :happy-message "Site API resources installed."
       :sad-message "Site API not installed. "
       :fix "Enter (put-site-api!) to fix this."}])))

(defn status
  ([] (status (steps (config))))
  ([steps]
   (println)
   (doseq [{:keys [complete? happy-message sad-message fix]} steps]
     (if complete?
       (println "[✔] " (ansi/green happy-message))
       (println
        "[ ] "
        (ansi/red sad-message)
        (ansi/yellow fix))))
   (println)
   (if (every? :complete? steps) :ok :incomplete)))

(defn put-site-api! []
  (let [config (config)
        xt-node (xt-node)]
    (init/put-site-api! xt-node config)
    (status (steps config))))

(defn update-site-graphql
  []
  (init/put-graphql-schema-endpoint! (xt-node) (config)))

(defn put-site-txfns! []
  (let [config (config)
        xt-node (xt-node)]
    (init/put-site-txfns! xt-node config)
    (status)))

(defn introspect-graphql []
  (let [config (config)
        schema (:juxt.grab.alpha/schema (e "/_site/graphql"))
        document (graphql.document/compile-document (graphql.parser/parse (slurp (io/file "opt/graphql/graphiql-introspection-query.graphql"))) schema)]
    (graphql/query schema document "IntrospectionQuery" {} {::site/db (db)})))

(defn repl-post-handler [{::site/keys [uri db]
                          ::pass/keys [subject]
                          :as req}]
  (let [body (some-> req ::site/received-representation ::http/body (String.) read-string)
        _ (when (nil? body)
            (throw
             (ex-info
              "Invalid body"
              {::site/request-context req})))

        results (try
                  (binding [*ns* (find-ns 'juxt.site.alpha.repl)]
                    (eval body))
                  (catch Exception e
                    (throw (ex-info "Syntax error" e))))]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string results))
        (update :ring.response/headers assoc "content-type" "application/json"))))

(def site-seed-zip "dev/seeds.edn.zip")

(defn is-site-initialized?
  "Returns true if insite console page is in the DB"
  []
  (log/info "Checking if site is ready")
  (some? (e "/_site/graphql")))

(defn init!
  []
  (let [xt-node (xt-node)
        config (config)]
     ;(ensure-init!)
    (put-site-api!)
    (init/put-graphql-operations! xt-node config)
    (init/put-graphql-schema-endpoint! xt-node config)
    (init/put-request-template! xt-node config)))
