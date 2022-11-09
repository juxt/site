;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.session-scope
  (:require
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.flip.alpha.core :as f]
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

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [site/request-body-as-edn
                (site/validate
                 [:map
                  [:xt/id [:re "https://example.org/session-scopes/(.+)"]]
                  [:juxt.pass.alpha/cookie-domain [:re "https?://[^/]*"]]
                  [:juxt.pass.alpha/cookie-path [:re "/.*"]]
                  [:juxt.pass.alpha/login-uri [:re "https?://[^/]*"]]])
                (site/set-type "https://meta.juxt.site/pass/session-scope")
                xtdb.api/put]))]))}

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
