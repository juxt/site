;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.repl-server
  (:require
   [clojure.core.server :as s]
   [clojure.main :as m]))

(defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (require 'juxt.site.alpha.repl)
  (in-ns 'juxt.site.alpha.repl)
  (apply require clojure.main/repl-requires)
  (println "Site by JUXT. Copyright (c) 2021, JUXT LTD.")
  (println)
  (let [steps (juxt.site.alpha.repl/steps)]
    (when-not (every? :complete? steps)
      (juxt.site.alpha.repl/status steps))))

(defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
   :init repl-init
   :read s/repl-read))
