;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.authentication-test
  (:require
   [clojure.set :as set]
   [clojure.test :refer [deftest is are use-fixtures] :as t]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [juxt.pass.alpha.process :as-alias pass.process]
   [juxt.pass.alpha.v3.authorization :as authz]
   [juxt.site.alpha :as-alias site]
   [juxt.test.util :refer [with-xt submit-and-await! *xt-node*]]
   [xtdb.api :as xt]))

(use-fixtures :each with-xt)

;; Note: if you're unfamiliar with the Alice and Bob characters, see
;; https://en.wikipedia.org/wiki/Alice_and_Bob#Cast_of_characters

(def ALICE
  {:xt/id "https://example.org/people/alice"
   ::type "Person"
   ::username "alice"})

;; Given an ID_TOKEN, can we match an XT document that represents a person'
;; identity, and create a subject that links to it


((t/join-fixtures [with-xt])
 (fn []
   ))
