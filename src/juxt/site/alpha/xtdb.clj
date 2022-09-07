;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.xtdb
  (:require
   [clojure.walk :refer [postwalk]]
   [xtdb.api :as xt]

   [juxt.http.alpha :as-alias http]))

(defn put! [xt-node & ms]
  (->>
   (xt/submit-tx
    xt-node
    (for [m ms]
      (let [vt (:xtdb.api/valid-time m)]
        [:xtdb.api/put (dissoc m :xtdb.api/valid-time) vt])))
   (xt/await-tx xt-node)))

(defn- sanitise-doc
  [doc]
  (if (and (vector? doc)
           (#{::http/content ::http/body} (first doc))
           (> (count (second doc)) 1024))

    [(first doc)
     (cond
       (= ::http/content (first doc)) (str (subs (second doc) 0 80) "…")
       :else (format "(%d bytes)" (count (second doc))))]
    doc))

(defn e [db id]
  (postwalk
   sanitise-doc
   (xt/entity db id)))

(defn hist [db id]
  (xt/entity-history db id :asc {:with-docs? true}))

(defn valid-time [db id] (:xtdb.api/valid-time (xt/entity-tx db id)))

(defn evict! [xt-node & ids]
  (->>
   (xt/submit-tx
    xt-node
    (for [id ids]
      [:xtdb.api/evict id]))
   (xt/await-tx xt-node)))

(defn rm! [xt-node & ids]
  (->>
   (xt/submit-tx
    xt-node
    (for [id ids]
      [:xtdb.api/delete id]))
   (xt/await-tx xt-node)))


