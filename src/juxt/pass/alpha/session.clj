;; Copyright © 2022, JUXT LTD.

;; References --
;; [OWASP-SM]: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html

(ns juxt.pass.alpha.session
  (:require
   [juxt.pass.alpha.util :refer [make-nonce]]
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
        session (assoc init-state :xt/id inner-session-id)

        ;; A session-id binding is a document that can be evicted, such that
        ;; switching the database to a different basis doesn't unintentionally
        ;; reanimate expired session ids. It maps the session id to the session.
        session-id-binding
        {:xt/id session-id
         ::pass/session (:xt/id session)}
        ]
    (let [tx
          (xt/submit-tx
           xt-node
           [[:xtdb.api/put session]
            [:xtdb.api/put session-id-binding]])]
      (xt/await-tx xt-node tx))
    session-id))

(defn lookup-session [db sid]
  (let [session-id-binding (xt/entity db sid)
        session-id (xt/entity db (::pass/session session-id-binding))]
    session-id))

(comment
  (let [xt-node (xt/start-node {})
        sid (create-session xt-node {:foo "foo"})]
    (lookup-session (xt/db xt-node) sid)))
