;; Copyright © 2022, JUXT LTD.

(ns juxt.book
  (:require
   [juxt.http.alpha :as-alias http]
   [crypto.password.bcrypt :as password]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [clojure.walk :refer [postwalk]]
   [clojure.string :as str]
   [malli.core :as m]
   [juxt.site.alpha.repl :refer [base-uri put! install-do-action-fn! do-action make-application-doc make-application-authorization-doc make-access-token-doc]]
   [juxt.site.alpha.util :refer [as-hex-str random-bytes]]))

(defn substitute-actual-base-uri [form]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://site.test" (base-uri)))
     )
   form))

(defn book-put-user! []
  ;; tag::install-user![]
  (put! {:xt/id "https://site.test/users/alice"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/user"
         :name "Alice" ; <1>
         :role #{"User" "Administrator"} ; <2>
         })
  ;; end::install-user![]
  )

(defn book-put-user-identity! []
  ;; tag::install-user-identity![]
  (put! {:xt/id "https://site.test/user-identities/alice"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/user-identity"
         :juxt.pass.alpha/user "https://site.test/users/alice"})
  ;; end::install-user-identity![]
  )

(defn book-put-subject! []
  ;; tag::install-subject![]
  (put! {:xt/id "https://site.test/subjects/repl-default"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
         :juxt.pass.alpha/user-identity "https://site.test/user-identities/alice"})
  ;; end::install-subject![]
  )

(defn book-install-create-action! []
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
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::install-create-action![]
  )

(defn book-install-do-action-fn! []
  ;; tag::install-do-action-fn![]
  (install-do-action-fn!)
  ;; end::install-do-action-fn![]
  )

(defn book-permit-create-action! []
  ;; tag::permit-create-action![]
  (put!
   {:xt/id "https://site.test/permissions/administrators/create-action" ; <1>
    :juxt.site.alpha/type "https://meta.juxt.site/pass/permission" ; <2>
    :juxt.pass.alpha/action "https://site.test/actions/create-action" ; <3>
    :juxt.pass.alpha/purpose nil ; <4>
    :role "Administrator" ; <5>
    })
  ;; end::permit-create-action![]
  )

(defn book-create-grant-permission-action! []
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
      [:juxt.pass.alpha/purpose [:maybe :string]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0]
      'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/permission"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-grant-permission-action![]
  )

(defn book-permit-grant-permission-action! []
  ;; tag::permit-grant-permission-action![]
  (put!
   {:xt/id "https://site.test/permissions/administrators/grant-permission"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/grant-permission"
    :juxt.pass.alpha/purpose nil})
  ;; end::permit-grant-permission-action![]
  )

;; Users Revisited

(defn book-create-action-put-user! []
  ;; tag::create-action-put-user![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-user"
    :juxt.pass.alpha/scope "write:users"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re "https://site.test/users/.*"]] ; <1>
      [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/user"]] ; <2>
      ]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0]
      'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/user" ; <3>
              :juxt.site.alpha/methods ; <4>
              {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
               :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
               :options {}}}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource) ; <5>
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-user![]
  )

(defn book-grant-permission-to-invoke-action-put-user! []
  ;; tag::grant-permission-to-invoke-action-put-user![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-user"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-user"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-user![]
  )

(defn book-create-action-put-user-identity! []
  ;; tag::create-action-put-user-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-user-identity"
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
      {:juxt.site.alpha/type "https://meta.juxt.site/pass/user-identity"
       :juxt.site.alpha/methods
       {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user-identity"}}
        :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user-identity"}}
        :options {}}}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-user-identity![]
  )

(defn book-grant-permission-to-invoke-action-put-user-identity! []
  ;; tag::grant-permission-to-invoke-action-put-user-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-user-identity"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-user-identity"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-user-identity![]
  )

(defn book-create-action-put-subject! []
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
      [:juxt.pass.alpha/user-identity [:re "https://site.test/user-identities/.+"]]
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
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-subject![]
  )

(defn book-grant-permission-to-invoke-action-put-subject! []
  ;; tag::grant-permission-to-invoke-action-put-subject![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-subject"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-subject"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-subject![]
  )

;; Hello World!

(defn book-create-action-put-immutable-public-resource! []
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
      {:juxt.site.alpha/methods ; <2>
       {:get {::pass/actions #{"https://site.test/actions/get-public-resource"}}
        :head {::pass/actions #{"https://site.test/actions/get-public-resource"}}
        :options {::pass/actions #{"https://site.test/actions/get-options"}}}}]

     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource) ; <3>
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-immutable-public-resource![]
  )

(defn book-grant-permission-to-invoke-action-put-immutable-public-resource! []
  ;; tag::grant-permission-to-invoke-action-put-immutable-public-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-immutable-public-resource"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-immutable-public-resource"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-immutable-public-resource![]
  )

(defn book-create-action-get-public-resource! []
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
  )

(defn book-grant-permission-to-invoke-get-public-resource! []
  ;; tag::grant-permission-to-invoke-get-public-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/public-resources-to-all"
    :juxt.pass.alpha/action "https://site.test/actions/get-public-resource"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-get-public-resource![]
  )

(defn book-create-hello-world-resource! []
  ;; tag::create-hello-world-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-immutable-public-resource"
   {:xt/id "https://site.test/hello"
    :juxt.http.alpha/content-type "text/plain"
    :juxt.http.alpha/content "Hello World!\r\n"})
  ;; end::create-hello-world-resource![]
  )


;; Representations

(defn book-create-hello-world-html-representation! []
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

(defn book-create-put-template-action! []
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
         {:juxt.site.alpha/methods {}}]
        [:juxt.pass.alpha.malli/validate]
        [:xtdb.api/put]]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :juxt.pass.alpha/user-identity i]
          [subject :juxt.pass.alpha/user-identity i]]]})
     ;; end::create-put-template-action![]
     ))))

(defn book-grant-permission-to-invoke-action-put-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-template![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/alice/put-template"
       :juxt.pass.alpha/user "https://site.test/users/alice"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-template"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-template![]
     ))))

(defn book-create-hello-world-html-template! []
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

(defn book-create-hello-world-with-html-template! []
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

;; Protecting Resources

(defn book-create-action-put-immutable-protected-resource! []
  ;; tag::create-action-put-immutable-protected-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-immutable-protected-resource"
    :juxt.pass.alpha/scope "write:resource"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re "https://site.test/.*"]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in
      [0] 'merge
      {:juxt.site.alpha/methods
       {:get {::pass/actions #{"https://site.test/actions/get-protected-resource"}}
        :head {::pass/actions #{"https://site.test/actions/get-protected-resource"}}
        :options {::pass/actions #{"https://site.test/actions/get-options"}}}}]

     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-immutable-protected-resource![]
  )

(defn book-grant-permission-to-put-immutable-protected-resource! []
  ;; tag::grant-permission-to-put-immutable-protected-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-immutable-protected-resource"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-immutable-protected-resource"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-put-immutable-protected-resource![]
  )

(defn book-create-action-get-protected-resource! []
  ;; tag::create-action-get-protected-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/scope "read:resource"

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]
       [permission :juxt.site.alpha/uri resource]]]})
  ;; end::create-action-get-protected-resource![]
  )

;; Protection Spaces

(defn book-create-action-put-protection-space! []
  ;; tag::create-action-put-protection-space![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-protection-space"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re "https://site.test/protection-spaces/(.+)"]]
      [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/protection-space"]]
      [:juxt.pass.alpha/canonical-root-uri [:re "https?://[^/]*"]]
      [:juxt.pass.alpha/realm {:optional true} [:string {:min 1}]]
      [:juxt.pass.alpha/auth-scheme [:enum "Basic" "Bearer"]]
      [:juxt.pass.alpha/authentication-scope [:string {:min 1}]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0]
      'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/protection-space"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-protection-space![]
  )

(defn book-grant-permission-to-put-protection-space! []
  ;; tag::grant-permission-to-put-protection-space![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-protection-space"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-protection-space"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-put-protection-space![]
  )

;; HTTP Basic Auth

(defn book-create-resource-protected-by-basic-auth! []
  ;; tag::create-resource-protected-by-basic-auth![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-immutable-protected-resource"
   {:xt/id "https://site.test/protected-by-basic-auth/document.html"
    :juxt.http.alpha/content-type "text/html;charset=utf-8"
    :juxt.http.alpha/content "<p>This is a protected message that those authorized are allowed to read.</p>"
    })
  ;; end::create-resource-protected-by-basic-auth![]
  )

(defn book-grant-permission-to-resource-protected-by-basic-auth! []
  ;; tag::grant-permission-to-resource-protected-by-basic-auth![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/alice/protected-by-basic-auth/document.html"
    :juxt.pass.alpha/action "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.site.alpha/uri "https://site.test/protected-by-basic-auth/document.html"
    :juxt.pass.alpha/purpose nil
    })
  ;; end::grant-permission-to-resource-protected-by-basic-auth![]
  )

(defn book-put-basic-protection-space! []
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-protection-space"
   {:xt/id "https://site.test/protection-spaces/basic/wonderland"

    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland" ; optional

    :juxt.pass.alpha/auth-scheme "Basic"
    :juxt.pass.alpha/authentication-scope "/protected-by-basic-auth/.*" ; regex pattern
    }))

;; HTTP Bearer Auth

(defn book-create-resource-protected-by-bearer-auth! []
  ;; tag::create-resource-protected-by-bearer-auth![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-immutable-protected-resource"
   {:xt/id "https://site.test/protected-by-bearer-auth/document.html"
    :juxt.http.alpha/content-type "text/html;charset=utf-8"
    :juxt.http.alpha/content "<p>This is a protected message that those authorized are allowed to read.</p>"
    })
  ;; end::create-resource-protected-by-bearer-auth![]
  )

(defn book-grant-permission-to-resource-protected-by-bearer-auth! []
  ;; tag::grant-permission-to-resource-protected-by-bearer-auth![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/alice/protected-by-bearer-auth/document.html"
    :juxt.pass.alpha/action "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.site.alpha/uri "https://site.test/protected-by-bearer-auth/document.html"
    :juxt.pass.alpha/purpose nil
    })
  ;; end::grant-permission-to-resource-protected-by-bearer-auth![]
  )

(defn book-put-bearer-protection-space! []
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-protection-space"
   {:xt/id "https://site.test/protection-spaces/bearer/wonderland"

    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland" ; optional

    :juxt.pass.alpha/auth-scheme "Bearer"
    :juxt.pass.alpha/authentication-scope "/protected-by-bearer-auth/.*" ; regex pattern
    }))

;; Cookie Scopes Preliminaries

(defn book-create-action-put-cookie-scope! []
  ;; tag::create-action-put-cookie-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-cookie-scope"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.pass.alpha.malli/args-schema
    [:tuple
     [:map
      [:xt/id [:re "https://site.test/cookie-scopes/(.+)"]]
      [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/cookie-scope"]]
      [:juxt.pass.alpha/cookie-domain [:re "https?://[^/]*"]]
      [:juxt.pass.alpha/cookie-path [:re "/.*"]]
      [:juxt.pass.alpha/login-uri [:re "https?://[^/]*"]]]]

    :juxt.pass.alpha/process
    [
     [:juxt.pass.alpha.process/update-in [0]
      'merge {:juxt.site.alpha/type "https://meta.juxt.site/pass/cookie-scope"}]
     [:juxt.pass.alpha.malli/validate]
     [:xtdb.api/put]]

    :juxt.pass.alpha/rules
    '[
      [(allowed? permission subject action resource)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-cookie-scope![]
  )

(defn book-grant-permission-to-put-cookie-scope! []
  ;; tag::grant-permission-to-put-cookie-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-cookie-scope"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-cookie-scope"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-put-cookie-scope![]
  )

;; Cookie Scope Example

(defn book-create-resource-protected-by-cookie! []
  ;; tag::create-resource-protected-by-cookie![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-immutable-protected-resource"
   {:xt/id "https://site.test/protected-by-cookie/document.html"
    :juxt.http.alpha/content-type "text/html;charset=utf-8"
    :juxt.http.alpha/content "<p>This is a protected message that is only visible when sending the correct cookie header.</p>"
    })
  ;; end::create-resource-protected-by-cookie![]
  )

(defn book-grant-permission-to-resource-protected-by-cookie! []
  ;; tag::grant-permission-to-resource-protected-by-cookie![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/alice/protected-html"
    :juxt.pass.alpha/action "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.site.alpha/uri "https://site.test/protected-by-cookie/document.html"
    :juxt.pass.alpha/purpose nil
    })
  ;; end::grant-permission-to-resource-protected-by-cookie![]
  )

(defn book-create-cookie-scope! []
  ;; tag::create-cookie-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-cookie-scope"
   {:xt/id "https://site.test/cookie-scopes/example"
    :juxt.pass.alpha/cookie-name "id"
    :juxt.pass.alpha/cookie-domain "https://site.test"
    :juxt.pass.alpha/cookie-path "/protected-by-cookie/"
    :juxt.pass.alpha/login-uri "https://site.test/login"})
    ;; end::create-cookie-scope![]
)

;; Applications

(defn book-create-action-put-application! []
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
      [:juxt.pass.alpha/oauth-client-id [:string {:min 10}]]
      [:juxt.pass.alpha/oauth-client-secret [:string {:min 16}]]]]
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
       [subject :juxt.pass.alpha/user-identity id]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-application![]
  )

(defn book-grant-permission-to-invoke-action-put-application!! []
  ;; tag::grant-permission-to-invoke-action-put-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-application"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-application"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-application![]
  )

(defn book-create-action-authorize-application! []
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
       [subject :juxt.pass.alpha/user-identity id]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-authorize-application![]
  )

(defn book-grant-permission-to-invoke-action-authorize-application! []
  ;; tag::grant-permission-to-invoke-action-authorize-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/users/authorize-application"
    :role "User"
    :juxt.pass.alpha/action "https://site.test/actions/authorize-application"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-authorize-application![]
  )

;;(defn book-create-resource)

(defn book-create-action-issue-access-token! []
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
       [subject :juxt.pass.alpha/user-identity id]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-issue-access-token![]
  )

(defn book-grant-permission-to-invoke-action-issue-access-token! []
  ;; tag::grant-permission-to-invoke-action-issue-access-token![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/users/issue-access-token"
    :role "User" ; <1>
    :juxt.pass.alpha/action "https://site.test/actions/issue-access-token"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-issue-access-token![]
  )

;; Authorization Server

(defn book-install-authorization-server! []
  ;; tag::install-authorization-server![]
  (put!
   {:xt/id "https://auth.site.test/oauth/authorize"
    :juxt.site.alpha/methods
    {:get
     {:juxt.site.alpha/handler 'juxt.pass.alpha.authorization-server/authorize
      :juxt.pass.alpha/actions #{"https://site.test/actions/authorize-application"}

      ;; Should we create a 'session space' which functions like a protection
      ;; space?  Like a protection space, it will extract the ::pass/subject
      ;; from the session and place into the request - see
      ;; juxt.pass.alpha.session/wrap-associate-session

      :juxt.pass.alpha/session-cookie "id"
      ;; This will be called with query parameter return-to set to ::site/uri
      ;; (effective URI) of request
      :juxt.pass.alpha/redirect-when-no-session-cookie "https://site.test/_site/openid/auth0/login"
      }}})
  ;; end::install-authorization-server![]
  )

;; TODO: Put Authorization Server in a protection space

;; First Application

(defn book-invoke-put-application! []
  ;; tag::invoke-put-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-application"
   (make-application-doc
    :prefix "https://site.test/applications/"
    :client-id "local-terminal"
    :client-secret (as-hex-str (random-bytes 20))))
  ;; end::invoke-put-application![]
  )

(defn book-invoke-authorize-application! []
  ;; tag::invoke-authorize-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/authorize-application"
   (make-application-authorization-doc
    :prefix "https://site.test/authorizations/"
    :user "https://site.test/users/alice"
    :application "https://site.test/applications/local-terminal"))
  ;; end::invoke-authorize-application![]
  )

(defn book-create-test-subject! []
  ;; tag::create-test-subject![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-subject"
   {:xt/id "https://site.test/subjects/test"
    :juxt.pass.alpha/user-identity "https://site.test/user-identities/alice"}
   )
  ;; end::create-test-subject![]
  )

(defn book-invoke-issue-access-token! []
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
  )

;; Other stuff

(defn book-create-action-put-error-resource! []
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
          [permission :juxt.pass.alpha/user-identity i]
          [subject :juxt.pass.alpha/user-identity i]]]})
     ;; end::create-action-put-error-resource![]
     ))))

(defn book-grant-permission-to-put-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-error-resource![]
     (do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/alice/put-error-resource"
       :juxt.pass.alpha/user "https://site.test/users/alice"
       :juxt.pass.alpha/action #{"https://site.test/actions/put-error-resource"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-error-resource![]
     ))))

(defn book-put-unauthorized-error-resource! []
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

(defn book-put-unauthorized-error-representation-for-html! []
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
     ))))

(defn book-put-unauthorized-error-representation-for-html-with-login-link! []
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

(defn install-not-found []
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-not-found"
    :juxt.pass.alpha/scope "read:resource"
    :juxt.pass.alpha/rules
    [
     ['(allowed? permission subject action resource)
      ['permission :xt/id]]]})

  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/get-not-found"
    :juxt.pass.alpha/action "https://site.test/actions/get-not-found"
    :juxt.pass.alpha/purpose nil})

  (put!
   {:xt/id "urn:site:resources:not-found"
    :juxt.site.alpha/methods
    {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-not-found"}}}}))

;; Complete all tasks thus far directed by the book
(defn preliminaries! []
  (book-put-user!)
  (book-put-user-identity!)
  (book-put-subject!)
  (book-install-create-action!)
  (book-install-do-action-fn!)
  (book-permit-create-action!)
  (book-create-grant-permission-action!)
  (book-permit-grant-permission-action!)
  (book-create-action-put-user!)
  (book-grant-permission-to-invoke-action-put-user!)
  (book-create-action-put-user-identity!)
  (book-grant-permission-to-invoke-action-put-user-identity!)
  (book-create-action-put-subject!)
  (book-grant-permission-to-invoke-action-put-subject!)
  ;; This tackles the '404' problem.
  (install-not-found))

(defn setup-hello-world! []
  (book-create-action-put-immutable-public-resource!)
  (book-grant-permission-to-invoke-action-put-immutable-public-resource!)
  (book-create-action-get-public-resource!)
  (book-grant-permission-to-invoke-get-public-resource!)
  (book-create-hello-world-resource!)
  )

(defn protected-resource-preliminaries! []
  (book-create-action-put-immutable-protected-resource!)
  (book-grant-permission-to-put-immutable-protected-resource!)
  (book-create-action-get-protected-resource!))

(defn protection-spaces-preliminaries! []
  (book-create-action-put-protection-space!)
  (book-grant-permission-to-put-protection-space!))

(defn cookies-scopes-preliminaries! []
  (book-create-action-put-cookie-scope!)
  (book-grant-permission-to-put-cookie-scope!))

(defn applications-preliminaries! []
  (book-create-action-put-application!)
  (book-grant-permission-to-invoke-action-put-application!!)
  (book-create-action-authorize-application!)
  (book-grant-permission-to-invoke-action-authorize-application!)
  (book-create-action-issue-access-token!)
  (book-grant-permission-to-invoke-action-issue-access-token!))

(defn setup-application! []
  (book-invoke-put-application!)
  (book-invoke-authorize-application!)
  (book-create-test-subject!)
  (book-invoke-issue-access-token!))

(defn init-all! []
  (preliminaries!)
  (setup-hello-world!)
  (protection-spaces-preliminaries!)
  (protected-resource-preliminaries!)
  (applications-preliminaries!)
  (setup-application!))

(defn book-put-basic-auth-user-identity! []
  ;; tag::put-basic-auth-user-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-user-identity"
   {:xt/id "https://site.test/user-identities/alice/basic"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    ;; Perhaps all user identities need this?
    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland"
    ;; Basic auth will only work if these are present
    :juxt.pass.alpha/username "alice"
    :juxt.pass.alpha/password-hash (password/encrypt "garden")

    })
  ;; end::put-basic-auth-user-identity![]
  )
