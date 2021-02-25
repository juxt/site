;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.dev-extras
  (:require
   [crux.api :as crux]
   [dev-extras :refer :all]))

(alias 'dave (create-ns 'juxt.dave.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn config []
  integrant.repl.state/config)

(defn local-uri []
  (format "http://localhost:%d" (get-in (config) [:juxt.site.alpha.server/server :port])))

(defn to-local [s]
  (if-let [[_ path] (re-matches #"https://home.juxt.site(.*)" s)]
    (str (local-uri) path)
    s))

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn crux-node []
  (:juxt.site.alpha.db/crux-node system))

(defn db []
  (crux/db (crux-node)))

(defn e [id]
  (crux/entity (db) id))

(defn put [& ms]
  (->>
   (crux/submit-tx
    (crux-node)
    (for [m ms]
      [:crux.tx/put m]))
   (crux/await-tx (crux-node))))

(defn rm [id]
  (->>
   (crux/submit-tx
    (crux-node)
    [[:crux.tx/delete id]])
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
   (sort-by
    str
    (map first
         (q '{:find [e] :where [[e :crux.db/id]]}))))
  ([t]
   (sort-by
    str
    (map first
         (q '{:find [(eql/project e [*])]
              :where [[e :crux.db/id]
                      [e ::site/type t]]
              :in [t]} t)))))

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
