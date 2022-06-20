;; Copyright © 2022, JUXT LTD.

(ns juxt.flip.core-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.flip.alpha.core :as flip]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node*]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha :as-alias site]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]))

(use-fixtures :each with-system-xt)

;; A login to Site.

;; A user sends in a map (containing their username and password, for a login
;; form).

;; We validate this map, validating they have given us sensible input.

;; We use the values in this map to find/match a user in our system.

;; If we find such a user, we construct a 'subject' to represent them, pointing
;; to their identity.

;; We 'put' this subject into the database, this subject will be forever
;; associated with the user when they access the system, and used in
;; authorization rules for the action the user performs.

(comment
  ((t/join-fixtures [with-system-xt])
   (fn []
     (repl/put!
      {:xt/id "https://site.test/user-identities/alice"
       ::pass/username "alice"
       ::pass/password-hash (password/encrypt "garden")})

     (let [
           eval-quotation-results
           (flip/eval-quotation
            (list)

            '(
              ;; Definitions

              ;; assoc is intended to be used in a list, whereby the value is the top
              ;; of the stack. (assoc k v)
              (define assoc [swap rot set-at])
              ;; assoc* means put the value at the top of the stack into the map with
              ;; the given key. (assoc* k)
              ;; TODO: Look up the equivalent Factor convention.
              (define assoc* [rot set-at])

              ;; (m k -- m m)
              (define ref-as [over second (of :xt/id) swap juxt.flip.alpha.hashtables/associate])

              ;; The top of the stack is the user identity
              ;; Create the subject
              (define make-subject
                [(juxt.flip.alpha.hashtables/associate ::pass/user-identity)
                 (assoc :juxt.site.alpha/type "https://meta.juxt.site/pass/subject")
                 ;; The subject has a random id
                 (as-hex-string (random-bytes 10))
                 (str "https://site.test/subjects/")
                 (assoc* :xt/id)
                 (xtdb.api/put)])

              ;; Create the session, linked to the subject
              (define make-session-linked-to-subject
                [(ref-as ::pass/subject)
                 (make-nonce 16)
                 (str "https://site.test/sessions/")
                 (assoc* :xt/id)
                 (assoc ::site/type "https://meta.juxt.site/pass/session")
                 (xtdb.api/put)])

              ;; Create the session token, linked to the session
              (define make-session-token-linked-to-session
                [(ref-as ::pass/session)
                 (make-nonce 16)
                 ;; This is more complicated because we want to use the nonce in the
                 ;; xt/id
                 swap over
                 (assoc* ::pass/session-token)
                 swap
                 (str "https://site.test/session-tokens/")
                 (assoc* :xt/id)
                 (assoc ::site/type "https://meta.juxt.site/pass/session-token")
                 (xtdb.api/put)])

              ;; Wrap quotation in a apply-to-request-context operation
              ;; (quotation -- op)
              (define apply-to-request-context
                [:juxt.site.alpha/apply-to-request-context
                 swap _2vector])

              (define set-status
                [_1vector
                 :ring.response/status
                 swap push
                 (symbol "rot")
                 swap push
                 (symbol "set-at")
                 swap push
                 apply-to-request-context])

              ;; Create an apply-to-request-context operation that sets a header
              ;; (header-name value -- op)
              (define set-header
                [(symbol "dup")
                 _1vector

                 (symbol "of")
                 :ring.response/headers
                 _2vector >list
                 swap push

                 (symbol "if*")
                 _1vector
                 0
                 <vector>
                 swap push

                 (symbol "<array-map>")
                 _1vector
                 swap push
                 >list
                 swap push

                 push                   ; the value on the stack
                 push                   ; the header name
                 (symbol "rot")
                 swap push
                 (symbol "set-at")
                 swap push

                 :ring.response/headers
                 swap push
                 (symbol "rot")
                 swap push
                 (symbol "set-at")
                 swap push

                 apply-to-request-context])

              ;; Start of program

              ;; Get form
              :juxt.site.alpha/received-representation env
              :juxt.http.alpha/body of
              bytes-to-string
              juxt.flip.alpha/form-decode

              ;; Validate we have what we're expecting
              (validate
               [:map
                ["username" [:string {:min 1}]]
                ["password" [:string {:min 1}]]])

              dup

              "username"
              of
              >lower   ; Make usernames case-insensitive as per OWASP guidelines

              swap
              "password"
              of
              swap

              ;; We now have a stack with: <user> <password>

              (juxt.flip.alpha.xtdb/q
               (find-matching-identity-on-password-query
                {:username-in-identity-key ::pass/username
                 :password-hash-in-identity-key ::pass/password-hash}))

              first first

              (if*
                  [make-subject
                   make-session-linked-to-subject
                   make-session-token-linked-to-session

                   ;; Get the session token back and set it on a header
                   dup second :juxt.pass.alpha/session-token of
                   "id=" str
                   "; Path=/; Secure; HttpOnly; SameSite=Lax" swap str
                   (set-header "set-cookie" swap)

                   ;; Finally we pull out and use the return_to query parameter
                   :ring.request/query env
                   (when* [juxt.flip.alpha/form-decode
                           "return-to" of
                           (when*
                               [
                                (set-header "location" swap)
                                ]
                               )

                           ;; A quotation that will set a status 302 on the request context
                           (set-status 302)])]

                ;; else
                [(throw (ex-info "Login failed" {:ring.response/status 400}))]))

            {::site/db (xt/db *xt-node*)
             ::site/received-representation
             {::http/body
              (.getBytes
               (codec/form-encode
                {"username" "aliCe"
                 "password" "garden"
                 "csrf-tok" "123"}))

              ::http/content-type "application/x-www-form-urlencoded"
              }
             :ring.request/query "return-to=/document.html"
             :ring.response/headers {"server" "jetty"}}

            )]

       eval-quotation-results

       (authz/apply-request-context-operations
        {:ring.response/headers {"foo" "bar"}}
        (->>
         eval-quotation-results
         (filter (fn [[op]]  (= op :juxt.site.alpha/apply-to-request-context)))))))))


(authz/apply-request-context-operations
 {:ring.response/headers {"foo" "bar"}}
 '[[:juxt.site.alpha/apply-to-request-context
    [dup
     (of :ring.response/headers)
     [drop <array-map>]
     unless*
     break
     "id=165882feeb5f2e1de3ea06ae28bc1755; Path=/; Secure; HttpOnly; SameSite=Lax"
     "set-cookie"
     break
     rot
     set-at
     :ring.response/headers
     rot
     set-at]]]
 )


(flip/eval-quotation
 (list {})
 '([1 1 +] when*))
