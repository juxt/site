;; Copyright © 2021, JUXT LTD.

(ns juxt.flip.alpha.repl-server
  (:require
   [clojure.core.server :as s]
   [clojure.main :as m]
   [clojure.pprint :refer [pprint]]
   [puget.printer :as puget]))

(def ^{:dynamic true} *stack* nil)

(defn repl-init
  "Initialize repl in user namespace and make standard repl requires."
  []
  (require 'juxt.site.alpha.repl)
  (in-ns 'juxt.site.alpha.repl)
  (apply require clojure.main/repl-requires)
  (println "Site by JUXT. Copyright (c) 2021, JUXT LTD.")
  (println "Type :repl/quit to exit")
  (println "Flip console")
  (clojure.lang.Var/pushThreadBindings {#'*stack* (atom [])}))

(defn repl-read
  "Enhanced :read hook for repl supporting :repl/quit."
  [request-prompt request-exit]
  (or ({:line-start request-prompt :stream-end request-exit}
       (m/skip-whitespace *in*))
      (let [input (read {:read-cond :allow} *in*)]
        (m/skip-if-eol *in*)
        (case input
          :repl/quit request-exit
          :repl/stack (prn @*stack*)
          `(do (swap! *stack* conj ~input) (prn @*stack*))))))

(defn repl
  "REPL with predefined hooks for attachable socket server."
  []
  (m/repl
   :init repl-init
   :read #'repl-read
   :prompt #(printf "flip> ")
   :print puget/cprint))
