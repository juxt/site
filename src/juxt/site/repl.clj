;; Copyright © 2021, JUXT LTD.

(ns juxt.site.repl
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
   [crypto.password.bcrypt :as password]
   [io.aviso.ansi :as ansi]
   [juxt.http :as-alias http]
   [juxt.site.actions :as actions]
   [juxt.site :as-alias site]
   [juxt.site.cache :as cache]
   [juxt.site.init :as init :refer [config base-uri xt-node system put! do-action]]
   [juxt.site.util :as util]
   [xtdb.api :as xt]
   [juxt.site.repl :as repl])
  (:import (java.util Date)))

(defn base64-reader [form]
  {:pre [(string? form)]}
  (let [decoder (java.util.Base64/getDecoder)]
    (.decode decoder form)))

(def edn-readers
  {'juxt.site/base64 base64-reader
   'regex #(re-pattern %)})

(defn help []
  (doseq [[_ v] (sort (ns-publics 'juxt.site.repl))
          :let [m (meta v)]]
    (println (format "%s %s: %s"
                     (:name m) (:arglists m) (:doc m))))
  :ok)

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

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn rm! [& ids]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [id ids]
      [:xtdb.api/delete id]))
   (xt/await-tx (xt-node))))

(defn evict! [& ids]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [id ids]
      [:xtdb.api/evict id]))
   (xt/await-tx (xt-node))))

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
        (filter (fn [e]
                  (not (#{"Request"
                          "ActionLogEntry"
                          "Session"
                          "SessionToken"
                          }
                        (::site/type e)))))
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

(defn import-resources
  ([] (import-resources "import/resources.edn"))
  ([filename]
   (let [node (xt-node)
         in (java.io.PushbackReader. (io/reader (io/input-stream (io/file filename))))]
     (doseq [rec (resources-from-stream in)]
       (println "Importing record" (:xt/id rec))
       (when (:xt/id rec)
         (xt/submit-tx node [[:xtdb.api/put rec]])))
     (xt/sync node)
     (println "Import finished."))))

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

(comment
  (export-resources
   {:pred (fn [x] (or (= (:juxt.home/type x) "Person")))
    :filename "/home/mal/Sync/persons.edn"
    :uri-mapping {"http://localhost:2021"
                  "https://home.juxt.site"}}))

(defn export-resources
  "Export all resources to a file."
  ([]
   (export-resources {}))
  ([{:keys [out pred filename uri-mapping]}]
   (let [out (or out
                 (when filename (io/output-stream (io/file filename)))
                 (get-zipped-output-stream))
         pred (or pred some?)
         encoder (java.util.Base64/getEncoder)
         resources
         (cond->> (q '{:find [(pull e [*])]
                       :where [[e :xt/id]]})
           true (map first)
           true (filter #(not= (::site/type %) "Request"))
           pred (filter pred)
           uri-mapping (map (apply-uri-mappings uri-mapping))
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
        (cache/recent cache/requests-cache n))
   ))

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

#_(defn sessions []
  (authn/expire-sessions! (java.util.Date.))
  (deref authn/sessions-by-access-token))

#_(defn clear-sessions []
  (reset! authn/sessions-by-access-token {}))

#_(defn superusers
  ([] (superusers (config)))
  ([{::site/keys [base-uri]}]
   (map first
        (xt/q (db) '{:find [user]
                     :where [[user ::site/type "User"]
                             [mapping ::site/type "UserRoleMapping"]
                             [mapping ::site/assignee user]
                             [mapping ::site/role superuser]]
                     :in [superuser]}
              (str base-uri "/_site/roles/superuser")))))

#_(defn admin-access-tokens
  ([] (admin-access-tokens (db) (base-uri)))
  ([db base-uri]
   (map
    first
    (xt/q db {:find '[e]
              :where [['e ::site/client (str base-uri "/_site/apps/admin")]
                      ['e ::site/type "AccessToken"]]}))))

(defn steps
  ([] (steps (config)))
  ([opts]
   (let [{::site/keys [base-uri]} opts
         _ (assert base-uri)
         db (xt/db (xt-node))]
     [ ;; Awaiting a fix to https://github.com/juxt/xtdb/issues/1480
      #_{:complete? (xt/entity db "urn:site:tx-fns:do-action")
         :happy-message "Site do-action transaction function installed."
         :sad-message "Site do-action transaction function not installed. "
         :fix "Enter (install-do-action-fn!) to fix this."}

      #_{:complete? (xt/entity db (str base-uri "/_site/apis/site/openapi.json"))
         :happy-message "Site API resources installed."
         :sad-message "Site API not installed. "
         :fix "Enter (put-site-api!) to fix this."}

      #_{:complete? (xt/entity db (str base-uri "/_site/token"))
         :happy-message "Authentication resources installed."
         :sad-message "Authentication resources not installed. "
         :fix "Enter (put-auth-resources!) to fix this."}

      {:complete? (xt/entity db (str base-uri "/subjects/system"))
       :happy-message "System subject exists."
       :sad-message "System subject does not exist."
       :fix "Enter (init/install-system-subject!) to fix this."}

      #_{:complete? (xt/entity db (str base-uri "/_site/roles/superuser"))
         :happy-message "Role of superuser exists."
         :sad-message "Role of superuser not yet created."
         :fix "Enter (put-superuser-role!) to fix this."}

      #_{:complete? (pos? (count (superusers opts)))
         :happy-message "At least one superuser exists."
         :sad-message "No superusers exist."
         :fix "Enter (put-superuser! <username> <fullname>) or (put-superuser! <username> <fullname> <password>) to fix this."}

      #_{:complete? (xt/entity db (str base-uri "/_site/apps/admin"))
         :happy-message "Admin app exists."
         :sad-message "Admin app does not yet exist."
         :fix "Enter (install-admin-app!) to fix this."}

      #_{:complete? (seq (admin-access-tokens db base-uri))
         :happy-message "Local admin access-token exists."
         :sad-message "Local admin access-token does not yet exist."
         :fix "Enter (create-local-admin-access-token! <subject>) to fix this."}

      ])))

(defn status
  ([] (status (steps (config))))
  ([steps]
   (println)
   (doseq [{:keys [complete? happy-message sad-message fix]} steps]
     (if complete?
       (println "[X] " (ansi/green happy-message))
       (println
        "[ ] "
        (ansi/red sad-message)
        (ansi/yellow fix))))
   (println)
   (if (every? :complete? steps) :ok :incomplete)))

;; The REPL is having to construct the more usual network representation of a
;; request context.

(defn check-permissions [actions options]
  (actions/check-permissions (db) actions options))

(defn factory-reset! []
  (apply evict! (->> (q '{:find [(pull e [:xt/id ::site/type])]
                          :where [[e :xt/id]]})
                     (map first)
                     (map :xt/id))))

(defn sessions []
  (let [db (db)]
    (for [tok (->> (q '{:find [e]
                        :where [[e :xt/id]
                                [e ::site/type "https://meta.juxt.site/site/session"]]
                        :in [t]} t)
                   (map first)
                   )
          :let [session-id (::site/session (xt/entity db tok))
                session (xt/entity db session-id)
                subject-id (::site/subject session)
                subject (xt/entity db subject-id)]]
      {:session-token tok
       :session session
       :subject subject})))

(defn evict-session! [token-id]
  (evict! (format "%s/session-tokens/%s" (base-uri) token-id)))

(defn evict-all-sessions! []
  (let [db (db)]
    (->>
     (for [tok (->> (q '{:find [e]
                         :where [[e :xt/id]
                                 [e ::site/type #{"https://meta.juxt.site/site/session"
                                                  "https://meta.juxt.site/site/session-token"}]]
                         :in [t]} t)
                    (map first)
                    )
           :let [session-id (::site/session (xt/entity db tok))
                 session (xt/entity db session-id)
                 subject (::site/subject session)]]
       (remove nil? [tok session-id subject]))
     (mapcat seq)
     (apply evict!))))

(defn random-bytes [size]
  (util/random-bytes size))

(defn as-hex-str [bytes]
  (util/as-hex-str bytes))

(defn encrypt-password [password]
  (password/encrypt password))
