;; Copyright © 2022, JUXT LTD.

;; References --
;; [OWASP-SM]: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html

(ns juxt.pass.alpha.session
  (:require
   [juxt.pass.alpha.util :refer [make-nonce]]
   [ring.middleware.cookies :refer [cookies-request cookies-response]]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

;; "The session ID or token binds the user authentication credentials (in the
;; form of a user session) to the user HTTP traffic and the appropriate access
;; controls enforced by the web application." [OWASP-SM]
(defn create-session
  "Create sesssion identifier."
  [xt-node init-state]
  ;; "The session ID content (or value) must be meaningless to prevent
  ;; information disclosure attacks, where an attacker is able to decode the
  ;; contents of the ID and extract details of the user, the session, or the
  ;; inner workings of the web application.
  ;;
  (let [ ;; The session ID must simply be an identifier on the client side, and its
        ;; value must never include sensitive information (or PII)." [OWASP-SM]
        session-id (make-nonce 16)

        inner-session-id (make-nonce 16)
        session (assoc init-state
                       :xt/id inner-session-id
                       ::site/type "Session")

        ;; A session-id binding is a document that can be evicted, such that
        ;; switching the database to a different basis doesn't unintentionally
        ;; reanimate expired session ids. It maps the session id to the session.
        session-id-binding
        {:xt/id session-id
         ::site/type "SessionId"
         ::pass/session (:xt/id session)}
        ]
    (let [tx
          (xt/submit-tx
           xt-node
           [[::xt/put session]
            [::xt/put session-id-binding]])]
      (xt/await-tx xt-node tx))
    session-id))

(defn lookup-session [db sid]
  (let [session-id-binding (xt/entity db sid)]
    (xt/entity db (::pass/session session-id-binding))))

(defn ->cookie [session-id]
  ;; TODO: In local testing (against home.test) it seems that setting
  ;; SameSite=Strict means that the cookie doesn't get passed through. I think
  ;; it's because the first-party is being 'called' from Auth0, which means that
  ;; samesite=strict cookies aren't sent across. Note: I've tried replacing the
  ;; POST to /_site/login-with-github with a GET but to no avail (I've left it
  ;; at a GET as that seems more mainstream)
  (format "id=%s; Path=/; Secure; HttpOnly; SameSite=Lax" session-id))

(defn set-cookie [req session-id]
  (-> req
      (update :ring.response/headers assoc "set-cookie" (->cookie session-id))))

(defn escalate-session
  "Update the session by applying f and return the result of rotating the session id."
  [{::site/keys [db xt-node] ::pass/keys [session-id!] :as req} f]
  (let [session (lookup-session db session-id!)
        _ (assert session)
        new-session-id! (make-nonce 16)
        new-session (-> (f session)
                        (assoc :xt/id (:xt/id session)))
        session-id-binding {:xt/id new-session-id!
                            ::site/type "SessionId"
                            ::pass/session (:xt/id session)}

        tx (xt/submit-tx
            xt-node
            [[::xt/match (:xt/id session) session]
             [::xt/put new-session]
             [::xt/evict session-id!]
             [::xt/put session-id-binding]])

        tx-status (xt/await-tx xt-node tx)]

    (log/tracef "escalate-session, tx-status: %s" tx-status)

    (if (xt/tx-committed? xt-node tx)
      (-> req
          (set-cookie new-session-id!))
      (throw (ex-info "Session wasn't escalated" {})))))

(defn wrap-associate-session [h]
  (fn [{::site/keys [db] :as req}]
    (let [session-id!
          (-> (assoc req :headers (get req :ring.request/headers))
              cookies-request
              :cookies (get "id") :value)

          session (when session-id!
                    (lookup-session db session-id!))

          req (cond-> req
                ;; The purpose of the trailing exclamation mark (!) is to
                ;; indicate sensitivity. Avoid logging sensitive data.
                session-id! (assoc ::pass/session-id! session-id!)
                session (assoc ::pass/session session))]

      (when req (log/tracef "assoc session: %s" session))
      (h req))))

(comment
  (let [xt-node (xt/start-node {})
        sid (create-session xt-node {:foo "foo"})]
    (lookup-session (xt/db xt-node) sid)))
