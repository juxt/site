;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.v2.acl-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.pass.alpha.authorization-2 :as authz]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*]]
   [xtdb.api :as xt]
   [malli.core :as m]
   [malli.transform :as mt]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'pass.malli (create-ns 'juxt.pass.alpha.malli))
(alias 'site (create-ns 'juxt.site.alpha))

(defn fail [ex-data] (throw (ex-info "FAIL" ex-data)))

(defn expect
  ([result pred ex-info]
   (is (pred result))
   (let [pred-result (pred result)]
     (when-not pred-result
       (fail (into ex-info {:pred pred :result result :pred-result pred-result}))))
   result)
  ([result pred]
   (expect result pred {})))



(defn with-scenario [f]
  (submit-and-await!
   [
    ;; Sue is our superuser, we must create her records when bootstrapping the
    ;; Site instance.
    [::xt/put
     {:xt/id "https://example.org/people/sue"
      ::pass/ruleset "https://example.org/ruleset"}]
    ;; A person may have many identities
    [::xt/put
     {:xt/id "https://example.org/people/sue/identities/example"
      ::site/type "Identity"
      :juxt.pass.jwt/iss "https://example.org"
      :juxt.pass.jwt/sub "sue"
      ::pass/subject "https://example.org/people/sue"
      ::pass/ruleset "https://example.org/ruleset"}]

    ;; Terry is someone who is NOT a superuser, for testing.
    [::xt/put
     {:xt/id "https://example.org/people/terry"
      ::pass/ruleset "https://example.org/ruleset"}]
    [::xt/put
     {:xt/id "https://example.org/people/terry/identities/example"
      ::site/type "Identity"
      :juxt.pass.jwt/iss "https://example.org"
      :juxt.pass.jwt/sub "terry"
      ::pass/subject "https://example.org/people/terry"
      ::pass/ruleset "https://example.org/ruleset"}]

    ;; Some effects that are pre-registered.
    [::xt/put
     {:xt/id "https://example.org/effects/create-user"
      ::site/type "Effect"
      ::pass/scope "admin:write"
      ::pass/effect-args [{}]}]

    [::xt/put
     {:xt/id "https://example.org/effects/create-identity"
      ::site/type "Effect"
      ::pass/scope "admin:write"
      ::pass/effect-args
      [{::pass.malli/schema
        [:map
         [:juxt.pass.jwt/iss [:re "https://.*"]]
         [:juxt.pass.jwt/sub [:re "[a-zA-Z][a-zA-Z0-9\\|]{2,}"]]
         [::site/type [:= "Identity"]]]
        ::pass/process
        [
         ;; Though we could use a Malli value transformer here, at this stage is
         ;; doesn't feel beneficial to lean too heavily on Malli's extensive
         ;; feature set.
         [::pass/merge {::site/type "Identity"}]
         [::pass.malli/validate]]}]}]

    [::xt/put
     {:xt/id "https://example.org/acls/sue-can-create-users"
      ::site/type "ACL"
      ::pass/subject "https://example.org/people/sue"
      ::pass/effect #{"https://example.org/effects/create-user"
                      "https://example.org/effects/create-identity"}
      ;; Is not constrained to a resource
      ::pass/resource nil #_"https://example.org/people/"
      }]

    [::xt/put
     {:xt/id "https://example.org/rules/1"
      ::site/description "Allow read access of resources to granted subjects"
      ::pass/rule-content
      (pr-str '[[(acl-applies-to-subject? acl subject)
                 [acl ::pass/subject subject]]

                [(acl-applies-to-resource? acl resource)
                 [acl ::pass/resource resource]]

                [(acl-applies-to-resource? acl resource)
                 [(some? resource)]
                 [acl ::pass/resource nil]]

                [(effect-applies-to-resource? effect resource)
                 [(some? effect)]
                 #_[effect ::pass/resource nil]]

                ])}]

    ;; We can now define the ruleset
    [::xt/put
     {:xt/id "https://example.org/ruleset"
      ::pass/rules ["https://example.org/rules/1"]}]

    [::xt/put
     {:xt/id ::pass/authorizing-put
      :xt/fn '(fn [ctx auth effect args]
                (let [db (xtdb.api/db ctx)]
                  (apply juxt.pass.alpha.authorization-2/authorizing-put-fn db auth effect args)))}]

    [::xt/put
     (into
      {::pass/name "Site Admininistration"
       ;; If specified (and it must be currently), ::pass/scope overrides
       ;; the subject's default scope.
       ::pass/scope
       #{"admin:write"}}
      (authz/make-oauth-client-doc {::site/base-uri "https://example.org"} "admin-client"))]

    [::xt/put
     (into
      {::pass/name "Example App"
       ;; If specified (and it must be currently), ::pass/scope overrides
       ;; the subject's default scope.
       ::pass/scope
       #{"read:index"
         "read:document" "write:document"
         "read:directory-contents" "write:create-new-document"
         "userdir:write"}}
      (authz/make-oauth-client-doc {::site/base-uri "https://example.org"} "example-client"))]])
  (f))

(defn acquire-access-token
  ([sub client-id db]
   (acquire-access-token sub client-id db nil))
  ([sub client-id db scope]
   (let [
         ;; First we'll need the subject. As a performance optimisation, we can
         ;; associate the subject with the stored access token itself, rather
         ;; than re-establish the subject on each request via the id-token
         ;; claims, because we assume the claims can never apply to a different
         ;; subject. However, this assertion needs to be written up and
         ;; communicated. If claims were ever to be reassigned to a different
         ;; subject, then all access-tokens would need to be made void
         ;; (removed).
         subject
         (authz/lookup->subject {:claims {"iss" "https://example.org" "sub" sub}} db)

         _ (when-not subject
             (throw (ex-info (format "Cannot find identity with sub: %s" sub) {:sub sub})))

         ;; The access-token links to the application, the subject and its own
         ;; scopes. The overall scope of the request is ascertained at each and
         ;; every request.
         access-token
         (if scope
           (authz/make-access-token-doc (:xt/id subject) client-id scope)
           (authz/make-access-token-doc (:xt/id subject) client-id))]

     ;; An access token must exist in the database, linking to the application,
     ;; the subject and its own granted scopes. The actual scopes are the
     ;; intersection of all three.
     (submit-and-await! [[::xt/put access-token]])

     (:xt/id access-token))))

(defn authorize-request [{::site/keys [db] :as req} access-token-id]
  (let [access-token (xt/entity db access-token-id)

        ;; Establish subject and client
        {:keys [subject client]}
        (first
         (xt/q
          db
          '{:find [subject
                   (pull client [:xt/id ::pass/client-id ::pass/scope])]
            :keys [subject client]
            :where [[access-token ::pass/subject subject]
                    [access-token ::pass/client client]]
            :in [access-token]}
          access-token-id))]

    (assert subject)
    (assert (string? subject))

    ;; Bind onto the request. For performance reasons, the actual scope
    ;; is determined now, since the db is now a value.
    (assoc req
           ::pass/subject subject
           ::pass/client client
           ::pass/access-token-effective-scope
           (authz/access-token-effective-scope access-token client)
           ::pass/access-token access-token
           ::pass/ruleset "https://example.org/ruleset"
           )))

(defn new-request [uri db access-token-id opts]
  (assert access-token-id)
  (let [req (merge {::site/db db ::site/uri uri} opts)]
    (authorize-request req access-token-id)))

(defn authorizing-put! [req
                        & effect-calls]

  (let [
        ;; We construct an authentication/authorization 'context' from the
        ;; request, which we pass to the function and name it simply
        ;; 'auth'. Entries of this auth context will be used when determining
        ;; whether access is approved or denied. The reason we need to do this
        ;; is because the request itself contains entries that can't be sent
        ;; into a transaction function.
        auth (-> req
                 (select-keys
                  [::pass/subject
                   ::pass/client
                   ::pass/access-token-effective-scope
                   ::pass/ruleset
                   ;; The URI may be used as part of the context, e.g. PUT to
                   ;; /documents/abc may be allowed but PUT to /index may not
                   ;; be.
                   ::site/uri]))

        tx (xt/submit-tx
            *xt-node*
            (mapv
             (fn [[effect & args]]
               [:xtdb.api/fn ::pass/authorizing-put auth effect args])
             effect-calls))
        tx (xt/await-tx *xt-node* tx)]

    ;; Currently due to https://github.com/xtdb/xtdb/issues/1672 the only way of
    ;; checking whether this tx was authorized is by checking it committed. We
    ;; don't get any errors through, so all we can do is point at the logs.
    (when-not (xt/tx-committed? *xt-node* tx)
      (throw
       (ex-info
        "Failed to commit, check logs"
        {:auth auth
         :effect-calls effect-calls
         })))))

(t/use-fixtures :each with-xt with-handler with-scenario)

;; As above but building up from a smaller seed.
(deftest scenario-1-test


(let [db (xt/db *xt-node*)
         ;; Access tokens for each sub/client pairing
         access-tokens
         {["sue" "admin-client"]
          (acquire-access-token
           "sue" "https://example.org/_site/apps/admin-client"
           db)

          ["sue" "admin-client" #{"limited"}]
          (acquire-access-token
           "sue" "https://example.org/_site/apps/admin-client"
           db #{"limited"})

          ["sue" "example-client"]
          (acquire-access-token
           "sue" "https://example.org/_site/apps/example-client"
           db)

          ["terry" "admin-client"]
          (acquire-access-token
           "terry" "https://example.org/_site/apps/admin-client"
           db)}]

     ;; Sue creates a new user, Alice

     ;; Effects

     ;; Each effect is associated, many-to-one, with a required (single)
     ;; scope. If an OpenAPI document defines an operation, that operation may
     ;; involve multiple effects, and the security requirement might require
     ;; multiple scopes. The security requirement of scopes may be implied
     ;; (and may affect the publishing of the openapi.json such that authors
     ;; don't need to concern themselves with declaring scope).

     ;; A effect such as 'https://example.org/effects/create-user' is registered in the database.

     ;; Since effects are themselves defined and stored in the database, they
     ;; can evolve over time (their behavior can be amended).

     ;; Scopes are an access token concern. An access token references an
     ;; application which references a particular API. Effects are therefore
     ;; part of the domain to which an API belongs. A GraphQL endpoint is
     ;; defined as part of an overall OpenAPI, which is the same group where
     ;; scopes, effects and rulesets are defined.

     ;; create-user is in the 'admin:write' scope.

     ;; create-user is defined with a description that can be showed to users
     ;; for the purposes of informed authorization.

     ;; The 'create-user' effect determines the applicable ACLs.

     ;; Subjects are mapped to effects. Applications are mapped to scopes.

     ;; Now to call 'create-user'
     ;; First we test various combinations
     (let [db (xt/db *xt-node*)
           test-fn
           (fn [db {:keys [uri expected error effect access-token args] :as all-args}]
             (assert access-token)
             (try
               (let [actual
                     (apply
                      authz/authorizing-put-fn
                      db
                      (new-request uri db access-token {})
                      effect args)]

                 (when (and expected (not= expected actual))
                   (throw
                    (ex-info
                     "Unexpected result"
                     {:expected expected
                      :actual actual
                      ::pass true})))

                 (when error
                   (throw
                    (ex-info
                     "Expected to fail but didn't"
                     {:all-args all-args
                      ::pass true})))

                 actual)

               (catch Exception e
                 (when (::pass (ex-data e)) (throw e))
                 (when-not (= (.getMessage e) error)
                   (throw
                    (ex-info
                     "Failed but with an unexpected error message"
                     {:expected-error error
                      :actual-error (.getMessage e)}
                     e))))))]

       ;; This is the happy case, Sue attempts to create a new user, Alice
       (test-fn
        db
        {:effect "https://example.org/effects/create-user"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["sue" "admin-client"])
         :expected [[:xtdb.api/put
                     {:xt/id "https://example.org/people/alice",
                      :juxt.pass.alpha/ruleset "https://example.org/ruleset"}]]})

       ;; Sue's permission to call create-user is not constrained by a
       ;; resource, there is no error if we set one.
       (test-fn
        db
        {:effect "https://example.org/effects/create-user"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["sue" "admin-client"])
         :uri "https://example.org/other/"})

       ;; She can't use the example client to create users
       (test-fn
        db
        {:effect "https://example.org/effects/create-user"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["sue" "example-client"])
         :error "Effect 'https://example.org/effects/create-user' denied as no ACLs found that approve it."})

       ;; She can't use these privileges to call a different effect
       (test-fn
        db
        {:effect "https://example.org/effects/create-superuser"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["sue" "admin-client"])
         :error "No such effect: https://example.org/effects/create-superuser"})

       ;; Neither can she used an access-token where she hasn't granted enough scope
       (test-fn
        db
        {:effect "https://example.org/effects/create-user"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["sue" "admin-client" #{"limited"}])
         :error "Effect 'https://example.org/effects/create-user' denied as no ACLs found that approve it."})

       ;; Terry should not be able to create-users, even with the admin-client
       (test-fn
        db
        {:effect "https://example.org/effects/create-user"
         :args [{:xt/id "https://example.org/people/alice"}]
         :access-token (get access-tokens ["terry" "admin-client"])
         :error "Effect 'https://example.org/effects/create-user' denied as no ACLs found that approve it."})

       ;; In a GraphQL mutation, there will be no resource. Arguably, ACLs
       ;; should not be tied to a resource.

       ;; The effects should be agnostic about whether they are called from
       ;; OpenAPI or GraphQL.

       ;; A GraphQL mutation to create a user would still create the web
       ;; resource at a given location.

       ;; Perhaps GraphQL mutations must always provide the ID of the 'new'
       ;; resource, and perhaps also the ID of the 'parent' resource?

       ;; Most, if not all, actions will require the caller to provide the
       ;; document, which in some cases will contain the :xt/id, which will
       ;; become the URI of the resource. Perhaps the effect or ACL should
       ;; qualify what kinds of documents are allowed?

       ;; create-user should accept a map.
       ;; It should ensure the map is valid (according to clojure.spec, Malli or JSON Schema?)

       ;; create identity may specify its own id
       ;; must provide :juxt.pass.jwt/iss and :juxt.pass.jwt/sub
       ;; may provide anything else, but not in ::site or ::pass namespaces
       ;; ::pass/subject must be provided
       ;; ::pass/ruleset is inherited
       ;; An identity may have to be created 'under' the person record.


       ;; Now we do the official request which mutates the database
       ;; This is the 'official' way to avoid race-conditions.
       (let [req (new-request
                  "https://example.org/people/"
                  (xt/db *xt-node*)
                  (get access-tokens ["sue" "admin-client"])
                  {:request-body-doc {:xt/id "https://example.org/people/alice"}})]
         (authorizing-put!
          req
          ;; The request body would be transformed into this new doc
          ["https://example.org/effects/create-user" (:request-body-doc req)]
          ;; TODO: Alice will need an identity
          ;; TODO: We need to create some ACLs for this user, ideally in the same tx
          )
         )

       (let [db (xt/db *xt-node*)]
         (expect
          (xt/entity db "https://example.org/people/alice")
          #(= % {:juxt.pass.alpha/ruleset "https://example.org/ruleset",
                 :xt/id "https://example.org/people/alice"}))

         ;; Now Alice wants to create a document under https://example.org/~alice/
         ;; Let's check that she can.


         ;; Sue will need to create an ACL for her

         #_(let [access-token (acquire-access-token "alice" "example-client" db)
                 db (xt/db *xt-node*)]
             (xt/entity db access-token)
             #_(test-fn
                db
                {:uri "https://example.org/people/"
                 :access-token access-token
                 :effect "https://example.org/effects/put-resource"
                 :expected []})))

       ;; OK, let's create an identity for Alice!
       (let [db (xt/db *xt-node*)

             req (new-request
                  "https://example.org/people/"
                  (xt/db *xt-node*)
                  (get access-tokens ["sue" "admin-client"])
                  {})]

         (authorizing-put!
          req
          ;; The request body would be transformed into this new doc
          ["https://example.org/effects/create-identity"
           {:xt/id "https://example.org/people/sue/identities/example"
            :juxt.pass.jwt/iss "https://example.org"
            :juxt.pass.jwt/sub "alice"
            ::pass/subject "https://example.org/people/alice"
            }]
          ;; TODO: Alice will need an identity
          ;; TODO: We need to create some ACLs for this user, ideally in the same tx
          )
         )


       #_(let [
               id-doc
               {:xt/id "https://example.org/people/sue/identities/example"
                :juxt.pass.jwt/iss "https://example.org"
                :juxt.pass.jwt/sub "alice"
                ::pass/subject "https://example.org/people/alice"
                }]
           (test-fn
            db
            {:effect "https://example.org/effects/create-identity"
             :access-token (get access-tokens ["sue" "admin-client"])
             :args [id-doc]
             }))

       ;; Alice can now log in
       (let
           [db (xt/db *xt-node*)
            _ (xt/submit-tx *xt-node*
                            [[::xt/put
                              {:xt/id "https://example.org/effects/put-user-dir-resource"
                               ::site/type "Effect"
                               ::pass/scope "userdir:write"
                               ::pass/resource nil
                               ::pass/effect-args [{}]}]

                             [::xt/put
                              {:xt/id "https://example.org/acls/alice-can-create-user-dir-content"
                               ::site/type "ACL"
                               ::pass/subject "https://example.org/people/alice"
                               ::pass/effect #{"https://example.org/effects/put-user-dir-resource"}
                               ;; Is not constrained to a resource
                               ::pass/resource nil #_"https://example.org/people/"
                               }]])
            alice-token (acquire-access-token "alice" "https://example.org/_site/apps/example-client" db)
            db (xt/db *xt-node*)]

           (is alice-token)

           (test-fn
            db
            {:uri "https://example.org/~alice/"
             :access-token alice-token
             :effect "https://example.org/effects/put-user-dir-resource"
             :args [{}]})

           #_(test-fn
              db
              {:uri "https://example.org/index.html"
               :access-token alice-token
               :effect "https://example.org/effects/put-user-dir-resource"
               :args [{}]
               :error "foo"}))))

  ;; Notes:

  ;; If accessing the API directly with a browser, the access-token is
  ;; generated and stored in the session (accessed via the cookie rather than
  ;; the Authorization header).

  ;; The bin/site tool might have to be configured with the client-id of the
  ;; 'Admin App'.

  ;; TODO: Sue creates Alice, with Alice's rights
  ;; scope is 'create:user'

  ;; Could we have an underlying 'DSL' that can be used by both OpenAPI and
  ;; GraphQL? Rather than OpenAPI wrapping GraphQL (and therefore requiring
  ;; it), could we have both call an underlying 'Site DSL' which integrates
  ;; scope-based authorization?

  ;; Consider a 'create-user' effect. Might these be the events that jms
  ;; likes to talk about? A effect is akin to set of GraphQL mutations,
  ;; often one per request.

  ;; Effects can cause mutations and also side-effects.

  ;; Consider a effect: create-user - a effect can be protected by a scope,
  ;; e.g. write:admin

  ;; Effects must just be EDN.


  )

#_((t/join-fixtures [with-xt with-handler with-scenario])
 (fn []

   )
 )


;; When mutating, use info in the ACL(s) to determine whether the document to
;; 'put' meets the defined criteria. This can restrict the URI path to enforce a
;; particular URI organisation. For example, a person writing an object might be
;; restricted to write under their own area.

;; Allowed methods reported in the Allow response header may be the intersection
;; of methods defined on the resource and the methods allowed by the 'auth'
;; context.


#_(m/validate
 [:map [:juxt.pass.jwt/iss [:re "https://.*"]]]
 {:juxt.pass.jwt/iss "https://foo"})







;; Conclusion: the determining domain-provide rule has to take the following parameters:
;; subject resource effect ACL

;; Only effects that are within scope are considered
;; An ACL must apply to an effect
