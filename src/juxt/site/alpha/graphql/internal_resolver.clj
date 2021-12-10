;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql.internal-resolver
  (:require
   [ring.util.codec :refer [form-decode]]
   [clojure.tools.logging :as log]
   [xtdb.api :as x]
   [clojure.string :as str]))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn subject [{::pass/keys [subject] :keys [db]}]
  (let [user (x/entity db (::pass/user subject))]
    {"user" user
     "authScheme" (::pass/auth-scheme subject)}))

(defn query-string [args]
  (some-> args :juxt.site.alpha/request-context :ring.request/query))

(defn- form [args]
  (let [form (some-> args :juxt.site.alpha/request-context :ring.request/query (form-decode "US-ASCII"))]
    (if (string? form)
      {form "true"}
      ;; Set empty strings to "true" rather than empty-string, to enable selmer
      ;; truthy on 'if'.
      (reduce-kv
       (fn [acc k v]
         (assoc acc k (if (str/blank? v) "true" v)))
       {}
       form))))

(defn query-parameters [args]
  (mapv (fn [[k v]] {"name" k "value" v}) (form args)))

(defn query-parameter [args]
  (get (form args) (-> args :argument-values (get "name"))))

(defn constant [args]
  (some-> args :argument-values (get "value")))
