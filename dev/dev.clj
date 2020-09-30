(ns dev
  (:require
   [dev-extras :refer :all]
   [crux.api :as crux]))

#_(defn crux-node []
  (:crux/node system))

#_(defn db []
  (crux/db (crux-node)))

#_(defn e [id]
  (crux/entity (db) id))

#_(defn q [query]
  (crux/q (db) query))
