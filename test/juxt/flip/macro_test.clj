;; Copyright © 2022, JUXT LTD.

(ns juxt.flip.macro-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.flip.alpha.core :as flip :refer :all]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.http-authentication :as authn]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :refer [put! do-action bootstrap!]]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [jsonista.core :as jsonista]
   [ring.util.codec :as codec]
   [clojure.edn :as edn]
   [xtdb.api :as xt]
   [juxt.site.alpha.init :as init]
   [juxt.flip.alpha.core :as f]
   [juxt.flip.clojure.core :as-alias fc]))

((t/join-fixtures [with-system-xt])
 (fn []
   (bootstrap!)

   (juxt.site.alpha.init/do-action
    "https://site.test/subjects/system"
    "https://site.test/actions/register-application"
    {:juxt.pass.alpha/client-id "local-terminal"
     :juxt.pass.alpha/redirect-uri "https://site.test/terminal/callback"
     :juxt.pass.alpha/scope "user:admin"})

   (xt/entity (xt/db *xt-node*) "https://site.test/applications/local-terminal")


   ;; Implicit grant (4.2)

   (flip/eval-quotation
    '[]
    `[
      ;; (application-id -- access-token)
      (define make-access-token
        ;; TODO: Add expiry, scope
        [::pass/application {} set-at
         (as-hex-str (random-bytes 16)) ::pass/token rot set-at
         (env ::pass/subject) ::pass/subject rot set-at
         "https://meta.juxt.site/pass/access-token" ::site/type rot set-at
         dup (of ::pass/token) "/access-tokens/" (env ::site/base-uri) str str :xt/id rot set-at])

      ;; assoc is intended to be used in a list, whereby the value is the top
      ;; of the stack. (assoc k v)
      (define assoc [swap rot set-at])
      ;; assoc* means put the value at the top of the stack into the map with
      ;; the given key. (assoc* k)
      (define assoc* [rot set-at])
      ;; Get, without losing the original map
      (define get [swap dup rot of])

      juxt.site.alpha/request-body-as-json

      ;; read OAuth spec
      ;; generate a code document, return the

      ;; Assoc the application, if it exists, indexed by client-id
      #_dup
      #_(set-at
       (dip [:application])
       (_2dip [(juxt.flip.alpha.xtdb/q
                (juxt.site.alpha/lookup
                 (of "client_id")
                 "https://meta.juxt.site/pass/application"
                 :juxt.pass.alpha/client-id
                 rot))
               first first]))

      ;; If an application exists, assoc an access-token
      #_dup
      #_(set-at
       (dip [:access-token])
       (_2dip
        [(of :application)
         (if* [make-access-token]
           ["No such client" {} ex-info throw])]))

      ;; Compose a 'body'

      #_dup
      #_(set-at
       (dip [:body])
       (_2dip
        [{} ;; start with an empty body
         (set-at
          (dip ["access_token"])
          (_2dip [(of dup :access-token) (of ::pass/token)]))

         (set-at
          (dip ["token_type"])
          (_2dip ["bearer"]))

         (set-at
          (dip ["token_type"])
          (_2dip ["bearer"]))

         ]))

      ;;break

      ;; Compose 'effects'

      #_(if* [ ;; assoc redirect_uri
            ;;(set-at (dip [(get ::pass/redirect-uri) :redirect-uri]))
            ;;(set-at (dip [(get :xt/id) make-access-token :access-token]))

            (get ::pass/token)
            dup ;; save access_token
            rot swap "access_token" rot set-at

            swap xtdb.api/put swap ;; tx-op for putting access-token

            ;; Add other parts of response
            "bearer" "token_type" rot set-at
            "3600" "expires_in" rot set-at

            dup (of :redirect-uri)
            swap :redirect-uri swap
            delete-at
            juxt.flip.alpha/form-encode
            "#" str swap str
            (juxt.site.alpha/set-header "location" swap)]

          ["No such client" swap ex-info throw])]

    (let [json-arg {"response_type" "token"
                    "client_id" "local-terminal"
                    "state" "abc123"}]
      {::site/db (xt/db *xt-node*)
       ::site/base-uri "https://example.org"
       ::pass/subject "https://site.test/subjects/alice"
       ::site/received-representation
       {::http/content-type "application/edn"
        ::http/body (jsonista/write-value-as-bytes json-arg)}}))))



;; Implicit grant - don't delete!
((t/join-fixtures [with-system-xt])
 (fn []
   (bootstrap!)

   (juxt.site.alpha.init/do-action
    "https://site.test/subjects/system"
    "https://site.test/actions/register-application"
    {:juxt.pass.alpha/client-id "local-terminal"
     :juxt.pass.alpha/redirect-uri "https://site.test/terminal/callback"
     :juxt.pass.alpha/scope "user:admin"})

   (xt/entity (xt/db *xt-node*) "https://site.test/applications/local-terminal")

   ;; Implicit grant (4.2)

   ;; (Assume Alice is authenticated)
   ;; Alice sends a request to

   ;; in: /authorize?response_type=token&client_id=local-terminal&state=joh123&redirect_uri=https://my.tv/callback
   ;; out: Return 302 with Location header set to https://my.tv/callback#access_token=bobtail123&token_type=bearer&state=joh123
   (flip/eval-quotation
    '[]
    `[
      (site/with-fx-acc
        [
         ;; Extract query string from environment, decode it and store it at
         ;; keyword :query
         (f/define extract-and-decode-query-string
           [(f/set-at
             (f/dip
              [:ring.request/query
               f/env
               f/form-decode
               :query]))])

         (f/define lookup-application-from-database
           [(f/set-at
             (f/keep
              [(of :query)
               (of "client_id")
               (juxt.flip.alpha.xtdb/q
                ~'{:find [(pull e [*])]
                   :where [[e :juxt.site.alpha/type "https://meta.juxt.site/pass/application"]
                           [e ::pass/client-id client-id]]
                   :in [client-id]})
               f/first
               f/first
               :application]))])

         (f/define redirect-to-application-redirect-uri
           [(site/push-fx (f/dip [(site/set-status 302)]))
            (site/push-fx
             (f/keep
              [dup (of :application) (of ::pass/redirect-uri)
               "#" swap str
               swap (of :fragment)
               (f/unless* [(f/throw (f/ex-info "Assert failed: No fragment found at :fragment" {}))])
               swap str
               (site/set-header "location" swap)]))])

         (f/define fail-if-no-application
           [(f/keep
             [
              ;; Grab the client-id for error reporting
              dup (of :query) (of "client_id") swap
              (of :application)
              ;; If no application entry, drop the client_id (to clean up the
              ;; stack)
              (f/if [f/drop]
                ;; else throw the error
                [:client-id {} f/set-at (f/throw (f/ex-info "No such app" f/swap))])])])

         (f/define extract-subject
           [(f/set-at (f/dip [(env ::pass/subject) :subject]))])

         (f/define assert-subject
           [(f/keep [(of :subject) (f/unless [(f/throw (f/ex-info "Cannot create access-token: no subject" {}))])])])

         extract-and-decode-query-string
         lookup-application-from-database
         fail-if-no-application

         ;; Get subject (it's in the environment, fail if missing subject)
         extract-subject
         assert-subject

         ;; TODO: Create access-token tied to subject, scope and application
         ;; https://docs.factorcode.org/content/vocab-strings.html
         (f/dip [{} (env ::site/base-uri) "/subjects/" f/swap f/str])


         ;; TODO: Construct fragment containing token, state and place in :fragment
         ;;(f/set-at (f/dip ["foobar" :fragment]))
         ;;redirect-to-application-redirect-uri

         ])]

    {::site/db (xt/db *xt-node*)
     ::site/base-uri "https://site.test"
     ::pass/subject "https://site.test/subjects/alice"
     :ring.request/method :get
     :ring.request/query "response_type=token&client_id=local-terminal&state=abc123vcb"

     })))


;; TODO: Look how login now creates sessions and copy that
      #_(flip/define make-access-token
          [(f/of :xt/id) ::pass/application {} f/set-at
           (juxt.pass.alpha.core/as-hex-str
            (juxt.pass.alpha.core/random-bytes 16))
           ::pass/token f/rot f/set-at
           (f/env ::pass/subject) ::pass/subject f/rot f/set-at
           "https://meta.juxt.site/pass/access-token" ::site/type f/rot f/set-at
           f/dup (f/of ::pass/token) "/access-tokens/" (f/env ::site/base-uri) f/str f/str :xt/id f/rot f/set-at])

      #_(flip/define locate-application
          [(set-at
            (keep
             [(site/lookup
               (of "client_id")
               "https://meta.juxt.site/pass/application"
               ::pass/client-id
               rot)
              juxt.flip.alpha.xtdb/q first first
              :application]))])

      #_(flip/define assoc-access-token
          [(set-at
            (keep
             [(of :application)
              ;; If the :application entry exists, add an access-token

              (if* [make-access-token]
                ["No such client" {} flip/ex-info flip/throw])
              :access-token]))])

      #_(flip/define create-response-params
          [(set-at
            ;; TODO: This can be implemented in a better way (I think) with
            ;; assoc-intersect
            ;; https://docs.factorcode.org/content/word-assoc-intersect%2Cassocs.html
            (keep [(flip/assoc-filter
                    [flip/drop
                     #{"token_type" "access_token" "state"}
                     flip/in?]) :response-params]))])

      #_(flip/define create-location-header
          [(site/push-fx
            (keep [(site/set-header
                    (of :response-params)
                    flip/form-encode
                    "redirect-uri#" str
                    "location")]))])

      #_(flip/define set-token-type
          ["token_type" flip/rot flip/set-at])

;;locate-application
      ;;      assoc-access-token

      ;;      (site/push-fx (keep [(of :access-token) xtdb.api/put]))
      ;;      (set-at (keep [(of :access-token) (of ::pass/token) "access_token"]))
      ;;      (set-token-type "bearer")
      ;;      (set-email )
      ;;      create-response-params
      ;;      create-location-header


;; All done, ready for another test
#_((t/join-fixtures [with-system-xt])
 (fn []
   (bootstrap!)

   #_(juxt.site.alpha.init/do-action
      "https://site.test/subjects/system"
      "https://site.test/actions/register-application"
      {:juxt.pass.alpha/client-id "local-terminal"
       :juxt.pass.alpha/redirect-uri "https://site.test/terminal/callback"
       :juxt.pass.alpha/scope "user:admin"})

   (xt/entity (xt/db *xt-node*) "https://site.test/applications/local-terminal")

   ;; Implicit grant (4.2)

   #_(flip/eval-quotation
    '[]
    `(
      (site/with-fx-acc
           [(site/push-fx
             (f/dip
              [site/request-body-as-edn
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
               (set-at
                (f/keep
                 [(of :juxt.pass.alpha/username) f/>lower :juxt.pass.alpha/username]))

               ;; Hash password
               (set-at
                (f/keep
                 [(of :juxt.pass.alpha/password) juxt.pass.alpha/encrypt-password :juxt.pass.alpha/password-hash]))
               (delete-at (f/dip [:juxt.pass.alpha/password]))

               (xtdb.api/put
                (fc/assoc
                 :juxt.site.alpha/methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-basic-user-identity"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-basic-user-identity"}}
                  :options {}}))]))]))

    (let [edn-arg
          {:xt/id "https://example.org/user-identities/alice"
           :juxt.pass.alpha/user "https://example.org/users/alice"
           :juxt.pass.alpha/username "ALICE"
           :juxt.pass.alpha/password "garden"
           }]
      {::site/db (xt/db *xt-node*)
       ::site/base-uri "https://example.org"
       ::pass/subject "https://site.test/subjects/alice"
       ::site/received-representation
       {::http/content-type "application/edn"
        ::http/body (.getBytes (pr-str edn-arg))}}))))



(flip/eval-quotation
    '[]
    `["a" "b" f/str]

    {})
