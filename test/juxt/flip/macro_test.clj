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

      break

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

   (flip/eval-quotation
    '[]
    `[
      (flip/define make-access-token
        [(of :xt/id) ::pass/application {} set-at
         (juxt.pass.alpha.core/as-hex-str
          (juxt.pass.alpha.core/random-bytes 16))
         ::pass/token rot set-at
         (env ::pass/subject) ::pass/subject rot set-at
         "https://meta.juxt.site/pass/access-token" ::site/type rot set-at
         dup (of ::pass/token) "/access-tokens/" (env ::site/base-uri) str str :xt/id rot set-at])

      (flip/define locate-application
        [(set-at
          (keep
           [(site/lookup
             (of "client_id")
             "https://meta.juxt.site/pass/application"
             ::pass/client-id
             rot)
            juxt.flip.alpha.xtdb/q first first
            :application]))])

      (flip/define assoc-access-token
        [(set-at
          (keep
           [(of :application)
            ;; If the :application entry exists, add an access-token
            (if* [make-access-token]
              ["No such client" {} flip/ex-info flip/throw])
            :access-token]))])

      (flip/define create-response-params
        [(set-at
          ;; TODO: This can be implemented in a better way (I think) with
          ;; assoc-intersect
          ;; https://docs.factorcode.org/content/word-assoc-intersect%2Cassocs.html
          (keep [(flip/assoc-filter
                  [flip/drop
                   #{"token_type" "access_token" "state"}
                   flip/in?]) :response-params]))])

      (flip/define create-location-header
        [(site/push-fx
          (keep [(site/set-header
                  (of :response-params)
                  flip/form-encode
                  "redirect-uri#" str
                  "location")]))])

      (flip/define set-token-type
        ["token_type" flip/rot flip/set-at])


      site/request-body-as-json
      locate-application
      assoc-access-token
      (site/push-fx (keep [(of :access-token) xtdb.api/put]))
      (set-at (keep [(of :access-token) (of ::pass/token) "access_token"]))
      (set-token-type "bearer")
      (set-email )
      create-response-params
      create-location-header
      (of :fx)]

    ;; implicit
    ;;    /authorize?response_type=token&client_id=blah&state=my-state
    ;;    cwi-app/callback#access_token=asfualskefhalksefhalskeh&token_type=bearer&state=my-state

    (let [json-arg {"response_type" "token"
                    "client_id" "local-terminal"
                    "state" "abc123vcb"}]
      {::site/db (xt/db *xt-node*)
       ::site/base-uri "https://example.org"
       ::pass/subject "https://site.test/subjects/alice"
       ::site/received-representation
       {::http/content-type "application/edn"
        ::http/body (jsonista/write-value-as-bytes json-arg)}}))))



;; All done, ready for another test
((t/join-fixtures [with-system-xt])
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
