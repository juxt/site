;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.util
  (:require
   [clojure.walk :refer [postwalk]])
  )

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

(defn check-freezable [m]
  (postwalk
   (fn [x]
     (when (fn? x)
       (throw (ex-info "Function!" {})))
     (when (vector? x)
       (when-let [[k v] x]
         (when (fn? v)
           (throw (ex-info "Function entry!" {:k k})))))
     x)
   m))
