;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.application
  (:require
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.util :refer [as-hex-str random-bytes]]))

(defn make-application-doc [& {:keys [prefix client-id client-secret]}]
  {:xt/id (str prefix client-id)
   ::pass/oauth-client-id client-id
   ::pass/oauth-client-secret client-secret})

(defn make-application-authorization-doc [& {:keys [prefix user application]}]
  {:xt/id (str prefix (as-hex-str (random-bytes 10)))
   ::pass/user user
   ::pass/application application})

(defn make-access-token-doc
  [& {:keys [prefix subject application scope token expires-in-seconds]
      :or {token (as-hex-str (random-bytes 16))
           expires-in-seconds (* 60 60)}}]
  {:xt/id (str prefix token)
   ::pass/subject subject
   ::pass/application application
   ::pass/scope scope
   ::pass/token token
   ::pass/expiry (java.util.Date/from (.plusSeconds (.toInstant (java.util.Date.)) expires-in-seconds))})
