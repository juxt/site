;; Copyright © 2021, JUXT LTD.

(ns juxt.site.repl-server
  (:require
   [clojure.core.server :as s]
   [clojure.main :as m]
   [clojure.pprint :refer [pprint]]
   [puget.printer :as puget]))

(defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (require 'juxt.site.repl)
  (in-ns 'juxt.site.repl)
  (apply require clojure.main/repl-requires)
  (println "Site by JUXT. Copyright (c) 2021, JUXT LTD.")
  (println "Type :repl/quit to exit")
  (let [f (requiring-resolve 'juxt.site.repl/steps)
        steps (f)]
    (when-not (every? :complete? steps)
      (let [g (requiring-resolve 'juxt.site.repl/status)]
        (g steps)))))

(defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
   :init repl-init
   :read s/repl-read
   :prompt #(printf "site> ")
   :print puget/cprint))
