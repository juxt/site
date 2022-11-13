;; Copyright © 2022, JUXT LTD.

(ns juxt.example-applications
  (:require
   [clojure.string :as str]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.pass.oauth :as oauth]
   [malli.core :as malli]))

;; These are example applications that are useful for testing

(def dependency-graph
  {"https://example.org/applications/{client}"
   {:create #'oauth/register-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}})
