;; Copyright © 2022, JUXT LTD.

(ns juxt.book
  (:require
   [juxt.pass.alpha :as-alias pass]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.book :as book]
   ))

(comment
  ;; tag::example-action[]
  {:xt/id "https://example.org/example/feed-cat"
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action" ; <1>
   :juxt.pass.alpha/rules ; <2>
   [
    '[(allowed? subject resource permission) …]
    ]
   :juxt.flip.alpha/quotation '(…) ; <3>
   }
  ;; end::example-action[]
  )

;; User actions

(defn create-action-put-user! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user"
       :juxt.pass.alpha/scope "write:users"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/users/.*"]]])

               (site/set-type "https://meta.juxt.site/pass/user")

               (site/set-methods
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                 :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                 :options {}})

               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject
           ;; TODO: This only needs to be 'subject', not hardcoded.
           "https://example.org/subjects/system"]]

         [(allowed? subject resource permission) ; <5>
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-user! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-user![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-user"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-user"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-user![]
     ))))

(defn create-action-put-basic-user-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-basic-user-identity"
       :juxt.pass.alpha/scope "write:users"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [(xtdb.api/put
                site/request-body-as-edn
                (site/validate
                 [:map
                  [:xt/id [:re "https://example.org/.*"]]
                  [:juxt.pass.alpha/user [:re "https://example.org/users/.+"]]

                  ;; Required by basic-user-identity
                  [:juxt.pass.alpha/username [:re "[A-Za-z0-9]{2,}"]]
                  ;; NOTE: Can put in some password rules here
                  [:juxt.pass.alpha/password [:string {:min 6}]]
                  ;;[:juxt.pass.jwt/iss {:optional true} [:re "https://.+"]]
                  ;;[:juxt.pass.jwt/sub {:optional true} [:string {:min 1}]]
                  ])

                (site/set-type
                 #{"https://meta.juxt.site/pass/user-identity"
                   "https://meta.juxt.site/pass/basic-user-identity"})

                ;; Lowercase username
                (f/set-at
                 (f/keep
                  [(f/of :juxt.pass.alpha/username) f/>lower :juxt.pass.alpha/username]))

                ;; Hash password
                (f/set-at
                 (f/keep
                  [(f/of :juxt.pass.alpha/password) juxt.pass.alpha/encrypt-password :juxt.pass.alpha/password-hash]))
                (f/delete-at (f/dip [:juxt.pass.alpha/password]))

                (site/set-methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-basic-user-identity"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-basic-user-identity"}}
                  :options {}}))

               ]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-basic-user-identity! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-basic-user-identity"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-basic-user-identity"
       :juxt.pass.alpha/purpose nil})))))

(defn create-action-put-subject! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-subject![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-subject"
       ;;:juxt.pass.alpha/scope "write:users"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/subjects/.*"]]
                 [:juxt.pass.alpha/user-identity [:re "https://example.org/user-identities/.+"]]])
               (site/set-type "https://meta.juxt.site/pass/subject")
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})
       ;; end::create-action-put-subject![]
     ))))

(defn grant-permission-to-invoke-action-put-subject! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-subject"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-subject"
       :juxt.pass.alpha/purpose nil})))))

;; Create Alice

;; TODO: Consider reserving 'put' to indicate a direct database put. Everything
;; that goes via an action is really a 'create' or similar.
(defn put-user-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-user-alice![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-user"
      {:xt/id "https://example.org/users/alice"
       :name "Alice"
       :role "User"})
       ;; end::put-user-alice![]
     ))))

(defn install-user-identity-no-credentials-for-alice!
  "Put a minimal user-identity for Alice, which has no credentials. There is no
  action because this is only for education and testing from the SYSTEM."
  []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::install-user-identity-no-credentials-for-alice![]
     (juxt.site.alpha.init/put!
      {:xt/id "https://example.org/user-identities/alice"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/user-identity"
       :juxt.pass.alpha/user "https://example.org/users/alice"})
     ;; end::install-user-identity-no-credentials-for-alice![]
     ))))

;; Hello World!

(defn create-action-put-immutable-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-immutable-public-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-immutable-public-resource"
       :juxt.pass.alpha/scope "write:resource" ; <1>

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [juxt.site.alpha/request-body-as-edn

               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/(.+)"]]])

               (site/set-methods ; <2>
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-public-resource"}}
                 :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-public-resource"}}
                 :options {:juxt.pass.alpha/actions #{"https://example.org/actions/get-options"}}})

               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission) ; <3>
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})
     ;; end::create-action-put-immutable-public-resource![]
     ))))

(defn grant-permission-to-invoke-action-put-immutable-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-put-immutable-public-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-immutable-public-resource"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-immutable-public-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-immutable-public-resource![]
     ))))

(defn create-action-get-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-get-public-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-public-resource"
       :juxt.pass.alpha/scope "read:resource" ; <1>

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id "https://example.org/permissions/public-resources-to-all"] ; <2>
          ]]})
       ;; end::create-action-get-public-resource![]
     ))))

(defn grant-permission-to-invoke-get-public-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-get-public-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/public-resources-to-all"
       :juxt.pass.alpha/action "https://example.org/actions/get-public-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-get-public-resource![]
     ))))

(defn create-hello-world-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-public-resource"
      {:xt/id "https://example.org/hello"
       :juxt.http.alpha/content-type "text/plain"
       :juxt.http.alpha/content "Hello World!\r\n"})
       ;; end::create-hello-world-resource![]
     ))))

;; Representations

(defn create-hello-world-html-representation! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-html-representation![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-public-resource"
      {:xt/id "https://example.org/hello.html"                 ; <1>
       :juxt.http.alpha/content-type "text/html;charset=utf-8" ; <2>
       :juxt.http.alpha/content "<h1>Hello World!</h1>\r\n"    ; <3>
       :juxt.site.alpha/variant-of "https://example.org/hello" ; <4>
       })
     ;; end::create-hello-world-html-representation![]
     ))))


;; Templating

#_(defn create-put-template-action! []
    (eval
     (substitute-actual-base-uri
      (quote
       ;; tag::create-put-template-action![]
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/create-action"
        {:xt/id "https://example.org/actions/put-template"
         :juxt.pass.alpha/scope "write:resource"

         :juxt.pass.alpha.malli/args-schema
         [:tuple
          [:map
           [:xt/id [:re "https://example.org/templates/.*"]]]]

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
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/put-template"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action #{"https://example.org/actions/put-template"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-put-template![]
     ))))

(defn create-hello-world-html-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-html-template![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-template"
      {:xt/id "https://example.org/templates/hello.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<h1>Hello {audience}!</h1>\r\n"})
     ;; end::create-hello-world-html-template![]
     ))))

(defn create-hello-world-with-html-template! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-hello-world-with-html-template![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-public-resource"
      {:xt/id "https://example.org/hello-with-template.html"
       :juxt.site.alpha/template "https://example.org/templates/hello.html"
       })
     ;; end::create-hello-world-with-html-template![]
     ))))

;; Protecting Resources

(defn create-action-put-immutable-protected-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-immutable-protected-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-immutable-protected-resource"
       :juxt.pass.alpha/scope "write:resource" ; <1>

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [juxt.site.alpha/request-body-as-edn

               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/(.+)"]]])

               (site/set-methods ; <2>
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-protected-resource"}}
                 :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-protected-resource"}}
                 :options {:juxt.pass.alpha/actions #{"https://example.org/actions/get-options"}}})

               xtdb.api/put
               ]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission) ; <3>
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :role role]
          [user :role role]]]})
     ;; end::create-action-put-immutable-protected-resource![]
     ))))

(defn grant-permission-to-put-immutable-protected-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-immutable-protected-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-immutable-protected-resource"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-immutable-protected-resource"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-immutable-protected-resource![]
     ))))

(defn create-action-get-protected-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-get-protected-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-protected-resource"
       :juxt.pass.alpha/scope "read:resource"

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]    ; <1>
          [permission :juxt.site.alpha/uri resource] ; <2>
          ]]})
     ;; end::create-action-get-protected-resource![]
     ))))

;; Protection Spaces

(defn create-action-put-protection-space! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-protection-space![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-protection-space"
       :juxt.pass.alpha/scope "write:admin"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/protection-spaces/(.+)"]]
                 [:juxt.pass.alpha/canonical-root-uri [:re "https?://[^/]*"]]
                 [:juxt.pass.alpha/realm {:optional true} [:string {:min 1}]]
                 [:juxt.pass.alpha/auth-scheme [:enum "Basic" "Bearer"]]
                 [:juxt.pass.alpha/authentication-scope [:string {:min 1}]]])
               (site/set-type "https://meta.juxt.site/pass/protection-space")
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :role role]
          [user :role role]]]})
     ;; end::create-action-put-protection-space![]
     ))))

(defn grant-permission-to-put-protection-space! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-protection-space![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-protection-space"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-protection-space"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-protection-space![]
     ))))

;; HTTP Basic Auth

(defn create-resource-protected-by-basic-auth! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-resource-protected-by-basic-auth![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-protected-resource"
      {:xt/id "https://example.org/protected-by-basic-auth/document.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>This is a protected message that those authorized are allowed to read.</p>"
       })
     ;; end::create-resource-protected-by-basic-auth![]
     ))))

(defn grant-permission-to-resource-protected-by-basic-auth! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-resource-protected-by-basic-auth![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/protected-by-basic-auth/document.html"
       :juxt.pass.alpha/action "https://example.org/actions/get-protected-resource"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.site.alpha/uri "https://example.org/protected-by-basic-auth/document.html"
       :juxt.pass.alpha/purpose nil
       })
     ;; end::grant-permission-to-resource-protected-by-basic-auth![]
     ))))

(defn put-basic-protection-space! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-basic-protection-space![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-protection-space"
      {:xt/id "https://example.org/protection-spaces/basic/wonderland"

       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm "Wonderland" ; optional

       :juxt.pass.alpha/auth-scheme "Basic"
       :juxt.pass.alpha/authentication-scope "/protected-by-basic-auth/.*" ; regex pattern
       })
       ;; end::put-basic-protection-space![]
     ))))

(defn put-basic-user-identity-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-basic-user-identity-alice![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-basic-user-identity"
      {:xt/id "https://example.org/user-identities/alice/basic"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       ;; Perhaps all user identities need this?
       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm "Wonderland"
       ;; Basic auth will only work if these are present
       :juxt.pass.alpha/username "ALICE" ; this will be downcased
       ;; This will be encrypted
       :juxt.pass.alpha/password "garden"})
     ;; end::put-basic-user-identity-alice![]
     ))))

;; HTTP Bearer Auth

(defn create-resource-protected-by-bearer-auth! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-resource-protected-by-bearer-auth![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-protected-resource"
      {:xt/id "https://example.org/protected-by-bearer-auth/document.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>This is a protected message that those authorized are allowed to read.</p>"
       })
       ;; end::create-resource-protected-by-bearer-auth![]
     ))))

(defn grant-permission-to-resource-protected-by-bearer-auth! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-resource-protected-by-bearer-auth![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/protected-by-bearer-auth/document.html"
       :juxt.pass.alpha/action "https://example.org/actions/get-protected-resource"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.site.alpha/uri "https://example.org/protected-by-bearer-auth/document.html"
       :juxt.pass.alpha/purpose nil
       })
       ;; end::grant-permission-to-resource-protected-by-bearer-auth![]
     ))))

(defn put-bearer-protection-space! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-protection-space"
      {:xt/id "https://example.org/protection-spaces/bearer/wonderland"

       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm "Wonderland" ; optional

       :juxt.pass.alpha/auth-scheme "Bearer"
       :juxt.pass.alpha/authentication-scope "/protected-by-bearer-auth/.*" ; regex pattern
       })))))

;; Session Scopes Preliminaries

(defn create-action-put-session-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-put-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-session-scope"
       :juxt.pass.alpha/scope "write:admin"

       :juxt.flip.alpha/quotation
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
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :role role]
          [user :role role]]]})
     ;; end::create-action-put-session-scope![]
     ))))

(defn grant-permission-to-put-session-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/repl/put-session-scope"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-session-scope"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-session-scope![]
     ))))

;; Session Scope Example

(defn create-resource-protected-by-session-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-resource-protected-by-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-protected-resource"
      {:xt/id "https://example.org/protected-by-session-scope/document.html"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>This is a protected message that is only visible when sending the correct session header.</p>"
       })
     ;; end::create-resource-protected-by-session-scope![]
     ))))

(defn grant-permission-to-resource-protected-by-session-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-resource-protected-by-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/protected-by-session-scope/document.html"
       :juxt.pass.alpha/action "https://example.org/actions/get-protected-resource"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.site.alpha/uri "https://example.org/protected-by-session-scope/document.html"
       :juxt.pass.alpha/purpose nil
       })
     ;; end::grant-permission-to-resource-protected-by-session-scope![]
     ))))

;; TODO: Poorly named since it only creates a session scope around /protected-by-session-scope/
(defn create-session-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-session-scope![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-session-scope"
      {:xt/id "https://example.org/session-scopes/example"
       :juxt.pass.alpha/cookie-name "id"
       :juxt.pass.alpha/cookie-domain "https://example.org"
       :juxt.pass.alpha/cookie-path "/protected-by-session-scope/"
       :juxt.pass.alpha/login-uri "https://example.org/login"})
       ;; end::create-session-scope![]
     ))))

(defn create-action-create-login-resource!
  "A very specific action that creates a login form."
  ;; TODO: We could make the HTML content a parameter, but it helps security if
  ;; the http methods remain unconfigurable.
  []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/create-login-resource"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [(xtdb.api/put
                {:xt/id "https://example.org/login"
                 :juxt.site.alpha/methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-public-resource"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-public-resource"}}
                  :post {:juxt.pass.alpha/actions #{"https://example.org/actions/login"}}
                  :options {:juxt.pass.alpha/actions #{"https://example.org/actions/get-options"}}
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
\r\n"})]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn grant-permission-to-create-login-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-create-login-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/create-login-resource"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/create-login-resource"
       :juxt.pass.alpha/purpose nil
       })
     ;; end::grant-permission-to-create-login-resource![]
     ))))

(defn create-login-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-login-resource"
      {})))))

(defn create-action-login! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/login"

       :juxt.flip.alpha/quotation
       `(
         (f/define extract-credentials
           [(f/set-at
             (f/dip [:juxt.site.alpha/received-representation f/env
                     :juxt.http.alpha/body f/of
                     f/bytes-to-string
                     f/form-decode

                     ;; Validate we have what we're expecting
                     (site/validate
                      [:map
                       ["username" [:string {:min 1}]]
                       ["password" [:string {:min 1}]]])

                     :input]))])

         (f/define find-user-or-fail
           [ ;; Pull out the username, and downcase as per OWASP guidelines for
            ;; case-insensitive usernames.
            (f/keep [(f/of :input) (f/of "username") f/>lower])
            ;; Pull out the password
            (f/keep [(f/of :input) (f/of "password")])

            ;; Find a user-identity that matches, fail-fast
            (f/set-at
             (f/dip
              [(juxt.flip.alpha.xtdb/q
                ~'{:find [(pull uid [*]) (pull user [*])]
                   :keys [uid user]
                   :where [
                           [uid :juxt.pass.alpha/user user]
                           [uid :juxt.pass.alpha/username username]
                           [uid :juxt.pass.alpha/password-hash password-hash]
                           [(crypto.password.bcrypt/check password password-hash)]]
                   ;; stack order
                   :in [password username]})
               f/first
               (f/unless* [(f/throw (f/ex-info "Login failed" {}))])
               :matched-user]))])

         (f/define make-subject
           [
            (f/set-at
             (f/dip
              [f/<sorted-map>
               (f/set-at
                (f/dip
                 [(pass/as-hex-str (pass/random-bytes 10))
                  (f/str "https://example.org/subjects/")
                  :xt/id]))
               (f/set-at (f/dip ["https://meta.juxt.site/pass/subject" :juxt.site.alpha/type]))
               :subject]))])

         (f/define link-subject-to-user-identity
           [
            (f/set-at
             (f/keep [(f/of :matched-user) (f/of :uid) (f/of :xt/id)
                      :juxt.pass.alpha/user-identity])
             (f/keep [(f/of :subject)])
             (f/dip [f/set-at :subject]))])

         (f/define commit-subject
           [(site/push-fx (f/keep [(xtdb.api/put (f/of :subject))]))])

         (f/define put-subject
           [make-subject
            link-subject-to-user-identity
            commit-subject])

         (f/define make-session
           [(f/set-at
             (f/dip
              [f/<sorted-map>
               (f/set-at
                (f/dip
                 [(pass/make-nonce 16)
                  (f/str "https://example.org/sessions/")
                  :xt/id]))
               (f/set-at (f/dip ["https://meta.juxt.site/pass/session" :juxt.site.alpha/type]))
               :session]))])

         (f/define link-session-to-subject
           [
            (f/set-at
             (f/keep [(f/of :subject) (f/of :xt/id)
                      :juxt.pass.alpha/subject])
             (f/keep [(f/of :session)])
             (f/dip [f/set-at :session]))])

         (f/define commit-session
           [(site/push-fx (f/keep [(xtdb.api/put (f/of :session))]))])

         (f/define put-session
           [make-session
            link-session-to-subject
            commit-session])

         (f/define make-session-token
           [
            (f/set-at
             (f/dip
              [f/<sorted-map>
               (f/set-at
                (f/dip
                 [(pass/make-nonce 16)
                  :juxt.pass.alpha/session-token]))
               (f/set-at
                (f/keep
                 [(f/of :juxt.pass.alpha/session-token)
                  (f/str "https://example.org/session-tokens/")
                  :xt/id]))
               (f/set-at (f/dip ["https://meta.juxt.site/pass/session-token" :juxt.site.alpha/type]))
               :session-token]))])

         (f/define link-session-token-to-session
           [ ;; Link the session-token to the session
            (f/set-at
             (f/keep [(f/of :session) (f/of :xt/id)
                      :juxt.pass.alpha/session])
             (f/keep [(f/of :session-token)])
             (f/dip [f/set-at :session-token]))])

         (f/define commit-session-token
           [(site/push-fx (f/keep [(xtdb.api/put (f/of :session-token))]))])

         (f/define put-session-token
           [make-session-token
            link-session-token-to-session
            commit-session-token])

         (f/define make-set-cookie-header
           [(f/set-at
             (f/keep
              [(f/of :session-token) (f/of :juxt.pass.alpha/session-token) "id=" f/str
               "; Path=/; Secure; HttpOnly; SameSite=Lax" f/swap f/str
               :session-cookie]))])

         (f/define commit-set-cookie-header
           [(site/push-fx
             (f/keep
              [(f/of :session-cookie)
               (juxt.site.alpha/set-header "set-cookie" f/swap)]))])

         (f/define set-cookie-header
           [make-set-cookie-header
            commit-set-cookie-header])

         (site/with-fx-acc
           [
            extract-credentials
            find-user-or-fail
            put-subject
            put-session
            put-session-token
            set-cookie-header

            (f/env :ring.request/query)

            (f/when*
             [
              f/form-decode
              ;; Finally we pull out and use the return_to query parameter
              (f/of "return-to")
              (f/when* [
                        (juxt.site.alpha/set-header "location" f/swap)
                        f/swap site/push-fx
                        (juxt.site.alpha/set-status 302)
                        f/swap site/push-fx
                        ])])]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn grant-permission-to-invoke-action-login! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/login"
       :juxt.pass.alpha/action "https://example.org/actions/login"
       :juxt.pass.alpha/purpose nil})))))

;; Applications

(defn create-action-oauth-authorize! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/oauth/authorize"

       ;; Eventually we should look up if there's a resource-owner decision in
       ;; place to cover the application and scopes requested.  The decision
       ;; should include details of what scope was requested by the application,
       ;; and what scope was approved by the resource-owner (which may be the
       ;; same). If additional scope is requested in a subsequent authorization
       ;; request, then a new approval decision will then be sought from the
       ;; resource-owner.
       ;;
       ;; If we can't find a decision, we create a new pending decision document
       ;; containing the state, application and scope. We redirect to a trusted
       ;; resource, within the same protection space or session scope,
       ;; e.g. /approve. This is given the id of a pending approval as a request
       ;; parameter, from which it can look up the pending approval document and
       ;; render the form appropriately given the attributes therein.
       ;;
       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [
            ;; Extract query string from environment, decode it and store it at
            ;; keyword :query
            (f/define extract-and-decode-query-string
              [(f/set-at
                (f/dip
                 [:ring.request/query
                  f/env
                  (f/unless* [(f/throw (f/ex-info "No query string" {:note "We should respond with a 400 status"}))])
                  f/form-decode
                  :query]))])

            (f/define lookup-application-from-database
              [(f/set-at
                (f/keep
                 [
                  (f/of :query)
                  (f/of "client_id")
                  (juxt.flip.alpha.xtdb/q
                   ~'{:find [(pull e [*])]
                      :where [[e :juxt.site.alpha/type "https://meta.juxt.site/pass/application"]
                              [e :juxt.pass.alpha/client-id client-id]]
                      :in [client-id]})
                  f/first
                  f/first
                  :application]))])

            (f/define fail-if-no-application
              [(f/keep
                [
                 ;; Grab the client-id for error reporting
                 f/dup (f/of :query) (f/of "client_id") f/swap
                 (f/of :application)
                 ;; If no application entry, drop the client_id (to clean up the
                 ;; stack)
                 (f/if [f/drop]
                   ;; else throw the error
                   [:client-id {:ring.response/status 400} f/set-at
                    (f/throw (f/ex-info "No such app" f/swap))])])])

            ;; Get subject (it's in the environment, fail if missing subject)
            (f/define extract-subject
              [(f/set-at (f/dip [(f/env :juxt.pass.alpha/subject) :subject]))])

            (f/define assert-subject
              [(f/keep [(f/of :subject) (f/unless [(f/throw (f/ex-info "Cannot create access-token: no subject" {}))])])])

            ;; "The authorization server SHOULD document the size of any value it issues." -- RFC 6749 Section 4.2.2
            (f/define access-token-length [16])

            ;; Create access-token tied to subject, scope and application
            (f/define make-access-token
              [(f/set-at
                (f/keep
                 [f/dup (f/of :subject) (f/of :xt/id) :juxt.pass.alpha/subject {} f/set-at f/swap
                  (f/of :application) (f/of :xt/id) :juxt.pass.alpha/application f/rot f/set-at
                  ;; :juxt.pass.alpha/token
                  (f/set-at (f/dip [(pass/as-hex-str (pass/random-bytes access-token-length)) :juxt.pass.alpha/token]))
                  ;; :xt/id (as a function of :juxt.pass.alpha/token)
                  (f/set-at (f/keep [(f/of :juxt.pass.alpha/token) (f/env ::site/base-uri) "/access-tokens/" f/swap f/str f/str :xt/id]))
                  ;; ::site/type
                  (f/set-at (f/dip ["https://meta.juxt.site/pass/access-token" ::site/type]))
                  ;; TODO: Add scope
                  ;; key in map
                  :access-token]))])

            (f/define push-access-token-fx
              [(site/push-fx
                (f/keep
                 [(f/of :access-token) xtdb.api/put]))])

            (f/define collate-response
              [(f/set-at
                (f/keep
                 [ ;; access_token
                  f/dup (f/of :access-token) (f/of :juxt.pass.alpha/token) "access_token" {} f/set-at
                  ;; token_token
                  "bearer" "token_type" f/rot f/set-at
                  ;; state
                  f/swap (f/of :query) (f/of "state") "state" f/rot f/set-at
                  ;; key in map
                  :response]))])

            (f/define encode-fragment
              [(f/set-at
                (f/keep
                 [(f/of :response) f/form-encode :fragment]))])

            (f/define redirect-to-application-redirect-uri
              [(site/push-fx (f/dip [(site/set-status 302)]))
               (site/push-fx
                (f/keep
                 [f/dup (f/of :application) (f/of :juxt.pass.alpha/redirect-uri)
                  "#" f/swap f/str
                  f/swap (f/of :fragment)
                  (f/unless* [(f/throw (f/ex-info "Assert failed: No fragment found at :fragment" {}))])
                  f/swap f/str
                  (site/set-header "location" f/swap)]))])

            extract-and-decode-query-string
            lookup-application-from-database
            fail-if-no-application
            extract-subject
            assert-subject
            make-access-token
            push-access-token-fx
            collate-response
            encode-fragment
            redirect-to-application-redirect-uri
            ]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

;; Authorization Server

(defn create-action-install-authorization-server! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-authorization-server"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/set-methods
                {:get #:juxt.pass.alpha{:actions #{"https://example.org/actions/oauth/authorize"}}})
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]]})))))

(defn grant-permission-install-authorization-server! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-authorization-server"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-authorization-server"
       :juxt.pass.alpha/purpose nil})))))

(defn install-authorization-server! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-authorization-server"
      {:xt/id "https://example.org/oauth/authorize"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>Welcome to the Site authorization server.</p>"})))))

;; First Application

(defn register-example-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::register-example-application![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/register-application"
      {:juxt.pass.alpha/client-id "local-terminal"
       :juxt.pass.alpha/redirect-uri "https://example.org/terminal/callback"
       :juxt.pass.alpha/scope "user:admin"})
     ;; end::register-example-application![]
     ))))

;; GraphQL endpoint

;; Some who has the install-graphql-schema action can put a GraphQL schema
;; wherever the granted permission allows.

;; Option: POST to the action's URI?

(defn create-action-install-graphql-endpoint! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-graphql-endpoint"

       :juxt.site.alpha/methods
       {
        ;; As this is an 'installer' actions, we expose this action.
        :post {:juxt.pass.alpha/actions #{"https://example.org/actions/install-graphql-endpoint"}}
        ;; (the default action of an action resource is itself.)
        }

       :juxt.flip.alpha/quotation
       `(
         ;; We check that the permission resource matches the xt/id
         (f/env ::pass/permissions)
         ::pass/permissions
         {:type :debug-info}
         f/set-at
         (f/throw (f/ex-info "break" f/swap))
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/set-methods
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-graphql-schema"}}
                 :put {:juxt.pass.alpha/actions #{"https://example.org/actions/put-graphql-schema"}}
                 :post {:juxt.pass.alpha/action "https://example.org/actions/graphql-request"}})
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]
          ;;[permission :juxt.site.alpha/resource resource]
          ;;[(re-pattern resource-pattern) compiled-pattern]
          ;;[(re-matches compiled-pattern) resource]
          ]]})))))

(defn grant-permission-install-graphql-endpoint-to-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-graphql-endpoint"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action "https://example.org/actions/install-graphql-endpoint"
       :juxt.site.alpha/resource "https://example.org/graphql"
       :juxt.pass.alpha/purpose nil})))))

#_(defn install-graphql-endpoint! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-graphql-endpoint"
      {:xt/id "https://example.org/graphql"
       })))))

;; Other stuff

(defn grant-permission-to-put-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-put-error-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/put-error-resource"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action #{"https://example.org/actions/put-error-resource"}
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-put-error-resource![]
     ))))

(defn put-unauthorized-error-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-resource![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-error-resource"
      {:xt/id "https://example.org/_site/errors/unauthorized"
       :juxt.site.alpha/type "ErrorResource"
       :ring.response/status 401})
     ;; end::put-unauthorized-error-resource![]
     ))))

(defn put-unauthorized-error-representation-for-html! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-representation-for-html![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-public-resource"
      {:xt/id "https://example.org/_site/errors/unauthorized.html"
       :juxt.site.alpha/variant-of "https://example.org/_site/errors/unauthorized"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<h1>Unauthorized</h1>\r\n"})
     ;; end::put-unauthorized-error-representation-for-html![]
     ))))

(defn put-unauthorized-error-representation-for-html-with-login-link! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::put-unauthorized-error-representation-for-html-with-login-link![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-public-resource"
      {:xt/id "https://example.org/_site/errors/unauthorized.html"
       :juxt.site.alpha/variant-of "https://example.org/_site/errors/unauthorized"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content (slurp "dev/unauthorized.html")})
     ;; end::put-unauthorized-error-representation-for-html-with-login-link![]
     ))))

(defn users-preliminaries! []
  (create-action-put-user!)
  (grant-permission-to-invoke-action-put-user!)
  ;; NOTE: The form of user identities can depend on the auth scheme, so we
  ;; don't add the corresponding actions here.
  (create-action-put-subject!)
  (grant-permission-to-invoke-action-put-subject!))

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

(defn authorization-server-preliminaries! []
  (create-action-oauth-authorize!)
  (create-action-install-authorization-server!)
  (grant-permission-install-authorization-server!)
  (install-authorization-server!))

;; TODO: We could use a dependency graph here, to allow installation of a set of
;; documents where there are dependencies on other documents being created.

(defn init-all! []
  (init/bootstrap!)
  (setup-hello-world!)

  (protected-resource-preliminaries!)

  (protection-spaces-preliminaries!)

  (create-resource-protected-by-basic-auth!)
  (grant-permission-to-resource-protected-by-basic-auth!)
  (put-basic-protection-space!)

  (users-preliminaries!)
  (create-action-put-basic-user-identity!)
  (grant-permission-to-invoke-action-put-basic-user-identity!)
  (put-basic-user-identity-alice!)

  (session-scopes-preliminaries!)
  (create-resource-protected-by-session-scope!)
  (grant-permission-to-resource-protected-by-session-scope!)
  (create-session-scope!)

  (create-action-create-login-resource!)
  (grant-permission-to-create-login-resource!)
  (create-login-resource!)
  (create-action-login!)
  (grant-permission-to-invoke-action-login!)

  #_(applications-preliminaries!)
  #_(setup-application!)

  (create-resource-protected-by-bearer-auth!)
  (grant-permission-to-resource-protected-by-bearer-auth!)
  (put-bearer-protection-space!))
