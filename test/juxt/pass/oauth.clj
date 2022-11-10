;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.oauth
  (:require
   [clojure.test :refer [deftest is are testing]]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]))

;; First Application

(defn register-example-application! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::register-example-application![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/register-application"
      {:juxt.pass.alpha/client-id "local-terminal"
       ;; TODO: What is this redirect-uri doing here?
       :juxt.pass.alpha/redirect-uri "https://example.org/terminal/callback"})
     ;; end::register-example-application![]
     ))))

(def dependency-graph
  {"https://example.org/applications/local-terminal"
   {:create #'register-example-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}
   })
