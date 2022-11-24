;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.session-scope
  (:require
   [juxt.site.init :as init :refer [substitute-actual-base-uri]]
   [juxt.site :as-alias site]
   xtdb.api
   ))

(defn create-action-put-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-session-scope![]
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-session-scope"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/session-scopes/(.+)"]]
        [:juxt.site/cookie-domain [:re "https?://[^/]*"]]
        [:juxt.site/cookie-path [:re "/.*"]]
        [:juxt.site/login-uri [:re "https?://[^/]*"]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
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
               (assoc
                :juxt.site/type "https://meta.juxt.site/site/session-scope")))))}

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
          [user :role role]]]})
     ;; end::create-action-put-session-scope![]
     ))))

(defn grant-permission-to-put-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-session-scope![]
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-session-scope"
       :juxt.site/subject "https://example.org/subjects/system"
       :juxt.site/action "https://example.org/actions/put-session-scope"
       :juxt.site/purpose nil})
     ;; end::grant-permission-to-put-session-scope![]
     ))))

(defn create-session-scope! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-session-scope"
      {:xt/id "https://example.org/session-scopes/default"
       :juxt.site/cookie-name "id"
       :juxt.site/cookie-domain "https://example.org"
       :juxt.site/cookie-path "/"
       :juxt.site/login-uri "https://example.org/login"})))))

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
