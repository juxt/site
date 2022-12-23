;; Copyright © 2022, JUXT LTD.

(ns juxt.site.eql-datalog-compiler-test
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [edn-query-language.core :as eql]
   [jsonista.core :as json]
   [juxt.site.init :as init]
   [juxt.site.repl :as repl]
   [juxt.site.test-helpers.oauth :as oauth]
   [juxt.site.test-helpers.login :as login]
   [juxt.grab.alpha.document :as grab.document]
   [juxt.grab.alpha.parser :as grab.parser]
   [juxt.grab.alpha.schema :as grab.schema]
   [juxt.site.eql-datalog-compiler :as eqlc]
   [juxt.site.graphql-eql-compiler :refer [graphql->eql-ast]]
   [juxt.test.util :refer [with-system-xt with-fixtures
                           with-handler *xt-node* *handler*] :as tutil]
   [juxt.site.resource-package :as pkg]
   [xtdb.api :as xt]))

(defn install-hospital! []
  (pkg/install-package-from-filesystem!
   "packages/bootstrap"
   {"https://example.org" "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/user-database"
   {#{"https://core.example.org" "https://example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/example-users"
   {#{"https://core.example.org" "https://example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/sessions"
   {#{"https://core.example.org" "https://example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/login-form"
   {#{"https://core.example.org" "https://example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/oauth2-auth-server"
   {#{"https://core.example.org" "https://example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/example-oauth-resources"
   {#{"https://core.example.org" "https://auth.example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/protection-spaces"
   {#{"https://core.example.org" "https://auth.example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/protection-spaces"
   {#{"https://core.example.org" "https://auth.example.org"} "https://auth.hospital.com"})

  (pkg/install-package-from-filesystem!
   "packages/hospital-demo"
   {"https://example.org" "https://hospital.com"
    #{"https://core.example.org" "https://auth.example.org"} "https://auth.hospital.com"}))

(defn with-hospital [f]
  (install-hospital!)
  (f))

(use-fixtures :each with-system-xt with-handler with-hospital)

(deftest eql-with-acl-test
  ;; Create some measurements
  (init/enact-create!
   *xt-node*
   {:juxt.site/subject-id "https://auth.hospital.com/subjects/system"
    :juxt.site/action-id "https://auth.hospital.com/actions/register-patient-measurement"
    :juxt.site/input
    {:xt/id "https://hospital.com/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1e"
     :patient "https://hospital.com/patients/004"
     :reading {"heartRate" "120"
               "bloodPressure" "137/80"}}})

  (init/enact-create!
   *xt-node*
   {:juxt.site/subject-id "https://auth.hospital.com/subjects/system"
    :juxt.site/action-id "https://auth.hospital.com/actions/register-patient-measurement"
    :juxt.site/input
    {:xt/id "https://hospital.com/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1e"
     :patient "https://hospital.com/patients/006"
     :reading {"heartRate" "82"
               "bloodPressure" "198/160"}}})

  (init/enact-create!
   *xt-node*
   {:juxt.site/subject-id "https://auth.hospital.com/subjects/system"
    :juxt.site/action-id "https://auth.hospital.com/actions/register-patient-measurement"
    :juxt.site/input
    {:xt/id "https://hospital.com/measurements/eeda3b49-2e96-42fc-9e6a-e89e2eb68c24"
     :patient "https://hospital.com/patients/010"
     :reading {"heartRate" "85"
               "bloodPressure" "120/80"}}})

  (init/enact-create!
   *xt-node*
   {:juxt.site/subject-id "https://auth.hospital.com/subjects/system"
    :juxt.site/action-id "https://auth.hospital.com/actions/register-patient-measurement"
    :juxt.site/input
    {:xt/id "https://hospital.com/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1d"
     :patient "https://hospital.com/patients/010"
     :reading {"heartRate" "87"
               "bloodPressure" "127/80"}}})

  ;; Fails because add-implicit-dependencies doesn't cope with :deps being a fn

  (let [alice-session-token
        (login/login-with-form!
         *handler*
         :juxt.site/uri "https://auth.hospital.com/login"
         "username" "alice"
         "password" "garden")

        {alice-access-token "access_token" error "error"}
        (oauth/authorize!
         "https://auth.hospital.com/oauth/authorize"
         (merge
          alice-session-token
          {"client_id" "local-terminal"
           ;; "scope" ["https://example.org/oauth/scope/read-personal-data"]
           }))
        _ (is (nil? error) (format "OAuth2 grant error: %s" error))

        bob-session-token
        (login/login-with-form!
         *handler*
         :juxt.site/uri "https://auth.hospital.com/login"
         "username" "bob"
         "password" "walrus")
        {bob-access-token "access_token"
         error "error"}
        (oauth/authorize!
         "https://auth.hospital.com/oauth/authorize"
         (merge
          bob-session-token
          {"client_id" "local-terminal"
           ;;"scope" ["https://example.org/oauth/scope/read-personal-data"]
           })
         )
        _ (is (nil? error) (format "OAuth2 grant error: %s" error))]

    ;; Add a /patient/XXX resource to serve an individual patient.

    ;; https://auth.hospital.com/actions/get-patient must perform an XT query.

    ;; In the future, it would be good if the http request can include a
    ;; header indicating the minimum required version in order to provide
    ;; read-your-own-writes consistency. Perhaps use standard http
    ;; conditional request headers for this.

    ;; The GET pathway skips the tx-fn (in the non-serializable case),
    ;; proceeding directly to calling add-payload.

    ;; Note: it would be useful to research whether a Flip database query
    ;; could be automatically limited by the actions in scope. This would
    ;; make it safer to allow people to add their own Flip quotations.

    ;; Here we have the conundrum: when the
    ;; https://example.org/actions/get-patient action rule has the clause
    ;; '[resource :juxt.site/type
    ;; "https://example.org/types/patient"]' then it is not a permitted
    ;; action. We must separate the actions that allow access to a
    ;; uri-template'd resource and the actions that create the body
    ;; payload.

    ;; Alice can access a particular patient because she has a particularly
    ;; broad permission on the get-patient action

    (testing "Access to /hospital/patient/005"
      (let [response
            (*handler*
             {:ring.request/method :get
              :juxt.site/uri "https://hospital.com/patients/005"
              :ring.request/headers
              {"authorization" (format "Bearer %s" alice-access-token)
               "accept" "application/json"}})]

        (is (= (json/write-value-as-string {"name" "Angie Solis"})
               (String. (:ring.response/body response))))
        (is (= 200 (:ring.response/status response))))

      ;; Bob can't see the patient details of Angie Solis
      (let [response (*handler*
                      {:ring.request/method :get
                       :juxt.site/uri "https://hospital.com/patients/005"
                       :ring.request/headers
                       {"authorization" (format "Bearer %s" bob-access-token)
                        "accept" "application/json"}})]
        (is (= 403 (:ring.response/status response)))))

    (testing "List patients with /patients/"

        ;; Alice sees all 20 patients
        (let [response
              (*handler*
               {:ring.request/method :get
                :juxt.site/uri "https://hospital.com/patients/"
                :ring.request/headers
                {"authorization" (format "Bearer %s" alice-access-token)
                 "accept" "application/json"}})
              body (:ring.response/body response)
              result (some-> body json/read-value)
              ]
          (is (= "application/json" (get-in response [:ring.response/headers "content-type"])))
          (is body)
          (is result)
          (is (vector? result))
          (is (= 20 (count result))))

        ;; Bob sees just 3 patients
        (let [response
              (*handler*
               {:ring.request/method :get
                :juxt.site/uri "https://hospital.com/patients/"
                :ring.request/headers
                {"authorization" (format "Bearer %s" bob-access-token)
                 "accept" "application/json"}})
              body (:ring.response/body response)
              result (json/read-value body)]
          (is (= "application/json" (get-in response [:ring.response/headers "content-type"])))
          (is (vector? result))
          (is (= 3 (count result)))))

    ;; We are calling juxt.site.actions/pull-allowed-resources which
    ;; provides our query, but we want to experiment with creating our own
    ;; query with sub-queries, which we can compile to with GraphQL.

    ;; Now we have a get-patient with rules that we can bring into a sub-query

    ;; Let's start with an EQL that represents our query.
    ;; Metadata attributed to the EQL contains actions.
    ;; The EQL could be the target of the compilation of a GraphQL query.

    (let [db (xt/db *xt-node*)
            extract-subject-with-token
            (fn [token]
              (:juxt.site/subject
               (ffirst
                (xt/q db '{:find [(pull e [*])]
                           :where [[e :juxt.site/token token]]
                           :in [token]} token))))
            alice (extract-subject-with-token alice-access-token)
            bob (extract-subject-with-token bob-access-token)]

        ;; Here are some EQL examples. These are easy to construct
        ;; manually or target with a compiler, for example, for the
        ;; production of GraphQL, XML or CSV.

        ;; The actions are associated with EQL properties using
        ;; metadata.

        ;; Actions are where 'Form' is defined. Or, more precisely,
        ;; actions are where 'Form' and 'Code' meet.

        ;; An action defines the mapping from the Data to the Form (a
        ;; view of the data that best suits a given domain or
        ;; application context).

        ;; Additionally, actions define access controls that restrict
        ;; who can see what.

        ;; The data processing activities of a system are entirely
        ;; expressable in terms of actions.

        (testing "Graph query with two-levels of results"
          (let [q1 (first
                    ;; ^ The compilation process allows multiple queries to be
                    ;; specified in the EQL specification, each may be run in
                    ;; parallel. For now, we just run the first query.
                    (eqlc/compile-ast
                     db
                     (eql/query->ast
                      '[
                        {(:patients {:juxt.site/action "https://auth.hospital.com/actions/get-patient"})
                         [:xt/id
                          :name
                          :juxt.site/type
                          {(:measurements {:juxt.site/action "https://auth.hospital.com/actions/read-any-measurement"})
                           [:reading]}]}])))]

            (testing "Alice's view"
              (is (= #{{:name "Terry Levine"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/001"
                        :measurements nil}
                       {:name "Moshe Lynch"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/015"
                        :measurements nil}
                       {:name "Hazel Huynh"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/013"
                        :measurements nil}
                       {:name "Valarie Campos"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/019"
                        :measurements nil}
                       {:name "Lila Dickson"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/004"
                        :measurements nil}
                       {:name "Floyd Castro"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/006"
                        :measurements
                        [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                       {:name "Jeannie Finley"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/002"
                        :measurements nil}
                       {:name "Beulah Leonard"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/008"
                        :measurements nil}
                       {:name "Francesco Casey"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/014"
                        :measurements nil}
                       {:name "Angie Solis"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/005"
                        :measurements nil}
                       {:name "Jewel Blackburn"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/003"
                        :measurements nil}
                       {:name "Sondra Richardson"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/010"
                        :measurements
                        [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                         {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}
                       {:name "Monica Russell"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/009"
                        :measurements nil}
                       {:name "Rudy King"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/018"
                        :measurements nil}
                       {:name "Mark Richard"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/012"
                        :measurements nil}
                       {:name "Blanca Lindsey"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/017"
                        :measurements nil}
                       {:name "Elisabeth Riddle"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/020"
                        :measurements nil}
                       {:name "Melanie Black"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/007"
                        :measurements nil}
                       {:name "Kim Robles"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/011"
                        :measurements nil}
                       {:name "Darrel Schwartz"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/016"
                        :measurements nil}}
                     (eqlc/prune-result (xt/q db q1 alice nil)))))

            (testing "Bob's view"
              (is (= #{{:name "Lila Dickson"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/004"
                        :measurements nil}
                       {:name "Sondra Richardson"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/010"
                        :measurements
                        [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                         {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}
                       {:name "Monica Russell"
                        :juxt.site/type "https://hospital.com/types/patient"
                        :xt/id "https://hospital.com/patients/009"
                        :measurements nil}}
                     (eqlc/prune-result (xt/q db q1 bob nil)))))))

        (testing "Graph query with 3 levels of nesting"
          (let [q1 (first
                    (eqlc/compile-ast
                     db
                     (eql/query->ast
                      '[{(:doctors {:juxt.site/action "https://auth.hospital.com/actions/get-doctor"})
                         [:xt/id
                          :name
                          :juxt.site/type
                          {(:patients {:juxt.site/action "https://auth.hospital.com/actions/get-patient"})
                           [:xt/id
                            :name
                            :juxt.site/type
                            {(:readings {:juxt.site/action "https://auth.hospital.com/actions/read-any-measurement"})
                             [:reading]}]}]}])))]

            (testing "Alice's view"
              (is (= #{{:name "Dr. Jack Conway"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/001"
                        :patients
                        [{:name "Terry Levine"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/001"
                          :readings nil}
                         {:name "Jeannie Finley"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/002"
                          :readings nil}
                         {:name "Jewel Blackburn"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/003"
                          :readings nil}
                         {:name "Angie Solis"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/005"
                          :readings nil}]}
                       {:name "Dr. Murillo"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/002"
                        :patients
                        [{:name "Lila Dickson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/004"
                          :readings nil}
                         {:name "Angie Solis"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/005"
                          :readings nil}]}
                       {:name "Dr. Jackson"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/003"
                        :patients
                        [{:name "Floyd Castro"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/006"
                          :readings
                          [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                         {:name "Sondra Richardson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/010"
                          :readings
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}
                       {:name "Dr. Kim"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/004"
                        :patients nil}}
                     (eqlc/prune-result (xt/q db q1 alice nil)))))

            (testing "Bob's view"
              (is (= #{{:name "Dr. Jack Conway"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/001"
                        :patients nil}
                       {:name "Dr. Murillo"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/002"
                        :patients
                        [{:name "Lila Dickson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/004"
                          :readings nil}]}
                       {:name "Dr. Jackson"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/003"
                        :patients
                        [{:name "Sondra Richardson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/010"
                          :readings
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}
                       {:name "Dr. Kim"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/004"
                        :patients nil}}
                     (eqlc/prune-result (xt/q db q1 bob nil)))))))

        (testing "Graph query with parameters"
          ;; Get a particular doctor, by a simple search term.
          ;; Uses EQL parameters for this.
          (let [q1 (first
                    (eqlc/compile-ast
                     db
                     (eql/query->ast
                      '[{(:doctor {:juxt.site/action "https://auth.hospital.com/actions/get-doctor"
                                   :search "jack"})
                         [:xt/id
                          :name
                          :juxt.site/type
                          {(:patients {:juxt.site/action "https://auth.hospital.com/actions/get-patient"})
                           [:xt/id
                            :name
                            :juxt.site/type
                            {(:readings {:juxt.site/action "https://auth.hospital.com/actions/read-any-measurement"})
                             [:reading]}]}]}])))]

            (testing "Alice's view"
              (is (= #{{:name "Dr. Jack Conway"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/001"
                        :patients
                        [{:name "Terry Levine"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/001"
                          :readings nil}
                         {:name "Jeannie Finley"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/002"
                          :readings nil}
                         {:name "Jewel Blackburn"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/003"
                          :readings nil}
                         {:name "Angie Solis"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/005"
                          :readings nil}]}
                       {:name "Dr. Jackson"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/003"
                        :patients
                        [{:name "Floyd Castro"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/006"
                          :readings
                          [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                         {:name "Sondra Richardson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/010"
                          :readings
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}}
                     (eqlc/prune-result (xt/q db q1 alice nil)))))

            (testing "Bob's view"
              (is (= #{{:name "Dr. Jack Conway"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/001"
                        :patients nil}
                       {:name "Dr. Jackson"
                        :juxt.site/type "https://hospital.com/types/doctor"
                        :xt/id "https://hospital.com/doctors/003"
                        :patients
                        [{:name "Sondra Richardson"
                          :juxt.site/type "https://hospital.com/types/patient"
                          :xt/id "https://hospital.com/patients/010"
                          :readings
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}}
                     (eqlc/prune-result (xt/q db q1 bob nil))))))))

    ;; Modelling ideas

    ;; Doctor's have patients, patients have an assigned doctor.
    ;; A measurement must be taken by a doctor or other individual.
    ;; From the doctor, you can see patients.
    ;; A patient should be able to see their own medical file.

    #_(repl/e "https://hospital.com/patients/014")

    ;; See NHS National role-based access control (RBAC) for developers
    ;; "The database consists of:
    ;; Job Roles (‘R’ codes) - the set of roles that can be assigned to users, for example Clinical Practitioner (R8000)
    ;; Activities (‘B’ codes) - the set of activities that users can perform, for
    ;;  example Amend Patient Demographics (B0825)"
    ;; -- https://digital.nhs.uk/developer/guides-and-documentation/security-and-authorisation/national-rbac-for-developers

    ;; https://digital.nhs.uk/developer/api-catalogue/spine-directory-service-fhir

    ;; Additional scenarios:

    ;; Alice, Bob, Carlos - multiple joins, 3-level queries, multiple concurrent actions

    ;; The challenge is to combine the following:

    ;; 1. GraphQL schemas where fields in queries reference actions. For example:
    ;;
    ;; type Hospital { patients: [String] @site(action: https://auth.hospital.com/actions/list-patients) }
    ;;
    ;; type Doctor { patients: [String] @site(action: https://auth.hospital.com/actions/list-patients-by-doctor) }

    ;; Should https://auth.hospital.com/actions/list-patients-by-doctor exist
    ;; independently or instead be a reference to
    ;; https://auth.hospital.com/actions/list-patients with a join key? The former
    ;; is overly cumbersome and would require a lot of extra actions and
    ;; associated admin costs. (DONE: we have gone with the notion of an action being called in the context of another)

    ;; type Doctor {
    ;;   id ID
    ;;   patients(gender: String, costBasis: String): [Patient] @site(action: "https://auth.hospital.com/actions/list-patients" join: "primary-doctor")
    ;; }

    ;; The `patients` field transforms to a sub-query.

    ;; This sub-query is composed using the rules of list-patients.

    ;; Additional clauses are added that correspond to the arguments, with
    ;; optional arg->attribute mappings specified in (GraphQL) field
    ;; directive.

    ;; The 'join' key becomes an implicit argument. By default, the 'id' of
    ;; the doctor is passed as the argument value.

    ;; But, for many models, there is a xref relation. A patient's
    ;; relationship to a doctor is governed by a dedicated 'assignment'
    ;; document. This way, it is straight-forward to remove this assignment,
    ;; inspect a history of assignments, and to add further metadata about
    ;; the assignment.

    ;; The 'form' of the GraphQL should not expose this xref relation
    ;; directly, but use it to generate a mapping between patients and
    ;; doctors.

    ;; Where should the metadata for this xref relation be provided? For
    ;; example, if the assignment document has a type (and it certainly
    ;; should), where should this type be specificed? If it's directly in
    ;; the GraphQL SDL, then a) it both complicates and exposes the GraphQL
    ;; SDL to structural details and b) needs to be repeated for other
    ;; access protocols, like REST.

    ;; Therefore, it makes sense that the actions themselves understand how
    ;; to navigate these relationships. Actions can be both shared between
    ;; applications while still allowing applications to demand bespoke
    ;; actions where necessary.

    ;; Therefore it seems that actions are the 'form' documents.

    ;; Let's take the list-patients action. In the context of a doctor, it
    ;; needs to know how to join on the doctor id. The 'parent context',
    ;; whether it be a hosital, doctor or other type, is extremely common
    ;; (since data is often emitted as DAGs). Therefore each action should
    ;; be aware of the context in which it runs.

    ;; TODO: First, rewrite list-patients/get-patient to use SCI.

    ;; 2. Actions that have 'query' logic. Should that query logic be Flip?
    ;; Or reference other actions? To what extent is 'list-patients = fmap
    ;; get-patient' - is this a common case? Looks like we may need a
    ;; 'calculus' for defining actions in terms of other more fundamental
    ;; actions. Note: I think we're just seeing that get-patient and
    ;; list-patient *share* the same rules. There is no reason rules can't
    ;; be deduped via reference to independent documents, or even one to the
    ;; other:

    ;; {:xt/id "list-patients" :juxt.site/rules "get-patient"}
    ))

(deftest graphql-test

  (let [alice-session-token
        (login/login-with-form!
         *handler*
         :juxt.site/uri "https://auth.hospital.com/login"
         "username" "alice"
         "password" "garden")
        {alice-access-token "access_token"}
        (oauth/authorize!
         "https://auth.hospital.com/oauth/authorize"
         (merge
          alice-session-token
          {"client_id" "local-terminal"
           ;;"scope" ["https://example.org/oauth/scope/read-personal-data"]
           }))

        bob-session-token
        (login/login-with-form!
         *handler*
         :juxt.site/uri "https://auth.hospital.com/login"
         "username" "bob"
         "password" "walrus")
        {bob-access-token "access_token"}
        (oauth/authorize!
         "https://auth.hospital.com/oauth/authorize"
         (merge
          bob-session-token
          {"client_id" "local-terminal"
           ;;"scope" ["https://example.org/oauth/scope/read-personal-data"]
           }))

        db (xt/db *xt-node*)

        ;; This is just a function to extract the subjects from the
        ;; database.  These subjects are then used below to test directly
        ;; against the database, rather than going via Ring .
        extract-subject-with-token
        (fn [token]
          (:juxt.site/subject
           (ffirst
            (xt/q db '{:find [(pull e [*])]
                       :where [[e :juxt.site/token token]]
                       :in [token]} token))))
        alice (extract-subject-with-token alice-access-token)
        bob (extract-subject-with-token bob-access-token)

        schema
        (-> "juxt/site/schema.graphql"
            io/resource
            slurp
            grab.parser/parse
            grab.schema/compile-schema)

        eql-ast
        (graphql->eql-ast
         schema
         (-> "query GetDoctors { doctors(search: \"jack\") { id _type name patients { id _type name readings { id _type } } } }"
             grab.parser/parse
             (grab.document/compile-document schema)
             (grab.document/get-operation "GetDoctors")))

        q (first (eqlc/compile-ast db eql-ast))

        ]

    (testing "From GraphQL to database results"
      (testing "Alice's view"
        (is (= #{{:name "Dr. Jack Conway",
                  :juxt.site/type "https://hospital.com/types/doctor",
                  :xt/id "https://hospital.com/doctors/001",
                  :patients
                  [{:name "Terry Levine",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/001",
                    :readings nil}
                   {:name "Jeannie Finley",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/002",
                    :readings nil}
                   {:name "Jewel Blackburn",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/003",
                    :readings nil}
                   {:name "Angie Solis",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/005",
                    :readings nil}]}
                 {:name "Dr. Jackson",
                  :juxt.site/type "https://hospital.com/types/doctor",
                  :xt/id "https://hospital.com/doctors/003",
                  :patients
                  [{:name "Floyd Castro",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/006",
                    :readings nil}
                   {:name "Sondra Richardson",
                    :juxt.site/type "https://hospital.com/types/patient",
                    :xt/id "https://hospital.com/patients/010",
                    :readings nil}]}}
               (eqlc/prune-result (xt/q db q alice nil))))))

    (testing "Bob's view"
      (is (= #{{:name "Dr. Jack Conway",
                :juxt.site/type "https://hospital.com/types/doctor",
                :xt/id "https://hospital.com/doctors/001",
                :patients nil}
               {:name "Dr. Jackson",
                :juxt.site/type "https://hospital.com/types/doctor",
                :xt/id "https://hospital.com/doctors/003",
                :patients
                [{:name "Sondra Richardson",
                  :juxt.site/type "https://hospital.com/types/patient",
                  :xt/id "https://hospital.com/patients/010",
                  :readings nil}]}}
             (eqlc/prune-result (xt/q db q bob nil)))))))
