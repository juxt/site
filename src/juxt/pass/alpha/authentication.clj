;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.authentication
  (:require
   [jsonista.core :as json]
   [clojure.tools.logging :as log]
   [crypto.password.bcrypt :as password]
   [crux.api :as x]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]
   [ring.util.codec :refer [form-decode]]
   [ring.middleware.cookies :refer [cookies-request cookies-response]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

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

(defn expire-sessions! [date-now]
  (swap! sessions-by-access-token
         (fn [sessions]
           (into {} (remove #(.isAfter (.toInstant date-now)
                                       (-> % second (get ::expiry-instant)))
                            sessions)))))

(defn lookup-session [k date-now]
  (expire-sessions! date-now)
  (get @sessions-by-access-token k))

;;(identity @sessions-by-access-token)

(defn token-response
  [{::site/keys [received-representation resource start-date]
    ::pass/keys [subject] :as req}]

  ;; Check grant_type of posted-representation
  (assert (.startsWith (::http/content-type received-representation)
             "application/x-www-form-urlencoded"))

  (when-not subject
    (throw
     (ex-info "Unauthorized"
              (into req {:ring.response/status 401
                         :ring.response/body "Unauthorized\r\n"}))))

  (let [posted-body (slurp (::http/body received-representation))

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

        expires-in (get resource ::pass/expires-in (* 24 3600))

        session {"access_token" access-token
                 "expires_in" expires-in
                 "user" (::pass/user subject)}

        _ (put-session!
           access-token
           (merge session subject)
           (.plusSeconds (.toInstant start-date) expires-in))

        body (.getBytes
              (str
               (json/write-value-as-string
                session
                (json/object-mapper {:pretty true}))
               (System/getProperty "line.separator")))]
    (-> req
        (into {:ring.response/status 200
               :ring.response/body body
               ::site/selected-representation
               {::http/content-type "application/json"
                ::http/content-length (count body)}})
        (update :ring.response/headers assoc "Cache-Control" "no-store"))))

(defn lookup-user
  "Return a vector of user, pwhash"
  [db username]
  (when-not (re-matches #"[\p{Alnum}-_]+" username)
    (throw (ex-info "Username not valid format" {})))

  (let [users (x/q db '{:find [r pwhash]
                        :where [[r ::site/type "User"]
                                [r ::pass/username username]
                                [pe ::pass/user r]
                                [pe ::site/type "Password"]
                                [pe ::pass/password-hash pwhash]]
                        :in [username]}
                   username)]
    (cond
      (> (count users) 1)
      (throw (ex-info (format "Multiple users found with username %s" username) {:users users})))

    (first users)))

(defn login-response
  [{::site/keys [received-representation db resource start-date]
    :ring.request/keys [query]
    :as req}]

  ;; Check grant_type of posted-representation
  (assert (.startsWith (::http/content-type received-representation)
             "application/x-www-form-urlencoded"))

  (let [posted-body (String. (::http/body received-representation))
        form (form-decode posted-body)
        username (get form "user")
        [user pwhash] (lookup-user db username)
        password (get form "password")]
    (or
     (when (and password pwhash (password/check password pwhash))
       (let [access-token (access-token)
             expires-in (get resource ::pass/expires-in 3600)
             session {"access_token" access-token
                      "token_type" "login"
                      "expires_in" expires-in}]
         (put-session!
          access-token
          (merge session {::pass/user user
                          ::pass/username username})
          (.plusSeconds (.toInstant start-date) expires-in))
         (-> req
             (assoc :ring.response/status 302
                    :ring.response/body
                    (format "Thanks! Your access token is %s\r\n" access-token)
                    ::http/content-type "text/plain")
             (update :ring.response/headers assoc
                     "cache-control" "no-store"
                     "location" query)
             (assoc :cookies {"site_session"
                              {:value
                               (json/write-value-as-string
                                {"access_token" access-token
                                 ;; The 'user' field tells user-agents where to
                                 ;; find information about the user. It isn't
                                 ;; otherwise used.
                                 "user" user})
                               :max-age expires-in
                               :same-site :strict
                               ;; We should set http-only to true.  However,
                               ;; this stops Swagger UI from make API
                               ;; requests. TODO: The plan is to figure out how
                               ;; to use the auth features in Swagger UI to
                               ;; allow these requests, and then we can set
                               ;; http-only to true.
                               :http-only false
                               :path "/"}})
             (cookies-response)
             ((fn [req] (assoc-in req [:ring.response/headers "set-cookie"] (get-in req [:headers "Set-Cookie"])))))))
     ;; else not user
     (throw
      (ex-info
       "Failed to login"
       (-> req
           (assoc :ring.response/status 302 :ring.response/body "Failed to login\r\n")
           (update :ring.response/headers assoc "location" "/")))))))

(defn logout-response
  [req]
  (-> req
      (assoc :ring.response/status 302
             :ring.response/body (format "Logged out\r\n")
             ::http/content-type "text/plain")
      (update :ring.response/headers assoc
              "cache-control" "no-store"
              "location" "/")
      (assoc :cookies {"site_session"
                       {:value  ""
                        :max-age 0
                        :same-site :strict
                        :path "/"}})
      (cookies-response)
      ((fn [req] (assoc-in req [:ring.response/headers "set-cookie"] (get-in req [:headers "Set-Cookie"]))))))

(defn authenticate
  "Authenticate a request. Return a pass subject, with information about user,
  roles and other credentials. The resource can be used to determine the
  particular Protection Space that it is part of, and the appropriate
  authentication scheme(s) for accessing the resource."
  [{::site/keys [db] :as req}]
  ;; TODO: This might be where we also add the 'on-behalf-of' info
  (let [{access-token "access_token"}
        (some-> req
                ((fn [req] (assoc req :headers (get req :ring.request/headers))))
                cookies-request
                :cookies (get "site_session") :value json/read-value)
        now (::site/start-date req)]

    (or
     ;; Cookie
     (when access-token
       (when-let [session (lookup-session access-token now)]
         (->
          (select-keys session [::pass/user ::pass/username])
          (assoc ::pass/auth-scheme "Session"))))

     ;; Authorization header
     (when-let [authorization-header
                (get-in req [:ring.request/headers "authorization"])]
       (let [{::rfc7235/keys [auth-scheme token68]}
             (reap/authorization authorization-header)]

         (case (.toLowerCase auth-scheme)
           "basic"
           (try
             (let [[_ username password]
                   (re-matches
                    #"([^:]*):([^:]*)"
                    (String. (.decode (java.util.Base64/getDecoder) token68)))

                   [user pwhash] (lookup-user db username)]

               (when (and password pwhash (password/check password pwhash))
                 {::pass/user user
                  ::pass/username username
                  ::pass/auth-scheme "Basic"}))
             (catch Exception e
               (log/error e)))

           "bearer"
           (when-let [session (lookup-session token68 now)]
             (->
              (select-keys session [::pass/user ::pass/username])
              (assoc ::pass/auth-scheme "Bearer"
                     ;;::pass/session-expiry (java.util.Date/from (::expiry-instant session))
                     )))

           (throw
            (ex-info "Auth scheme unsupported"
                     (into req {:ring.response/status 401
                                :ring.response/body "Unauthorized\r\n"})))))))))
