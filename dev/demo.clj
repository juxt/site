;; Copyright © 2022, JUXT LTD.

(ns demo
  (:require
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [clojure.walk :refer [postwalk]]
   [clojure.string :as str]
   [malli.core :as m]
   [juxt.site.alpha.repl :refer [base-uri put! install-do-action-fn!]]))

(defn substitute-actual-base-uri [form]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://site.test" (base-uri)))
     )
   form))

(defn demo-put-user! []
  ;; tag::install-user![]
  (put! {:xt/id "https://site.test/users/mal"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/user"
         :juxtcode "mal"
         :name "Malcolm Sparks"})
  ;; end::install-user![]
  )

(defn demo-put-user-identity! []
  ;; tag::install-user-identity![]
  (put! {:xt/id "https://site.test/identities/mal"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
         :juxt.pass.alpha/user "https://site.test/users/mal"
         :juxt.pass.jwt/iss "https://juxt.eu.auth0.com/"
         :juxt.pass.jwt/sub "github|163131"})
  ;; end::install-user-identity![]
  )

(defn demo-put-subject! []
  ;; tag::install-subject![]
  (put! {:xt/id "https://site.test/subjects/repl-default"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
         :juxt.pass.alpha/identity "https://site.test/identities/mal"})
  ;; end::install-subject![]
  )

(defn demo-install-create-action! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::install-create-action![]
     (put!
      {:xt/id "https://site.test/actions/create-action" ; <1>
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
       :juxt.pass.alpha/scope "write:admin"
       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/actions/(.+)"]] ; <2>
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/action"]]
         [:juxt.pass.alpha/rules [:vector [:vector :any]]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0] ; <3>
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/action"}]
        [:juxt.pass.alpha.malli/validate] ; <4>
        [:xtdb.api/put]] ; <5>

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource) ; <6>
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::install-create-action![]
     ))))

(defn demo-install-do-action-fn! []
  ;; tag::install-do-action-fn![]
  (install-do-action-fn!)
  ;; end::install-do-action-fn![]
  )

(defn demo-permit-create-action! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::permit-create-action![]
     (put!
      {:xt/id "https://site.test/permissions/mal/create-action" ; <1>
       :juxt.site.alpha/type "https://meta.juxt.site/pass/permission" ; <2>
       :juxt.pass.alpha/user "https://site.test/users/mal" ; <3>
       :juxt.pass.alpha/action "https://site.test/actions/create-action" ; <4>
       :juxt.pass.alpha/purpose nil}) ; <5>
     ;; end::permit-create-action![]
     ))))

(defn demo-create-grant-permission-action! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-grant-permission-action![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/grant-permission"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
       :juxt.pass.alpha/scope "write:admin"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/permissions/(.+)"]]
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/permission"]]
         [:juxt.pass.alpha/action [:re "https://site.test/actions/(.+)"]]
         [:juxt.pass.alpha/purpose [:maybe :string]]
         ]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0]
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/permission"}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-grant-permission-action![]
     ))))

(defn demo-permit-grant-permission-action! []
  ;; tag::permit-grant-permission-action![]
  (put!
   {:xt/id "https://site.test/permissions/mal/grant-permission"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
    :juxt.pass.alpha/user "https://site.test/users/mal"
    :juxt.pass.alpha/action "https://site.test/actions/grant-permission"
    :juxt.pass.alpha/purpose nil})
  ;; end::permit-grant-permission-action![]
  )

(defn demo-create-action-put-user! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-user![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-user"
       :juxt.pass.alpha/scope "write:users"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/users/.*"]]
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/user"]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0]
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/user"
                 :juxt.http.alpha/methods
                 {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
                  :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
                  :options {}}}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-user![]
     ))))

(defn demo-create-action-put-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-identity![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-identity"
       :juxt.pass.alpha/scope "write:users"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/.*"]]
         [:juxt.pass.alpha/user [:re "https://site.test/users/.+"]]
         [:juxt.pass.jwt/iss {:optional true} [:re "https://.+"]]
         [:juxt.pass.jwt/sub {:optional true} [:string {:min 1}]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in
         [0] 'merge
         {:juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
          :juxt.http.alpha/methods
          {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-identity"}}
           :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-identity"}}
           :options {}}}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-identity![]
     ))))

(defn demo-create-action-put-subject! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-subject![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-subject"
       ;;:juxt.pass.alpha/scope "write:users"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/.*"]]
         [:juxt.pass.alpha/identity [:re "https://site.test/identities/.+"]]
         ]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in
         [0] 'merge
         {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-subject![]
     ))))

(defn demo-grant-permission-to-invoke-action-put-subject! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-subject![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-subject"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/put-subject"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-subject![]
     ))))

#_(defn demo-bootstrap-actions! []
  (demo-install-do-action-fn!)
  (demo-put-repl-user!)
  (demo-install-create-action!)
  (demo-permit-create-action!)
  (demo-create-grant-permission-action!)
  (demo-permit-grant-permission-action!))

(defn demo-create-action-put-immutable-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-immutable-public-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-immutable-public-resource"
       :juxt.pass.alpha/scope "write:resource" ; <1>

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/.*"]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in
         [0] 'merge
         {::http/methods                 ; <2>
          {:get {::pass/actions #{"https://site.test/actions/get-public-resource"}}
           :head {::pass/actions #{"https://site.test/actions/get-public-resource"}}
           :options {::pass/actions #{"https://site.test/actions/get-options"}}}}]

        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource) ; <3>
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-immutable-public-resource![]
     ))))

(defn demo-grant-permission-to-invoke-action-put-immutable-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-immutable-public-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-immutable-public-resource"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/put-immutable-public-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-immutable-public-resource![]
     ))))

(defn demo-create-action-get-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-get-public-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/get-public-resource"
       :juxt.pass.alpha/scope "read:resource" ; <1>

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :xt/id "https://site.test/permissions/public-resources-to-all"] ; <2>
          ]]})
     ;; end::create-action-get-public-resource![]
     ))))

(defn demo-grant-permission-to-invoke-get-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-get-public-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/public-resources-to-all"
       :juxt.pass.alpha/action "https://site.test/actions/get-public-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-get-public-resource![]
     ))))

(defn demo-create-hello-world-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/hello"
       :juxt.http.alpha/content-type "text/plain"
       :juxt.http.alpha/content "Hello World!\r\n"})
     ;; end::create-hello-world-resource![]
     ))))

(defn demo-create-hello-world-html-representation! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-html-representation![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/hello.html" ; <1>
       :juxt.http.alpha/content-type "text/html;charset=utf-8" ; <2>
       :juxt.http.alpha/content "<h1>Hello World!</h1>\r\n" ; <3>
       :juxt.site.alpha/variant-of "https://site.test/hello" ; <4>
       })
     ;; end::create-hello-world-html-representation![]
     ))))


;; Templating

(defn demo-create-put-template-action! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-put-template-action![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-template"
       :juxt.pass.alpha/scope "write:resource"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/templates/.*"]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in
         [0] 'merge
         {::http/methods {}}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :juxt.pass.alpha/identity i]
          [subject :juxt.pass.alpha/identity i]]]})
     ;; end::create-put-template-action![]
     ))))

(defn demo-grant-permission-to-invoke-action-put-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-template![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-template"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-template"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-template![]
     ))))

(defn demo-create-hello-world-html-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-html-template![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-template"
      {:xt/id "https://site.test/templates/hello.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<h1>Hello {audience}!</h1>\r\n"})
     ;; end::create-hello-world-html-template![]
     ))))

(defn demo-create-hello-world-with-html-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-with-html-template![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/hello-with-template.html"
       :juxt.site.alpha/template "https://site.test/templates/hello.html"
       })
     ;; end::create-hello-world-with-html-template![]
     ))))


#_(defn demo-grant-permission-to-invoke-action-put-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-identity![]
     (grant-permission!
      {:xt/id "https://site.test/permissions/repl/put-identity"
       :juxt.pass.alpha/identity "urn:site:identities:repl"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-identity"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-identity![]
     ))))

#_(defn demo-create-action-get-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-get-identity![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/get-identity"
       :juxt.pass.alpha/scope "read:identity"

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :juxt.pass.alpha/identity i]
          [subject :juxt.pass.alpha/identity i]
          ]]})
     ;; end::create-action-get-identity![]
     ))))

#_(defn demo-create-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-identity![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-identity"
      {:xt/id "https://site.test/~mal"
       :code "mal"
       :juxt.pass.jwt/iss "https://juxt.eu.auth0.com/"
       :juxt.pass.jwt/sub "github|163131"
       :juxt.http.alpha/content "Malcolm Sparks"
       :juxt.http.alpha/content-type "text/plain"})
     ;; end::create-identity![]
     ))))

#_(defn demo-grant-permission-for-mal-to-get-mal! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-for-mal-to-get-mal![]
     (grant-permission!
      {:xt/id "https://site.test/permissions/mal/get-identity"
       :juxt.pass.alpha/identity "https://site.test/~mal"
       :juxt.pass.alpha/action #{"https://site.test/actions/get-identity"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-for-mal-to-get-mal![]
     )))
  )

;; We don't need to show the login page
#_(defn demo-put-login-page! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-login-page![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/login"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content (slurp "dev/login.html")}))
    ;; end::put-login-page![]
    )))

;; APIs

#_(defn demo-create-action-put-api-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-api-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-api-resource"
       :juxt.pass.alpha/scope "write:api"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/.*"]]
         [:juxt.http.alpha/methods
          [:map-of
           :keyword
           [:map
            [:juxt.pass.alpha/actions [:set [:string]]]]]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :juxt.pass.alpha/identity i]
          [subject :juxt.pass.alpha/identity i]]]})
     ;; end::create-action-put-api-resource![]
     ))))

#_(defn demo-grant-permission-to-invoke-action-put-api-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-api-resource![]
     (grant-permission!
      {:xt/id "https://site.test/permissions/repl/put-api-resource"
       :juxt.pass.alpha/identity "urn:site:identities:repl"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-api-resource"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-api-resource![]
     ))))

;; API example

#_(defn demo-create-action-list-identities! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-list-identities![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/list-identities"
       :juxt.pass.alpha/scope "read:identity"

       :juxt.pass.alpha/pull '[*]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [action :xt/id "https://site.test/actions/list-identities"]
          [resource :juxt.site.alpha/type "Identity"]
          [permission :juxt.pass.alpha/action action]]]})
     ;; end::create-action-list-identities![]
     ))))

#_(defn demo-grant-permission-to-list-identities! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-list-identities![]
     (grant-permission!
      {:xt/id "https://site.test/permissions/repl/list-identities"
       ;; Comment out just to make public
       ;;:juxt.pass.alpha/subject "urn:site:subjects:repl"
       :juxt.pass.alpha/action #{"https://site.test/actions/list-identities"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-list-identities![]
     ))))

;; Create an action to invoke a 'read' API

#_(defn demo-create-action-invoke-read-api! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-invoke-read-api![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/invoke-read-api"
       :juxt.pass.alpha/scope "read:resource"
       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [action :xt/id "https://site.test/actions/invoke-read-api"]
          [permission :juxt.pass.alpha/action action]]]})
     ;; end::create-action-invoke-read-api![]
     ))))

;; Grant everyone permission to invoke an API

#_(defn demo-grant-permission-to-invoke-read-api! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-read-api![]
     (grant-permission!
      {:xt/id "https://site.test/permissions/invoke-read-api"
       :juxt.pass.alpha/action #{"https://site.test/actions/invoke-read-api"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-read-api![]
     ))))

;; Connect up an API

#_(defn demo-create-list-users-api! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-list-users-api![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-api-resource"
      {:xt/id "https://site.test/users"
       :juxt.http.alpha/content-type "text/plain"
       :juxt.http.alpha/methods
       {:get
        {:juxt.pass.alpha/actions
         #{"https://site.test/actions/invoke-read-api"}
         #_:juxt.pass.alpha/actions
         #_#{"https://site.test/actions/list-identities"}}}})
     ;; end::create-list-users-api![]
     ))))

;; Why 401? where is this coming from? perhaps if we gave permission to everyone to list-identities?

;; Perhaps do private resources and authentication first

;; Private resources and authentication

(defn demo-create-action-put-immutable-private-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-immutable-private-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-immutable-private-resource"
       :juxt.pass.alpha/scope "write:resource"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/.*"]]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in
         [0] 'merge
         {::http/methods
          {:get {::pass/actions #{"https://site.test/actions/get-private-resource"}}
           :head {::pass/actions #{"https://site.test/actions/get-private-resource"}}
           :options {::pass/actions #{"https://site.test/actions/get-options"}}}}]

        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [subject :juxt.pass.alpha/identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-immutable-private-resource![]
     ))))

(defn demo-grant-permission-to-put-immutable-private-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-immutable-private-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-immutable-private-resource"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/put-immutable-private-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-immutable-private-resource![]
     ))))

(defn demo-create-action-get-private-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-get-private-resource[]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/get-private-resource"
       :juxt.pass.alpha/scope "read:resource"

       :juxt.pass.alpha/rules
       [
        ['(allowed? permission subject action resource)
         ['permission :juxt.pass.alpha/action "https://site.test/actions/get-private-resource"]
         ['subject :juxt.pass.alpha/identity]]]})
     ;; end::create-action-get-private-resource[]
     ))))

(defn demo-grant-permission-to-get-private-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-get-private-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/any-subject/get-private-resource"
       :juxt.pass.alpha/action "https://site.test/actions/get-private-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-get-private-resource![]
     ))))

(defn demo-create-immutable-private-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-immutable-private-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-private-resource"
      {:xt/id "https://site.test/private.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>This is a protected message that those authorized are allowed to read.</p>"})
     ;; end::create-immutable-private-resource![]
     ))))

(defn demo-create-action-put-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-error-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-error-resource"
       :juxt.pass.alpha/scope "write:resource"

       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/_site/errors/[a-z\\-]{3,}"]]
         [:juxt.site.alpha/type [:= "ErrorResource"]]
         [:ring.response/status :int]]]

       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :juxt.pass.alpha/identity i]
          [subject :juxt.pass.alpha/identity i]]]})
     ;; end::create-action-put-error-resource![]
     ))))

(defn demo-grant-permission-to-put-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-error-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-error-resource"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-error-resource"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-error-resource![]
     ))))

(defn demo-put-unauthorized-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-error-resource"
      {:xt/id "https://site.test/_site/errors/unauthorized"
       :juxt.site.alpha/type "ErrorResource"
       :ring.response/status 401})
     ;; end::put-unauthorized-error-resource![]
     ))))

#_{:xt/id "{{base-uri}}/_site/errors/unauthorized.html"
 :juxt.http.alpha/methods #{:get :head :options}
 :juxt.site.alpha/variant-of "{{base-uri}}/_site/errors/unauthorized"
 :juxt.site.alpha/type "TemplatedRepresentation"
 :juxt.site.alpha/template "{{base-uri}}/_site/templates/unauthorized.html"
 :juxt.site.alpha/template-model juxt.pass.alpha.authentication/unauthorized-template-model
 :juxt.pass.alpha/classification "PUBLIC"}

(defn demo-put-unauthorized-error-representation-for-html! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-representation-for-html![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/_site/errors/unauthorized.html"
       :juxt.site.alpha/variant-of "https://site.test/_site/errors/unauthorized"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<h1>Unauthorized</h1>\r\n"})
     ;; end::put-unauthorized-error-representation-for-html![]
     )))
  )

(defn demo-bootstrap-resources! []
  (demo-create-action-put-immutable-public-resource!)
  (demo-grant-permission-to-invoke-action-put-immutable-public-resource!)
  (demo-create-action-get-public-resource!)
  (demo-grant-permission-to-invoke-get-public-resource!)

  (demo-create-action-put-immutable-private-resource!)
  (demo-grant-permission-to-put-immutable-private-resource!)
  (demo-create-action-get-private-resource!)

  ;; This is 'just' an example showing how to create a /privte.html resource. We
  ;; don't want this resource part of every install!
  ;; (demo-create-immutable-private-resource!)

  (demo-create-action-put-error-resource!)
  (demo-grant-permission-to-put-error-resource!)
  (demo-put-unauthorized-error-resource!)
  (demo-put-unauthorized-error-representation-for-html!))

(defn demo-put-unauthorized-error-representation-for-html-with-login-link! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-representation-for-html-with-login-link![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-immutable-public-resource"
      {:xt/id "https://site.test/_site/errors/unauthorized.html"
       :juxt.site.alpha/variant-of "https://site.test/_site/errors/unauthorized"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content (slurp "dev/unauthorized.html")})
     ;; end::put-unauthorized-error-representation-for-html-with-login-link![]
     ))))

;; Applications

(defn demo-create-action-put-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/put-application"
       :juxt.pass.alpha/scope "write:application"
       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/applications/(.+)"]]
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/application"]]
         [:juxt.pass.alpha/oauth2-client-id [:string {:min 10}]]
         [:juxt.pass.alpha/oauth2-client-secret [:string {:min 16}]]]]
       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0]
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/application"}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]
        ]
       :juxt.pass.alpha/rules
       '[[(allowed? permission subject action resource)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/identity id]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-put-application![]
     ))))

(defn demo-grant-permission-to-invoke-action-put-application!! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/put-application"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/put-application"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-application![]
     ))))

(defn demo-invoke-put-application!! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::invoke-put-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-application"
      (make-application-doc
       :prefix "https://site.test/applications/"
       :client-id "local-terminal"
       :client-secret (as-hex-str (random-bytes 20))))
     ;; end::invoke-put-application![]
     ))))

(defn demo-create-action-authorize-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-authorize-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/authorize-application"
       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/authorizations/(.+)"]]
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/authorization"]]
         [:juxt.pass.alpha/user [:re "https://site.test/users/(.+)"]]
         [:juxt.pass.alpha/application [:re "https://site.test/applications/(.+)"]]
         ;; A space-delimited list of permissions that the application requires.
         [:juxt.pass.alpha/scope {:optional true} :string]]]
       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0]
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/authorization"}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]
        ]
       :juxt.pass.alpha/rules
       '[[(allowed? permission subject action resource)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/identity id]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-authorize-application![]
     ))))

(defn demo-grant-permission-to-invoke-action-authorize-application!! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-authorize-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/authorize-application"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/authorize-application"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-authorize-application![]
     ))))

(defn demo-invoke-authorize-application!! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::invoke-authorize-application![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/authorize-application"
      (make-application-authorization-doc
       :prefix "https://site.test/authorizations/"
       :user "https://site.test/users/mal"
       :application "https://site.test/applications/local-terminal"))
     ;; end::invoke-authorize-application![]
     ))))

(defn demo-create-action-issue-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-issue-access-token![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/issue-access-token"
       :juxt.pass.alpha.malli/args-schema
       [:tuple
        [:map
         [:xt/id [:re "https://site.test/access-tokens/(.+)"]]
         [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/access-token"]]
         [:juxt.pass.alpha/subject [:re "https://site.test/subjects/(.+)"]]
         [:juxt.pass.alpha/application [:re "https://site.test/applications/(.+)"]]
         [:juxt.pass.alpha/scope {:optional true} :string]]]
       :juxt.pass.alpha/process
       [
        [:juxt.pass.alpha.process/update-in [0]
         'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/access-token"}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]
        ]
       :juxt.pass.alpha/rules
       '[[(allowed? permission subject action resource)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/identity id]
          [permission :juxt.pass.alpha/user user]]]})
     ;; end::create-action-issue-access-token![]
     ))))

(defn demo-grant-permission-to-invoke-action-issue-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-issue-access-token![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/mal/authorize-application"
       :juxt.pass.alpha/user "https://site.test/users/mal"
       :juxt.pass.alpha/action "https://site.test/actions/issue-access-token"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-issue-access-token![]
     ))))

(defn demo-create-test-subject! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-test-subject![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-subject"
      {:xt/id "https://site.test/subjects/test"
       :juxt.pass.alpha/identity "https://site.test/identities/mal"}
      )
     ;; end::create-test-subject![]
     ))))

(defn demo-invoke-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::invoke-issue-access-token![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/issue-access-token"
      (make-access-token-doc
       :token "test-access-token"
       :prefix "https://site.test/access-tokens/"
       :subject "https://site.test/subjects/test"
       :application "https://site.test/applications/local-terminal"
       :scope "read:admin"
       :expires-in-seconds (* 5 60)))
     ;; end::invoke-issue-access-token![]
     ))))
