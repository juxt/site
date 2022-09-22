;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.schema
  (:require
   [malli.core :as m]
   [juxt.pass.alpha :as-alias pass]))

(def schema-registry
  {::pass/subject (m/schema [:map [:xt/id [:string {:min 1}]]])
   ::pass/purpose [:string {:min 1}]})

#_(defn
  ^{:malli/schema [:=> [:cat ::pass/subject] ::pass/subject]}
  check-subject [subject]
  subject)

#_(check-subject {::foo :bar})
