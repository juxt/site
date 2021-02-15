;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.response
  (:require
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.site.alpha.util :refer [assoc-when-some]]
   [juxt.spin.alpha :as spin]))

(defn representation-metadata-headers [rep]
  (-> {}
      (assoc-when-some "content-type" (some-> rep ::spin/content-type))
      (assoc-when-some "content-encoding" (some-> rep ::spin/content-encoding))
      (assoc-when-some "content-language" (some-> rep ::spin/content-language))
      (assoc-when-some "content-location" (some-> rep ::spin/content-location str))
      (assoc-when-some "last-modified" (some-> rep ::spin/last-modified format-http-date))
      (assoc-when-some "etag" (some-> rep ::spin/etag))
      (assoc-when-some "vary" (some-> rep ::spin/vary))))

(defn payload-headers [rep body]
  (-> {}
      (assoc-when-some "content-length" (or
                                         (some-> rep ::spin/content-length str)
                                         (some-> body count str)))
      (assoc-when-some "content-range" (::spin/content-range rep))
      (assoc-when-some "trailer" (::spin/trailer rep))
      (assoc-when-some "transfer-encoding" (::spin/transfer-encoding rep))))
