;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources
  (:require
   [selmer.parser :as selmer]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]))

(comment
  (selmer/render "Hello {{foo|lower}}" {"foo" "World!"}))

(defn load-dependency-graph [path]
  (edn/read-string
   {:readers {'juxt.pprint (fn [x] (with-out-str (pprint x)))}}
   (slurp (io/file "resources" path))))

(comment
  (load-dependency-graph "juxt/site/openid.edn"))
