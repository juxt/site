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

(defn session-token-id->urn [session-token-id!]
  (format "urn:site:session-token:%s" session-token-id!))

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
        ;;
        ;; We suffix this symbol to indicate it is sensitive (we should strive
        ;;to prevent an attacker getting access to this token).
        session-token-id! (make-nonce 16)

        ;; It could get confusing to have both session-token and session
        ;; documents, so the id-of-the-session-doc is indicated with a URN, as
        ;; per https://datatracker.ietf.org/doc/html/rfc2141.
        session (assoc init-state
                       :xt/id (format "urn:site:session:%s" (make-nonce 16))
                       ::site/type "Session")

        ;; A session-id binding is a document that can be evicted, such that
        ;; switching the database to a different basis doesn't unintentionally
        ;; reanimate expired session ids. It maps the session id to the session.
        session-token
        {:xt/id (session-token-id->urn session-token-id!)
         ::site/type "SessionToken"
         ::pass/session (:xt/id session)}
        ]
    (let [tx
          (xt/submit-tx
           xt-node
           [[::xt/put session]
            [::xt/put session-token]])]
      (xt/await-tx xt-node tx))
    session-token-id!))

(defn lookup-session [db session-token-id!]
  (let [session-token (xt/entity db (session-token-id->urn session-token-id!))]
    (xt/entity db (::pass/session session-token))))

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
  "Update the session by applying f and return the result of rotating the session id."
  [{::site/keys [db xt-node] ::pass/keys [session-token-id!] :as req} f]
  (let [session (lookup-session db session-token-id!)
        _ (assert session)
        new-session-token-id! (make-nonce 16)
        new-session (-> (f session)
                        (assoc :xt/id (:xt/id session)))

        session-token {:xt/id (session-token-id->urn new-session-token-id!)
                       ::site/type "SessionToken"
                       ::pass/session (:xt/id session)}

        tx (xt/submit-tx
            xt-node
            [[::xt/match (:xt/id session) session]
             [::xt/put new-session]
             ;; TODO: Mark this token as stale and have a separate eviction
             ;; process
             [::xt/evict (session-token-id->urn session-token-id!)]
             [::xt/put session-token]])

        _ (xt/await-tx xt-node tx)]

    (if (xt/tx-committed? xt-node tx)
      (-> req
          (set-cookie new-session-token-id!))
      (throw (ex-info "Session wasn't escalated" {})))))

(defn wrap-associate-session [h]
  (fn [{::site/keys [db] :as req}]
    (let [session-token-id!
          (-> (assoc req :headers (get req :ring.request/headers))
              cookies-request
              :cookies (get "id") :value)

          session (when session-token-id!
                    (lookup-session db session-token-id!))

          req (cond-> req
                ;; The purpose of the trailing exclamation mark (!) is to
                ;; indicate sensitivity. Avoid logging sensitive data.
                session-token-id! (assoc ::pass/session-token-id! session-token-id!)
                session (assoc ::pass/session session))]

      (h req))))

(comment
  (let [xt-node (xt/start-node {})
        sid (create-session xt-node {:foo "foo"})]
    (lookup-session (xt/db xt-node) sid)))
