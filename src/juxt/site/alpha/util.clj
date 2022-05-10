;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.util
  (:require
   [clojure.string :as str]
   [taoensso.nippy.utils :refer [freezable?]]))

(alias 'site (create-ns 'juxt.site.alpha))

(defn assoc-when-some [m k v]
  (cond-> m v (assoc k v)))

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

(defn as-hex-str
  "This uses java.util.HexFormat which requires Java 17 and above. If required,
  this can be re-coded, see
  https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
  and similar. For the size parameter, try 12."
  [bytes]
  (.formatHex (java.util.HexFormat/of) bytes))

(defn random-bytes [size]
  (let [result (byte-array size)]
    (.nextBytes (java.security.SecureRandom/getInstanceStrong) result)
    result))
