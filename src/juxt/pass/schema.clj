;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.schema
  (:require
   [malli.core :as m]
   [juxt.pass :as-alias pass]))

(def schema-registry
  {::pass/subject (m/schema [:map [:xt/id [:string {:min 1}]]])
   ::pass/purpose [:string {:min 1}]})
