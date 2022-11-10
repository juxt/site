;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.oauth
  (:require
   [clojure.test :refer [deftest is are testing]]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]))

;; First Application

(defn register-example-application! [{:keys [params]}]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/register-application")
   (substitute-actual-base-uri
    {:juxt.pass.alpha/client-id (get params "client")
     ;; TODO: What is this redirect-uri doing here?
     :juxt.pass.alpha/redirect-uri "https://example.org/terminal/callback"})))

(def dependency-graph
  {"https://example.org/applications/{client}"
   {:create #'register-example-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}
   })
