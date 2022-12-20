;; Copyright © 2021, JUXT LTD.

(ns juxt.site.repl-server
  (:require
   [clojure.main :as m]
   [juxt.site.repl :as repl]
   [puget.printer :as puget]))

(defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (require 'juxt.site.repl)
  (in-ns 'juxt.site.repl)
  (apply require clojure.main/repl-requires)
  (println "Site by JUXT. Copyright (c) 2020-2022, JUXT LTD.")
  (println "Type :quit to exit, :help for help.")
  (let [f (requiring-resolve 'juxt.site.repl/steps)
        steps (f)]
    (when-not (every? :complete? steps)
      (let [g (requiring-resolve 'juxt.site.repl/status)]
        (g steps)))))

(defn- handle-input [input]
  (if-let [v (get (into {} (repl/keyword-commands)) input)]
    (v)
    input))

(defn repl-read
  "Enhanced :read hook for repl supporting :repl/quit and :site/* commands."
  [request-prompt request-exit]
  (or ({:line-start request-prompt :stream-end request-exit}
       (m/skip-whitespace *in*))
      (let [input (read {:read-cond :allow} *in*)]
        (m/skip-if-eol *in*)
        (case input
          (:quit :repl/quit) request-exit
          (handle-input input)))))

(defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
   :init repl-init
   :read repl-read
   :prompt #(printf "site> ")
   :print puget/cprint))
