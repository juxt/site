;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.graphql.flip
  (:require
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha.graphql.graphql-compiler :as gcompiler]))

(defmethod f/word 'juxt.site.alpha.graphql.flip/compile-schema
  [[schema-str & stack] [_ & queue] env]
  (let [compiled-schema (gcompiler/compile-schema schema-str)]
    [(cons compiled-schema stack) queue env]))
