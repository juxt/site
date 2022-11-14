;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.oauth
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.test :refer [deftest is are testing]]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.test.util :refer [*handler*]]
   [malli.core :as malli]
   [ring.util.codec :as codec]))

(defn register-application! [{:keys [params]}]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/register-application")
   (substitute-actual-base-uri
    {:juxt.pass.alpha/client-id (get params "client")
     ;; TODO: What is this redirect-uri doing here?
     :juxt.pass.alpha/redirect-uri (format "https://example.org/terminal/%s" (get params "client"))})))

(defn create-action-install-authorization-server! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-authorization-server"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (with-out-str
          (clojure.pprint/pprint
           '(let [input (:juxt.site.alpha/received-representation *ctx*)
                  content-type (:juxt.http.alpha/content-type input)]
              (case content-type
                "application/edn"
                (some->
                 input
                 :juxt.http.alpha/body
                 (String.)
                 clojure.edn/read-string
                 juxt.site.malli/validate-input
                 (assoc
                  :juxt.site.alpha/methods
                  {:get #:juxt.pass.alpha{:actions #{"https://example.org/actions/oauth/authorize"}}}))))))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]]})))))

(defn grant-permission-install-authorization-server! [_]
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

(defn install-authorization-server! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-authorization-server"
      {:xt/id "https://example.org/oauth/authorize"
       :juxt.http.alpha/content-type "text/html;charset=utf-8"
       :juxt.http.alpha/content "<p>Welcome to the Site authorization server.</p>"})))))

(defn create-action-oauth-authorize! [_]
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


       :juxt.site.alpha/prepare
       {#_#_:juxt.flip.alpha/quotation
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
                  [f/dup (f/of :subject) (f/of :xt/id) :juxt.pass.alpha/subject {} f/set-at f/swap
                   f/dup (f/of :application) (f/of :xt/id) :juxt.pass.alpha/application f/rot f/set-at
                   (f/of :scope) :juxt.pass.alpha/scope f/rot f/set-at
                   (f/set-at (f/dip [(pass/as-hex-str (pass/random-bytes access-token-length)) :juxt.pass.alpha/token]))
                   ;; :xt/id (as a function of :juxt.pass.alpha/token)
                   (f/set-at (f/keep [(f/of :juxt.pass.alpha/token) (f/env :juxt.site.alpha/base-uri) "/access-tokens/" f/swap f/str f/str :xt/id]))
                   ;; :juxt.site.alpha/type
                   (f/set-at (f/dip ["https://meta.juxt.site/pass/access-token" :juxt.site.alpha/type]))
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

        :juxt.site.alpha.sci/program
        (with-out-str
          (clojure.pprint/pprint
           '(let [qs (:ring.request/query *ctx*)
                  _ (when-not qs
                      (throw
                       (ex-info "No query string" {:ring.response/status 400})))

                  query (ring.util.codec/form-decode qs)

                  client-id (get query "client_id")
                  _ (when-not client-id
                      (throw
                       (ex-info "A client_id parameter is required" {:ring.response/status 400})))

                  response-type (get query "response_type")
                  _ (when-not response-type
                      (throw
                       (ex-info
                        "A response_type parameter is required"
                        {"error" "invalid_request"})))

                  _ (when (sequential? response-type)
                      (throw
                       (ex-info
                        "The response_type parameter must only be provided once"
                        {"error" "invalid_request"})))

                  _ (when-not (contains? #{"code" "token"} response-type)
                      (throw (ex-info "Only response types of 'code' and 'token' are currently supported" {})))

                  subject (:juxt.pass.alpha/subject *ctx*)
                  _ (when-not subject
                      (throw (ex-info "Cannot create access-token: no subject" {})))

                  ;; "The authorization server SHOULD document the size of any
                  ;; value it issues." -- RFC 6749 Section 4.2.2
                  access-token-length 16

                  access-token (juxt.pass.util/make-nonce access-token-length)
                  ]

              {:client-id client-id
               :query query
               :access-token access-token
               :subject (:xt/id subject)})))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [{:keys [client-id query access-token subject]} *prepare*
                application (juxt.pass/lookup-application client-id)
                _ (when-not application
                    (throw
                     (ex-info
                      (format "No application found with client-id of %s" client-id)
                      {:client-id client-id})))

                ;; Leave scope for now for tests to flush out
                scopes (some-> *prepare* (get-in [:query "scope"]) ring.util.codec/form-decode (clojure.string/split #"\\s") set)
                _ (doall (map juxt.pass/lookup-scope scopes))

                access-token-doc
                (cond->
                    {:xt/id (format "%s/access-tokens/%s" (:juxt.site.alpha/base-uri *ctx*) access-token)
                     :juxt.site.alpha/type "https://meta.juxt.site/pass/access-token"
                     :juxt.pass.alpha/subject subject
                     :juxt.pass.alpha/application (:xt/id application)
                     :juxt.pass.alpha/token access-token}
                    scopes (assoc :juxt.pass.alpha/scope scopes))

                fragment
                (ring.util.codec/form-encode
                 {"access_token" access-token
                  "token_type" "bearer"
                  "state" (get query "state")})

                location (format "%s#%s" (:juxt.pass.alpha/redirect-uri application) fragment)]

            [[:xtdb.api/put access-token-doc]
             [:ring.response/status 303]
             [:ring.response/headers {"location" location}]]))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(def dependency-graph
  {"https://example.org/applications/{client}"
   {:create #'register-example-application!
    :deps #{::init/system
            "https://example.org/actions/register-application"
            "https://example.org/permissions/system/register-application"}}

   "https://example.org/actions/install-authorization-server"
   {:create #'create-action-install-authorization-server!
    :deps #{::init/system}}

   "https://example.org/permissions/system/install-authorization-server"
   {:create #'grant-permission-install-authorization-server!
    :deps #{::init/system
            "https://example.org/actions/install-authorization-server"}}

   "https://example.org/actions/oauth/authorize"
   {:create #'create-action-oauth-authorize!
    :deps #{::init/system}}

   "https://example.org/oauth/authorize"
   {:create #'install-authorization-server!
    :deps #{::init/system
            "https://example.org/actions/install-authorization-server"
            "https://example.org/permissions/system/install-authorization-server"
            "https://example.org/actions/oauth/authorize"}}

   ::authorization-server
   {:deps #{"https://example.org/oauth/authorize"}}})

(defn authorize-response!
  "Authorize response"
  [{:juxt.pass.alpha/keys [session-token]
    client-id "client_id"
    scope "scope"
    :as args}]
  (let [state (make-nonce 10)
        request {:ring.request/method :get
                 :juxt.site.alpha/uri (substitute-actual-base-uri "https://example.org/oauth/authorize")
                 :ring.request/headers {"cookie" (format "id=%s" session-token)}
                 :ring.request/query
                 (codec/form-encode
                  (cond->
                      {"response_type" "token"
                       "client_id" client-id
                       "state" state}
                      scope (assoc "scope" (codec/url-encode (str/join " " scope)))))}]
    (*handler* request)))

(malli/=>
 authorize-response!
 [:=> [:cat
       [:map
        ^{:doc "to authenticate with authorization server"} [:juxt.pass.alpha/session-token :string]
        ["client_id" :string]
        ["scope" {:optional true} [:sequential :string]]]]
  [:map
   ["access_token" {:optional true} :string]
   ["error" {:optional true} :string]]])

(defn authorize!
  "Authorize an application, and return decoded fragment parameters as a string->string map"
  [args]
  (let [response (authorize-response! args)
        _ (case (:ring.response/status response)
            (302 303) :ok
            400 (throw (ex-info "Client error" (assoc args :response response)))
            403 (throw (ex-info "Forbidden to authorize" (assoc args :response response)))
            (throw (ex-info "Unexpected error" (assoc args :response response))))

        location-header (-> response :ring.response/headers (get "location"))

        [_ _ encoded] (re-matches #"https://(.*?)/.*?#(.*)" location-header)]

    (assert encoded)
    (codec/form-decode encoded)))

(malli/=>
 authorize!
 [:=> [:cat
       [:map
        ^{:doc "to authenticate with authorization server"} [:juxt.pass.alpha/session-token :string]
        ["client_id" :string]
        ["scope" {:optional true} [:sequential :string]]]]
  [:map
   ["access_token" {:optional true} :string]
   ["error" {:optional true} :string]]])
