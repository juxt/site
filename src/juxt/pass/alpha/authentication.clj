;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.authentication
  (:require
   [jsonista.core :as json]
   [clojure.tools.logging :as log]
   [crypto.password.bcrypt :as password]
   [crux.api :as crux]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]
   [juxt.spin.alpha :as spin]
   [ring.util.codec :refer [form-decode]]
   [ring.middleware.cookies :refer [cookies-request cookies-response]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(def SECURE-RANDOM (new java.security.SecureRandom))
(def BASE64-ENCODER (java.util.Base64/getUrlEncoder))

(defn access-token []
  (let [bytes (byte-array 24)]
    (.nextBytes SECURE-RANDOM bytes)
    (.encodeToString BASE64-ENCODER bytes)))

(defonce sessions-by-access-token (atom {}))

(defn put-session! [k session ^java.time.Instant expiry-instant]
  (swap! sessions-by-access-token
         assoc k (assoc session
                        ::expiry-instant expiry-instant)))

(defn lookup-session [k date-now]
  (when-let [{::keys [expiry-instant] :as session} (get @sessions-by-access-token k)]
    (if (.isBefore (.toInstant date-now) expiry-instant)
      session
      (do
        (swap! sessions-by-access-token dissoc sessions-by-access-token k)
        nil))))

(defn token-response
  [resource date posted-representation subject]

  ;; Check grant_type of posted-representation

  (assert (= "application/x-www-form-urlencoded" (::http/content-type posted-representation)))

  (let [posted-body (slurp (::http/body posted-representation))

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

        _ (put-session!
           access-token
           (merge session subject)
           (.plusSeconds (.toInstant date) expires-in))

        body (.getBytes
              (str
               (json/write-value-as-string
                session
                (json/object-mapper {:pretty true}))
               "\r\n"))]
    (->
     (spin/response
      200
      {::http/content-type "application/json"
       ::http/content-length (str (count body))}
      nil nil date body)
     (update :headers assoc "Cache-Control" "no-store"))))

(defn login-response
  [resource date posted-representation db]

  ;; Check grant_type of posted-representation
  (assert (= "application/x-www-form-urlencoded" (::http/content-type posted-representation)))

  (let [posted-body (String. (::http/body posted-representation))
        {:strs [user password]} (form-decode posted-body)
        uid (format "https://home.juxt.site/_site/users/%s" user)]

    (or
     (when-let [e (crux/entity db uid)]
       (when (password/check password (::pass/password-hash!! e))
         (let [access-token (access-token)
               expires-in (get resource ::pass/expires-in 3600)

               session {"access_token" access-token
                        "token_type" "login"
                        "expires_in" expires-in}

               _ (put-session!
                  access-token
                  (merge session {::pass/user uid
                                  ::pass/username user})
                  (.plusSeconds (.toInstant date) expires-in))]

           (->
            (spin/response
             302
             {::http/content-type "text/plain"}
             nil nil date (.getBytes (format "Thanks! Your access token is %s\r\n" access-token)))
            (update :headers assoc
                    "cache-control" "no-store"
                    "location" (format "/~%s/" user))
            (assoc :cookies {"access_token"
                             {:value access-token
                              :max-age expires-in
                              :same-site :strict
                              :http-only true
                              :path "/"}})
            (cookies-response)))))
     ;; else not user
     (throw
      (ex-info
       "Failed to login"
       {::spin/response
        {:status 302
         :headers {"location" "/"}}})))))

(defn logout-response
  [resource date posted-representation db]

  (->
   (spin/response
    302
    {::http/content-type "text/plain"}
    nil nil date (.getBytes (format "Logged out %s\r\n" access-token)))
   (update :headers assoc
           "cache-control" "no-store"
           "location" "/")
   (assoc :cookies {"access_token"
                    {:value  ""
                     :max-age 0
                     :same-site :strict
                     :http-only true
                     :path "/"}})
   (cookies-response)))

(defn authenticate
  "Authenticate a request. Return a pass subject, with information about user,
  roles and other credentials. The resource can be used to determine the
  particular Protection Space that it is part of, and the appropriate
  authentication scheme(s) for accessing the resource."
  [request resource date db]
  ;; TODO: This might be where we also add the 'on-behalf-of' info
  (let [access-token (some-> request cookies-request :cookies (get "access_token") :value)]
    (or
     ;; Cookie
     (when access-token
       (when-let [session (lookup-session access-token date)]
         (select-keys session [::pass/user ::pass/username])))

     ;; Authorization header
     (when-let [authorization-header (get-in request [:headers "authorization"])]
       (let [{::rfc7235/keys [auth-scheme token68 auth-params]}
             (reap/authorization authorization-header)]

         (case auth-scheme
           "Basic"
           (try
             (let [[_ user password]
                   (re-matches
                    #"([^:]*):([^:]*)"
                    (String. (.decode (java.util.Base64/getDecoder) token68)))
                   uid (format "https://home.juxt.site/_site/users/%s" user)]
               (when-let [e (crux/entity db uid)]
                 (when (password/check password (::pass/password-hash!! e))
                   {::pass/user uid
                    ::pass/username user})))
             (catch Exception e
               (log/error e)))

           "Bearer"
           (when-let [session (lookup-session token68 date)]
             (select-keys session [::pass/user ::pass/username]))

           (throw
            (ex-info
             "Unauthorized"
             {::spin/response
              {:status 401
               :headers {}
               :body "Unauthorized\r\n"}}))))))))
