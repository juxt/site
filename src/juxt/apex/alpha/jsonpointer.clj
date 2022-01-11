;; Copyright © 2022, JUXT LTD.

(ns juxt.apex.alpha.jsonpointer
  (:require
   [clojure.string :as str]))

(def reference-token-pattern #"/((?:[^/~]|~0|~1)*)")

(defn decode [token]
  (-> token
      (str/replace "~1" "/")
      (str/replace "~0" "~")))

(defn reference-tokens [s]
  (map decode (map second (re-seq reference-token-pattern s))))

(defn json-pointer [doc pointer]
  (loop [tokens (reference-tokens (or pointer ""))
         subdoc doc]
    (if (seq tokens)
      (recur
       (next tokens)
       (cond
         (map? subdoc)
         (let [token (first tokens)
               subsubdoc (or (get subdoc token) (get subdoc (keyword token)))]
           (when (some? subsubdoc) subsubdoc))
         (sequential? subdoc)
         (when (re-matches #"[0-9]+" (first tokens))
           (let [subsubdoc (get subdoc (Integer/parseInt (first tokens)))]
             (when (some? subsubdoc) subsubdoc)))))
      subdoc)))

(comment
  (json-pointer
   {:a [{:b "alpha"} {:b [{:c {"greek" "delta"}}]}]}
   "/a/1/b/0/c/greek"))
