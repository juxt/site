;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.session-scope
  (:require
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.site.alpha :as-alias site]
   xtdb.api
   ))

(defn create-action-put-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-session-scope"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/session-scopes/(.+)"]]
        [:juxt.pass.alpha/cookie-domain [:re "https?://[^/]*"]]
        [:juxt.pass.alpha/cookie-path [:re "/.*"]]
        [:juxt.pass.alpha/login-uri [:re "https?://[^/]*"]]]

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
               (assoc
                :juxt.site.alpha/type "https://meta.juxt.site/pass/session-scope")))))}

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
          [user :role role]]]})
     ;; end::create-action-put-session-scope![]
     ))))

(defn grant-permission-to-put-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-session-scope"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-session-scope"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-session-scope![]
     ))))

(defn create-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-session-scope"
      {:xt/id "https://example.org/session-scopes/default"
       :juxt.pass.alpha/cookie-name "id"
       :juxt.pass.alpha/cookie-domain "https://example.org"
       :juxt.pass.alpha/cookie-path "/"
       :juxt.pass.alpha/login-uri "https://example.org/login"})))))

(def dependency-graph
  {"https://example.org/actions/put-session-scope"
   {:create #'create-action-put-session-scope!
    :deps #{::init/system}}

   "https://example.org/permissions/system/put-session-scope"
   {:create #'grant-permission-to-put-session-scope!
    :deps #{::init/system
            "https://example.org/actions/put-session-scope"}}

   "https://site.test/session-scopes/default"
   {:deps #{::init/system
            "https://example.org/actions/put-session-scope"
            "https://example.org/permissions/system/put-session-scope"}
    :create #'create-session-scope!}})
