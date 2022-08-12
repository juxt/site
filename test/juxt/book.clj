;; Copyright © 2022, JUXT LTD.

(ns juxt.book
  (:require
   [clojure.java.io :as io]
   [ring.util.codec :as codec]
   [juxt.pass.alpha :as-alias pass]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha.graphql.flip :as graphql-flip]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.test.util :refer [*handler*]]
   [malli.core :as malli]
   [clojure.string :as str]))

(comment
  ;; tag::example-action[]
  {:xt/id "https://example.org/example/feed-cat"
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action" ; <1>
   :juxt.pass.alpha/rules                                     ; <2>
   [
    '[(allowed? subject resource permission) …]
    ]
   :juxt.flip.alpha/quotation '(…)      ; <3>
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

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc-with-checks
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
          [permission :juxt.pass.alpha/subject subject]]

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

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc-with-checks
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

(defn create-user! [& {:keys [username name role]}]
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-user"
      {:xt/id ~(format "https://example.org/users/%s" username)
       :name ~name
       :role ~(or role "User")}))))

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

(defn create-basic-user-identity! [& {:juxt.pass.alpha/keys [username password realm]}]
  (assert username)
  (assert password)
  (assert realm)
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-basic-user-identity"
      {:xt/id ~(format "https://example.org/user-identities/%s/basic" (str/lower-case username))
       :juxt.pass.alpha/user ~(format "https://example.org/users/%s" (str/lower-case username))
       ;; Perhaps all user identities need this?
       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm ~realm
       ;; Basic auth will only work if these are present
       :juxt.pass.alpha/username ~username
       ;; This will be encrypted
       :juxt.pass.alpha/password ~password}))))

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
(defn
  create-session-scope! []
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

(defn create-internal-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-immutable-protected-resource"
      {:xt/id "https://example.org/private/internal.html"
       :juxt.http.alpha/content-type "text/plain"
       :juxt.http.alpha/content "Internal message"})))))

(defn grant-alice-permission-to-internal-resource! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/alice/private/internal.html"
       :juxt.pass.alpha/action "https://example.org/actions/get-protected-resource"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.site.alpha/uri "https://example.org/private/internal.html"
       :juxt.pass.alpha/purpose nil})))))

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
               (f/unless* [(f/throw-exception (f/ex-info "Login failed" {}))])
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
                 [(f/env :ring.request/query)
                  (f/unless* [(f/throw-exception (f/ex-info "No query string" {:note "We should respond with a 400 status"}))])
                  f/form-decode
                  :query]))])

            (f/define check-response-type
              [(f/keep
                [(f/of :query) (f/of "response_type")
                 (f/unless* [(f/throw
                              {"error" "invalid_request"
                               "error_description" "A response_type parameter is required"})])
                 f/dup f/sequential?
                 (f/when [(f/throw
                           {"error" "invalid_request"
                            "error_description" "The response_type parameter is provided more than once"})])
                 (f/in? #{"code" "token"})
                 (f/unless [(f/throw
                             {"error" "unsupported_response_type"
                              "error_description" "Only a response type of 'token' is currently supported"})])])])

            (f/define lookup-application-from-database
              [ ;; Get client_id
               f/dup (f/of :query) (f/of "client_id")

               (f/unless* [(f/throw-exception (f/ex-info "A client_id parameter is required" {:ring.response/status 400}))])

               ;; Query it
               (juxt.flip.alpha.xtdb/q
                ~'{:find [(pull e [*])]
                   :where [[e :juxt.site.alpha/type "https://meta.juxt.site/pass/application"]
                           [e :juxt.pass.alpha/client-id client-id]]
                   :in [client-id]})
               f/first
               f/first

               (f/if* [:application f/rot f/set-at]
                      [(f/throw-exception (f/ex-info "No such client" {:ring.response/status 400}))])])

            ;; Get subject (it's in the environment, fail if missing subject)
            (f/define extract-subject
              [(f/set-at (f/dip [(f/env :juxt.pass.alpha/subject) :subject]))])

            (f/define assert-subject
              [(f/keep [(f/of :subject) (f/unless [(f/throw-exception (f/ex-info "Cannot create access-token: no subject" {}))])])])

            (f/define extract-and-decode-scope
              [f/dup
               (f/of :query) (f/of "scope")
               (f/if* [f/form-decode "\\s" f/<regex> f/split] [nil])
               (f/when* [:scope f/rot f/set-at])])

            (f/define validate-scope
              [(f/keep
                [(f/of :scope)
                 (f/when*
                  [(f/all? ["https://meta.juxt.site/pass/oauth-scope" :xt/id f/rot
                            juxt.site.alpha/lookup
                            juxt.flip.alpha.xtdb/q
                            f/first])
                   (f/if* [f/drop] [(f/throw {"error" "invalid_scope"})])])])])

            ;; "The authorization server SHOULD document the size of any value it issues." -- RFC 6749 Section 4.2.2
            (f/define access-token-length [16])

            ;; Create access-token tied to subject, scope and application
            (f/define make-access-token
              [(f/set-at
                (f/keep
                 [f/dup (f/of :subject) :juxt.pass.alpha/subject {} f/set-at f/swap
                  f/dup (f/of :application) (f/of :xt/id) :juxt.pass.alpha/application f/rot f/set-at
                  (f/of :scope) :juxt.pass.alpha/scope f/rot f/set-at
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

            (f/define save-error
              [:error f/rot f/set-at])

            (f/define collate-error-response
              [(f/set-at
                (f/keep
                 [ ;; error
                  f/dup (f/of :error)
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
                  ;; TODO: Add any error in the query string
                  "#" f/swap f/str
                  f/swap (f/of :fragment)
                  (f/unless* [(f/throw-exception (f/ex-info "Assert failed: No fragment found at :fragment" {}))])
                  f/swap f/str
                  (site/set-header "location" f/swap)]))])

            extract-and-decode-query-string
            lookup-application-from-database

            (f/recover
             [check-response-type
              extract-subject
              assert-subject
              extract-and-decode-scope
              validate-scope
              make-access-token
              push-access-token-fx
              collate-response]

             [save-error
              collate-error-response])

            encode-fragment
            redirect-to-application-redirect-uri]))

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
       :juxt.pass.alpha/redirect-uri "https://example.org/terminal/callback"})
     ;; end::register-example-application![]
     ))))

;; GraphQL

;; Someone who has permission to perform the install-graphql-endpoint action can
;; put a GraphQL schema wherever the granted permission allows.
(defn create-action-install-graphql-endpoint! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-graphql-endpoint"

       :juxt.pass.alpha/scope "https://example.org/oauth/scope/graphql/administer"

       :juxt.site.alpha/methods
       {
        ;; As this is an 'installer' actions, we expose this action.
        :post {:juxt.pass.alpha/actions #{"https://example.org/actions/install-graphql-endpoint"}}
        ;; (the default action of an action resource is itself.)
        }

       :juxt.flip.alpha/quotation
       `(
         (f/define extract-input
           [(f/set-at
             (f/dip
              [site/request-body-as-edn
               (site/validate [:map {:closed true}
                               [:xt/id [:re "https://example.org/.*"]]])
               :input]))])

         (f/define extract-permissions
           [(f/set-at (f/dip [(f/env ::pass/permissions) :permissions]))])

         (f/define determine-if-match-resource-pattern
           ;; We check that the permission resource matches the xt/id
           [(f/set-at
             (f/keep
              [f/dup (f/of :input) (f/of :xt/id) f/swap (f/of :permissions)
               (f/any? [(f/of ::site/resource-pattern) f/<regex> f/matches?])
               f/nip :matches?]))])

         (f/define throw-if-not-match
           [(f/keep
             [f/dup
              (f/of :matches?)
              (f/if
                  [f/drop]
                  [(f/throw-exception
                    (f/ex-info
                     f/dup "No permission allows installation of GraphQL endpoint: " f/swap (f/of :input) (f/of :xt/id) f/swap f/str
                     f/swap (f/of :input) (f/of :xt/id) :location {:ring.response/status 403} f/set-at))])])])

         (f/define create-resource
           [(f/set-at
             (f/keep
              [(f/of :input)
               (site/set-methods
                {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-graphql-schema"}}
                 :put {:juxt.pass.alpha/actions #{"https://example.org/actions/put-graphql-schema"}
                       :juxt.site.alpha/acceptable {"accept" "application/graphql"}}
                 :post {:juxt.pass.alpha/action "https://example.org/actions/graphql-request"}})
               (f/set-at (f/dip ["https://meta.juxt.site/site/graphql-endpoint" :juxt.site.alpha/type]))
               (f/set-at (f/dip ["GraphQL endpoint" :juxt.site.alpha/description]))
               :new-resource]))])

         (f/define extract-owner
           [(f/set-at
             (f/dip
              [(f/env :juxt.pass.alpha/subject) site/entity (f/of :juxt.pass.alpha/user-identity) site/entity
               (f/of :juxt.pass.alpha/user)
               :owner]))])

         (f/define add-owner
           [(f/set-at
             (f/keep
              [f/dup (f/of :new-resource)
               f/swap (f/of :owner) :juxt.pass.alpha/owner f/rot f/set-at
               :new-resource]))])

         (f/define push-resource
           [(site/push-fx
             (f/keep
              [(f/of :new-resource)
               xtdb.api/put]))])

         (f/define push-permission
           [(site/push-fx
             (f/keep
              [(f/of :new-permission)
               xtdb.api/put]))])

         (f/define configure-response
           [(site/push-fx (f/dip [(site/set-status 201)]))
            (site/push-fx (f/keep [(site/set-header "location" f/swap (f/of :input) (f/of :xt/id))]))])

         (f/define create-permission
           [(f/set-at
             (f/dip
              [{:juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
                :juxt.site.alpha/description "Permission for endpoint owner to put GraphQL schema"
                :juxt.pass.alpha/action "https://example.org/actions/put-graphql-schema"
                :juxt.pass.alpha/purpose nil}
               :new-permission]))])

         (site/with-fx-acc
           [extract-input

            extract-permissions
            determine-if-match-resource-pattern
            throw-if-not-match

            create-resource
            extract-owner
            add-owner
            push-resource

            create-permission

            ;; Set new-permission to owner
            ;; TODO: Need some kind of 'update' combinator for this common operation
            (f/set-at
             (f/keep
              [f/dup (f/of :new-permission) f/swap
               (f/of :owner) :juxt.pass.alpha/user f/rot f/set-at :new-permission]))

            ;; Set xt/id
            (f/set-at
             (f/keep
              [(f/of :new-permission)
               (pass/make-nonce 10)
               "https://example.org/permissions/"
               f/str
               :xt/id
               f/rot f/set-at :new-permission]))

            push-permission

            configure-response]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn grant-permission-install-graphql-endpoint-to-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      ;; TODO: We need a specialist grant-permission for this because we want to documnent/validate the ::site/resource-pattern
      {:xt/id "https://example.org/permissions/alice/install-graphql-endpoint"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action "https://example.org/actions/install-graphql-endpoint"
       ;; This permission can restrict exactly where a GraphQL endpoint can be
       ;; installed.
       :juxt.site.alpha/resource-pattern "\\Qhttps://example.org/graphql\\E"
       :juxt.pass.alpha/purpose nil})))))

(defn create-action-put-graphql-schema []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-graphql-schema"

       :juxt.pass.alpha/scope "https://example.org/oauth/scope/graphql/develop"

       ;; TODO: This is required to compile the GraphQL schema and communicate
       ;; any schema validation or compilation errors. Perhaps this can be done
       ;; with a custom word.
       :juxt.flip.alpha/quotation
       `(
       (f/define ^{:f/stack-effect '[ctx -- ctx]} extract-input
         [(f/set-at
           (f/dip
            [site/request-body-as-string
             :input]))])

       (f/define ^{:f/stack-effect '[ctx -- ctx]} compile-input-to-schema
         [(f/set-at
           (f/keep
            [(f/of :input)
             graphql-flip/compile-schema
             :compiled-schema]))])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} update-base-resource
         [(f/dip
           [(site/entity (f/env ::site/resource))
            (f/set-at (f/dip [f/dup (f/of :input) ::http/content]))
            (f/set-at (f/dip ["application/graphql" ::http/content-type]))
            (f/set-at (f/dip [f/dup (f/of :compiled-schema) ::site/graphql-compiled-schema]))])
          f/rot
          f/set-at])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} create-edn-resource
         [(f/dip
           ;; Perhaps could we use a template with eval-embedded-quotations?
           [{}
            (f/set-at (f/dip ["application/edn" ::http/content-type]))
            (f/set-at (f/dip [(f/env ::site/resource) ".edn" f/swap f/str :xt/id]))
            (f/set-at (f/dip [(f/env ::site/resource) ::site/variant-of]))
            (f/set-at (f/dip [f/dup (f/of :compiled-schema) f/pr-str ::http/content]))])
          f/rot
          f/set-at])

       (f/define ^{:f/stack-effect '[ctx key -- ctx]} push-resource
         [(f/push-at
           (xtdb.api/put
            f/dupd
            f/of
            (f/unless* [(f/throw-exception (f/ex-info "No object to push as an fx" {}))]))
           ::site/fx
           f/rot)])

       (f/define ^{:f/stack-effect '[ctx -- ctx]} determine-status
         [(f/of (site/entity (f/env ::site/resource)) ::http/content)
          [200 :status f/rot f/set-at]
          [201 :status f/rot f/set-at]
          f/if])

       (site/with-fx-acc ;;-with-checks - adding -with-checks somehow messes things up! :(
         [
          ;; The following can be done in advance of the fx-fn.
          extract-input
          compile-input-to-schema

          ;; The remainder would need to be done in the tx-fn because it looks
          ;; up the /graphql resource in order to update it.
          (update-base-resource :new-resource)
          (push-resource :new-resource)

          ;; The application/edn resource serves the compiled version
          (create-edn-resource :edn-resource)
          (push-resource :edn-resource)

          ;; Return a 201 if there is no existing schema, 200 otherwise
          determine-status
          (site/set-status f/dup (f/of :status))
          f/swap
          site/push-fx
          ]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn grant-permission-put-graphql-schema-to-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      ;; TODO: We need a specialist grant-permission for this because we want to
      ;; documnent/validate the ::site/resource
      {:xt/id "https://example.org/permissions/alice/put-graphql-schema"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action "https://example.org/actions/put-graphql-schema"
       :juxt.pass.alpha/purpose nil
       :juxt.site.alpha/resource "https://example.org/graphql"})))))

(defn create-action-get-graphql-schema []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-graphql-schema"
       :juxt.pass.alpha/scope "https://example.org/oauth/scope/graphql/develop"
       :juxt.flip.alpha/quotation '()
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn grant-permission-get-graphql-schema-to-alice! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      ;; TODO: We need a specialist grant-permission for this because we want to
      ;; documnent/validate the ::site/resource
      {:xt/id "https://example.org/permissions/alice/get-graphql-schema"
       :juxt.pass.alpha/user "https://example.org/users/alice"
       :juxt.pass.alpha/action "https://example.org/actions/get-graphql-schema"
       :juxt.pass.alpha/purpose nil
       :juxt.site.alpha/resource "https://example.org/graphql"})))))

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

(defn grant-permission-to-authorize!
  [& {:keys [username] :as args}]
  {:pre [(malli/validate [:map [:username [:string]]] args)]}
  ;; Grant user permission to perform /actions/oauth/authorize
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id (format "https://example.org/permissions/%s-can-authorize" ~(str/lower-case username))
       ::pass/action "https://example.org/actions/oauth/authorize"
       ::pass/user (format "https://example.org/users/%s" ~(str/lower-case username))
       ::pass/purpose nil}))))

(defn create-bearer-protection-space []
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-protection-space"
      {:xt/id "https://example.org/protection-spaces/bearer"

       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       ;;:juxt.pass.alpha/realm "Wonderland" ; optional

       :juxt.pass.alpha/auth-scheme "Bearer"
       :juxt.pass.alpha/authentication-scope "/private/.*" ; regex pattern
       }))))

(defn create-basic-protection-space []
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-protection-space"
      {:xt/id "https://example.org/protection-spaces/basic"

       :juxt.pass.alpha/canonical-root-uri "https://example.org"
       :juxt.pass.alpha/realm "Wonderland"   ; optional

       :juxt.pass.alpha/auth-scheme "Basic"
       :juxt.pass.alpha/authentication-scope "/private/.*" ; regex pattern
       }))))

(defn create-oauth-session-scope []
  (eval
   (substitute-actual-base-uri
    `(init/do-action
     "https://example.org/subjects/system"
     "https://example.org/actions/put-session-scope"
     {:xt/id "https://example.org/session-scopes/oauth"
      :juxt.pass.alpha/cookie-name "id"
      :juxt.pass.alpha/cookie-domain "https://example.org"
      :juxt.pass.alpha/cookie-path "/oauth"
      :juxt.pass.alpha/login-uri "https://example.org/login"}))))

(defn with-body [req body-bytes]
  (-> req
      (update :ring.request/headers (fnil assoc {}) "content-length" (str (count body-bytes)))
      (assoc :ring.request/body (io/input-stream body-bytes))))

(defn login-with-form!
  "Return a session id (or nil) given a map of fields."
  [& {:strs [username password] :as args}]
  {:pre [(malli/validate
          [:map
           ["username" [:string {:min 2}]]
           ["password" [:string {:min 6}]]] args)]
   :post [(malli/validate [:string] %)]}
  (let [form (codec/form-encode args)
        body (.getBytes form)
        req {:ring.request/method :post
             :ring.request/path "/login"
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/x-www-form-urlencoded"}
             :ring.request/body (io/input-stream body)}
        response (*handler* req)
        {:strs [set-cookie]} (:ring.response/headers response)
        [_ id] (when set-cookie (re-matches #"id=(.*?);.*" set-cookie))]
    (when-not id (throw (ex-info "Login failed" args)))
    id))

(defn authorize-response!
  "Authorize response"
  [& {:keys [session-id]
      client-id "client_id"
      scope "scope"
      :as args}]
  {:pre [(malli/validate
          [:map
           ^{:doc "to authenticate with authorization server"} [:session-id :string]
           ["client_id" :string]
           ["scope" {:optional true} [:sequential :string]]]
          args)]}
  (let [state (make-nonce 10)
        request {:ring.request/method :get
                 :ring.request/path "/oauth/authorize"
                 :ring.request/headers {"cookie" (format "id=%s" session-id)}
                 :ring.request/query
                 (codec/form-encode
                  (cond->
                      {"response_type" "token"
                       "client_id" client-id
                       "state" state}
                    scope (assoc "scope" (codec/url-encode (str/join " " scope)))))}]
    (*handler* request)))

(defn authorize!
  "Authorize an application, and return decoded fragment parameters as a string->string map"
  [& {:as args}]
  {:pre [(malli/validate
          [:map
           ^{:doc "to authenticate with authorization server"} [:session-id :string]
           ["client_id" :string]
           ["scope" {:optional true} [:sequential :string]]]
          args)]
   :post [(malli/validate [:map-of :string :string] %)]}
  (let [response (authorize-response! args)
        _ (case (:ring.response/status response)
            302 :ok
            400 (throw (ex-info "Client error" (assoc args :response response)))
            403 (throw (ex-info "Forbidden to authorize" (assoc args :response response)))
            (throw (ex-info "Unexpected error" (assoc args :response response))))

        location-header (-> response :ring.response/headers (get "location"))

        [_ _ encoded] (re-matches #"https://(.*?)/terminal/callback#(.*)" location-header)]

    (codec/form-decode encoded)))

(defn create-graphql-endpoint []
  (let [session-id (login-with-form! {"username" "ALICE" "password" "garden"})
        {access-token "access_token"
         error "error"}
        (authorize!
         :session-id session-id
         "client_id" "local-terminal"
         "scope" [(format "%s/oauth/scope/graphql/administer" (substitute-actual-base-uri "https://example.org"))])
        _ (assert (nil? error) (format "OAuth2 grant error: %s" error))
        request
        {:ring.request/method :post
         :ring.request/path "/actions/install-graphql-endpoint"
         :ring.request/headers
         {"authorization" (format "Bearer %s" access-token)
          "content-type" "application/edn"}}]
    (*handler*
     (with-body request
       (.getBytes
        (pr-str
         {:xt/id (format "%s/graphql" (substitute-actual-base-uri "https://example.org"))}))))))

(defn create-action-create-oauth-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/create-oauth-scope"

       :juxt.flip.alpha/quotation
       `(
         (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
               (site/validate
                [:map
                 [:xt/id [:re "https://example.org/oauth/scope/.*"]]])
               (site/set-type "https://meta.juxt.site/pass/oauth-scope")
               xtdb.api/put]))]))

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]]})))))

(defn grant-permission-to-invoke-action-create-oauth-scope! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/create-oauth-scope"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/create-oauth-scope"
       :juxt.pass.alpha/purpose nil})))))

(defn create-oauth-scope! [scope]
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-oauth-scope"
      {:xt/id ~scope}))))

(defn create-action-put-user-owned-content! []
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-user-owned-content"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [resource :owner user]]]})))))

(defn grant-permission-to-put-user-owned-content! [username]
  (eval
   (substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id ~(format "https://example.org/permissions/%s/put-user-owned-content" username)
       :juxt.pass.alpha/action "https://example.org/actions/put-user-owned-content"
       :juxt.pass.alpha/user ~(format "https://example.org/users/%s" username)
       :juxt.pass.alpha/purpose nil}))))

(def dependency-graph
  {"https://example.org/hello"
   {:create #'create-hello-world-resource!
    :deps #{::init/system
            "https://example.org/actions/put-immutable-public-resource"
            "https://example.org/permissions/repl/put-immutable-public-resource"}}

   "https://example.org/actions/put-immutable-public-resource"
   {:create #'create-action-put-immutable-public-resource!
    :deps #{::init/system
            "https://example.org/actions/get-public-resource"
            "https://example.org/permissions/public-resources-to-all"}}

   "https://example.org/permissions/repl/put-immutable-public-resource"
   {:create #'grant-permission-to-invoke-action-put-immutable-public-resource!
    :deps #{::init/system}}

   "https://example.org/actions/get-public-resource"
   {:create #'create-action-get-public-resource!
    :deps #{::init/system}}

   "https://example.org/permissions/public-resources-to-all"
   {:create #'grant-permission-to-invoke-get-public-resource!
    :deps #{::init/system}}

   "https://example.org/actions/put-user"
   {:create #'create-action-put-user!
    :deps #{::init/system}}

   "https://example.org/permissions/repl/put-user"
   {:create #'grant-permission-to-invoke-action-put-user!
    :deps #{::init/system}}

   "https://example.org/actions/put-immutable-protected-resource"
   {:create #'create-action-put-immutable-protected-resource!
    :deps #{::init/system}}

   "https://example.org/permissions/repl/put-immutable-protected-resource"
   {:create #'grant-permission-to-put-immutable-protected-resource!
    :deps #{::init/system}}

   "https://example.org/actions/get-protected-resource"
   {:create #'create-action-get-protected-resource!
    :deps #{::init/system}}

   "https://example.org/actions/put-protection-space"
   {:create #'create-action-put-protection-space!
    :deps #{::init/system}}

   "https://example.org/permissions/repl/put-protection-space"
   {:create #'grant-permission-to-put-protection-space!
    :deps #{::init/system}}

   "https://example.org/actions/put-basic-user-identity"
   {:create #'create-action-put-basic-user-identity!
    :deps #{::init/system}}

   "https://example.org/permissions/repl/put-basic-user-identity"
   {:create #'grant-permission-to-invoke-action-put-basic-user-identity!
    :deps #{::init/system}}

   "https://example.org/users/alice"
   {:create (fn [] (create-user! {:username "alice" :name "Alice"}))
    :deps #{::init/system
            "https://example.org/actions/put-user"
            "https://example.org/permissions/repl/put-user"}}

   "https://example.org/user-identities/alice/basic"
   {:create (fn [] (create-basic-user-identity!
                    {::pass/username "alice" ::pass/password "garden" ::pass/realm "Wonderland"}))
    :deps #{::init/system
            "https://example.org/actions/put-basic-user-identity"
            "https://example.org/permissions/repl/put-basic-user-identity"
            "https://example.org/users/alice"}}

   "https://example.org/users/bob"
   {:create (fn [] (create-user! {:username "bob" :name "Bob"}))
    :deps #{::init/system
            "https://example.org/actions/put-user"
            "https://example.org/permissions/repl/put-user"}}

   "https://example.org/user-identities/bob/basic"
   {:create (fn [] (create-basic-user-identity!
                    #::pass{:username "bob" :password "walrus" :realm "Wonderland"}))
    :deps #{::init/system
            "https://example.org/actions/put-basic-user-identity"
            "https://example.org/permissions/repl/put-basic-user-identity"
            "https://example.org/users/bob"}}

   "https://example.org/protected-by-session-scope/document.html"
   {:create #'create-resource-protected-by-session-scope!
    :deps #{::init/system
            "https://example.org/actions/put-immutable-protected-resource"
            "https://example.org/permissions/repl/put-immutable-protected-resource"
            "https://example.org/actions/get-protected-resource"}}

   "https://example.org/session-scopes/example"
   {:create #'create-session-scope!
    :deps #{::init/system
            "https://example.org/actions/put-session-scope"
            "https://example.org/permissions/repl/put-session-scope"}}

   "https://example.org/actions/oauth/authorize"
   {:create #'create-action-oauth-authorize!
    :deps #{::init/system}}

   "https://example.org/actions/install-authorization-server"
   {:create #'create-action-install-authorization-server!
    :deps #{::init/system}}

   "https://example.org/permissions/system/install-authorization-server"
   {:create #'grant-permission-install-authorization-server!
    :deps #{::init/system}}

   "https://example.org/oauth/authorize"
   {:create #'install-authorization-server!
    :deps #{::init/system
            "https://example.org/actions/install-authorization-server"
            "https://example.org/permissions/system/install-authorization-server"
            "https://example.org/actions/oauth/authorize"}}

   "https://example.org/actions/put-session-scope"
   {:create #'create-action-put-session-scope!
    :deps #{::init/system}}

   "https://example.org/permissions/repl/put-session-scope"
   {:create #'grant-permission-to-put-session-scope!
    :deps #{::init/system}}

   "https://example.org/actions/login"
   {:create #'create-action-login!
    :deps #{::init/system}}

   "https://example.org/permissions/login"
   {:create #'grant-permission-to-invoke-action-login!
    :deps #{::init/system}}

   "https://example.org/actions/create-login-resource"
   {:create #'create-action-create-login-resource!
    :deps #{::init/system
            "https://example.org/actions/login"
            "https://example.org/permissions/login"}}

   "https://example.org/permissions/system/create-login-resource"
   {:create #'grant-permission-to-create-login-resource!
    :deps #{::init/system}}

   "https://example.org/login"
   {:create #'create-login-resource!
    :deps #{::init/system
            "https://example.org/actions/create-login-resource"
            "https://example.org/permissions/system/create-login-resource"}}

   "https://example.org/permissions/alice-can-authorize"
   {:create (fn [] (grant-permission-to-authorize! :username "alice"))
    :deps #{::init/system
            "https://example.org/actions/oauth/authorize"
            "https://example.org/users/alice"}}

   "https://example.org/permissions/bob-can-authorize"
   {:create (fn [] (grant-permission-to-authorize! :username "bob"))
    :deps #{::init/system
            "https://example.org/actions/oauth/authorize"
            "https://example.org/users/bob"}}

   "https://example.org/applications/local-terminal"
   {:create #'register-example-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}

   "https://example.org/actions/install-graphql-endpoint"
   {:create #'create-action-install-graphql-endpoint!
    :deps #{::init/system}}

   "https://example.org/permissions/alice/install-graphql-endpoint"
   {:create #'grant-permission-install-graphql-endpoint-to-alice!
    :deps #{::init/system}}

   "https://example.org/private/internal.html"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-immutable-protected-resource"
            "https://example.org/permissions/repl/put-immutable-protected-resource"
            "https://example.org/actions/get-protected-resource"}
    :create #'create-internal-resource!}

   "https://example.org/permissions/alice/private/internal.html"
   {:deps #{::init/system
            "https://example.org/subjects/system"
            "https://example.org/actions/grant-permission"
            "https://example.org/actions/get-protected-resource"}
    :create #'grant-alice-permission-to-internal-resource!}

   "https://example.org/protection-spaces/bearer"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/repl/put-protection-space"}
    :create #'create-bearer-protection-space}

   "https://example.org/protection-spaces/basic"
   {:deps #{::init/system
            "https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/put-protection-space"
            "https://example.org/permissions/repl/put-protection-space"}
    :create #'create-basic-protection-space}

   "https://example.org/session-scopes/oauth"
   {:deps #{::init/system
            "https://example.org/actions/put-session-scope"
            "https://example.org/permissions/repl/put-session-scope"}
    :create #'create-oauth-session-scope}

   "https://example.org/actions/put-graphql-schema"
   {:deps #{::init/system}
    :create #'create-action-put-graphql-schema}

   "https://example.org/permissions/alice/put-graphql-schema"
   {:deps #{::init/system}
    :create #'grant-permission-put-graphql-schema-to-alice!}

   "https://example.org/actions/get-graphql-schema"
   {:deps #{::init/system}
    :create #'create-action-get-graphql-schema}

   "https://example.org/permissions/alice/get-graphql-schema"
   {:deps #{::init/system}
    :create #'grant-permission-get-graphql-schema-to-alice!}

   "https://example.org/graphql"
   {:deps #{::init/system
            "https://example.org/oauth/authorize"
            "https://example.org/session-scopes/oauth"
            "https://example.org/login"
            "https://example.org/user-identities/alice/basic"
            "https://example.org/permissions/alice-can-authorize"

            "https://example.org/applications/local-terminal"

            "https://example.org/actions/install-graphql-endpoint"
            "https://example.org/permissions/alice/install-graphql-endpoint"
            "https://example.org/actions/put-graphql-schema"

            "https://example.org/oauth/scope/graphql/administer"
            "https://example.org/oauth/scope/graphql/develop"}

    :create #'create-graphql-endpoint
    }

   ;; OAuth Scopes

   "https://example.org/actions/create-oauth-scope"
   {:deps #{::init/system}
    :create #'create-action-create-oauth-scope!}

   "https://example.org/permissions/system/create-oauth-scope"
   {:deps #{::init/system}
    :create #'grant-permission-to-invoke-action-create-oauth-scope!}

   ;; Individual OAuth2 scopes

   "https://example.org/oauth/scope/graphql/administer"
   {:deps #{::init/system
            "https://example.org/actions/create-oauth-scope"
            "https://example.org/permissions/system/create-oauth-scope"}
    :create (fn [] (#'create-oauth-scope! "https://example.org/oauth/scope/graphql/administer"))}

   "https://example.org/oauth/scope/graphql/develop"
   {:deps #{::init/system
            "https://example.org/actions/create-oauth-scope"
            "https://example.org/permissions/system/create-oauth-scope"}
    :create (fn [] (#'create-oauth-scope! "https://example.org/oauth/scope/graphql/develop"))}

   "https://example.org/oauth/scope/graphql/read"
   {:deps #{::init/system
            "https://example.org/actions/create-oauth-scope"
            "https://example.org/permissions/system/create-oauth-scope"}
    :create (fn [] (#'create-oauth-scope! "https://example.org/oauth/scope/graphql/read"))}

   "https://example.org/oauth/scope/graphql/write"
   {:deps #{::init/system
            "https://example.org/actions/create-oauth-scope"
            "https://example.org/permissions/system/create-oauth-scope"}
    :create (fn [] (#'create-oauth-scope! "https://example.org/oauth/scope/graphql/write"))}

   ;; Required by user-directory-test

   "https://example.org/actions/put-user-owned-content"
   {:deps #{::init/system}
    :create #'create-action-put-user-owned-content!}

   "https://example.org/permissions/alice/put-user-owned-content"
   {:deps #{::init/system}
    :create (fn [] (#'grant-permission-to-put-user-owned-content! "alice"))}

   "https://example.org/permissions/bob/put-user-owned-content"
   {:deps #{::init/system}
    :create (fn [] (#'grant-permission-to-put-user-owned-content! "bob"))}
   }
  )
