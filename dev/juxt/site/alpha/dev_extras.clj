;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.dev-extras
  (:require
   [crux.api :as crux]
   [dev-extras :refer :all]
   [juxt.pass.alpha.authentication :as authn])
  (:import (java.util Date)))

(alias 'dave (create-ns 'juxt.dave.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn config [] integrant.repl.state/config)

(defn local-uri []
  (format "http://localhost:%d" (get-in (config) [:juxt.site.alpha.server/server :port])))

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn crux-node []
  (:juxt.site.alpha.db/crux-node system))

(defn db []
  (crux/db (crux-node)))

(defn e [id]
  (crux/entity (db) id))

(defn put! [& ms]
  (->>
   (crux/submit-tx
    (crux-node)
    (for [m ms]
      [:crux.tx/put m]))
   (crux/await-tx (crux-node))))

(defn GET [id content-type]
  (some #(when (= content-type (::http/content-type %)) %) (::http/representations (e id))))

(defn rm! [& ids]
  (->>
   (crux/submit-tx
    (crux-node)
    (for [id ids]
      [:crux.tx/delete id]))
   (crux/await-tx (crux-node))))

(defn evict! [& ids]
  (->>
   (crux/submit-tx
    (crux-node)
    (for [id ids]
      [:crux.tx/evict id]))
   (crux/await-tx (crux-node))))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn q [query & args]
  (apply crux/q (db) query args))

(defn t [t]
  (map
   first
   (crux/q (db) '{:find [e] :where [[e ::site/type t]] :in [t]} t)))

(defn t* [t]
  (map
   first
   (crux/q (db) '{:find [e] :where [[e :type t]] :in [t]} t)))

(defn ls
  ([]
   (->> (q '{:find [e] :where [[e :crux.db/id]]})
        (map first)
        ;;(remove #(and (string? %) (re-seq #"_site/requests" %)))
        (sort-by str)))
  ([t]
   (->> (q '{:find [(eql/project e [*])]
             :where [[e :crux.db/id]
                     [e ::site/type t]]
             :in [t]} t)
        (map first)
        (sort-by str))))

(defn rules []
  (sort-by
   str
   (map first
        (q '{:find [(eql/project e [*])] :where [[e ::site/type "Rule"]]}))))

(defn uuid [s]
  (cond
    (string? s) (java.util.UUID/fromString s)
    (uuid? s) s))

(defn uri [s]
  (cond
    (string? s) (java.net.URI. s)
    (uri? s) s))

(defn we
  "Lookup a 'web entity'"
  [u]
  (e (uri u)))

(defn wes
  "List all web entities"
  []
  (sort
   (for [[e m] (q '{:find [e (distinct m)] :where [[e ::http/methods m]]})
         :let [ent (crux/entity (db) e)]]
     [(str e) m (count (::http/representations ent))])))

(defn reqs
  "Display up to 5 of the most recent web requests, most recent first."
  []
  (map first
       (q '{:find [(eql/project e [*]) ended]
            :where [[e ::site/type "Request"]
                    [e ::site/end-date ended]]
            :order-by [[ended :desc]]
            :limit 5})))

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
   (apply
    evict!
    (map first
         (q '{:find [e]
              :where [[e ::site/type "Request"]
                      [e ::site/end-date ended]
                      [(< ended checkpoint)]]
              :in [checkpoint]}
            (Date. (- (.getTime (Date.)) (* seconds 1000))))))))

(defn sessions []
  (authn/expire-sessions! (java.util.Date.))
  (deref authn/sessions-by-access-token))
