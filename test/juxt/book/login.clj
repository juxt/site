;; Copyright © 2022, JUXT LTD.

(ns juxt.book.login
  (:require
   [juxt.site.alpha.init :as init :refer [substitute-actual-base-uri]]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]
   [juxt.flip.alpha.core :as f]))

;; Note: It might be considered a good idea to create unit tests around this
;; quotation, but think carefully before doing so. The point of a quotation is
;; that it runs in the context of an existing database and is very sensitive to
;; both the current state of the database and the contents of a given
;; request. Therefore, the best way of testing a quotation is with an
;; integration test. The danger of creating smaller tests is that the setup
;; required will be brittle in the face of code changes and possibly lose
;; accuracy, providing false test positives.

(def login-quotation
  (substitute-actual-base-uri
   {:juxt.flip.alpha/quotation
    `(
      (f/define extract-credentials
        [(f/set-at
          (f/dip [:juxt.site.alpha/received-representation f/env
                  :juxt.http.alpha/body f/of
                  f/bytes-to-string
                  f/form-decode

                  ;; Validate we have what we're expecting
                  (site/validate
                   [:map
                    ["username" [:string {:min 1}]]
                    ["password" [:string {:min 1}]]])

                  :input]))])

      (f/define find-user-or-fail
        [ ;; Pull out the username, and downcase as per OWASP guidelines for
         ;; case-insensitive usernames.
         (f/keep [(f/of :input) (f/of "username") f/>lower])
         ;; Pull out the password
         (f/keep [(f/of :input) (f/of "password")])

         ;; Find a user-identity that matches, fail-fast
         (f/set-at
          (f/dip
           [(juxt.flip.alpha.xtdb/q
             ~'{:find [(pull uid [*]) (pull user [*])]
                :keys [uid user]
                :where [
                        [uid :juxt.pass.alpha/user user]
                        [uid :juxt.pass.alpha/username username]
                        [uid :juxt.pass.alpha/password-hash password-hash]
                        [(crypto.password.bcrypt/check password password-hash)]]
                ;; stack order
                :in [password username]})
            f/first
            (f/unless* [(f/throw-exception (f/ex-info "Login failed" {}))])
            :matched-user]))])

      (f/define make-subject
        [
         (f/set-at
          (f/dip
           [f/<sorted-map>
            (f/set-at
             (f/dip
              [(pass/as-hex-str (pass/random-bytes 10))
               (f/str "https://example.org/subjects/")
               :xt/id]))
            (f/set-at (f/dip ["https://meta.juxt.site/pass/subject" :juxt.site.alpha/type]))
            :subject]))])

      (f/define link-subject-to-user-identity
        [
         (f/set-at
          (f/keep [(f/of :matched-user) (f/of :uid) (f/of :xt/id)
                   :juxt.pass.alpha/user-identity])
          (f/keep [(f/of :subject)])
          (f/dip [f/set-at :subject]))])

      (f/define commit-subject
        [(site/push-fx (f/keep [(xtdb.api/put (f/of :subject))]))])

      (f/define put-subject
        [make-subject
         link-subject-to-user-identity
         commit-subject])

      (f/define make-session
        [(f/set-at
          (f/dip
           [f/<sorted-map>
            (f/set-at
             (f/dip
              [(pass/make-nonce 16)
               (f/str "https://example.org/sessions/")
               :xt/id]))
            (f/set-at (f/dip ["https://meta.juxt.site/pass/session" :juxt.site.alpha/type]))
            :session]))])

      (f/define link-session-to-subject
        [
         (f/set-at
          (f/keep [(f/of :subject) (f/of :xt/id)
                   :juxt.pass.alpha/subject])
          (f/keep [(f/of :session)])
          (f/dip [f/set-at :session]))])

      (f/define commit-session
        [(site/push-fx (f/keep [(xtdb.api/put (f/of :session))]))])

      (f/define put-session
        [make-session
         link-session-to-subject
         commit-session])

      (f/define make-session-token
        [
         (f/set-at
          (f/dip
           [f/<sorted-map>
            (f/set-at
             (f/dip
              [(pass/make-nonce 16)
               :juxt.pass.alpha/session-token]))
            (f/set-at
             (f/keep
              [(f/of :juxt.pass.alpha/session-token)
               (f/str "https://example.org/session-tokens/")
               :xt/id]))
            (f/set-at (f/dip ["https://meta.juxt.site/pass/session-token" :juxt.site.alpha/type]))
            :session-token]))])

      (f/define link-session-token-to-session
        [ ;; Link the session-token to the session
         (f/set-at
          (f/keep [(f/of :session) (f/of :xt/id)
                   :juxt.pass.alpha/session])
          (f/keep [(f/of :session-token)])
          (f/dip [f/set-at :session-token]))])

      (f/define commit-session-token
        [(site/push-fx (f/keep [(xtdb.api/put (f/of :session-token))]))])

      (f/define put-session-token
        [make-session-token
         link-session-token-to-session
         commit-session-token])

      (f/define make-set-cookie-header
        [(f/set-at
          (f/keep
           [(f/of :session-token) (f/of :juxt.pass.alpha/session-token) "id=" f/str
            "; Path=/; Secure; HttpOnly; SameSite=Lax" f/swap f/str
            :session-cookie]))])

      (f/define commit-set-cookie-header
        [(site/push-fx
          (f/keep
           [(f/of :session-cookie)
            (juxt.site.alpha/set-header "set-cookie" f/swap)]))])

      (f/define set-cookie-header
        [make-set-cookie-header
         commit-set-cookie-header])

      (site/with-fx-acc
        [
         extract-credentials
         find-user-or-fail
         put-subject
         put-session
         put-session-token
         set-cookie-header

         (f/env :ring.request/query)

         (f/when*
          [
           f/form-decode
           ;; Finally we pull out and use the return_to query parameter
           (f/of "return-to")
           (f/when* [
                     (juxt.site.alpha/set-header "location" f/swap)
                     f/swap site/push-fx
                     (juxt.site.alpha/set-status 302)
                     f/swap site/push-fx
                     ])])]))}))
