;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.crux)

(defn inline-clj-pred [f & args]
  (eval (list apply f args)))
