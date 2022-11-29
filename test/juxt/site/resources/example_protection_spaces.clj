;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.example-protection-spaces
  (:require
   [juxt.site.init :as init]))

(def dependency-graph
  {"https://example.org/bearer-protection-space"
   {:deps #{:juxt.site.init/system}
    :create (fn [{:keys [id]}]
              (init/put!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.site/auth-scheme "Bearer"})))}})
