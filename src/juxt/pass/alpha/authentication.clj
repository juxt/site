;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.authentication
  (:require
   [jsonista.core :as json]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.pass.alpha :as pass]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.util :refer [hexdigest]]
   [juxt.spin.alpha :as spin]
   [juxt.spin.alpha.auth :refer [decode-authorization]]))

(def GRANT-TYPES #{"client_credentials"})

(defmethod ig/init-key ::resources [_ {:keys [crux-node]}]
  (println "Adding authentication resources")
  (try
    (crux/submit-tx
     crux-node

     ;; Authentication resources - promote these into own ns
     (let [token-endpoint "/_site/token"]
       [[:crux.tx/put
         {:crux.db/id token-endpoint
          ::spin/methods #{:post}
          ::spin/acceptable "application/x-www-form-urlencoded"
          ::pass/expires-in 60}]

        (let [content
              (str
               (json/write-value-as-string
                {"issuer" "https://juxt.site" ; draft
                 "token_endpoint" token-endpoint
                 "token_endpoint_auth_methods_supported" ["client_secret_basic"]
                 "grant_types_supported" (vec GRANT-TYPES)}
                (json/object-mapper
                 {:pretty true}))
               "\r\n")]
          [:crux.tx/put
           {:crux.db/id "/.well-known/openid-configuration"

            ;; OpenID Connect Discovery documents are publically available
            ::pass/classification "PUBLIC"

            ::spin/methods #{:get :head :options}
            ::spin/representations
            [{::spin/content-type "application/json"
              ::spin/last-modified (java.util.Date.)
              ::spin/etag (subs (hexdigest (.getBytes content)) 0 32)
              ::spin/content content}]}])]))

    (catch Exception e
      (prn e))))

(def SECURE-RANDOM (new java.security.SecureRandom))
(def BASE64-ENCODER (java.util.Base64/getUrlEncoder))

(def sessions-by-access-token (atom {}))

(defn access-token []
  (let [bytes (byte-array 24)]
    (.nextBytes SECURE-RANDOM bytes)
    (.encodeToString BASE64-ENCODER bytes)))

;; FIXME: The multimethod here is the wrong level of abstaction. Needs a review.
(defmethod decode-authorization "Bearer" [{:juxt.reap.alpha.rfc7235/keys [token68]}]
  (when-let [session (get @sessions-by-access-token token68)]
    (select-keys session [::pass/user ::pass/username])))

(defn token-response
  [resource date posted-representation subject]

  ;; Check grant_type of posted-representation

  (assert (= "application/x-www-form-urlencoded" (::spin/content-type posted-representation)))

  (let [posted-body (slurp (::spin/bytes posted-representation))

        params (java.net.URLDecoder/decode
                posted-body
                ;; https://tools.ietf.org/html/rfc6749#section-4.4.2 says UTF-8
                "UTF-8")

        ;; TODO: Do a form decode of the bytes (can reap or the jdk provide
        ;; this?)

        ;; TODO: Switch on the grant_type (e.g. client_credentials)

        ;; TODO: Check first that the grant type is supported. This really might
        ;; be a case for a multimethod.

        access-token (access-token)

        expires-in (get resource ::pass/expires-in 3600)

        session {"access_token" access-token
                 "token_type" "example"
                 "expires_in" expires-in
                 ;;"example_parameter" "example_value"
                 }

        _ (swap! sessions-by-access-token
                 assoc access-token
                 (merge
                  session
                  subject
                  {:expiry-date (java.util.Date/from (.plusSeconds (.toInstant date) expires-in))}))

        ;; TODO: Put this access-token in Crux, with its expiry

        body (.getBytes
              (str
               (json/write-value-as-string
                session

                (json/object-mapper {:pretty true}))
               "\r\n"))

        response-representation {::spin/content-type "application/json"
                                 ::spin/content-length (str (count body))}]
    (->
     (spin/response
      200
      (response/representation-metadata-headers response-representation)
      nil nil nil date body)
     (update :headers assoc "Cache-Control" "no-store"))))
