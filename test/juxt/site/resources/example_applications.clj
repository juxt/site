;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.example-applications
  (:require
   [juxt.site.init :as-alias init]
   [juxt.site.resources.oauth :as oauth]))

;; These are example applications that are useful for testing

(def dependency-graph
  {"https://example.org/applications/{client}"
   {:create #'oauth/register-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}})
