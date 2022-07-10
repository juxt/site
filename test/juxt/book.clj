;; Copyright © 2022, JUXT LTD.

(ns juxt.book
  (:require
   [juxt.pass.alpha :as-alias pass]
   [juxt.flip.alpha.core :as f]
   [juxt.flip.clojure.core :as-alias fc]
   [juxt.site.alpha :as-alias site]
   [juxt.flip.alpha :as-alias flip]
   [juxt.site.alpha.repl :refer [encrypt-password]]
   [juxt.site.alpha.init :as init :refer [do-action put! substitute-actual-base-uri]]
   [juxt.site.alpha.util :refer [as-hex-str random-bytes]]
   [juxt.book :as book]))

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

               (fc/assoc ::site/type "https://meta.juxt.site/pass/user")

               (xtdb.api/put
                (fc/assoc
                 :juxt.site.alpha/methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user"}}
                  :options {}}))]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

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

                (fc/assoc ::site/type #{"https://meta.juxt.site/pass/user-identity"
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

                (fc/assoc
                 :juxt.site.alpha/methods
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
               (fc/assoc ::site/type "https://meta.juxt.site/pass/subject")
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

(defn put-subject-no-credentials-for-alice!
  "Put a subject document for Alice, pointing to the user identity with no credentials"
  []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-subject"
      {:xt/id "https://example.org/subjects/alice"
       :juxt.pass.alpha/user-identity "https://example.org/user-identities/alice"})))))

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

               (xtdb.api/put
                (fc/assoc
                 :juxt.site.alpha/methods
                 {:get {::pass/actions #{"https://example.org/actions/get-public-resource"}}
                  :head {::pass/actions #{"https://example.org/actions/get-public-resource"}}
                  :options {::pass/actions #{"https://example.org/actions/get-options"}}}))]))]))

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
       :juxt.pass.alpha/scope "write:resource"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [juxt.site.alpha/request-body-as-edn

               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/(.+)"]]])

               (xtdb.api/put
                (fc/assoc
                 :juxt.site.alpha/methods
                 {:get {::pass/actions #{"https://example.org/actions/get-protected-resource"}}
                  :head {::pass/actions #{"https://example.org/actions/get-protected-resource"}}
                  :options {::pass/actions #{"https://example.org/actions/get-options"}}}))

               ;; An action can be called as a transaction function, to allow actions to compose
               #_:xt/fn
               #_(quote (fn [xt-ctx ctx & args]
                          (juxt.pass.alpha.authorization/juxt.site.alpha.init/do-action* xt-ctx ctx args)))]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission) ; <2>
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
               (fc/assoc ::site/type "https://meta.juxt.site/pass/protection-space")
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
                 [::pass/cookie-domain [:re "https?://[^/]*"]]
                 [::pass/cookie-path [:re "/.*"]]
                 [::pass/login-uri [:re "https?://[^/]*"]]])
               (fc/assoc ::site/type "https://meta.juxt.site/pass/session-scope")
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
                           [uid ::pass/user user]
                           [uid ::pass/username username]
                           [uid ::pass/password-hash password-hash]
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
                      ::pass/user-identity])
             (f/keep [(f/of :subject)])
             (f/dip [f/set-at :subject]))])

         (f/define commit-subject
           [(site/push-fx (f/keep [(xtdb.api/put (f/of :subject))]))])

         (f/define put-subject
           [make-subject
            link-subject-to-user-identity
            commit-subject])

         (f/define make-session
           [
            (f/set-at
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
                      ::pass/subject])
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
                  ::pass/session-token]))
               (f/set-at
                (f/keep
                 [(f/of ::pass/session-token)
                  (f/str "https://example.org/session-tokens/")
                  :xt/id]))
               (f/set-at (f/dip ["https://meta.juxt.site/pass/session-token" :juxt.site.alpha/type]))
               :session-token]))])

         (f/define link-session-token-to-session
           [ ;; Link the session-token to the session
            (f/set-at
             (f/keep [(f/of :session) (f/of :xt/id)
                      ::pass/session])
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
              [(f/of :session-token) (f/of ::pass/session-token) "id=" f/str
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

(defn create-action-authorize-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-authorize-application![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/authorize-application"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:juxt.pass.alpha/user [:re "https://example.org/users/(.+)"]]
                 [:juxt.pass.alpha/application [:re "https://example.org/applications/(.+)"]]
                 ;; A space-delimited list of permissions that the application requires.
                 [:juxt.pass.alpha/scope {:optional true} :string]])

               (fc/assoc ::site/type "https://meta.juxt.site/pass/authorization")

               (f/set-at
                (f/dip
                 [(pass/as-hex-str (pass/random-bytes 20)) "/authorizations/" f/str (f/env ::site/base-uri) f/str
                  :xt/id]))

               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[[(allowed? subject resource permission)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/user-identity id]
          [user :role role]
          [permission :role role]]]})
     ;; end::create-action-authorize-application![]
     ))))

;; TODO: Rename function to indicate this permission is granted to those in the role 'user'
(defn grant-permission-to-invoke-action-authorize-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-authorize-application![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/users/authorize-application"
       :role "User"
       :juxt.pass.alpha/action "https://example.org/actions/authorize-application"
       :juxt.pass.alpha/purpose nil})
     ;; end::grant-permission-to-invoke-action-authorize-application![]
     ))))

(defn create-action-issue-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-action-issue-access-token![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/issue-access-token"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:juxt.pass.alpha/subject [:re "https://example.org/subjects/(.+)"]]
                 [:juxt.pass.alpha/application [:re "https://example.org/applications/(.+)"]]
                 [:juxt.pass.alpha/scope {:optional true} :string]])

               (fc/assoc ::site/type "https://meta.juxt.site/pass/access-token")

               (f/set-at
                (f/dip
                 [(pass/as-hex-str (pass/random-bytes 16))
                  :juxt.pass.alpha/token]))

               (f/set-at
                (f/keep
                 [(f/of :juxt.pass.alpha/token) "/access-tokens/" f/str (f/env ::site/base-uri) f/str
                  :xt/id]))

               ;; TODO: Add ::pass/expiry: (java.util.Date/from (.plusSeconds (.toInstant (java.util.Date.)) expires-in-seconds))

               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[[(allowed? subject resource permission)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/user-identity id]
          [permission :role role]
          [user :role role]]]})
     ;; end::create-action-issue-access-token![]
     ))))

(defn grant-permission-to-invoke-action-issue-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-invoke-action-issue-access-token![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/users/issue-access-token"
       :role "User"                        ; <1>
       :juxt.pass.alpha/action "https://example.org/actions/issue-access-token"
       :juxt.pass.alpha/purpose nil})
       ;; end::grant-permission-to-invoke-action-issue-access-token![]
     ))))

;; Authorization Server

#_(defn create-action-put-authorization-server []
    )

(defn install-authorization-server! []
  ;; tag::install-authorization-server![]
  (juxt.site.alpha.init/put!
   {:xt/id "https://auth.example.org/oauth/authorize"
    :juxt.site.alpha/methods
    {:get
     {:juxt.site.alpha/handler 'juxt.pass.alpha.authorization-server/authorize
      :juxt.pass.alpha/actions #{"https://example.org/actions/authorize-application"}

      ;; Should we create a 'session space' which functions like a protection
      ;; space?  Like a protection space, it will extract the ::pass/subject
      ;; from the session and place into the request - see
      ;; juxt.pass.alpha.session/wrap-associate-session

      :juxt.pass.alpha/session-cookie "id"
      ;; This will be called with query parameter return-to set to ::site/uri
      ;; (effective URI) of request
      :juxt.pass.alpha/redirect-when-no-session-session "https://example.org/_site/openid/auth0/login"
      }}})
  ;; end::install-authorization-server![]
  )

;; TODO: Put Authorization Server in a protection space

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

(defn invoke-authorize-application! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::invoke-authorize-application![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/alice"
      "https://example.org/actions/authorize-application"
      {:juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/application "https://example.org/applications/local-terminal"})
     ;; end::invoke-authorize-application![]
     ))))

(defn invoke-issue-access-token! []
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::invoke-issue-access-token![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/alice"
      "https://example.org/actions/issue-access-token"
      {:juxt.pass.alpha/subject "https://example.org/subjects/alice"
       :juxt.pass.alpha/application "https://example.org/applications/local-terminal"
       :juxt.pass.alpha/scope "read:admin"})
       ;; end::invoke-issue-access-token![]
     ))))

;; Other stuff

#_(defn create-action-put-error-resource! []
    (eval
     (substitute-actual-base-uri
      (quote
       ;; tag::create-action-put-error-resource![]
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/create-action"
        {:xt/id "https://example.org/actions/put-error-resource"
         :juxt.pass.alpha/scope "write:resource"

         :juxt.pass.alpha.malli/args-schema
         [:tuple
          [:map
           [:xt/id [:re "https://example.org/_site/errors/[a-z\\-]{3,}"]]
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

(defn applications-preliminaries! []
  (create-action-authorize-application!)
  (grant-permission-to-invoke-action-authorize-application!)
  (create-action-issue-access-token!)
  (grant-permission-to-invoke-action-issue-access-token!))

;; TODO: Too coarse, find usages and break up
(defn setup-application! []
  (register-example-application!)
  (users-preliminaries!)
  (put-user-alice!)
  (install-user-identity-no-credentials-for-alice!)
  (put-subject-no-credentials-for-alice!)
  (invoke-authorize-application!)
  (invoke-issue-access-token!))

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

  (applications-preliminaries!)
  (setup-application!)

  (create-resource-protected-by-bearer-auth!)
  (grant-permission-to-resource-protected-by-bearer-auth!)
  (put-bearer-protection-space!))
