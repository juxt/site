;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.util
  (:require
   [juxt.clojars-mirrors.nippy.v3v1v1.taoensso.nippy.utils :refer [freezable?]]
   [xtdb.api :as xt]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))

(defn assoc-when-some [m k v]
  (cond-> m v (assoc k v)))

;;TODO find out what is different about this compared to assoc-when-some above
(defn assoc-some
  "Associates a key with a value in a map, if and only if the value is not nil."
  ([m k v]
   (if (or (nil? v) (false? v)) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))

(defn hexdigest
  "Returns the hex digest of an object.
  computing entity-tags."
  ([input] (hexdigest input "SHA-256"))
  ([input hash-algo]
   (let [hash (java.security.MessageDigest/getInstance hash-algo)]
     (. hash update input)
     (let [digest (.digest hash)]
       (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))))

(def mime-types
  {"html" "text/html;charset=utf-8"
   "js" "application/javascript"
   "map" "application/json"
   "css" "text/css"
   "png" "image/png"
   "adoc" "text/asciidoc"})

(defn paths
  "Given a nested structure, return the paths to each leaf."
  [form]
  (if (coll? form)
    (for [[k v] (if (map? form) form (map vector (range) form))
          w (paths v)]
      (cons k (if (coll? w) w [w])))
    (list form)))

(comment
  (for [path
        (paths {:x {:y {:z [:a :b :c] :z2 [0 1 {:u {:v 1}}]}}
                :p {:q {:r :s :t :u :y (fn [_] nil)}}})
        :when (not (fn? (last path)))]
    path))

(defn deep-replace
  "Apply f to x, where x is a map value, collection member or scalar, anywhere in
  the form's structure. This is similar, but not identical to,
  clojure.walk/postwalk."
  [form f]
  (cond
    (map? form) (reduce-kv (fn [acc k v] (assoc acc k (deep-replace v f))) {} form)
    (vector? form) (mapv (fn [i] (deep-replace i f)) form)
    (coll? form) (map (fn [i] (deep-replace i f)) form)
    :else (f form)))

(comment
  (deep-replace {:a :b :c [identity {:x [{:g [:a :b identity]}]}]} #(if (fn? %) :replaced %)))

(defn ->freezeable [form]
  (deep-replace
   form
   (fn [form]
     (cond-> form
       (not (freezable? form))
       ((fn [_] ::site/unfreezable))))))

(defn etag [representation]
  (format
   "\"%s\""
   (subs
    (hexdigest
     (cond
       (::http/body representation)
       (::http/body representation)
       (::http/content representation)
       (.getBytes (::http/content representation)
                  (get representation ::http/charset "UTF-8")))) 0 32)))
