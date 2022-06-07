;; Copyright © 2022, JUXT LTD.

(ns juxt.pipe.core-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.pipe.alpha.core :as pipe]
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

(def LOGIN
  (list
   [:validate
    [:map
     ["username" [:string {:min 1}]]
     ["password" [:string {:min 1}]]]]

   :dup

   [:push "username"]
   ;; TODO: Doesn't Factor return two values here, including a boolean?
   :of

   :swap
   [:push "password"]
   :of
   :swap

   ;; We now have a stack with: <user> <password>

   [:find-matching-identity-on-password-query
    {:username-in-identity-key :username
     :password-hash-in-identity-key :password-hash}]

   :xtdb-query :first :first

   [:push ::pass/user-identity]
   :swap :associate

   [:push ::site/type "https://meta.juxt.site/pass/subject"]
   :set-at

   ;; Make subject
   [:push :xt/id]
   [:push 10] :random-bytes :as-hex-string
   [:push "https://site.test/subjects/alice/"] :str
   :set-at

   ;; Create the session, linked to the subject
   :dup [:push :xt/id] :of
   [:push ::pass/subject] :swap :associate

   ;; Now we're good to wrap up the subject in a tx-op
   :swap :xtdb.api/put :swap

   [:push :xt/id]
   [:push 16] :make-nonce
   [:push "https://site.test/sessions/"] :str
   :set-at
   [:push ::site/type "https://meta.juxt.site/pass/session"]
   :set-at

   :dup [:push :xt/id] :of
   [:push ::pass/session] :swap :associate

   :swap :xtdb.api/put :swap

   [:push :xt/id]
   [:push 16] :make-nonce
   [:push "https://site.test/session-tokens/"] :str
   :set-at
   [:push ::site/type "https://meta.juxt.site/pass/session-token"]
   :set-at

   :xtdb.api/put))

(deftest match-password-test

  (repl/put!
   {:xt/id "https://site.test/user-identities/alice"
    :username "alice"
    :password-hash (password/encrypt "garden")})

  (testing "Correct password creates subject and session"
    (let [result (pipe/pipe
                  (list {"username" "alice"
                         "password" "garden"})
                  LOGIN
                  {:db (xt/db *xt-node*)})]
      (is
       (= 3 (count result)))))

  (testing "Incorrect password throws exception"
    (is
     (thrown-with-msg?
      clojure.lang.ExceptionInfo
      #"\QError, query didn't return any results\E"
      (pipe/pipe
       (list {"username" "alice"
              "password" "wrong-password"})
       LOGIN
       {:db (xt/db *xt-node*)})))))

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
              "password" "gardenj"})
       (list
        [:validate
         [:map
          ["username" [:string {:min 1}]]
          ["password" [:string {:min 1}]]]]

        :dup

        [:push "username"]
        ;; TODO: Doesn't Factor return two values here, including a boolean?
        :of

        :swap
        [:push "password"]
        :of
        :swap

        ;; We now have a stack with: <user> <password>

        [:find-matching-identity-on-password-query
         {:username-in-identity-key :username
          :password-hash-in-identity-key :password-hash}]

        :xtdb-query :first :first

        [:push ::pass/user-identity]
        :swap :associate

        [:push ::site/type "https://meta.juxt.site/pass/subject"]
        :set-at

        ;; Make subject
        [:push :xt/id]
        [:push 10] :random-bytes :as-hex-string
        [:push "https://site.test/subjects/alice/"] :str
        :set-at

        ;; Create the session, linked to the subject
        :dup [:push :xt/id] :of
        [:push ::pass/subject] :swap :associate

        ;; Now we're good to wrap up the subject in a tx-op
        :swap :xtdb.api/put :swap

        [:push :xt/id]
        [:push 16] :make-nonce
        [:push "https://site.test/sessions/"] :str
        :set-at
        [:push ::site/type "https://meta.juxt.site/pass/session"]
        :set-at

        :dup [:push :xt/id] :of
        [:push ::pass/session] :swap :associate

        :swap :xtdb.api/put :swap

        [:push :xt/id]
        [:push 16] :make-nonce
        [:push "https://site.test/session-tokens/"] :str
        :set-at
        [:push ::site/type "https://meta.juxt.site/pass/session-token"]
        :set-at

        :xtdb.api/put)

       {:db (xt/db *xt-node*)})
      ))))
