;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql-resolver
  (:require
   [juxt.site.alpha.repl :as repl]
   [juxt.site.alpha.main :as main]))

(defn config [_]
  (pr-str (main/config)))

(defn system [_]
  (pr-str (main/system)))

(defn status [_]
  (pr-str (repl/status)))
