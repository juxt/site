;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.pipe-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.pass.alpha.pipe :as pipe]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node*]]
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

  (repl/put! {:xt/id "alice-identity"
              :username "alice"
              :password-hash (password/encrypt "garden")})

  (let [cold-program
        [
         ;; Initial validation on user provided data
         [::pipe/validate
          [:map
           ["username" [:string {:min 1}]]
           ["password" [:string {:min 1}]]]]

         [::pipe/nest :input]]

        hot-program
        [
         ;; Find an entry
         [::pipe/find-matching-identity-on-password
          :juxt.pass.alpha/identity
          {:username-in-identity-key :username
           :path-to-username [:input "username"]
           :password-hash-in-identity-key :password-hash
           :path-to-password [:input "password"]}]

         [::pipe/merge
          {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]

         ^{:doc "Add an id"}
         [::pipe/merge {:xt/id "https://juxt.site/subjects/alice438348348"}]

         ^{:doc "Strip input"}
         [::pipe/dissoc :input]

         ^{:doc "Final validation before going into the database"}
         [::pipe/validate
          [:map
           [:xt/id [:re "(.+)"]]
           [:juxt.pass.alpha/identity :string]
           [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/subject"]]]]

         [::pipe/db-single-put]]]

    (testing "Correct password creates subject"
      (is
       (=
        [[:xtdb.api/put
          {:juxt.pass.alpha/identity "alice-identity",
           :juxt.site.alpha/type "https://meta.juxt.site/pass/subject",
           :xt/id "https://juxt.site/subjects/alice438348348"}]]
        (pipe/pipe
         (concat cold-program hot-program)
         {"username" "alice"
          "password" "garden"}
         {:db (xt/db *xt-node*)}))))

    (testing "Incorrect password throws exception"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"\QLogin failed\E"
        (pipe/pipe
         (concat cold-program hot-program)
         {"username" "alice"
          "password" "wrong-password"}
         {:db (xt/db *xt-node*)}))))))

(comment
  ((t/join-fixtures [with-system-xt])
   (fn []
     )))
