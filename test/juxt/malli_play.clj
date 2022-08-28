;; Copyright © 2022, JUXT LTD.

(ns juxt.malli-play
  (:require
   [malli.core :as m]
   [malli.registry :as mr]
   [malli.instrument :as mi]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]
   [clojure.test :refer [deftest is are testing]]))

(def pass-registry
  (merge
   (m/default-schemas)
   {::pass/subject [:map [:xt/id [:string {:min 1}]]]}))

(m/validate
 [:map [::pass/subject {:optional true}]]
 {::pass/subject {:xt/id "jil"}
  }
 {:registry pass-registry})

(defn
  ^{:malli/schema [:=> [:cat :int :int] :int]}
  plus [x y]
  (+ x y)
  )

(mi/instrument!)

(plus 2 "foo")

(mi/collect!)
