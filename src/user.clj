(ns user
  (:require [integrant.repl :as ir]
            [integrant.core :as ig]
            [juxt.site.server]
            [juxt.site.db]))

(defn start
  []
  (ir/set-prep! (constantly (-> "src/config.edn"
                                slurp
                                ig/read-string
                                :ig/system)))
  (ir/prep)
  (ir/init)
  (ir/go))
