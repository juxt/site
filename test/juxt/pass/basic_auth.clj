;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.basic-auth
  (:require
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri do-action]]))

(defn put-basic-protection-space! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-basic-protection-space![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-protection-space"
      {:xt/id "https://example.org/protection-spaces/basic/wonderland"

       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm "Wonderland" ; optional

       :juxt.pass.alpha/auth-scheme "Basic"})
     ;; end::put-basic-protection-space![]
     ))))

(def dependency-graph
  {"https://example.org/protection-spaces/basic"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/system/put-protection-space"}
    :create #'put-basic-protection-space!}})
