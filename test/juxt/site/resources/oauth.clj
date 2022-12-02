;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.oauth
  (:require
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [juxt.site.util :refer [make-nonce]]
   [juxt.site.init :as init :refer [substitute-actual-base-uri do-action]]
   [juxt.test.util :refer [*handler*]]
   [malli.core :as malli]
   [ring.util.codec :as codec]))

(defn register-application! [{:keys [params]}]
  (do-action
   "https://example.org/subjects/system"
   "https://example.org/actions/register-application"
   {:juxt.site/client-id (get params "client")
    ;; TODO: What is this redirect-uri doing here?
    :juxt.site/redirect-uri (format "https://example.org/terminal/%s" (get params "client"))}))

(defn create-action-install-authorization-server! [_]
  (do-action
   "https://example.org/subjects/system"
   "https://example.org/actions/create-action"
   {:xt/id "https://example.org/actions/install-authorization-server"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/.*"]]
     [:juxt.site/session-scope {:optional true} [:re "https://example.org/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (with-out-str
       (clojure.pprint/pprint
        '(let [input (:juxt.site/received-representation *ctx*)
               content-type (:juxt.http/content-type input)]
           (case content-type
             "application/edn"
             (some->
              input
              :juxt.http/body
              (String.)
              clojure.edn/read-string
              juxt.site.malli/validate-input
              (assoc
               :juxt.site/methods
               {:get #:juxt.site{:actions #{"https://example.org/actions/oauth/authorize"}}}))))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]]}))

(defn grant-permission-install-authorization-server! [_]
  (do-action
   "https://example.org/subjects/system"
   "https://example.org/actions/grant-permission"
   {:xt/id "https://example.org/permissions/system/install-authorization-server"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/install-authorization-server"
    :juxt.site/purpose nil}))

(defn install-authorization-server! [{:juxt.site/keys [session-scope]}]
  (do-action
   "https://example.org/subjects/system"
   "https://example.org/actions/install-authorization-server"
   {:xt/id "https://example.org/oauth/authorize"
    :juxt.http/content-type "text/html;charset=utf-8"
    :juxt.http/content "<p>Welcome to the Site authorization server.</p>"
    :juxt.site/session-scope session-scope}))

(defn create-action-oauth-authorize! [_]
  (do-action
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


    :juxt.site/prepare
    {:juxt.site.sci/program
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

               subject (:juxt.site/subject *ctx*)
               _ (when-not subject
                   (throw (ex-info "Cannot create access-token: no subject" {})))

               ;; "The authorization server SHOULD document the size of any
               ;; value it issues." -- RFC 6749 Section 4.2.2
               access-token-length 16

               access-token (juxt.site.util/make-nonce access-token-length)
               ]

           {:client-id client-id
            :query query
            :access-token access-token
            :subject (:xt/id subject)})))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '(let [{:keys [client-id query access-token subject]} *prepare*
             application (juxt.site/lookup-application client-id)
             _ (when-not application
                 (throw
                  (ex-info
                   (format "No application found with client-id of %s" client-id)
                   {:client-id client-id})))

             ;; Leave scope for now for tests to flush out
             scopes (some-> *prepare* (get-in [:query "scope"]) ring.util.codec/form-decode (clojure.string/split #"\\s") set)
             _ (doall (map juxt.site/lookup-scope scopes))

             access-token-doc
             (cond->
                 {:xt/id (format "%s/access-tokens/%s" (:juxt.site/base-uri *ctx*) access-token)
                  :juxt.site/type "https://meta.juxt.site/site/access-token"
                  :juxt.site/subject subject
                  :juxt.site/application (:xt/id application)
                  :juxt.site/token access-token}
               scopes (assoc :juxt.site/scope scopes))

             fragment
             (ring.util.codec/form-encode
              {"access_token" access-token
               "token_type" "bearer"
               "state" (get query "state")})

             location (format "%s#%s" (:juxt.site/redirect-uri application) fragment)]

         [[:xtdb.api/put access-token-doc]
          [:ring.response/status 303]
          [:ring.response/headers {"location" location}]]))}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]]]}))

(def dependency-graph
  {"https://example.org/applications/{client}"
   {:create #'register-application!
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
   {:create (fn [_]
              (install-authorization-server!
               {:juxt.site/session-scope "https://example.org/session-scopes/default"}))
    :deps #{::init/system
            "https://example.org/session-scopes/default"
            "https://example.org/actions/install-authorization-server"
            "https://example.org/permissions/system/install-authorization-server"
            "https://example.org/actions/oauth/authorize"}}

   ::authorization-server
   {:deps #{"https://example.org/oauth/authorize"}}})

(defn authorize-response!
  "Authorize response"
  [{:juxt.site/keys [session-token]
    client-id "client_id"
    scope "scope"}]
  (let [state (make-nonce 10)
        request {:ring.request/method :get
                 :juxt.site/uri (substitute-actual-base-uri "https://example.org/oauth/authorize")
                 ;; TODO: Instead, use assoc-body
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
        ^{:doc "to authenticate with authorization server"} [:juxt.site/session-token :string]
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
        ^{:doc "to authenticate with authorization server"} [:juxt.site/session-token :string]
        ["client_id" :string]
        ["scope" {:optional true} [:sequential :string]]]]
  [:map
   ["access_token" {:optional true} :string]
   ["error" {:optional true} :string]]])
