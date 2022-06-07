;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.pipe-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.pass.alpha.pipe :as pipe]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node*]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
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

(deftest match-password-test

  (repl/put!
   {:xt/id "alice-identity"
    :username "alice"
    :password-hash (password/encrypt "garden")})

  (let [program
        [
         ;; Initial validation on user provided data
         [::pipe/validate
          [:map
           ["username" [:string {:min 1}]]
           ["password" [:string {:min 1}]]]]

         [::pipe/find-matching-identity-on-password
          :juxt.pass.alpha/identity
          {:username-in-identity-key :username
           :path-to-username [:input "username"]
           :password-hash-in-identity-key :password-hash
           :path-to-password [:input "password"]}]

         [::pipe/merge
          {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]

         ^{:doc "Add an id"}
         [::pipe/merge {:xt/id "https://site.test/subjects/alice438348348"}]

         ^{:doc "Strip input"}
         [::pipe/dissoc :input]

         ^{:doc "Final validation before going into the database"}
         [::pipe/validate
          [:map
           [:xt/id [:re "(.+)"]]
           [:juxt.pass.alpha/identity :string]
           [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/subject"]]]]

         [::pipe/db-single-put]]]

    (testing "Correct password creates subject and session"
      (is
       (=
        [[:xtdb.api/put
          {:juxt.pass.alpha/identity "alice-identity",
           :juxt.site.alpha/type "https://meta.juxt.site/pass/subject",
           :xt/id "https://juxt.site/subjects/alice438348348"}]]
        (pipe/pipe
         (list {"username" "alice"
                "password" "garden"})
         program
         {:db (xt/db *xt-node*)}))))

    (testing "Incorrect password throws exception"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"\QLogin failed\E"
        (pipe/pipe
         (list {"username" "alice"
                "password" "wrong-password"})
         program
         {:db (xt/db *xt-node*)}))))))

(comment
  ((t/join-fixtures [with-system-xt])
   (time
    (fn []
      (repl/put!
       {:xt/id "https://site.test/user-identities/alice"
        :username "alice"
        :password-hash (password/encrypt "garden")})

      (pipe/pipe
       (list {"username" "alice"
              "password" "garden"})
       '[
         [::pipe/validate
          [:map
           ["username" [:string {:min 1}]]
           ["password" [:string {:min 1}]]]]

         ::pipe/dup

         [::pipe/push "username"]
         ;; TODO: Doesn't Factor return two values here, including a boolean?
         ::pipe/of

         ::pipe/swap
         [::pipe/push "password"]
         ::pipe/of
         ::pipe/swap

         ;; We now have a stack with: <user> <password>

         [::pipe/find-matching-identity-on-password-query
          {:username-in-identity-key :username
           :password-hash-in-identity-key :password-hash}]

         ::pipe/xtdb-query ::pipe/first ::pipe/first

         [::pipe/push ::pass/user-identity]
         ::pipe/swap ::pipe/associate

         [::pipe/push ::site/type "https://meta.juxt.site/pass/subject"]
         ::pipe/set-at

         ;; Make subject
         [::pipe/push :xt/id]
         [::pipe/push 10] ::pipe/random-bytes ::pipe/as-hex-string
         [::pipe/push "https://site.test/subjects/alice/"] ::pipe/str
         ::pipe/set-at

         ;; Create the session, linked to the subject
         ::pipe/dup [::pipe/push :xt/id] ::pipe/of
         [::pipe/push ::pass/subject] ::pipe/swap ::pipe/associate
         ;; Add id
         [::pipe/push :xt/id]
         [::pipe/push 16] ::pipe/make-nonce
         [::pipe/push "https://site.test/sessions/"] ::pipe/str
         ::pipe/set-at
         ;; Add type
         [::pipe/push ::site/type "https://meta.juxt.site/pass/session"]
         ::pipe/set-at

         ;; Create the session token, linked to the session
         ::pipe/dup [::pipe/push :xt/id] ::pipe/of
         [::pipe/push ::pass/session] ::pipe/swap ::pipe/associate
         ;; Add id
         [::pipe/push :xt/id]
         [::pipe/push 16] ::pipe/make-nonce
         [::pipe/push "https://site.test/session-tokens/"] ::pipe/str
         ::pipe/set-at
         ;; Add type
         [::pipe/push ::site/type "https://meta.juxt.site/pass/session-token"]
         ::pipe/set-at
         ]

       {:db (xt/db *xt-node*)})
      ))))
