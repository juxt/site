;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql-resolver
  (:require
   [juxt.site.alpha.repl :as repl]))

(defn config [_]
  (repl/config))

(defn system [_]
  (repl/system))

(defn status [_]
  (repl/status))
