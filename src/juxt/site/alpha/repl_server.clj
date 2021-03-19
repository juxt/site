;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.repl-server
  (:require
   [clojure.core.server :as s]
   [clojure.main :as m]
   [clojure.pprint :refer [pprint]]))

(defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (require 'juxt.site.alpha.repl)
  (in-ns 'juxt.site.alpha.repl)
  (apply require clojure.main/repl-requires)
  (println "Site by JUXT. Copyright (c) 2021, JUXT LTD.")
  (let [f (requiring-resolve 'juxt.site.alpha.repl/steps)
        steps (f)]
    (when-not (every? :complete? steps)
      (let [g (requiring-resolve 'juxt.site.alpha.repl/status)]
        (g steps)))))

(defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
   :init repl-init
   :read s/repl-read
   :print (comp pprint pr-str)))
