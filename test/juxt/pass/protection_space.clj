;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.protection-space
  (:require
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]))

(defn create-action-put-protection-space! [_]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/create-action")
   (substitute-actual-base-uri
    {:xt/id "https://example.org/actions/put-protection-space"

     :juxt.site.alpha.malli/input-schema
     [:map
      [:xt/id [:re "https://example.org/protection-spaces/(.+)"]]
      [:juxt.pass.alpha/auth-scheme [:enum "Basic" "Bearer"]]
      [:juxt.pass.alpha/realm {:optional true} [:string {:min 1}]]
      [:juxt.pass.alpha/canonical-root-uri {:optional true} [:re "https?://[^/]*"]]]

     :juxt.site.alpha/prepare
     {:juxt.site.alpha.sci/program
      (pr-str
       '(let [content-type (-> *ctx*
                               :juxt.site.alpha/received-representation
                               :juxt.http.alpha/content-type)
              body (-> *ctx*
                       :juxt.site.alpha/received-representation
                       :juxt.http.alpha/body)]
          (case content-type
            "application/edn"
            (some->
             body
             (String.)
             clojure.edn/read-string
             juxt.site.malli/validate-input
             (assoc :juxt.site.alpha/type "https://meta.juxt.site/pass/protection-space")))))}

     :juxt.site.alpha/transact
     {:juxt.site.alpha.sci/program
      (pr-str
       '[[:xtdb.api/put *prepare*]])}

     :juxt.pass.alpha/rules
     '[
       [(allowed? subject resource permission)
        [permission :juxt.pass.alpha/subject subject]]

       [(allowed? subject resource permission)
        [subject :juxt.pass.alpha/user-identity id]
        [id :juxt.pass.alpha/user user]
        [permission :role role]
        [user :role role]]]})))

(defn grant-permission-to-put-protection-space! [_]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/grant-permission")
   (substitute-actual-base-uri
    {:xt/id "https://example.org/permissions/system/put-protection-space"
     :juxt.pass.alpha/subject "https://example.org/subjects/system"
     :juxt.pass.alpha/action "https://example.org/actions/put-protection-space"
     :juxt.pass.alpha/purpose nil})))

(defn put-basic-protection-space! [_]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-protection-space")
   (substitute-actual-base-uri
    {:xt/id "https://example.org/protection-spaces/basic/wonderland"

     :juxt.pass.alpha/canonical-root-uri "https://example.org"
     :juxt.pass.alpha/realm "Wonderland" ; optional

     :juxt.pass.alpha/auth-scheme "Basic"})))

(defn put-bearer-protection-space! [_]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-protection-space")
   (substitute-actual-base-uri
    {:xt/id "https://example.org/protection-spaces/bearer"
     :juxt.pass.alpha/auth-scheme "Bearer"})))

(def dependency-graph
  {"https://example.org/actions/put-protection-space"
   {:create #'create-action-put-protection-space!
    :deps #{::init/system}}

   "https://example.org/permissions/system/put-protection-space"
   {:create #'grant-permission-to-put-protection-space!
    :deps #{::init/system}}

   "https://example.org/protection-spaces/basic"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/system/put-protection-space"}
    :create #'put-basic-protection-space!}

   "https://example.org/protection-spaces/bearer"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/system/put-protection-space"}
    :create #'put-bearer-protection-space!}})
