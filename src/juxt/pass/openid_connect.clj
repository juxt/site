;; Copyright © 2022, JUXT LTD.

;; References
;;
;; https://openid.net/
;; https://www.scottbrady91.com/openid-connect/identity-tokens
;; https://www.scottbrady91.com/jose/jwts-which-signing-algorithm-should-i-use

(ns juxt.pass.openid-connect
  (:require
   [clojure.tools.logging :as log]
   [juxt.site :as-alias site]
   [juxt.pass :as-alias pass]
   [xtdb.api :as-alias xt])
  (:import
   (com.auth0.jwt.exceptions JWTVerificationException)
   (com.auth0.jwt JWT)
   (com.auth0.jwt.algorithms Algorithm)
   (com.auth0.jwk Jwk)))

(defn find-key [kid jwks]
  (when kid
    (some (fn [m] (when (= kid (get m "kid")) m)) (get jwks "keys"))))

(defn decode-id-token [{:keys [id-token jwks openid-configuration client-id]}]
  ;; TODO: Use Malli to validate arguments? This may be called from untrusted
  ;; user-provided code
  (assert id-token)
  (assert jwks)
  (assert openid-configuration)
  (assert client-id)

  (let [decoded-jwt (JWT/decode id-token)
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
      (throw
       (ex-info
        (format
         "The iss claim (%s) does not match the issuer identifier of the OpenID Provider (%s)"
         (get claims "iss")
         issuer)
        {:iss (get claims "iss")
         :issuer issuer})))

    ;; 3. The Client MUST validate that the aud (audience) Claim contains its
    ;; client_id value registered at the Issuer identified by the iss (issuer)
    ;; Claim as an audience. The aud (audience) Claim MAY contain an array
    ;; with more than one element. The ID Token MUST be rejected if the ID
    ;; Token does not list the Client as a valid audience, or if it contains
    ;; additional audiences not trusted by the Client.

    ;; Also see https://openid.net/specs/openid-connect-core-1_0.html section
    ;; 2, aud MUST be the OAuth 2.0 client_id.
    (when-not (= client-id (get claims "aud"))
      (throw
       (ex-info
        "Audience claim must be the OAuth 2.0 client id"
        {:client-id client-id
         :aud (get claims "aud")})))

    ;; 6. The Client MUST validate the signature of all other ID Tokens
    ;; according to JWS [JWS] using the algorithm specified in the JWT alg
    ;; Header Parameter. The Client MUST use the keys provided by the Issuer.

    ;; See also https://github.com/auth0/java-jwt

    (when-not (= (get ky "alg") (.getAlgorithm decoded-jwt))
      (throw (ex-info
              (format
               "Algorithm of JWT (%s) doesn't match algorithm of key (%s)"
               (.getAlgorithm decoded-jwt)
               (get ky "alg"))
              {:kid kid :jwt-alg (.getAlgorithm decoded-jwt) :key-alg (get ky "alg") })))

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
          (let [verification (.verify verifier id-token)]
            (log/debugf "JWT successfully verified: %s" verification))
          (catch JWTVerificationException e
            (throw (ex-info "" {} e)))))

      ;; TODO: EdDSA, etc.
      ;; See https://www.scottbrady91.com/jose/jwts-which-signing-algorithm-should-i-use

      ;; We definitely do not support 'alg: none' or symmetric algorithms (HS256).
      (throw
       (ex-info (format "No support for algo: %s" (.getAlgorithm decoded-jwt)) {})))

    {:verification-status true
     :key-id kid
     :claims claims
     :key ky
     :decoded-jwt decoded-jwt}))
