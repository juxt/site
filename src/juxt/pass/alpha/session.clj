(ns juxt.pass.alpha.session

;; Copyright © 2022, JUXT LTD.

;; References --
;; [OWASP-SM]: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html

  (:require
   [juxt.pass.alpha.authentication :as authz]
   [ring.middleware.cookies :refer [cookies-request cookies-response]]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

;; The id cookie is only used during the OAuth Authorization Flow using
;; OpenID Connect for Authentication. It is used to store the session-token-id
;; created before calling the authorize endpoint. The session is used to prevent
;; CSRF-attacks as it stores the random state and nonce values for comparison.

(defn ->cookie [session-token-id]
  ;; TODO: In local testing (against home.test) it seems that setting
  ;; SameSite=Strict means that the cookie doesn't get passed through. I think
  ;; it's because the first-party is being 'called' from Auth0, which means that
  ;; samesite=strict cookies aren't sent across. Note: I've tried replacing the
  ;; POST to /_site/login-with-github with a GET but to no avail (I've left it
  ;; at a GET as that seems more mainstream)
  (format "id=%s; Path=/; Secure; HttpOnly; SameSite=Lax" session-token-id))

(defn set-cookie [req session-id]
  (-> req
      (update :ring.response/headers assoc "set-cookie" (->cookie session-id))))

(defn escalate-session
  "If provided session-token-id! matches, create a new session with the matched identity"
  [{::site/keys [db crux-node resource start-date]
    ::pass/keys [session-token-id!] :as req} matched-identity]
  (let [session (authz/lookup-session session-token-id! start-date)
        _ (assert session)
        new-session-token-id! (authz/access-token)
        expires-in (get resource ::pass/expires-in (* 24 3600))]
    (authz/remove-session! session-token-id!)
    (authz/put-session!
     new-session-token-id!
     {::pass/user matched-identity}
     (.plusSeconds (.toInstant start-date) expires-in))

    (-> req
        (set-cookie new-session-token-id!)
        (update-in [:ring.response/headers "location"] #(str % "?code=" new-session-token-id!)))))

(defn wrap-associate-session [h]
  (fn [{::site/keys [db start-date] :as req}]
    (let [session-token-id!
          (-> (assoc req :headers (get req :ring.request/headers))
              cookies-request
              :cookies (get "id") :value)

          session (when session-token-id!
                    (authz/lookup-session session-token-id! start-date))

          subject (some->
                   (select-keys session [::pass/user ::pass/username])
                   (assoc ::pass/auth-scheme "Session"))

          req (cond-> req
                ;; The purpose of the trailing exclamation mark (!) is to
                ;; indicate sensitivity. Avoid logging sensitive data.
                session-token-id! (assoc ::pass/session-token-id! session-token-id!)
                session (assoc ::pass/session session)
                subject (assoc ::pass/subject subject))]

      (h req))))
