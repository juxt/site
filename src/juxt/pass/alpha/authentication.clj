;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.authentication
  (:require
   [clojure.tools.logging :as log]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]

   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site])
  (:import
   (com.auth0.jwt JWT)))

(defn authenticate
  "If a request has a bearer token in the authorization header according to
  rfc7235, try and decode it as a JWT and add the claims to the request.
  Otherwise simply log the relevent error and return nil. This means there is
  no way to get a 401 back from the KG at present which should be ok because
  Authn is really handled by the API gateway."
  [req]
  (when-let [authorization-header (get-in req [:ring.request/headers "authorization"])]
    (let [{::rfc7235/keys [auth-scheme token68]}
          (reap/authorization authorization-header)]
      (case (.toLowerCase auth-scheme)
        "bearer"
        (try
          (when-let [claims (into {}
                                  (for [[k v] (.getClaims (JWT/decode token68))]
                                    [k (case k
                                         "email_verified" (.asBoolean v)
                                         ("iat" "exp" "nbf") (.asDate v)
                                         (.asString v))]))]
            (log/debug "Valid JWT found" claims)
            {::pass/claims claims})
          (catch Exception e
            (log/error "Invalid JWT" e)))

        (log/error
         "Auth scheme unsupported, must be a bearer token"
         {::site/request-context (assoc req :ring.response/status 401)})))))
