;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.protection-space
  (:require
   [clojure.pprint :refer [pprint]]))

(defn create-action-put-protection-space! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/put-protection-space"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/protection-spaces/(.+)"]]
     [:juxt.site/auth-scheme [:enum "Basic" "Bearer"]]
     [:juxt.site/realm {:optional true} [:string {:min 1}]]
     [:juxt.site/canonical-root-uri {:optional true} [:re "https?://[^/]*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (with-out-str
       (pprint
        '(let [content-type (-> *ctx*
                                :juxt.site/received-representation
                                :juxt.http/content-type)
               body (-> *ctx*
                        :juxt.site/received-representation
                        :juxt.http/body)]
           (case content-type
             "application/edn"
             (some->
              body
              (String.)
              clojure.edn/read-string
              juxt.site.malli/validate-input
              (assoc :juxt.site/type "https://meta.juxt.site/site/protection-space"))))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]

      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :role role]
       [user :role role]]]}})

(defn grant-permission-to-put-protection-space! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/system/put-protection-space"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/put-protection-space"
    :juxt.site/purpose nil}})

(def dependency-graph
  {"https://example.org/actions/put-protection-space"
   {:create #'create-action-put-protection-space!
    :deps #{:juxt.site.init/system}}

   "https://example.org/permissions/system/put-protection-space"
   {:create #'grant-permission-to-put-protection-space!
    :deps #{:juxt.site.init/system}}})

(defn put-basic-protection-space! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/put-protection-space"
   :juxt.site/input
   {:xt/id "https://example.org/protection-spaces/basic/wonderland"

    :juxt.site/canonical-root-uri "https://example.org"
    :juxt.site/realm "Wonderland"    ; optional

    :juxt.site/auth-scheme "Basic"}})

(defn put-bearer-protection-space! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/put-protection-space"
   :juxt.site/input
   {:xt/id "https://example.org/protection-spaces/bearer"
    :juxt.site/auth-scheme "Bearer"}})
