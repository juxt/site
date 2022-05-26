;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.http-authentication
  (:require
   [jsonista.core :as json]
   [juxt.reap.alpha.encoders :refer [www-authenticate]]
   [clojure.tools.logging :as log]
   [crypto.password.bcrypt :as password]
   [xtdb.api :as x]
   [juxt.pass.alpha.util :refer [new-subject-urn]]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [ring.util.codec :refer [form-decode]]
   [ring.middleware.cookies :refer [cookies-request cookies-response]]
   [xtdb.api :as xt]))

#_(def SECURE-RANDOM (new java.security.SecureRandom))
#_(def BASE64-ENCODER (java.util.Base64/getUrlEncoder))

#_(defn access-token []
  (let [bytes (byte-array 24)]
    (.nextBytes SECURE-RANDOM bytes)
    (.encodeToString BASE64-ENCODER bytes)))

#_(defonce sessions-by-access-token (atom {}))

#_(defn put-session! [k session ^java.time.Instant expiry-instant]
  (swap! sessions-by-access-token
         assoc k (assoc session
                        ::expiry-instant expiry-instant)))

#_(defn expire-sessions! [date-now]
  (swap! sessions-by-access-token
         (fn [sessions]
           (into {} (remove #(.isAfter (.toInstant date-now)
                                       (-> % second (get ::expiry-instant)))
                            sessions)))))

#_(defn lookup-session [k date-now]
  (expire-sessions! date-now)
  (get @sessions-by-access-token k))

#_(defn token-response
  [{::site/keys [received-representation resource start-date]
    ::pass/keys [subject] :as req}]

  ;; Check grant_type of posted-representation
  (assert (.startsWith (::http/content-type received-representation)
                       "application/x-www-form-urlencoded"))

  (when-not subject
    (throw
     (ex-info
      "Unauthorized"
      {::site/request-context (assoc req :ring.response/status 401)})))

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

;; Deprecated until reinstated with subjects from the database
#_(defn lookup-user
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


#_(defn ^:deprecated login-response
  "This is the original login called by a POST of user credentials to
  /_site/login. It checks the credentials and creates an access-token which is
  sets in the returned cookie. This will be replaced entirely with an OAuth2
  standard way of issuing tokens with token-response (above), which is being
  extended to support implicit and authorization code grant flows, in addition
  to client_credentials."
  [{::site/keys [received-representation db resource start-date uri]
    :as req}]

  ;; Check grant_type of posted-representation
  (assert (.startsWith (::http/content-type received-representation)
                       "application/x-www-form-urlencoded"))

  (let [posted-body (String. (::http/body received-representation))
        form (form-decode posted-body)
        username (get form "user")
        [user pwhash] (lookup-user db username)
        password (get form "password")

        redirect (some-> form (get "_query") form-decode (get "redirect"))]

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
                    "Login successful"
                    ::http/content-type "text/plain")
             (update :ring.response/headers assoc
                     "cache-control" "no-store"
                     "location" redirect)
             (assoc :cookies {"site_session"
                              (cond
                                (.startsWith uri "https")
                                {:value
                                 (json/write-value-as-string
                                  {"access_token" access-token
                                   ;; The 'user' field tells user-agents where to
                                   ;; find information about the user. It isn't
                                   ;; otherwise used.
                                   "user" user})
                                 :max-age expires-in

                                 :same-site :none
                                 :secure true
                                 ;; We should set http-only to true.  However,
                                 ;; this stops Swagger UI from make API
                                 ;; requests. TODO: The plan is to figure out how
                                 ;; to use the auth features in Swagger UI to
                                 ;; allow these requests, and then we can set
                                 ;; http-only to true.
                                 :http-only false
                                 :path "/"}

                                :else
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
                                 :path "/"})})
             (cookies-response)
             ((fn [req] (assoc-in req [:ring.response/headers "set-cookie"] (get-in req [:headers "Set-Cookie"])))))))
     ;; else not user
     (throw
      (ex-info
       "Failed to login"
       {::site/request-context
        (-> req
            (assoc :ring.response/status 302 :ring.response/body "Failed to login\r\n")
            (update :ring.response/headers assoc "location" "/"))})))))

#_(defn logout-response
  [req]
  ;; TODO: We must clear out the session!
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

(defn protection-spaces [db uri]
  (seq
   (for [{:keys [protection-space]}
         (xt/q
          db
          '{:find [(pull ps [*])]
            :keys [protection-space]
            :where [[ps ::site/type "https://meta.juxt.site/pass/protection-space"]
                    [ps ::pass/authentication-scope auth-scope]
                    [ps ::pass/canonical-root-uri root]
                    [(format "\\Q%s\\E%s" root auth-scope) regex]
                    [(re-pattern regex) regex-pattern]
                    [(re-matches regex-pattern uri)]
                    ]
            :in [uri]}
          uri)]
     protection-space)))

(defn authenticate-with-bearer-auth [req db token68 protection-spaces]
  (let [subject
        (:subject
         (first
          (xt/q db '{:find [(pull sub [*])]
                     :keys [subject]
                     :where [[at ::pass/token tok]
                             [at ::site/type "https://meta.juxt.site/pass/access-token"]
                             [at ::pass/subject sub]
                             [sub ::site/type "https://meta.juxt.site/pass/subject"]]
                     :in [tok]} token68)))]
    (cond-> req subject (assoc ::pass/subject subject))))

;; TODO (idea): Tie bearer token to other security aspects such as remote IP so that
;; the bearer token is more difficult to use if intercepted.

(defn find-or-create-basic-auth-subject [req user-identity protection-space]
  (let [xt-node (::site/xt-node req)
        subject {:xt/id (new-subject-urn)
                 :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
                 :juxt.pass.alpha/user-identity (:xt/id user-identity)
                 :juxt.pass.alpha/protection-space (:xt/id protection-space)}
        tx (xt/submit-tx xt-node [[:xtdb.api/put subject]])]
    (xt/await-tx xt-node tx)
    ;; TODO: Find an existing subject we can re-use or we create a subject for
    ;; every basic auth request. All attributes must match the above.
    (cond-> req
      subject (assoc ::pass/subject subject)
      ;; We need to update the db because we have injected a subject and it will
      ;; need to be in the database for authorization rules to work.
      subject (assoc ::site/db (xt/db xt-node)))))

(defn authenticate-with-basic-auth [req db token68 protection-spaces]
  (when-let [{::pass/keys [canonical-root-uri realm] :as protection-space} (first protection-spaces)]
    (let [[_ username password]
          (re-matches
           #"([^:]*):([^:]*)"
           (String. (.decode (java.util.Base64/getDecoder) token68)))

          query (cond-> '{:find [(pull e [*])]
                          :keys [identity]
                          :where [[e ::site/type "https://meta.juxt.site/pass/user-identity"]
                                  [e ::pass/canonical-root-uri canonical-root-uri]
                                  [e ::pass/username username]]
                          :in [username canonical-root-uri realm]}
                  realm (update :where conj '[e ::pass/realm realm]))

          candidates
          (xt/q db query username canonical-root-uri realm)]

      (when (> (count candidates) 1)
        (log/warnf "Multiple candidates in basic auth found for username %s, using first found" username))

      (when-let [user-identity (:identity (first candidates))]
        (when-let [password-hash (::pass/password-hash user-identity)]
          (when (password/check password password-hash)
            (find-or-create-basic-auth-subject req user-identity protection-space)))))))

(defn www-authenticate-header
  "Create the WWW-Authenticate header value"
  [protection-spaces]
  (www-authenticate
   (for [ps protection-spaces
         :let [realm (::pass/realm ps)]]
     {:juxt.reap.alpha.rfc7235/auth-scheme (::pass/auth-scheme ps)
      :juxt.reap.alpha.rfc7235/auth-params
      (cond-> []
        realm (conj
               {:juxt.reap.alpha.rfc7235/auth-param-name "realm"
                :juxt.reap.alpha.rfc7235/auth-param-value realm}))})))

(defn authenticate
  "Authenticate a request. Return a modified request, with information about user,
  roles and other credentials."
  [{::site/keys [db uri] :as req}]

  ;; TODO: This might be where we also add the 'on-behalf-of' info

  (let [protection-spaces (protection-spaces db uri)
        req (cond-> req protection-spaces (assoc ::pass/protection-spaces protection-spaces))
        authorization-header (get-in req [:ring.request/headers "authorization"])]

    (or
     (when authorization-header
       (let [{::rfc7235/keys [auth-scheme token68]}
             (reap/authorization authorization-header)]

         (or
          (case (.toLowerCase auth-scheme)
            "basic" (authenticate-with-basic-auth req db token68 (filter #(= (::pass/auth-scheme %) "Basic") protection-spaces))
            "bearer" (authenticate-with-bearer-auth req db token68 (filter #(= (::pass/auth-scheme %) "Bearer") protection-spaces))
            (throw
             (ex-info
              "Auth scheme unsupported"
              {::site/request-context
               (cond-> (assoc req :ring.response/status 401)
                 protection-spaces
                 (assoc
                  :ring.response/headers
                  {"www-authenticate"
                   (www-authenticate-header protection-spaces)}))})))
          req)))
     req)))

(defn ^:deprecated login-template-model [req]
  {:query (str (:ring.request/query req))})

(defn ^:deprecated unauthorized-template-model [req]
  {:redirect (str
              (:ring.request/path req)
              (when-let [query (:ring.request/query req)] (str "?" query)))})
