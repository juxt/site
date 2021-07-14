;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.repl
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.walk :refer [postwalk]]
   [crux.api :as x]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [clojure.java.shell :as sh]
   [io.aviso.ansi :as ansi]
   [juxt.pass.alpha.authentication :as authn]
   [juxt.site.alpha.main :refer [system config]]
   [juxt.site.alpha.handler :as handler]
   [juxt.site.alpha.cache :as cache]
   [juxt.site.alpha.init :as init]
   [clojure.string :as str])
  (:import (java.util Date)))

(alias 'dave (create-ns 'juxt.dave.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn help []
  (doseq [[_ v] (sort (ns-publics 'juxt.site.alpha.repl))
          :let [m (meta v)]]
    (println (format "%s %s: %s"
                     (:name m) (:arglists m) (:doc m))))
  :ok)

(defn crux-node []
  (:juxt.site.alpha.db/crux-node system))

(defn db []
  (x/db (crux-node)))

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
   (x/entity (db) (str/trim id))))

(defn put! [& ms]
  (->>
   (x/submit-tx
    (crux-node)
    (for [m ms]
      [:crux.tx/put m]))
   (x/await-tx (crux-node))))

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn rm! [& ids]
  (->>
   (x/submit-tx
    (crux-node)
    (for [id ids]
      [:crux.tx/delete id]))
   (x/await-tx (crux-node))))

(defn evict! [& ids]
  (->>
   (x/submit-tx
    (crux-node)
    (for [id ids]
      [:crux.tx/evict id]))
   (x/await-tx (crux-node))))

(defn q [query & args]
  (apply x/q (db) query args))

(defn t [t]
  (map
   first
   (x/q (db) '{:find [e] :where [[e ::site/type t]] :in [t]} t)))

(defn t* [t]
  (map
   first
   (x/q (db) '{:find [e] :where [[e :type t]] :in [t]} t)))

(defn types []
  (->> (q '{:find [t]
            :where [[_ ::site/type t]]})
       (map first)
       (sort)))

(defn ls
  "List Site resources"
  ([]
   (->> (q '{:find [e]
             :where [[e :crux.db/id]
                     [e ::site/type typ]]
             :in [[typ ...]]}
           (disj (set (types)) "Request"))
        (map first)
        (sort-by str)))
  ([pat]
   (->> (q '{:find [e]
             :where [[e :crux.db/id]
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
            :where [[e :crux.db/id]
                    [e ::site/type t]]
            :in [t]} t)
       (map first)
       (sort)))

(defn cat-type
  [t]
  (->> (q '{:find [(pull e [*])]
            :where [[e :crux.db/id]
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
  (cache/find
   handler/requests-cache
   (re-pattern (str "/_site/requests/" s))))

(defn cache []
  handler/requests-cache)

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

(defn sessions []
  (authn/expire-sessions! (java.util.Date.))
  (deref authn/sessions-by-access-token))

(defn clear-sessions []
  (reset! authn/sessions-by-access-token {}))

(defn superusers
  ([] (superusers (config)))
  ([{::site/keys [base-uri]}]
   (map first
        (x/q (db) '{:find [user]
                    :where [[user ::site/type "User"]
                            [mapping ::site/type "UserRoleMapping"]
                            [mapping ::pass/assignee user]
                            [mapping ::pass/role superuser]]
                    :in [superuser]}
             (str base-uri "/_site/roles/superuser")))))

(defn steps
  ([] (steps (config)))
  ([opts]
   (let [{::site/keys [base-uri]} opts
         _ (assert base-uri)
         db (x/db (crux-node))]
     [;; Awaiting a fix to https://github.com/juxt/crux/issues/1480
      #_{:complete? (and
                   (x/entity db (str base-uri "/_site/tx_fns/put_if_match_wildcard"))
                   (x/entity db (str base-uri "/_site/tx_fns/put_if_match_etags")))
       :happy-message "Site transaction functions installed."
       :sad-message "Site transaction functions not installed. "
       :fix "Enter (put-site-txfns!) to fix this."}

      {:complete? (x/entity db (str base-uri "/_site/apis/site/openapi.json"))
       :happy-message "Site API resources installed."
       :sad-message "Site API not installed. "
       :fix "Enter (put-site-api!) to fix this."}

      {:complete? (x/entity db (str base-uri "/_site/token"))
       :happy-message "Authentication resources installed."
       :sad-message "Authentication resources not installed. "
       :fix "Enter (put-auth-resources!) to fix this."}

      {:complete? (x/entity db (str base-uri "/_site/roles/superuser"))
       :happy-message "Role of superuser exists."
       :sad-message "Role of superuser not yet created."
       :fix "Enter (put-superuser-role!) to fix this."}

      {:complete? (pos? (count (superusers opts)))
       :happy-message "At least one superuser exists."
       :sad-message "No superusers exist."
       :fix "Enter (put-superuser! <username> <fullname>) or (put-superuser! <username> <fullname> <password>) to fix this."}])))

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
  (let [config (config)]
    (init/put-site-api!
     (crux-node)
     (as-> "juxt/site/alpha/openapi.edn" %
       (io/resource %)
       (slurp %)
       (edn/read-string
        {:readers
         ;; Forms marked as #edn need to be encoded into a string for transfer
         ;; as JSON and then decoded back into EDN. This is to preserve
         ;; necessary EDN features such as symbols.
         {'juxt.site.alpha/as-str pr-str}} %)
       (json/write-value-as-string %))
     config)
    (status (steps config))))

(defn put-auth-resources! []
  (let [config (config)
        crux-node (crux-node)]
    (init/put-openid-token-endpoint! crux-node config)
    (init/put-login-endpoint! crux-node config)
    (init/put-logout-endpoint! crux-node config)
    (status (steps config))))

(defn put-superuser-role! []
  (let [config (config)
        crux-node (crux-node)]
    (init/put-superuser-role! crux-node config)
    (status (steps config))))

(defn get-password [pass-name]
  (println "Getting password" pass-name)
  (let [{:keys [exit out err]} (sh/sh "pass" "show" pass-name)]
    (if (zero? exit) (str/trim out) (println (ansi/red "Failed to get password")))))

(defn put-superuser!
  ([username fullname]
   (if-let [password-prefix (:juxt.site.alpha.unix-pass/password-prefix (config))]
     (if-let [password (get-password (str password-prefix username))]
       (put-superuser! username fullname password)
       (println (ansi/red "Failed to get password")))
     (println (ansi/red "Password required!"))))
  ([username fullname password]
   (let [config (config)
         crux-node (crux-node)]
     (init/put-superuser!
      crux-node
      {:username username
       :fullname fullname
       :password password}
      config)
     (status (steps config)))))

(defn allow-public-access-to-public-resources! []
  (let [config (config)
        crux-node (crux-node)]
    (init/allow-public-access-to-public-resources! crux-node config)))

(defn put-site-txfns! []
  (let [config (config)
        crux-node (crux-node)]
    (init/put-site-txfns! crux-node config)
    (status)))

(defn reset-password! [username password]
  (let [user (str (::site/base-uri (config))  "/_site/users/" username)]
    (put!
     {:crux.db/id (str user "/password")
      ::site/type "Password"
      ::http/methods #{:post}
      ::pass/user user
      ::pass/password-hash (password/encrypt password)
      ::pass/classification "RESTRICTED"})))

(defn user [username]
  (e (format "http://localhost:2021/_site/users/%s" username)))

(defn user-apps [username]
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
