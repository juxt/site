;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.example-protection-spaces
  (:require
   [juxt.site.init :as init]
   [juxt.site.resources.protection-space :refer [put-bearer-protection-space!
                                                 put-basic-protection-space!]]))

;; Depends on juxt.site.resources.protection-space/dependency-graph
(def dependency-graph
  {"https://example.org/protection-spaces/basic"
   {:deps #{::init/system
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/system/put-protection-space"}
    :create #'put-basic-protection-space!}

   "https://example.org/protection-spaces/bearer"
   {:deps #{::init/system
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/system/put-protection-space"}
    :create #'put-bearer-protection-space!}})
