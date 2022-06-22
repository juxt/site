;; Copyright © 2022, JUXT LTD.

(ns juxt.book
  (:require
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [clojure.walk :refer [postwalk]]
   [clojure.string :as str]
   [juxt.site.alpha.repl :refer [base-uri put! install-do-action-fn! do-action make-application-doc make-application-authorization-doc make-access-token-doc encrypt-password]]
   [juxt.site.alpha.util :refer [as-hex-str random-bytes]]))

(defn substitute-actual-base-uri [form]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://site.test" (base-uri)))
     )
   form))

(comment
  ;; tag::example-action[]
  {:xt/id "https://site.test/example/add-cat"
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action" ; <1>
   :juxt.pass.alpha/rules ; <2>
   [
    ['(allowed? subject resource permission) …]
    ]
   :juxt.flip.alpha/quotation '(…) ; <3>
   }
  ;; end::example-action[]
  )

(defn put-user! []
  ;; tag::install-user![]
  (put! {:xt/id "https://site.test/users/alice"
         :name "Alice" ; <1>
         :role #{"User" "Administrator"} ; <2>
         })
  ;; end::install-user![]
  )

(defn put-user-identity! []
  ;; tag::install-user-identity![]
  (put! {:xt/id "https://site.test/user-identities/alice"
         :juxt.pass.alpha/user "https://site.test/users/alice"})
  ;; end::install-user-identity![]
  )

(defn put-subject! []
  ;; tag::install-subject![]
  (put! {:xt/id "https://site.test/subjects/repl-default"
         :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
         :juxt.pass.alpha/user-identity "https://site.test/user-identities/alice"})
  ;; end::install-subject![]
  )

(defn install-create-action! []
  ;; tag::install-create-action![]
  (put!
   {:xt/id "https://site.test/actions/create-action" ; <1>
    :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/action" :juxt.site.alpha/type juxt.flip.alpha/assoc
      (validate [:map
                 [:xt/id [:re "https://site.test/actions/(.+)"]]
                 [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/action"]]
                 [:juxt.pass.alpha/rules [:vector [:vector :any]]]])
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission) ; <6>
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

(defn permit-create-action! []
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

(defn create-grant-permission-action! []
  ;; tag::create-grant-permission-action![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/grant-permission"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/permission" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/permissions/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/permission"]]
       [:juxt.pass.alpha/action [:re "https://site.test/actions/(.+)"]]
       [:juxt.pass.alpha/purpose [:maybe :string]]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-grant-permission-action![]
  )

(defn permit-grant-permission-action! []
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

(defn create-action-put-user! []
  ;; tag::create-action-put-user![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-user"
    :juxt.pass.alpha/scope "write:users"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call

      [:map
       [:xt/id [:re "https://site.test/users/.*"]]                     ; <1>
       ]
      validate

      {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
       :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-user"}}
       :options {}}
      swap
      :juxt.site.alpha/methods
      swap
      set-at

      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission) ; <5>
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-user![]
  )

(defn grant-permission-to-invoke-action-put-user! []
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

(defn create-action-put-basic-auth-identity! []
  ;; tag::create-action-put-basic-auth-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-basic-auth-identity"
    :juxt.pass.alpha/scope "write:users"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/basic-auth-identity" :juxt.site.alpha/type juxt.flip.alpha/assoc
      (validate
       [:map
        [:xt/id [:re "https://site.test/.*"]]
        [:juxt.pass.alpha/user [:re "https://site.test/users/.+"]]
        [:juxt.pass.alpha/username [:re "[A-Za-z0-9]{2,}"]]
        [:juxt.pass.alpha/password-hash [:string]]])

      ;; Lowercase the username, if it exists.
      dup :juxt.pass.alpha/username of (if* [>lower :juxt.pass.alpha/username juxt.flip.alpha/assoc] [])

      {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-basic-auth-identity"}}
       :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-basic-auth-identity"}}
       :options {}}
      :juxt.site.alpha/methods
      juxt.flip.alpha/assoc

      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-basic-auth-identity![]
  )

(defn grant-permission-to-invoke-action-put-basic-auth-identity! []
  ;; tag::grant-permission-to-invoke-action-put-basic-auth-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-basic-auth-identity"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-basic-auth-identity"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-action-put-basic-auth-identity![]
  )

(defn create-action-put-subject! []
  ;; tag::create-action-put-subject![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-subject"
    ;;:juxt.pass.alpha/scope "write:users"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/subject" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/.*"]]
       [:juxt.pass.alpha/user-identity [:re "https://site.test/user-identities/.+"]]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-subject![]
  )

(defn grant-permission-to-invoke-action-put-subject! []
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

(defn create-action-put-immutable-public-resource! []
  ;; tag::create-action-put-immutable-public-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-immutable-public-resource"
    :juxt.pass.alpha/scope "write:resource" ; <1>

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call

      [:map
       [:xt/id [:re "https://site.test/.*"]]]
      validate

      {:get {::pass/actions #{"https://site.test/actions/get-public-resource"}}
       :head {::pass/actions #{"https://site.test/actions/get-public-resource"}}
       :options {::pass/actions #{"https://site.test/actions/get-options"}}}
      :juxt.site.alpha/methods
      juxt.flip.alpha/assoc

      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission) ; <3>
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-immutable-public-resource![]
  )

(defn grant-permission-to-invoke-action-put-immutable-public-resource! []
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

(defn create-action-get-public-resource! []
  ;; tag::create-action-get-public-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-public-resource"
    :juxt.pass.alpha/scope "read:resource" ; <1>

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :xt/id "https://site.test/permissions/public-resources-to-all"] ; <2>
       ]]})
  ;; end::create-action-get-public-resource![]
  )

(defn grant-permission-to-invoke-get-public-resource! []
  ;; tag::grant-permission-to-invoke-get-public-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/public-resources-to-all"
    :juxt.pass.alpha/action "https://site.test/actions/get-public-resource"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-invoke-get-public-resource![]
  )

(defn create-hello-world-resource! []
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

(defn create-hello-world-html-representation! []
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

#_(defn create-put-template-action! []
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
           [(allowed? subject resource permission)
            [permission :juxt.pass.alpha/user-identity i]
            [subject :juxt.pass.alpha/user-identity i]]]})
       ;; end::create-put-template-action![]
       ))))

(defn grant-permission-to-invoke-action-put-template! []
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

(defn create-hello-world-html-template! []
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

(defn create-hello-world-with-html-template! []
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

(defn create-action-put-immutable-protected-resource! []
  ;; tag::create-action-put-immutable-protected-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-immutable-protected-resource"
    :juxt.pass.alpha/scope "write:resource"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call

      "https://meta.juxt.site/pass/action" swap :juxt.site.alpha/type swap set-at

      [:map
       [:xt/id [:re "https://site.test/.*"]]]
      validate

      {:get {::pass/actions #{"https://site.test/actions/get-protected-resource"}}
       :head {::pass/actions #{"https://site.test/actions/get-protected-resource"}}
       :options {::pass/actions #{"https://site.test/actions/get-options"}}}
      swap
      :juxt.site.alpha/methods
      swap
      set-at

      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission) ; <2>
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-immutable-protected-resource![]
  )

(defn grant-permission-to-put-immutable-protected-resource! []
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

(defn create-action-get-protected-resource! []
  ;; tag::create-action-get-protected-resource![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/scope "read:resource"

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user] ; <1>
       [permission :juxt.site.alpha/uri resource] ; <2>
       ]]})
  ;; end::create-action-get-protected-resource![]
  )

;; Protection Spaces

(defn create-action-put-protection-space! []
  ;; tag::create-action-put-protection-space![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-protection-space"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/protection-space" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/protection-spaces/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/protection-space"]]
       [:juxt.pass.alpha/canonical-root-uri [:re "https?://[^/]*"]]
       [:juxt.pass.alpha/realm {:optional true} [:string {:min 1}]]
       [:juxt.pass.alpha/auth-scheme [:enum "Basic" "Bearer"]]
       [:juxt.pass.alpha/authentication-scope [:string {:min 1}]]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-protection-space![]
  )

(defn grant-permission-to-put-protection-space! []
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

(defn create-resource-protected-by-basic-auth! []
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

(defn grant-permission-to-resource-protected-by-basic-auth! []
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

(defn put-basic-protection-space! []
  ;; tag::put-basic-protection-space![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-protection-space"
   {:xt/id "https://site.test/protection-spaces/basic/wonderland"

    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland" ; optional

    :juxt.pass.alpha/auth-scheme "Basic"
    :juxt.pass.alpha/authentication-scope "/protected-by-basic-auth/.*" ; regex pattern
    })
  ;; end::put-basic-protection-space![]
  )

(defn put-basic-auth-user-identity! []
  ;; tag::put-basic-auth-user-identity![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-basic-auth-identity"
   {:xt/id "https://site.test/user-identities/alice/basic-auth-identity"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/basic-auth-identity"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    ;; Perhaps all user identities need this?
    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland"
    ;; Basic auth will only work if these are present
    :juxt.pass.alpha/username "ALICE" ; this will be downcased
    :juxt.pass.alpha/password-hash (encrypt-password "garden")
    })
  ;; end::put-basic-auth-user-identity![]
  )

;; HTTP Bearer Auth

(defn create-resource-protected-by-bearer-auth! []
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

(defn grant-permission-to-resource-protected-by-bearer-auth! []
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

(defn put-bearer-protection-space! []
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-protection-space"
   {:xt/id "https://site.test/protection-spaces/bearer/wonderland"

    :juxt.pass.alpha/canonical-root-uri "https://site.test"
    :juxt.pass.alpha/realm "Wonderland" ; optional

    :juxt.pass.alpha/auth-scheme "Bearer"
    :juxt.pass.alpha/authentication-scope "/protected-by-bearer-auth/.*" ; regex pattern
    }))

;; Session Scopes Preliminaries

(defn create-action-put-session-scope! []
  ;; tag::create-action-put-session-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-session-scope"
    :juxt.pass.alpha/scope "write:admin"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call

      "https://meta.juxt.site/pass/session-scope" swap :juxt.site.alpha/type swap set-at

      [:map
       [:xt/id [:re "https://site.test/session-scopes/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/session-scope"]]
       [:juxt.pass.alpha/cookie-domain [:re "https?://[^/]*"]]
       [:juxt.pass.alpha/cookie-path [:re "/.*"]]
       [:juxt.pass.alpha/login-uri [:re "https?://[^/]*"]]]
      validate

      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-put-session-scope![]
  )

(defn grant-permission-to-put-session-scope! []
  ;; tag::grant-permission-to-put-session-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/administrators/put-session-scope"
    :role "Administrator"
    :juxt.pass.alpha/action "https://site.test/actions/put-session-scope"
    :juxt.pass.alpha/purpose nil})
  ;; end::grant-permission-to-put-session-scope![]
  )

;; Session Scope Example

(defn create-resource-protected-by-session-scope! []
  ;; tag::create-resource-protected-by-session-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-immutable-protected-resource"
   {:xt/id "https://site.test/protected-by-session-scope/document.html"
    :juxt.http.alpha/content-type "text/html;charset=utf-8"
    :juxt.http.alpha/content "<p>This is a protected message that is only visible when sending the correct session header.</p>"
    })
  ;; end::create-resource-protected-by-session-scope![]
  )

(defn grant-permission-to-resource-protected-by-session-scope! []
  ;; tag::grant-permission-to-resource-protected-by-session-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/alice/protected-by-session-scope/document.html"
    :juxt.pass.alpha/action "https://site.test/actions/get-protected-resource"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.site.alpha/uri "https://site.test/protected-by-session-scope/document.html"
    :juxt.pass.alpha/purpose nil
    })
  ;; end::grant-permission-to-resource-protected-by-session-scope![]
  )

(defn create-session-scope! []
  ;; tag::create-session-scope![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-session-scope"
   {:xt/id "https://site.test/session-scopes/example"
    :juxt.pass.alpha/cookie-name "id"
    :juxt.pass.alpha/cookie-domain "https://site.test"
    :juxt.pass.alpha/cookie-path "/protected-by-session-scope/"
    :juxt.pass.alpha/login-uri "https://site.test/login"})
  ;; end::create-session-scope![]
  )

;; TODO: Create an action for this - it's rather exotic, might need a privileged
;; action which is very lax about what it accepts.
(defn create-login-resource! []
  ;; tag::create-login-resource![]
  (put!
   {:xt/id "https://site.test/login"
    :juxt.site.alpha/methods
    {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-public-resource"}}
     :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-public-resource"}}
     :post {:juxt.pass.alpha/actions #{"https://site.test/actions/login"}}
     :options {:juxt.pass.alpha/actions #{"https://site.test/actions/get-options"}}
     }
    :juxt.http.alpha/content-type "text/html;charset=utf-8"
    :juxt.http.alpha/content
    "
<html>
<head>
<link rel='icon' href='data:,'>
</head>
<body>
<form method=POST>
<p>
Username: <input name=username type=text>
</p>
<p>
Password: <input name=password type=password>
</p>
<p>
<input type=submit value=Login>
</p>
</form>
</body>
</html>
\r\n"})
  ;; end::create-login-resource![]
  )

(defn create-action-login! []
  ;; tag::create-action-login![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/login"

    :juxt.flip.alpha/quotation
    '(
      ;; Definitions

      ;; assoc is intended to be used in a list, whereby the value is the top
      ;; of the stack. (assoc k v)
      (define assoc [swap rot set-at])
      ;; assoc* means put the value at the top of the stack into the map with
      ;; the given key. (assoc* k)
      ;; TODO: Look up the equivalent Factor convention.
      (define assoc* [rot set-at])

      ;; (m k -- m m)
      (define ref-as [over second (of :xt/id) swap juxt.flip.alpha.hashtables/associate])

      ;; The top of the stack is the user identity
      ;; Create the subject
      (define make-subject
        [(juxt.flip.alpha.hashtables/associate ::pass/user-identity)
         (assoc :juxt.site.alpha/type "https://meta.juxt.site/pass/subject")
         ;; The subject has a random id
         (as-hex-string (random-bytes 10))
         (str "https://site.test/subjects/")
         (assoc* :xt/id)
         (xtdb.api/put)])

      ;; Create the session, linked to the subject
      (define make-session-linked-to-subject
        [(ref-as ::pass/subject)
         (make-nonce 16)
         (str "https://site.test/sessions/")
         (assoc* :xt/id)
         (assoc ::site/type "https://meta.juxt.site/pass/session")
         (xtdb.api/put)])

      ;; Create the session token, linked to the session
      (define make-session-token-linked-to-session
        [(ref-as ::pass/session)
         (make-nonce 16)
         ;; This is more complicated because we want to use the nonce in the
         ;; xt/id
         swap over
         (assoc* ::pass/session-token)
         swap
         (str "https://site.test/session-tokens/")
         (assoc* :xt/id)
         (assoc ::site/type "https://meta.juxt.site/pass/session-token")
         (xtdb.api/put)])

      ;; Wrap quotation in a apply-to-request-context operation
      ;; (quotation -- op)
      (define apply-to-request-context
        [:juxt.site.alpha/apply-to-request-context
         swap _2vector])

      (define set-status
        [_1vector
         :ring.response/status
         swap push
         (symbol "rot")
         swap push
         (symbol "set-at")
         swap push
         apply-to-request-context])

      ;; Create an apply-to-request-context operation that sets a header
      ;; (header-name value -- op)
      (define set-header
        [(symbol "dup")
         _1vector

         (symbol "of")
         :ring.response/headers
         _2vector >list
         swap push

         (symbol "if*")
         _1vector
         0
         <vector>
         swap push

         (symbol "<array-map>")
         _1vector
         swap push
         >list
         swap push

         push                           ; the value on the stack
         push                           ; the header name
         (symbol "rot")
         swap push
         (symbol "set-at")
         swap push

         :ring.response/headers
         swap push
         (symbol "rot")
         swap push
         (symbol "set-at")
         swap push

         apply-to-request-context])

      ;; Start of program

      ;; Get form
      :juxt.site.alpha/received-representation env
      :juxt.http.alpha/body of
      bytes-to-string
      juxt.flip.alpha/form-decode

      ;; Validate we have what we're expecting
      (validate
       [:map
        ["username" [:string {:min 1}]]
        ["password" [:string {:min 1}]]])

      dup

      "username"
      of
      >lower           ; Make usernames case-insensitive as per OWASP guidelines

      swap
      "password"
      of
      swap

      ;; We now have a stack with: <user> <password>

      (juxt.flip.alpha.xtdb/q
       (find-matching-identity-on-password-query
        {:username-in-identity-key ::pass/username
         :password-hash-in-identity-key ::pass/password-hash}))

      first first

      (if*
          [make-subject
           make-session-linked-to-subject
           make-session-token-linked-to-session

           ;; Get the session token back and set it on a header
           dup second :juxt.pass.alpha/session-token of
           "id=" str
           "; Path=/; Secure; HttpOnly; SameSite=Lax" swap str
           (set-header "set-cookie" swap)

           ;; Finally we pull out and use the return_to query parameter
           :ring.request/query env
           (when* [juxt.flip.alpha/form-decode
                   "return-to" of
                   (when*
                       [
                        (set-header "location" swap)
                        ]
                     )

                   ;; A quotation that will set a status 302 on the request context
                   (set-status 302)])]

        ;; else
        [(throw (ex-info "Login failed" {:ring.response/status 400}))]))


    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :xt/id]]]})
  ;; end::create-action-login![]
  )

(defn grant-permission-to-invoke-action-login! []
  ;; tag::permit-action-login![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/login"
    :juxt.pass.alpha/action "https://site.test/actions/login"
    :juxt.pass.alpha/purpose nil})
  ;; end::permit-action-login![]
  )

;; Applications

(defn create-action-put-application! []
  ;; tag::create-action-put-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-application"
    :juxt.pass.alpha/scope "write:application"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/application" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/applications/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/application"]]
       [:juxt.pass.alpha/oauth-client-id [:string {:min 10}]]
       [:juxt.pass.alpha/oauth-client-secret [:string {:min 16}]]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[[(allowed? subject resource permission)
       [id :juxt.pass.alpha/user user]
       [subject :juxt.pass.alpha/user-identity id]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-put-application![]
  )

(defn grant-permission-to-invoke-action-put-application! []
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

(defn create-action-authorize-application! []
  ;; tag::create-action-authorize-application![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/authorize-application"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/authorization" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/authorizations/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/authorization"]]
       [:juxt.pass.alpha/user [:re "https://site.test/users/(.+)"]]
       [:juxt.pass.alpha/application [:re "https://site.test/applications/(.+)"]]
       ;; A space-delimited list of permissions that the application requires.
       [:juxt.pass.alpha/scope {:optional true} :string]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[[(allowed? subject resource permission)
       [id :juxt.pass.alpha/user user]
       [subject :juxt.pass.alpha/user-identity id]
       [user :role role]
       [permission :role role]]]})
  ;; end::create-action-authorize-application![]
  )

(defn grant-permission-to-invoke-action-authorize-application! []
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

(defn create-action-issue-access-token! []
  ;; tag::create-action-issue-access-token![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/issue-access-token"

    :juxt.flip.alpha/quotation
    '(
      "https://site.test/flip/quotations/req-to-edn-body" juxt.flip.alpha.xtdb/entity :juxt.flip.alpha/quotation of call
      "https://meta.juxt.site/pass/access-token" swap :juxt.site.alpha/type swap set-at
      [:map
       [:xt/id [:re "https://site.test/access-tokens/(.+)"]]
       [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/access-token"]]
       [:juxt.pass.alpha/subject [:re "https://site.test/subjects/(.+)"]]
       [:juxt.pass.alpha/application [:re "https://site.test/applications/(.+)"]]
       [:juxt.pass.alpha/scope {:optional true} :string]]
      validate
      xtdb.api/put)

    :juxt.pass.alpha/rules
    '[[(allowed? subject resource permission)
       [id :juxt.pass.alpha/user user]
       [subject :juxt.pass.alpha/user-identity id]
       [permission :role role]
       [user :role role]]]})
  ;; end::create-action-issue-access-token![]
  )

(defn grant-permission-to-invoke-action-issue-access-token! []
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

(defn install-authorization-server! []
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
      :juxt.pass.alpha/redirect-when-no-session-session "https://site.test/_site/openid/auth0/login"
      }}})
  ;; end::install-authorization-server![]
  )

;; TODO: Put Authorization Server in a protection space

;; First Application

(defn invoke-put-application! []
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

(defn invoke-authorize-application! []
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

;; Deprecated: This overlaps with an existing subject
(defn create-test-subject! []
  ;; tag::create-test-subject![]
  (do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/put-subject"
   {:xt/id "https://site.test/subjects/test"
    :juxt.pass.alpha/user-identity "https://site.test/user-identities/alice"}
   )
  ;; end::create-test-subject![]
  )

(defn invoke-issue-access-token! []
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

#_(defn create-action-put-error-resource! []
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
           [(allowed? subject resource permission)
            [permission :juxt.pass.alpha/user-identity i]
            [subject :juxt.pass.alpha/user-identity i]]]})
       ;; end::create-action-put-error-resource![]
       ))))

(defn grant-permission-to-put-error-resource! []
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

(defn put-unauthorized-error-resource! []
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

(defn put-unauthorized-error-representation-for-html! []
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

(defn put-unauthorized-error-representation-for-html-with-login-link! []
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
     ['(allowed? subject resource permission)
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
    {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-not-found"}}
     :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-not-found"}}}}))

;; Complete all tasks thus far directed by the book
(defn preliminaries! []
  ;; Puts
  (put-user!)
  (put-user-identity!)
  (put-subject!)
  (install-create-action!)
  (book-install-do-action-fn!)
  (permit-create-action!)
  ;; Quotations
  (put! {:xt/id "https://site.test/flip/quotations/req-to-edn-body"
         :juxt.flip.alpha/quotation
         '(:juxt.site.alpha/received-representation
           env
           ::http/body
           of
           bytes-to-string
           juxt.flip.alpha.edn/read-string)})

  ;; Actions
  (create-grant-permission-action!)
  (permit-grant-permission-action!)
  (create-action-put-user!)
  (grant-permission-to-invoke-action-put-user!)

  (create-action-put-subject!)
  (grant-permission-to-invoke-action-put-subject!)
  ;; This tackles the '404' problem.
  (install-not-found)

  ;; TODO: These might not be preliminaries
  (create-action-put-basic-auth-identity!)
  (grant-permission-to-invoke-action-put-basic-auth-identity!))

(defn setup-hello-world! []
  (create-action-put-immutable-public-resource!)
  (grant-permission-to-invoke-action-put-immutable-public-resource!)
  (create-action-get-public-resource!)
  (grant-permission-to-invoke-get-public-resource!)
  (create-hello-world-resource!))

(defn protected-resource-preliminaries! []
  (create-action-put-immutable-protected-resource!)
  (grant-permission-to-put-immutable-protected-resource!)
  (create-action-get-protected-resource!))

(defn protection-spaces-preliminaries! []
  (create-action-put-protection-space!)
  (grant-permission-to-put-protection-space!))

(defn session-scopes-preliminaries! []
  (create-action-put-session-scope!)
  (grant-permission-to-put-session-scope!))

(defn applications-preliminaries! []
  (create-action-put-application!)
  (grant-permission-to-invoke-action-put-application!)
  (create-action-authorize-application!)
  (grant-permission-to-invoke-action-authorize-application!)
  (create-action-issue-access-token!)
  (grant-permission-to-invoke-action-issue-access-token!))

(defn setup-application! []
  (invoke-put-application!)
  (invoke-authorize-application!)
  (create-test-subject!)
  (invoke-issue-access-token!))

(defn init-all! []
  (preliminaries!)
  (setup-hello-world!)

  (protected-resource-preliminaries!)

  (protection-spaces-preliminaries!)

  (create-resource-protected-by-basic-auth!)
  (grant-permission-to-resource-protected-by-basic-auth!)
  (put-basic-protection-space!)
  (put-basic-auth-user-identity!)

  (session-scopes-preliminaries!)

  (create-resource-protected-by-session-scope!)
  (grant-permission-to-resource-protected-by-session-scope!)
  (create-session-scope!)
  (create-login-resource!)
  (create-action-login!)
  (grant-permission-to-invoke-action-login!)

  (applications-preliminaries!)
  (setup-application!)

  (create-resource-protected-by-bearer-auth!)
  (grant-permission-to-resource-protected-by-bearer-auth!)
  (put-bearer-protection-space!))
