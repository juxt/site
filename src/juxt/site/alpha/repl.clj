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
   [juxt.pass.alpha.application :as application]
   [juxt.pass.alpha.authorization :as authz]
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
   [juxt.dave.alpha :as-alias dave]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.util :as util])
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
  main/*system*)

(defn base-uri []
  (::site/base-uri (config)))

(defn help []
  (doseq [[_ v] (sort (ns-publics 'juxt.site.alpha.repl))
          :let [m (meta v)]]
    (println (format "%s %s: %s"
                     (:name m) (:arglists m) (:doc m))))
  :ok)

(defn xt-node []
  (:juxt.site.alpha.db/xt-node (system)))

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


#_(defn cat-type
  [t]
  (->> (q '{:find [(pull e [*])]
            :where [[e :xt/id]
                    [e ::site/type t]]
            :in [t]} t)
       (map first)
       (sort-by str)))

#_(defn rules []
  (sort-by
   str
   (map first
        (q '{:find [(pull e [*])] :where [[e ::site/type "Rule"]]}))))

#_(defn uuid
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
                             [mapping ::pass/assignee user]
                             [mapping ::pass/role superuser]]
                     :in [superuser]}
              (str base-uri "/_site/roles/superuser")))))

#_(defn admin-access-tokens
  ([] (admin-access-tokens (db) (base-uri)))
  ([db base-uri]
   (map
    first
    (xt/q db {:find '[e]
              :where [['e ::pass/client (str base-uri "/_site/apps/admin")]
                      ['e ::site/type "AccessToken"]]}))))

(defn steps
  ([] (steps (config)))
  ([opts]
   (let [{::site/keys [base-uri]} opts
         _ (assert base-uri)
         db (xt/db (xt-node))]
     [ ;; Awaiting a fix to https://github.com/juxt/xtdb/issues/1480
      {:complete? (xt/entity db "urn:site:tx-fns:do-action")
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

#_(defn put-site-api! []
  (let [config (config)
        xt-node (xt-node)]
    (init/put-site-api! xt-node config)
    (status (steps config))))

#_(defn install-admin-app! []
  (let [config (config)
        xt-node (xt-node)]
    (init/install-admin-app! xt-node config)
    (status (steps config))))

#_(defn create-admin-access-token! [subject]
  (let [config (config)
        xt-node (xt-node)]
    (init/create-admin-access-token! xt-node subject config)
    (status (steps config))))

#_(defn put-auth-resources! []
    (let [config (config)
          xt-node (xt-node)]
      ;;(init/put-openid-token-endpoint! xt-node config)
      ;;(init/put-login-endpoint! xt-node config)
      ;;(init/put-logout-endpoint! xt-node config)
      (status (steps config))))

#_(defn put-superuser-role! []
    (let [config (config)
          xt-node (xt-node)]
      (init/put-superuser-role! xt-node config)
      (status (steps config))))

#_(defn get-password [pass-name]
    (println "Getting password" pass-name)
    (let [{:keys [exit out err]} (sh/sh "pass" "show" pass-name)]
      (if (zero? exit) (str/trim out) (println (ansi/red "Failed to get password")))))

#_(defn put-superuser!
    ([username fullname]
     (if-let [password-prefix (:juxt.site.alpha.unix-pass/password-prefix (config))]
       (if-let [password (get-password (str password-prefix username))]
         (put-superuser! username fullname password)
         (println (ansi/red "Failed to get password")))
       (println (ansi/red "Password required!"))))
    ([username fullname password]
     (let [config (config)
           xt-node (xt-node)]
       (init/put-superuser!
        xt-node
        {:username username
         :fullname fullname
         :password password}
        config)
       (status (steps config)))))

#_(defn update-site-graphql
  []
  (init/put-graphql-schema-endpoint! (xt-node) (config)))

;; No longer any users so no username/password
#_(defn init!
    [username password]
    (let [xt-node (xt-node)
          config (config)]
      (put-site-api!)
      (put-auth-resources!)
      (put-superuser-role!)
      (put-superuser! username "Administrator" password)
      (init/put-graphql-operations! xt-node config)
      (init/put-graphql-schema-endpoint! xt-node config)
      (init/put-request-template! xt-node config)))

#_(defn allow-public-access-to-public-resources! []
  (let [config (config)
        xt-node (xt-node)]
    (init/allow-public-access-to-public-resources! xt-node config)))

#_(defn put-site-txfns! []
  (let [config (config)
        xt-node (xt-node)]
    (init/put-site-txfns! xt-node config)
    (status)))

#_(defn reset-password! [username password]
  (let [user (str (::site/base-uri (config)) "/_site/users/" username)]
    (put!
     {:xt/id (str user "/password")
      ::site/type "Password"
      ::http/methods {:post {}}
      ::pass/user user
      ::pass/password-hash (password/encrypt password)
      ::pass/classification "RESTRICTED"})))

#_(defn user [username]
  (e (format "%s/_site/users/%s" (::site/base-uri (config)) username)))

#_(defn user-apps [username]
  (q '{:find [(pull application [*])]
       :keys [app]
       :where [[grant :juxt.site.alpha/type "Grant"]
               [subject :juxt.pass.alpha/user user]
               [user :juxt.pass.alpha/username username]
               [grant :juxt.pass.alpha/user user]
               [grant :juxt.pass.alpha/permission permission]
               [permission :juxt.site.alpha/application application]]
       :in [username]}
     username))

#_(defn introspect-graphql []
  (let [config (config)
        schema (:juxt.grab.alpha/schema (e (format "%s/_site/graphql" (::site/base-uri config))))
        document (graphql.document/compile-document (graphql.parser/parse (slurp (io/file "opt/graphql/graphiql-introspection-query.graphql"))) schema)]
    (graphql/query schema document "IntrospectionQuery" {} {::site/db (db)})))

;; The REPL is having to construct the more usual network representation of a
;; request context.

;; TODO: Rename this, it's too confusing and has different semantics to
;; init/do-action
(defn make-action-context [subject action edn-arg]
  (let [xt-node (xt-node)]
    {::site/xt-node xt-node
     ::site/db (xt/db xt-node)
     ::pass/subject subject
     ::pass/action action
     ::site/received-representation
     {::http/content-type "application/edn"
      ::http/body (.getBytes (pr-str edn-arg))}}))

(defn do-action [subject action edn-arg]
  (init/do-action (make-action-context subject action edn-arg)))

#_(defn do-action-with-purpose [action purpose & args]
  (apply init/do-action-with-purpose (xt-node) action purpose args))

(defn install-do-action-fn! []
  (put! (authz/install-do-action-fn)))

#_(defn install-repl-user! []
  (put! {:xt/id (repl-subject)
         ::site/type "Subject"
         ::pass/identity (repl-identity)})
  (put! {:xt/id (repl-identity)
         ::site/type "Identity"}))

(defn check-permissions [actions options]
  (authz/check-permissions (db) actions options))

#_(defn install-create-action! []
  (init/install-create-action! (xt-node) (config)))

#_(defn permit-create-action! []
  (init/permit-create-action! (xt-node) (config)))

#_(defn install-grant-permission-action! []
  (init/install-grant-permission-action! (xt-node) (config)))

#_(defn permit-grant-permission-action! []
  (init/permit-grant-permission-action! (xt-node) (config)))

#_(defn create-action! [action]
  (init/create-action! (xt-node) (config) action))

#_(defn grant-permission! [permission]
  (init/grant-permission! (xt-node) (config) permission))

(defn install-openid-provider! [issuer]
  (init/install-openid-provider! (xt-node) issuer))

(defn install-openid-resources!
  [& options]
  (apply init/install-openid-resources! (xt-node) (config) options))

;; Needs a review
#_(defn bootstrap-actions! []
  (install-do-action-fn!)
  (install-repl-user!)
  ;;(install-create-action!)
  (permit-create-action!)
  (install-grant-permission-action!)
  (permit-grant-permission-action!))


#_(defn example-bootstrap! []
  (bootstrap-actions!)

  ;; Create create-person action
  (create-action!
   {:xt/id (str (base-uri) "/actions/create-person")
    :juxt.pass.alpha/scope "write:admin"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re (str (base-uri) "/people/\\p{Alpha}{2,}")]]
      [:example/type [:= "Person"]]
      [:example/name [:string]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0] 'merge {:example/type "Person"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    ::pass/rules
    '[
      [(allowed? subject resource permission)
       [permission ::pass/subject subject]]]})

  (grant-permission!
   {:xt/id (str (base-uri) "/permissions/repl/create-person")
    ::pass/subject (me)
    ::pass/action #{(str (base-uri) "/actions/create-person")}
    ::pass/purpose nil})

  (do-action
   (str (base-uri) "/actions/create-person")
   {:xt/id (str (base-uri) "/people/alice")
    :example/name "Alice"})

  ;; Create the create-identity action
  (create-action!
   {:xt/id (str (base-uri) "/actions/create-identity")
    :juxt.pass.alpha/scope "write:admin"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:juxt.site.alpha/type [:= "Identity"]]
      [:example/person [:re (str (base-uri) "/people/\\p{Alpha}{2,}")]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0] 'merge {:juxt.site.alpha/type "Identity"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]})

  (grant-permission!
   {:xt/id (str (base-uri) "/permissions/repl/create-identity")
    :juxt.pass.alpha/subject "urn:site:subjects:repl"
    :juxt.pass.alpha/action #{(str (base-uri) "/actions/create-identity")}
    :juxt.pass.alpha/purpose nil})

  (do-action
   (str (base-uri) "/actions/create-identity")
   {:xt/id (str (base-uri) "/identities/alice")
    :example/person "https://site.test/people/alice"
    :juxt.pass.jwt/iss "https://juxt.eu.auth0.com/"
    :juxt.pass.jwt/sub "github|123456"}))

(defn factory-reset! []
  (apply evict! (->> (q '{:find [(pull e [:xt/id ::site/type])]
                          :where [[e :xt/id]]})
                     (map first)
                     (map :xt/id))))

(defn sessions []
  (let [db (db)]
    (for [tok (->> (q '{:find [e]
                        :where [[e :xt/id]
                                [e ::site/type "https://meta.juxt.site/pass/session"]]
                        :in [t]} t)
                   (map first)
                   )
          :let [session-id (::pass/session (xt/entity db tok))
                session (xt/entity db session-id)
                subject-id (::pass/subject session)
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
                                 [e ::site/type #{"https://meta.juxt.site/pass/session"
                                                  "https://meta.juxt.site/pass/session-token"}]]
                         :in [t]} t)
                    (map first)
                    )
           :let [session-id (::pass/session (xt/entity db tok))
                 session (xt/entity db session-id)
                 subject (::pass/subject session)]]
       (remove nil? [tok session-id subject]))
     (mapcat seq)
     (apply evict!))))

(defn make-application-doc [& options]
  (apply application/make-application-doc options))

(defn make-application-authorization-doc [& options]
  (apply application/make-application-authorization-doc options))

(defn make-access-token-doc [& options]
  (apply application/make-access-token-doc options))

(defn random-bytes [size]
  (util/random-bytes size))

(defn as-hex-str [bytes]
  (util/as-hex-str bytes))

(defn encrypt-password [password]
  (password/encrypt password))
