;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.process-test
  (:require
   [clojure.set :as set]
   [clojure.test :refer [deftest is are use-fixtures] :as t]
   [crypto.password.bcrypt :as password]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [juxt.pass.alpha.process :as-alias pass.process]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt submit-and-await! *xt-node*]]
   [malli.core :as m]
   [malli.util :as mu]
   [xtdb.api :as xt]))

(use-fixtures :each with-system-xt)

;; A login to Site

;; A user sends in a map (containing their username and password, for a login form)

;; We validate this map, validating they have given us sensible input

;; We use the values in this map to find/match a user in our system

;; If we find such a user, we construct a 'subject' to represent them, pointing to their identity

;; We 'put' this subject into the database, this subject will be forever
;; associated with the user when they access the system, and used in
;; authorization rules for the action the user performs.

(deftest match-password-test

  (repl/put! {:xt/id "alice-identity"
              :username "alice"
              :password (password/encrypt "garden")})

  (let [methods
        {::nest
         (fn [[_ k] acc] {k acc})

         ::merge
         (fn [[_ m] acc] (merge acc m))

         ::dissoc
         (fn [[_ & ks] acc] (apply dissoc acc ks))

         ::match-identity-on-password
         (fn [[_ k {:keys [username-in-identity-key username-location
                           password-in-identity-key password-location]}] acc]
           (let [identity
                 (first
                  (map first
                       (xt/q
                        (xt/db *xt-node*)
                        {:find '[e]
                         :where [
                                 ['e username-in-identity-key 'username]
                                 ['e password-in-identity-key 'password-hash]
                                 ['(crypto.password.bcrypt/check password password-hash)]]
                         :in '[username password]}
                        (get-in acc username-location)
                        (get-in acc password-location))))]
             (cond-> acc
               identity (assoc k identity))))

         ::validate
         (fn [[_ schema] acc]
           (if-not (m/validate schema acc)
             (throw
              (ex-info
               "Failed validation check"
               (m/explain schema acc)))
             acc))

         ::db-single-put
         (fn [_ acc] [[:xtdb.api/put acc]])}

        cold-program
        [
         ;; Initial validation on user provided data
         [::validate
          [:map
           ["username" [:string {:min 1}]]
           ["password" [:string {:min 1}]]]]

         [::nest :input]]

        hot-program
        [
         ;; Find an entry
         [::match-identity-on-password
          :juxt.pass.alpha/identity
          {:username-in-identity-key :username
           :username-location [:input "username"]
           :password-in-identity-key :password
           :password-location [:input "password"]}]

         [::merge
          {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]

         ^{:doc "Add an id"}
         [::merge {:xt/id "https://juxt.site/subjects/alice438348348"}]

         ^{:doc "Strip input"}
         [::dissoc :input]

         ^{:doc "Final validation before going into the database"}
         [::validate
          (mu/closed-schema
           [:map
            [:xt/id [:re "(.+)"]]
            [:juxt.pass.alpha/identity :string]
            [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/subject"]]])]

         [::db-single-put]]]

    (letfn [(apply-step [acc step]
              (if-let [method-fn (get methods (first step))]
                (method-fn step acc)
                (throw (ex-info "No such method" {:method (first step)}))))

            (process [steps seed] (reduce apply-step seed (filter some? steps)))]

      (let [result (process
                    (concat cold-program hot-program)

                    {"username" "alice"
                     "password" "garden"})]
        (is (= [[:xtdb.api/put
                 {:juxt.pass.alpha/identity "alice-identity",
                  :juxt.site.alpha/type "https://meta.juxt.site/pass/subject",
                  :xt/id "https://juxt.site/subjects/alice438348348"}]] result))))))


(comment
  ((t/join-fixtures [with-system-xt])
   (fn []
     )))
