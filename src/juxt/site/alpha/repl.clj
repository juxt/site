;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.repl
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [crux.api :as x]
   [clojure.walk :refer [postwalk]]
   [jsonista.core :as json]
   [io.aviso.ansi :as ansi]
   [juxt.pass.alpha.authentication :as authn]
   [juxt.site.alpha.main :refer [system config]]
   [juxt.site.alpha.init :as init])
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
   (x/entity (db) id)))

(defn put! [& ms]
  (->>
   (x/submit-tx
    (crux-node)
    (for [m ms]
      [:crux.tx/put m]))
   (x/await-tx (crux-node))))

(defn GET [id content-type]
  (some #(when (= content-type (::http/content-type %)) %) (::http/representations (e id))))

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

(defn uuid []
  (str (java.util.UUID/randomUUID)))

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

(defn ls
  "List Site resources"
  ([]
   (->> (q '{:find [e] :where [[e :crux.db/id]
                               (not [e ::site/type "Request"])]})
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

(defn cat-type
  [t]
  (->> (q '{:find [(eql/project e [*])]
            :where [[e :crux.db/id]
                    [e ::site/type t]]
            :in [t]} t)
       (map first)
       (sort-by str)))

(defn rules []
  (sort-by
   str
   (map first
        (q '{:find [(eql/project e [*])] :where [[e ::site/type "Rule"]]}))))

(defn uuid [s]
  (cond
    (string? s) (java.util.UUID/fromString s)
    (uuid? s) s))

(defn reqs
  "Display up to 5 of the most recent web requests, most recent first."
  []
  (map first
       (q '{:find [(eql/project e [*]) ended]
            :where [[e ::site/type "Request"]
                    [e ::site/end-date ended]]
            :order-by [[ended :desc]]
            :limit 5})))

(defn count-reqs
  "Count web requests"
  []
  (count
       (q '{:find [e]
            :where [[e ::site/type "Request"]]})))


;; TODO: Use a prefix search for performance
(defn req
  "Display the most recent request."
  ([]
   (into (sorted-map) (first (reqs))))
  ([search]
   (let [results
         (q '{:find [(eql/project e [*])]
              :where [[e ::site/type "Request"]
                      [(re-matches pat e)]]
              :in [pat]
              :limit 5}
            (re-pattern (str "https://.*/_site/requests/" search ".*")))]
     (cond
       (= (count results) 1)  (ffirst results)
       (zero? (count results)) (throw (ex-info "Not found" {:search search}))
       (> (count results) 1) (throw (ex-info "Ambiguous search" {:search search}))))))

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
     [{:complete? (x/entity db (str base-uri "/_site/apis/site/openapi.json"))
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
       :fix "Enter (put-superuser! <username> <password> <fullname>) to fix this."}])))

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
     (-> "juxt/site/alpha/openapi.edn"
         io/resource
         slurp
         edn/read-string
         json/write-value-as-string)
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

(defn put-superuser! [username password fullname]
  (let [config (config)
        crux-node (crux-node)]
    (init/put-superuser! crux-node username password fullname config)
    (status (steps config))))

(defn allow-public-access-to-public-resources! []
  (let [config (config)
        crux-node (crux-node)]
    (init/allow-public-access-to-public-resources! crux-node config)))
