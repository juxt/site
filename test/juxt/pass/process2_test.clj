;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.process2-test
  (:require
   [clojure.test :refer [deftest is use-fixtures] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.pass.alpha.process2 :as proc]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node*]]
   [malli.util :as mu]
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
              :password (password/encrypt "garden")})

  (let [cold-program
        [
         ;; Initial validation on user provided data
         [::proc/validate
          [:map
           ["username" [:string {:min 1}]]
           ["password" [:string {:min 1}]]]]

         [::proc/nest :input]]

        hot-program
        [
         ;; Find an entry
         [::proc/match-identity-on-password
          :juxt.pass.alpha/identity
          {:username-in-identity-key :username
           :username-location [:input "username"]
           :password-in-identity-key :password
           :password-location [:input "password"]}]

         [::proc/merge
          {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]

         ^{:doc "Add an id"}
         [::proc/merge {:xt/id "https://juxt.site/subjects/alice438348348"}]

         ^{:doc "Strip input"}
         [::proc/dissoc :input]

         ^{:doc "Final validation before going into the database"}
         [::proc/validate
          (mu/closed-schema
           [:map
            [:xt/id [:re "(.+)"]]
            [:juxt.pass.alpha/identity :string]
            [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/subject"]]])]

         [::proc/db-single-put]]

        result
        (proc/process
         (concat cold-program hot-program)
         {"username" "alice"
          "password" "garden"}
         {:db (xt/db *xt-node*)})]

    (is (= [[:xtdb.api/put
             {:juxt.pass.alpha/identity "alice-identity",
              :juxt.site.alpha/type "https://meta.juxt.site/pass/subject",
              :xt/id "https://juxt.site/subjects/alice438348348"}]] result))))


(comment
  ((t/join-fixtures [with-system-xt])
   (fn []
     )))
