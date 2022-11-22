;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.openid
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]))

(defn create-action-put-openid-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/put-openid-user-identity"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]
        [:juxt.pass.alpha/user [:re "https://example.org/users/.+"]]

        [:juxt.pass.jwt.claims/iss [:re "https://.+"]]
        [:juxt.pass.jwt.claims/sub {:optional true} [:string {:min 1}]]
        [:juxt.pass.jwt.claims/nickname {:optional true} [:string {:min 1}]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            (-> *input*
                (assoc :juxt.site.alpha/type #{"https://meta.juxt.site/pass/user-identity"
                                               "https://meta.juxt.site/pass/openid-user-identity"}
                       :juxt.site.alpha/methods
                       {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user-identity"}}
                        :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-user-identity"}}
                        :options {}}))))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-put-openid-user-identity! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/put-openid-user-identity"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/put-openid-user-identity"
       :juxt.pass.alpha/purpose nil})))))

(defn create-action-install-openid-issuer! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-openid-issuer"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]
        [:juxt.pass.alpha/issuer [:re "https://.*"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            (let [config-uri
                  ;; See https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.4.1
                  ;;
                  ;; "If the Issuer value contains a path component, any terminating / MUST be
                  ;; removed before appending /.well-known/openid-configuration."
                  ;;
                  ;; This uses a reluctant regex qualifier.
                  (str (second (re-matches #"(.*?)/?" (:juxt.pass.alpha/issuer *input*))) "/.well-known/openid-configuration")

                  _ (logf "Config uri %s" config-uri)

                  config-response
                  (java-http-clj.core/send
                   {:method :get
                    :uri config-uri
                    :headers {"Accept" "application/json"}
                    :connect-timeout (java.time.Duration/ofSeconds 2)}
                   {:as :byte-array})

                  _ (logf "Config response status %s" (:status config-response))

                  config (jsonista.core/read-value (:body config-response))]
              {:xt/id (:xt/id *input*)
               :juxt.pass.alpha/issuer (:juxt.pass.alpha/issuer *input*)
               :juxt.pass.alpha/openid-configuration config})))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn create-action-install-openid-client! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-openid-client"

       :juxt.site.alpha.malli/input-schema
        [:map
         [:xt/id [:re "https://example.org/.*"]]
         [:juxt.pass.alpha/issuer-configuration [:re "https://example.org/.*"]]
         [:juxt.pass.alpha/client-id [:string {:min 12}]]
         [:juxt.pass.alpha/client-secret [:string {:min 20}]]
         [:juxt.pass.alpha/redirect-uri [:re "https://example.org/.*"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            *input*))}

       :juxt.site.alpha/transact
       {
        :juxt.site.alpha.sci/program
        (pr-str '[[:xtdb.api/put *prepare*]])}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn create-action-install-openid-login-endpoint! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-openid-login-endpoint"

       :juxt.site.alpha.malli/input-schema
        [:map
         [:xt/id [:re "https://example.org/.*"]]
         [:juxt.pass.alpha/session-scope [:re "https://example.org/.*"]]
         [:juxt.pass.alpha/openid-client-configuration [:re "https://example.org/.*"]]]

       :juxt.site.alpha/transact
       {
        :juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            [[:xtdb.api/put
              (assoc
               *input*
               :juxt.site.alpha/methods
               {:get
                {:juxt.pass.alpha/actions #{"https://example.org/actions/login-with-openid"}}}
               :juxt.http.alpha/content-type "text/html;charset=utf-8"
               :juxt.http.alpha/content "<p>This should redirect</p>")]]))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn create-action-install-openid-callback-endpoint! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-openid-callback-endpoint"

       :juxt.site.alpha.malli/input-schema
        [:map
         [:xt/id [:re "https://example.org/.*"]]
         [:juxt.pass.alpha/openid-client-configuration [:re "https://example.org/.*"]]]

       :juxt.site.alpha/transact
       {
        :juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            [[:xtdb.api/put
              (assoc
               *input*
               :juxt.site.alpha/methods
               {:get
                {:juxt.pass.alpha/actions #{"https://example.org/actions/openid/exchange-code-for-id-token"}}}
               :juxt.http.alpha/content-type "text/html;charset=utf-8"
               :juxt.http.alpha/content "<p>This should redirect</p>")]]))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]})))))

(defn create-action-login-with-openid! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/login-with-openid"

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '{:state (juxt.pass.util/make-nonce 8)
           :nonce (juxt.pass.util/make-nonce 12)
           :session-id (str "https://example.org/sessions/" (juxt.pass.util/make-nonce 16))
           :session-token (juxt.pass.util/make-nonce 16)})}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [openid-client-configuration-id (:juxt.pass.alpha/openid-client-configuration *resource*)
                _ (when-not openid-client-configuration-id
                    (throw
                     (ex-info
                      "No :juxt.pass.alpha/openid-client-configuration on resource"
                      {:resource *resource*})))

                openid-client-configuration (xt/entity openid-client-configuration-id)
                _ (when-not openid-client-configuration
                    (throw
                     (ex-info
                      "No openid-client-configuration doc in database"
                      {:openid-client-configuration openid-client-configuration-id})))

                issuer-config (:juxt.pass.alpha/issuer-configuration openid-client-configuration)
                _ (when-not issuer-config
                    (throw
                     (ex-info
                      "No :juxt.pass.alpha/issuer-configuration on client doc"
                      {:openid-client-configuration openid-client-configuration})))

                issuer-config-doc (xt/entity issuer-config)
                _ (when-not issuer-config-doc
                    (throw
                     (ex-info
                      (format "OpenID configuration document could not be found in database for issuer: %s" issuer-config)
                      {:issuer-config issuer-config})))

                configuration (:juxt.pass.alpha/openid-configuration issuer-config-doc)
                _ (when-not configuration
                    (throw
                     (ex-info
                      "OpenID configuration document does not have a :juxt.pass.alpha/openid-configuration entry"
                      {:issuer-config-document issuer-config-doc})))

                authorization-endpoint (get configuration "authorization_endpoint")
                _ (when-not authorization-endpoint
                    (throw (ex-info "No authorization_endpoint entry in OpenID configuration"
                                    {:issuer-config issuer-config
                                     :configuration-keys (keys configuration)})))

                state (:state *prepare*)
                _ (when-not state
                    (throw (ex-info "Prepare step should have prepared a random 'state' value" {})))

                nonce (:nonce *prepare*)
                _ (when-not nonce
                    (throw (ex-info "Prepare step should have prepared a random 'nonce' value" {})))

                session-id (:session-id *prepare*)
                _ (when-not session-id
                    (throw (ex-info "Prepare step should have prepared a random 'session-id' value" {})))

                query-params (some-> *ctx* :ring.request/query ring.util.codec/form-decode)

                return-to (get query-params "return-to")

                session
                (cond-> {:xt/id session-id
                         :juxt.site.alpha/type "https://meta.juxt.site/pass/session"
                         :juxt.pass.alpha/state state
                         :juxt.pass.alpha/nonce nonce}
                  return-to (assoc :juxt.pass.alpha/return-to return-to))

                session-token (:session-token *prepare*)
                _ (when-not session-token
                    (throw (ex-info "Prepare step should have prepared a random 'session-token' value" {})))

                session-token-id (str "https://example.org/session-tokens/" session-token)

                session-token-doc
                {:xt/id session-token-id
                 :juxt.site.alpha/type "https://meta.juxt.site/pass/session-token"
                 :juxt.pass.alpha/session-token session-token
                 :juxt.pass.alpha/session (:xt/id session)}

                client-id (:juxt.pass.alpha/client-id openid-client-configuration)

                redirect-uri (:juxt.pass.alpha/redirect-uri openid-client-configuration)
                _ (when-not redirect-uri
                    (throw
                     (ex-info
                      "Login resource should be configured with a :juxt.pass.alpha/redirect-uri entry containing the URI of the callback resource."
                      {:resource *resource*})))

                query-string
                (ring.util.codec/form-encode
                 {"response_type" "code"
                  "scope" "openid name picture profile email" ; TODO: configure in the XT entity
                  "client_id" client-id
                  "redirect_uri" redirect-uri
                  "state" state
                  "nonce" nonce
                  "connection" "github"})

                location (str authorization-endpoint "?" query-string)

                session-scope (:juxt.pass.alpha/session-scope *resource*)
                _ (when-not session-scope
                    (throw
                     (ex-info "No :juxt.pass.alpha/session-scope on resource"
                              {:resource *resource*})))

                session-scope-doc (xt/entity session-scope)
                _ (when-not session-scope-doc
                    (throw
                     (ex-info
                      "No session-scope entity in database"
                      {:juxt.pass.alpha/session-scope session-scope})))

                cookie-name (:juxt.pass.alpha/cookie-name session-scope-doc)
                _ (when-not cookie-name
                    (throw
                     (ex-info
                      "No :juxt.pass.alpha/cookie-name found in session-scope"
                      {:juxt.pass.alpha/session-scope session-scope-doc})))

                cookie-path (or (:juxt.pass.alpha/cookie-path session-scope-doc) "/")]

            ;; Pretty much all of this can be computed in the prepare phase,
            ;; unless we decide to honor an existing session.

            [[:xtdb.api/put session]
             [:xtdb.api/put session-token-doc]
             [:ring.response/status 303]
             [:ring.response/headers
              {"location" location
               "set-cookie"
               (format "%s=%s; Path=%s; Secure; HttpOnly; SameSite=Lax"
                       cookie-name
                       session-token
                       cookie-path)}]]))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]
       })))))

(defn create-action-exchange-code-for-id-token! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"

      {:xt/id "https://example.org/actions/openid/exchange-code-for-id-token"

       ;; We can process the request outside of the transaction, since the query
       ;; parameters are indepedent of the database.
       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [query-params (some-> *ctx* :ring.request/query (ring.util.codec/form-decode "US-ASCII"))
                _ (when-not query-params
                    (throw (ex-info "No query params" {})))

                received-state (when (map? query-params) (get query-params "state"))
                _ (when-not received-state
                    (throw
                     (ex-info
                      "No state in query params"
                      {})))

                code (when (map? query-params) (get query-params "code"))
                _ (when-not code
                    (throw
                     (ex-info "No code in query params" {})))

                openid-client-configuration-id (:juxt.pass.alpha/openid-client-configuration *resource*)
                _ (when-not openid-client-configuration-id
                    (throw
                     (ex-info
                      "Resource does not have a non-nil :juxt.pass.alpha/openid-client entry"
                      {:resource *resource*})))

                ;; The use of the asterisk as a suffix is meant to indicate that
                ;; this data may be stale, and shouldn't be used to make
                ;; decisions. However, in this case, we need to make an HTTP
                ;; request and this cannot be done in a transaction
                ;; function. The returned ID_TOKEN will be checked and verified
                ;; in the transaction function.
                {:juxt.pass.alpha/keys [client-id client-secret redirect-uri]
                 :as openid-client-configuration*}
                (xt/entity* openid-client-configuration-id)
                _ (when-not openid-client-configuration*
                    (throw
                     (ex-info
                      "OpenID client document not found in database"
                      {:openid-client-configuration-id openid-client-configuration-id})))

                issuer-config-id* (:juxt.pass.alpha/issuer-configuration openid-client-configuration*)
                _ (when-not issuer-config-id*
                    (throw (ex-info "No issuer config in client" {})))

                issuer-configuration* (xt/entity* issuer-config-id*)
                _ (when-not issuer-configuration*
                    (throw (ex-info "Issuer configuation document not found in database"
                                    {:issuer-config-id issuer-config-id*})))

                openid-configuration* (:juxt.pass.alpha/openid-configuration issuer-configuration*)
                _ (when-not openid-configuration*
                    (throw
                     (ex-info
                      "OpenID configuration has not yet been fetched"
                      {:issuer-configuration issuer-configuration*})))

                token-endpoint* (get openid-configuration* "token_endpoint")
                _ (when-not token-endpoint*
                    (throw (ex-info "No token_endpoint found in configuration" {})))

                ;; TODO: Promote this, e.g. (juxt.pass/get-token {:uri token-endpoint :grant-type "authorization_code" ...}) => id-token
                token-response
                (java-http-clj.core/send
                 {:method :post
                  :uri token-endpoint*
                  :headers {"Content-Type" "application/json" #_"application/x-www-form-urlencoded"
                            "Accept" "application/json"}
                  :body (jsonista.core/write-value-as-string
                         {"grant_type" "authorization_code"
                          "client_id" client-id
                          "client_secret" client-secret
                          "code" code
                          "redirect_uri" redirect-uri})}
                 {:as :byte-array})

                ;; The id_token is a JWT embedded in the JSON object returned in
                ;; the response body .
                json (jsonista.core/read-value (:body token-response))
                encoded-id-token (get json "id_token")

                _ (when-not encoded-id-token
                    (throw (ex-info "ID_TOKEN not returned in JSON response" {:json-response json})))

                ;; The subject-id will be randomized, so we can't generate this
                ;; in the transaction function (since different nodes would
                ;; almost certainly generate different values!)
                subject-id (juxt.pass.util/make-nonce 10)]

            ;; We send the issuer and the encoded-id-token. Although the client
            ;; configuration may still change prior to this transaction reaching
            ;; the head of the queue, the id token is still valid with respect
            ;; to the issuer. We include the issuer to allow the transaction to
            ;; check it hasn't changed (if it has, the id-token must be
            ;; reacquired from the new issuer). Note that while this might
            ;; appear to be an unreasonable about of caution to preserve strict
            ;; serializability of data.
            {:issuer (:juxt.pass.alpha/issuer issuer-configuration*)
             :encoded-id-token encoded-id-token
             :received-state received-state
             :subject-id subject-id
             :new-session-token (juxt.pass.util/make-nonce 16)}))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str

         '(let [{issuer :issuer
                 encoded-id-token :encoded-id-token
                 received-state :received-state
                 subject-id :subject-id
                 new-session-token :new-session-token}
                *prepare*

                openid-client-configuration-id (:juxt.pass.alpha/openid-client-configuration *resource*)
                openid-client-configuration (xt/entity openid-client-configuration-id)
                client-id (:juxt.pass.alpha/client-id openid-client-configuration)
                issuer-config-id (:juxt.pass.alpha/issuer-configuration openid-client-configuration)
                issuer-configuration (xt/entity issuer-config-id)
                openid-configuration (:juxt.pass.alpha/openid-configuration issuer-configuration)

                id-token
                (juxt.pass/decode-id-token
                 {:id-token encoded-id-token
                  :jwks (:juxt.pass.alpha/jwks issuer-configuration)
                  :openid-configuration openid-configuration
                  :client-id client-id})

                ;; Arguably do the nonce check below in the validation of the
                ;; ID_TOKEN.  Pass in the session if necessary.

                session (:juxt.pass.alpha/session *ctx*)
                _ (when-not session
                    (throw (ex-info "No session in request context" {})))

                expected-state (:juxt.pass.alpha/state session)
                _ (when-not expected-state
                    (throw (ex-info "No state stored in session" {})))

                ;; Check the states match
                _ (when-not (= expected-state received-state)
                    ;; This could be a CSRF attack, we should log an alert
                    (throw
                     (ex-info
                      (format "State mismatch (received: %s, expected: %s, session: %s)" received-state expected-state (pr-str session))
                      {:received-state received-state
                       :expected-state expected-state})))

                session-nonce (:juxt.pass.alpha/nonce session)
                _ (when-not session-nonce
                    (throw (ex-info "Expected to find nonce in session" {:session session})))

                claimed-nonce (get-in id-token [:claims "nonce"])
                _ (when-not claimed-nonce
                    (throw (ex-info "Expected to find nonce claim in ID_TOKEN" {})))

                ;; TODO: This really ought to be promoted to library code
                _ (when-not (= claimed-nonce session-nonce)
                    ;; TODO: This is possibly an attack, we should log an alert
                    (throw
                     (ex-info
                      "Nonce received in ID_TOKEN claims does not match the original one sent!"
                      {:claimed-nonce claimed-nonce
                       :session-nonce session-nonce})))

                extract-standard-claims
                (fn [claims]
                  (let [standard-claims
                        ["iss" "sub" "aud" "exp" "iat" "auth_time" "nonce" "acr" "amr" "azp"
                         "name" "given_name" "family_name" "middle_name" "nickname" "preferred_username"
                         "profile" "picture" "website" "email" "email_verified" "gender" "birthdate"
                         "zoneinfo" "locale" "phone_number" "phone_number_verified" "address" "updated_at"]]
                    (->>
                     (for [c standard-claims
                           :let [v (get claims c)]
                           :when v]
                       ;; See https://www.rfc-editor.org/rfc/rfc7519#section-4
                       [(keyword "juxt.pass.jwt.claims" (clojure.string/replace c "_" "-")) v])
                     (into {}))))

                claims (extract-standard-claims (:claims id-token))

                user-identity
                (juxt.pass/match-identity
                 {:juxt.pass.jwt.claims/iss (get claims :juxt.pass.jwt.claims/iss)
                  :juxt.pass.jwt.claims/nickname (get claims :juxt.pass.jwt.claims/nickname)})

                issued-date (get-in id-token [:claims "iat"])
                expiry-date (get-in id-token [:claims "exp"]) ;;(java.util.Date/from (.plusSeconds (java.time.Instant/now) 30)) ;;

                subject
                (when user-identity
                  (into
                   {:xt/id (str "https://example.org/subjects/" subject-id)
                    :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
                    :juxt.pass.alpha/id-token-claims (:claims id-token)
                    :juxt.pass.alpha/user-identity user-identity
                    :juxt.pass.alpha/issued-date issued-date
                    :juxt.pass.alpha/expiry-date expiry-date}
                   claims))

                new-session-token-doc
                (when subject
                  {:xt/id (str "https://example.org/session-tokens/" new-session-token)
                   :juxt.site.alpha/type "https://meta.juxt.site/pass/session-token"
                   :juxt.pass.alpha/session-token new-session-token
                   :juxt.pass.alpha/session (:xt/id session)})]

            (cond-> []
              subject
              (conj
               [:xtdb.api/put subject
                (get-in subject [:juxt.pass.alpha/id-token-claims "iat"])
                ;; TODO: Expire the subject in the bitemporal timeline according
                ;; to the 'exp' (expiry) in the claims. This has the added
                ;; desirable effect of keeping the database free from clutter.
                (get-in subject [:juxt.pass.alpha/id-token-claims "exp"])]

               ;; Update session with subject
               [:xtdb.api/put (assoc session :juxt.pass.alpha/subject (:xt/id subject))]

               ;; Escalate session (as recommended by OWASP as this session has
               ;; been promoted)
               [:xtdb.api/put new-session-token-doc]

               ;; TODO: Original return-to query param should be in session -
               ;; use it here.
               ;; [:ring.response/status 303]

               [:ring.response/headers
                (let [session-scope (:juxt.pass.alpha/session-scope *ctx*)
                      _ (when-not session-scope
                          (throw
                           (ex-info
                            "No :juxt.pass.alpha/session-scope attached to context"
                            {})))

                      cookie-name (:juxt.pass.alpha/cookie-name session-scope)
                      _ (when-not cookie-name
                          (throw
                           (ex-info
                            "No :juxt.pass.alpha/cookie-name found in session-scope"
                            {:juxt.pass.alpha/session-scope session-scope})))

                      cookie-path (or (:juxt.pass.alpha/cookie-path session-scope) "/")]
                  { ;;"location" return-to
                   "set-cookie"
                   (format "%s=%s; Path=%s; Secure; HttpOnly; SameSite=Lax"
                           cookie-name
                           new-session-token
                           cookie-path)})]))))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn create-action-fetch-jwks! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"

      {:xt/id "https://example.org/actions/openid/fetch-jwks"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(do
            (juxt.site.malli/validate-input)
            (let [issuer (:xt/id *input*)
                  issuer-doc (xt/entity* issuer)
                  _ (when-not issuer-doc
                      (throw (ex-info "Issuer not installed in database" {:issuer (:juxt.pass.alpha/issuer *input*)})))
                  jwks-uri (get-in issuer-doc [:juxt.pass.alpha/openid-configuration "jwks_uri"])
                  _ (when-not jwks-uri
                      (if (nil? (:juxt.pass.alpha/openid-configuration issuer-doc))
                        (throw
                         (ex-info
                          "The entry :juxt.pass.alpha/openid-configuration is missing from the issuer entity"
                          {:issuer issuer-doc}))
                        (throw
                         (ex-info
                          "jwks_uri not found in :juxt.pass.alpha/openid-configuration entry of issuer entity"
                          {:issuer issuer-doc
                           :openid-configuration (:juxt.pass.alpha/openid-configuration issuer-doc)}))))

                  get-jwks-response
                  (java-http-clj.core/send
                   {:method :get
                    :uri jwks-uri
                    :headers {"Accept" "application/json"}}
                   {:as :byte-array})

                  _ (when-not (= 200 (:status get-jwks-response))
                      (throw (ex-info "Failed to get JWKS from issuer" {:issuer issuer})))

                  jwks (jsonista.core/read-value (:body get-jwks-response))]
              {:jwks-uri jwks-uri
               :juxt.pass.alpha/jwks jwks})))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [issuer-doc (xt/entity (:xt/id *input*))
                jwks-uri (get-in issuer-doc [:juxt.pass.alpha/openid-configuration "jwks_uri"])]
            (cond-> []
              (= jwks-uri (:jwks-uri *prepare*))
              (conj [:xtdb.api/put (assoc issuer-doc :juxt.pass.alpha/jwks (:juxt.pass.alpha/jwks *prepare*))]))))}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn grant-permission-to-install-openid-issuer! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-openid-issuer"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-openid-issuer"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-install-openid-client! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-openid-client"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-openid-client"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-install-openid-login-endpoint! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-openid-login-endpoint"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-openid-login-endpoint"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-install-openid-callback-endpoint! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-openid-callback-endpoint"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-openid-callback-endpoint"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-fetch-jwks! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/fetch-jwks"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/openid/fetch-jwks"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-invoke-action-login-with-openid! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/login-with-openid"
       :juxt.pass.alpha/action "https://example.org/actions/login-with-openid"
       :juxt.pass.alpha/purpose nil})))))

(defn grant-permission-to-invoke-action-exchange-code-for-id-token! [{:keys [id]}]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/openid/exchange-code-for-id-token"
       :juxt.pass.alpha/action "https://example.org/actions/openid/exchange-code-for-id-token"
       :juxt.pass.alpha/purpose nil})))))

(defn install-openid-issuer! [m]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-openid-issuer"
      ~m))))

(defn fetch-jwks!
  [id]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/openid/fetch-jwks"
      {:xt/id ~id}))))

(defn install-openid-client [m]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-openid-client"
      ~m))))

(defn install-openid-login-endpoint!
  [m]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-openid-login-endpoint"
      ~m))))

(defn install-session-scope! [m]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/put-session-scope"
      ~m))))

(defn install-openid-callback-endpoint!
  [m]
  (eval
   (substitute-actual-base-uri
    `(juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/install-openid-callback-endpoint"
      ~m))))

(defn openid-config []
  (-> "user.home"
      System/getProperty
      (io/file ".config/site/openid-client.edn")
      slurp
      edn/read-string))

(def dependency-graph
  {
   "https://example.org/actions/put-openid-user-identity"
   {:create #'create-action-put-openid-user-identity!
    :deps #{::init/system}}

   "https://example.org/permissions/system/put-openid-user-identity"
   {:create #'grant-permission-to-invoke-action-put-openid-user-identity!
    :deps #{::init/system}}

   "https://example.org/actions/install-openid-issuer"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-install-openid-issuer!}

   "https://example.org/actions/install-openid-client"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-install-openid-client!}

   ;; TODO: Rename https://example.org/actions/openid/login ?
   "https://example.org/actions/login-with-openid"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-login-with-openid!}

   "https://example.org/actions/install-openid-login-endpoint"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-install-openid-login-endpoint!}

   "https://example.org/actions/openid/exchange-code-for-id-token"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-exchange-code-for-id-token!}

   "https://example.org/actions/install-openid-callback-endpoint"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-install-openid-callback-endpoint!}

   "https://example.org/actions/openid/fetch-jwks"
   {:deps #{:juxt.site.alpha.init/system}
    :create #'create-action-fetch-jwks!}

   ;; Simple system grants

   "https://example.org/permissions/system/install-openid-issuer"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/install-openid-issuer"}
    :create #'grant-permission-to-install-openid-issuer!}

   "https://example.org/permissions/system/install-openid-client"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/install-openid-client"}
    :create #'grant-permission-to-install-openid-client!}

   "https://example.org/permissions/system/install-openid-login-endpoint"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/install-openid-login-endpoint"}
    :create #'grant-permission-to-install-openid-login-endpoint!}

   "https://example.org/permissions/system/install-openid-callback-endpoint"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/install-openid-callback-endpoint"}
    :create #'grant-permission-to-install-openid-callback-endpoint!}

   "https://example.org/permissions/system/fetch-jwks"
   {:deps #{"https://example.org/actions/openid/fetch-jwks"}
    :create #'grant-permission-to-fetch-jwks!}

   ;; Anyone can login/exchange

   "https://example.org/permissions/login-with-openid"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/login-with-openid"}
    :create #'grant-permission-to-invoke-action-login-with-openid!}

   "https://example.org/permissions/openid/exchange-code-for-id-token"
   {:deps #{:juxt.site.alpha.init/system
            "https://example.org/actions/openid/exchange-code-for-id-token"}
    :create #'grant-permission-to-invoke-action-exchange-code-for-id-token!}

   ;; Possible endpoints

   ::all-actions
   {:deps #{"https://example.org/actions/install-openid-issuer"
            "https://example.org/permissions/system/fetch-jwks"
            "https://example.org/actions/install-openid-client"
            "https://example.org/actions/login-with-openid"
            "https://example.org/actions/install-openid-login-endpoint"
            "https://example.org/actions/openid/exchange-code-for-id-token"
            "https://example.org/actions/install-openid-callback-endpoint"}}

   ::default-permissions
   {:deps #{"https://example.org/permissions/system/install-openid-issuer"
            "https://example.org/permissions/system/fetch-jwks"
            "https://example.org/permissions/system/install-openid-client"
            "https://example.org/permissions/system/install-openid-login-endpoint"
            "https://example.org/permissions/system/install-openid-callback-endpoint"
            "https://example.org/permissions/login-with-openid"
            "https://example.org/permissions/openid/exchange-code-for-id-token"}}

   "https://example.org/openid/auth0/issuer"
   {:deps #{"https://example.org/permissions/system/install-openid-issuer"}
    :create (fn [{:keys [id]}]
              (let [{issuer :juxt.pass.alpha/issuer} (openid-config)]
                (install-openid-issuer!
                 {:xt/id id
                  :juxt.pass.alpha/issuer issuer})
                (fetch-jwks! id)))}

   "https://example.org/openid/auth0/client-configuration"
   {:deps #{"https://example.org/openid/auth0/issuer"}
    :create (fn [{:keys [id]}]
              (install-openid-client
               (merge
                {:xt/id id
                 :juxt.pass.alpha/issuer-configuration "https://example.org/openid/auth0/issuer"}
                (select-keys
                 (openid-config)
                 [:juxt.pass.alpha/client-id
                  :juxt.pass.alpha/client-secret
                  :juxt.pass.alpha/redirect-uri]))))}

   "https://example.org/openid/login"
   {:deps #{"https://example.org/openid/callback"
            "https://example.org/openid/auth0/client-configuration"
            "https://example.org/permissions/system/install-openid-login-endpoint"}
    :create (fn [{:keys [id]}]
              (install-openid-login-endpoint!
               {:xt/id id
                :juxt.pass.alpha/openid-client-configuration "https://example.org/openid/auth0/client-configuration"
                :juxt.pass.alpha/session-scope "https://example.org/session-scopes/openid"}))}

   "https://example.org/openid/callback"
   {:deps #{"https://example.org/session-scopes/openid"}
    :create (fn [{:keys [id]}]
              (install-openid-callback-endpoint!
               {:xt/id id
                :juxt.pass.alpha/openid-client-configuration "https://example.org/openid/auth0/client-configuration"}))}

   "https://example.org/session-scopes/openid"
   {:deps #{"https://example.org/permissions/system/put-session-scope"}
    :create (fn [{:keys [id]}]
              (install-session-scope!
               {:xt/id id
                :juxt.pass.alpha/cookie-name "sid"
                :juxt.pass.alpha/cookie-domain "https://example.org"
                :juxt.pass.alpha/cookie-path "/"
                :juxt.pass.alpha/login-uri "https://example.org/openid/login"}))}})

(defn put-openid-user-identity! [& {:keys [username]
                                    :juxt.pass.jwt.claims/keys [iss sub nickname]}]
  (init/do-action
   (substitute-actual-base-uri "https://example.org/subjects/system")
   (substitute-actual-base-uri "https://example.org/actions/put-openid-user-identity")
   (substitute-actual-base-uri
    (cond-> {:xt/id (format "https://example.org/user-identities/%s/openid" (str/lower-case username))
             :juxt.pass.alpha/user ~(format "https://example.org/users/%s" (str/lower-case username))
             :juxt.pass.jwt.claims/iss iss}
      sub (assoc :juxt.pass.jwt.claims/sub sub)
      nickname (assoc :juxt.pass.jwt.claims/nickname nickname)))))
