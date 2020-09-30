(ns dev
  (:require
   [dev-extras :refer :all]
   [crux.api :as crux]))

(defn crux-node []
  (:juxt.site.alpha.db/crux system))

(defn db []
  (crux/db (crux-node)))

(defn e [id]
  (crux/entity (db) id))

(defn q [query]
  (crux/q (db) query))
