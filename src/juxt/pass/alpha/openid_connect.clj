;; Copyright © 2022, JUXT LTD.

;; References
;;
;; https://openid.net/
;; https://www.scottbrady91.com/openid-connect/identity-tokens
;; https://www.scottbrady91.com/jose/jwts-which-signing-algorithm-should-i-use

(ns juxt.pass.alpha.openid-connect
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [java-http-clj.core :as hc]
   [jsonista.core :as json]

   [juxt.pass.alpha.session :as session]
   [juxt.pass.alpha.authentication :refer [put-session!]]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [juxt.site.alpha.response :refer [redirect]]
   [juxt.site.alpha.return :refer [return]]
   [ring.util.codec :as codec])
  (:import
   (com.auth0.jwk Jwk)
   (com.auth0.jwt JWT)
   (com.auth0.jwt.algorithms Algorithm)
   (com.auth0.jwt.exceptions JWTVerificationException)
   (java.util Date)))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn lookup [id db]
  (crux/entity db id))

(defn login
  "Redirect to an authorization endpoint"
  [{::site/keys [db crux-node resource start-date]
    :ring.request/keys [query]
    :as req}]

  (let [{::pass/keys [oauth-client]} resource
        {::pass/keys [oauth-client-id redirect-uri openid-issuer-id]} (crux/entity db oauth-client)

        query-params (some-> req :ring.request/query (codec/form-decode "US-ASCII"))
        return-to (get query-params "return-to")
        fail-to (get query-params "fail-to")

        _ (when-not oauth-client-id
            (return req 500 "No oauth client id found" {}))
        _ (when-not redirect-uri
            (return req 500 "A :juxt.pass.alpha/redirect entry must be specified" {}))

        openid-configuration (some-> openid-issuer-id (lookup db) ::pass/openid-configuration)
        _ (when-not openid-configuration
            (return req 500 "No openid configuration found" {:openid-configuration-id (some-> resource ::pass/openid-configuration-id)}))

        authorization-endpoint (get openid-configuration "authorization_endpoint")
        _ (when-not authorization-endpoint
            (return req 500 "No authorization endpoint found in OpenID configuration" {:openid-configuration openid-configuration}))

        state (make-nonce 8)
        nonce (make-nonce 12)

        ;; Create a pre-auth session
        session-token-id! (make-nonce 16)
        expires-in (get resource ::pass/expires-in (* 24 3600))
        _ (put-session!
           session-token-id!
           (cond-> {::pass/state state ::pass/nonce nonce}
             return-to (assoc ::pass/return-to return-to)
             fail-to (assoc ::pass/fail-to fail-to))
           (.plusSeconds (.toInstant start-date) expires-in))

        query-string
        (codec/form-encode
         {"response_type" "code"
          "scope" "openid"
          "client_id" oauth-client-id
          "redirect_uri" redirect-uri
          "state" state
          "nonce" nonce
          "prompt" "login"})

        location (format "%s?%s" authorization-endpoint query-string)]

    (-> req
        (redirect 303 location)
        (session/set-cookie session-token-id!))))

(defn find-key [kid jwks]
  (when kid
    (some (fn [m] (when (= kid (get m "kid")) m)) (get jwks "keys"))))

(defn decode-id-token [req jwt jwks openid-configuration oauth-client-id]
  (when jwt
    (let [decoded-jwt (JWT/decode jwt)
          kid (.getKeyId decoded-jwt)
          ky (find-key kid jwks)
          issuer (get openid-configuration "issuer")
          claims (into {}
                       (for [[k v] (.getClaims decoded-jwt)]
                         [k (case k
                              "email_verified" (.asBoolean v)
                              ("iat" "exp" "nbf") (.asDate v)
                              (.asString v))]))]

      ;; https://openid.net/specs/openid-connect-core-1_0.html Section 3.1.3.7
      ;; ID Token Validation

      ;; 2. The Issuer Identifier for the OpenID Provider (which is typically
      ;; obtained during Discovery) MUST exactly match the value of the iss
      ;; (issuer) Claim.

      (when-not (and issuer (= (get claims "iss") issuer))
        (return req 500 "The iss claim (%s) does not match the issuer identifier of the OpenID Provider (%s)"
                {:iss (get claims "iss")
                 :issuer issuer}
                (get claims "iss")
                issuer))

      ;; 3. The Client MUST validate that the aud (audience) Claim contains its
      ;; client_id value registered at the Issuer identified by the iss (issuer)
      ;; Claim as an audience. The aud (audience) Claim MAY contain an array
      ;; with more than one element. The ID Token MUST be rejected if the ID
      ;; Token does not list the Client as a valid audience, or if it contains
      ;; additional audiences not trusted by the Client.

      ;; Also see https://openid.net/specs/openid-connect-core-1_0.html section
      ;; 2, aud MUST be the OAuth 2.0 client_id.
      (when-not (= oauth-client-id (get claims "aud"))
        (return req 500
                "Audience claim must be the OAuth 2.0 client id"
                {:client-id oauth-client-id
                 :aud (get claims "aud")}))

      ;; 6. The Client MUST validate the signature of all other ID Tokens
      ;; according to JWS [JWS] using the algorithm specified in the JWT alg
      ;; Header Parameter. The Client MUST use the keys provided by the Issuer.

      ;; See also https://github.com/auth0/java-jwt

      (when-not (= (get ky "alg") (.getAlgorithm decoded-jwt))
        (return req 500 "Algorithm of JWT (%s) doesn't match algorithm of key (%s)"
                {:kid kid :jwt-alg (.getAlgorithm decoded-jwt) :key-alg (get ky "alg")}
                (.getAlgorithm decoded-jwt)
                (get ky "alg")))

      (case (.getAlgorithm decoded-jwt)
        "RS256"                         ; https://github.com/auth0/jwks-rsa-java
        (let [public-key (.getPublicKey (Jwk/fromValues ky))
              algorithm (Algorithm/RSA256 public-key nil)
              verifier (.. (JWT/require algorithm)
                           (withIssuer issuer)
                           ;; 9. The current time MUST be before the time
                           ;; represented by the exp Claim.
                           (acceptLeeway 1)
                           (build))]
          (try
            (let [verification (.verify verifier jwt)]
              (log/tracef "JWT successfully verified: %s" verification))
            (catch JWTVerificationException e
              (throw (ex-info "" {} e)))))

        ;; TODO: EdDSA, etc.
        ;; See https://www.scottbrady91.com/jose/jwts-which-signing-algorithm-should-i-use

        ;; We definitely do not support 'alg: none' or symmetric algorithms (HS256).
        (return req 500 "No support for algo: %s" {} (.getAlgorithm decoded-jwt)))

      {:verification-status true
       :key-id kid
       :claims claims
       :key ky
       :decoded-jwt decoded-jwt})))

(defn cached-jwks-fetch
  "Fetch JWKS from jwks_uri found in OpenID Connect configuration. Cache in XT for
  a day."
  [{::site/keys [crux-node db] :as req} uri expiry-in-seconds]
  (let [cached (crux/entity db uri)]
    (if (and cached (.after (:expiry cached) (java.util.Date.)))
      (do
        (log/tracef "Returning JWKS from cache")
        (::pass/jwks cached))
      (do
        (log/infof "Fetching JWKS from %s" uri)
        (let [{:keys [status body]}
              (hc/send
               {:method :get
                :uri uri
                :headers {"Accept" "application/json"}})
              _ (when-not (= status 200)
                  (return req 500 "Failed to fetch JWKS from %s" {} uri))
              result
              {:crux.db/id uri
               ::pass/jwks (json/read-value body)
               :expiry (Date/from (.plusSeconds (.toInstant (java.util.Date.)) expiry-in-seconds))}]
          (log/infof "Storing JWKS in database: %s" uri)
          (crux/submit-tx crux-node [[:crux.tx/put result]])
          (::pass/jwks result))))))

(defn match-identity [db id-token-claims]
  (let [identities
        (map first
             (crux/q db '{:find [i]
                          :where [[i :juxt.site.alpha/type "User"]
                                  [i :juxt.pass.jwt/iss iss]
                                  [i :juxt.pass.jwt/sub sub]]
                          :in [iss sub]}
                     (get id-token-claims "iss")
                     (get id-token-claims "sub")))]
    (cond
      (= (count identities) 1) (first identities)

      (> (count identities) 1)
      (do
        (log/warnf "Multiple identities match id-token-claims: %s" (pr-str id-token-claims))
        nil)

      :else nil)))

(defn callback
  "OAuth2 callback"
  [{::site/keys [resource db crux-node]
    ::pass/keys [session session-token-id!]
    :ring.request/keys [query]
    :as req}]

  (when-not session-token-id!
    (return req 500 "No session token id" {}))

  (when-not session
    (return req 500 "No session found" {}))

  ;; Exchange code for JWT
  (let [{::pass/keys [oauth-client]} resource
        {::pass/keys [oauth-client-id oauth-client-secret redirect-uri openid-issuer-id]}
        (crux/entity db oauth-client)

        openid-configuration (some-> openid-issuer-id (lookup db) ::pass/openid-configuration)

        _ (when-not openid-configuration
            (return req 500 "No openid configuration found" {:openid-issuer-id openid-issuer-id}))

        token-endpoint (get openid-configuration "token_endpoint")

        query-params (some-> query (codec/form-decode "US-ASCII"))

        state-received (when (map? query-params) (get query-params "state"))
        state-sent (::pass/state session)

        _ (when-not state-sent
            (return req 500 "Expected to find state in session" {}))

        _ (when-not (= state-sent state-received)
            ;; This could be a CSRF attack, we should log an alert
            (return req 500 "State mismatch" {:state-received state-received
                                              :session session}))

        code (when (map? query-params) (get query-params "code"))

        _ (when-not code
            (return req 500 "No code returned from provider" {}))

        _ (when-not oauth-client-id
            (return req 500 "The required OAuth client id was not found" {}))

        _ (when-not oauth-client-secret
            (return req 500 "The required OAuth client secret was not found" {}))

        jwks-uri (get openid-configuration "jwks_uri")
        _ (when-not jwks-uri
            (return req 500 "No JWKS URI in configuration" {}))

        jwks (cached-jwks-fetch req jwks-uri (* 60 60 24))
        _ (when-not jwks
            (return req 500 "No JWKS found" {}))

        token-request
        {:method :post
         :uri token-endpoint
         :headers {"Content-Type" "application/json" #_"application/x-www-form-urlencoded"
                   "Accept" "application/json"}
         :body (json/write-value-as-string
                {"grant_type" "authorization_code"
                 "client_id" oauth-client-id
                 "client_secret" oauth-client-secret
                 "code" code
                 "redirect_uri" redirect-uri})}

        {:keys [status headers body] :as response}
        (hc/send
         token-request
         {:as :byte-array})

        json-body (json/read-value body)

        id-token (decode-id-token req (get json-body "id_token") jwks openid-configuration oauth-client-id)

        original-nonce (::pass/nonce session)
        claimed-nonce (get-in id-token [:claims "nonce"])

        _ (when-not original-nonce
            (return req 500 "Expected to find nonce in session" {}))

        _ (when-not (=  claimed-nonce original-nonce)
            ;; This is possibly an attack, we should log an alert
            (return req 500 "Nonce received does not match expected" {}))

        ;; Does the id-token match any identities in our database?
        matched-identity (match-identity db (:claims id-token))]

    ;; Put the ID_TOKEN into the session, cycle the session id and redirect to
    ;; the redirect URI stored in the original session.

    (if matched-identity

      (do
        (log/warnf "Successful login! %s" (pr-str (select-keys (:claims id-token) ["iss" "sub"])))
        (-> req
            (redirect 303 (get session ::pass/return-to "/login-succeeded"))
            (session/escalate-session matched-identity)))

      ;; Login unsuccessful.
      (do
        (log/warnf "Unsuccessful login, no known identity match for claims %s" (pr-str (select-keys (:claims id-token) ["iss" "sub"])))
        (redirect req 303 (get session ::pass/fail-to "/login-failed"))))))
