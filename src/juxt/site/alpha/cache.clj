;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.cache
  (:refer-clojure :exclude [find])
  (:import (java.lang.ref SoftReference)))

(defprotocol ICache
  (put! [_ k obj] "Put object into the cache")
  (find [_ pred] "Find object with k matching pred"))

;; This cache is designed for storing recent requests for debugging purposes
;; purposes.

;; The design contraints of this cache is for:
;; * a fixed capacity of entries
;; * older entries GC'd
;; * use of soft references to relinquish entries early under memory pressure
;; * fast writes, slow reads

(deftype FifoSoftAtomCache [a capacity]
  clojure.lang.ILookup
  (valAt [_ k]
    (reduce
     (fn [_ [k2 sr]]
       (when (= k k2) (reduced (.get sr))))
     nil @a))
  (valAt [this k not-found] (or (.valAt this k) not-found))

  clojure.lang.Seqable
  (seq [_] (keep (fn [[k sr]] (when-some [v (.get sr)] v)) @a))

  ICache
  (put! [_ k obj]
    (swap!
     a
     (fn [v]
       (let [prior-size (count v)]
         (cond-> (conj v [k (SoftReference. obj)])
           (>= prior-size capacity)
           (subvec (inc (- prior-size capacity))))))))
  (find [_ f]
    (let [results
          (keep
           (fn [_ [k2 sr]]
             (when (f k2) (.get sr)))
           @a)]
      (case (count results)
        0 nil
        1 (first results)
        (throw (ex-info (format "%d results" (count results)) {}))))))

(defn new-fifo-soft-atom-cache [capacity]
  (->FifoSoftAtomCache (atom []) capacity))

(comment
  (let [cache (new-fifo-soft-atom-cache 4)]
    (put! cache "1" "one")
    (put! cache "2" "two")
    (put! cache "3" "three")
    (put! cache "4" "four")
    (put! cache "5" "five")
    (get cache "3")
    ))

(comment
  (let [cache (new-fifo-soft-atom-cache 4)]
    (put! cache "1" "one")
    (put! cache "2" "two")
    (find cache #(= % "1") )
    ))

(comment
  (let [cache (new-fifo-soft-atom-cache 4)]
    (put! cache "1" "one")
    (put! cache "2" "two")
    (seq cache)
    ))
