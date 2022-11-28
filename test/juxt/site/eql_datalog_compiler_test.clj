;; Copyright © 2022, JUXT LTD.

(ns juxt.site.eql-datalog-compiler-test
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [edn-query-language.core :as eql]
   [jsonista.core :as json]
   [juxt.site.resources.example-users :as example-users]
   [juxt.grab.alpha.document :as grab.document]
   [juxt.grab.alpha.parser :as grab.parser]
   [juxt.grab.alpha.schema :as grab.schema]
   [juxt.http :as-alias http]
   [juxt.site.resources.form-based-auth :as form-based-auth]
   [juxt.site.resources.oauth :as oauth]
   [juxt.site.resources.protection-space :as protection-space]
   [juxt.site.resources.session-scope :as session-scope]
   [juxt.site.resources.user :as user]
   [juxt.site :as-alias site]
   [juxt.site.eql-datalog-compiler :as eqlc]
   [juxt.site.graphql-eql-compiler :refer [graphql->eql-ast]]
   [juxt.site.init :as init]
   [juxt.test.util :refer [with-system-xt with-fixtures with-resources with-handler *xt-node* *handler*] :as tutil]
   [xtdb.api :as xt]))

(use-fixtures :each with-system-xt with-handler)

(defn create-action-register-doctor! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-doctor"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://site.test/doctors/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program

     (pr-str
      '(let [content-type (-> *ctx*
                              :juxt.site/received-representation
                              :juxt.http/content-type)
             body (-> *ctx*
                      :juxt.site/received-representation
                      :juxt.http/body)]
         (case content-type
           "application/edn"
           (some->
            body
            (String.)
            clojure.edn/read-string
            juxt.site.malli/validate-input
            (assoc
             :juxt.site/type "https://site.test/types/doctor"
             :juxt.site/protection-spaces #{"https://site.test/protection-spaces/bearer"}
             :juxt.site/methods
             {:get {:juxt.site/actions #{"https://site.test/actions/get-doctor"}}
              :head {:juxt.site/actions #{"https://site.test/actions/get-doctor"}}
              :options {}})))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-doctor! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-doctor"
    :juxt.site/subject "https://site.test/subjects/system"
    :juxt.site/action "https://site.test/actions/register-doctor"
    :juxt.site/purpose nil}))

(defn create-action-register-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-patient"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://site.test/patients/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (pr-str
      '(let [content-type (-> *ctx*
                              :juxt.site/received-representation
                              :juxt.http/content-type)
             body (-> *ctx*
                      :juxt.site/received-representation
                      :juxt.http/body)]
         (case content-type
           "application/edn"
           (some->
            body
            (String.)
            clojure.edn/read-string
            juxt.site.malli/validate-input
            (assoc
             :juxt.site/type "https://site.test/types/patient"
             :juxt.site/protection-spaces #{"https://site.test/protection-spaces/bearer"}
             :juxt.site/methods
             {:get {:juxt.site/actions #{"https://site.test/actions/get-patient"}}
              :head {:juxt.site/actions #{"https://site.test/actions/get-patient"}}
              :options {}})))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-patient"
    :juxt.site/subject "https://site.test/subjects/system"
    :juxt.site/action "https://site.test/actions/register-patient"
    :juxt.site/purpose nil}))

;; A patient's record contains an attribute that indicates the set of assigned doctors.
(defn create-action-assign-doctor-to-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/assign-doctor-to-patient"

    ;; A POST to a patient URL?
    ;; What if there are a number of actions one can perform on a patient?
    ;; A PATCH to a patient????

    :juxt.site.malli/input-schema
    [:map
     [:patient [:re "https://site.test/patients/.*"]]
     [:doctor [:re "https://site.test/doctors/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (pr-str
      '(let [content-type (-> *ctx*
                              :juxt.site/received-representation
                              :juxt.http/content-type)
             body (-> *ctx*
                      :juxt.site/received-representation
                      :juxt.http/body)]
         (let [input
               (case content-type
                 "application/edn"
                 (some->
                  body
                  (String.)
                  clojure.edn/read-string
                  juxt.site.malli/validate-input
                  ))]

           (when-not input
             (throw (ex-info "No input" {})))

           (let [[_ patient-id] (re-matches #"https://site.test/patients/(.*)" (:patient input))
                 [_ doctor-id] (re-matches #"https://site.test/doctors/(.*)" (:doctor input))
                 id (format "https://site.test/assignments/patient/%s/doctor/%s" patient-id doctor-id)]
             {:patient-id (:patient input)
              :doctor-id (:doctor input)
              :id id}))))}

    :juxt.site/transact
    {
     :juxt.site.sci/program
     (pr-str
      '(let [{:keys [id patient-id doctor-id]} *prepare*]
         [[:xtdb.api/put
           {:xt/id id
            :patient patient-id
            :doctor doctor-id
            ::site/type "https://site.test/types/doctor-patient-assignment"}]]))}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]]}))

(defn grant-permission-to-invoke-action-assign-doctor-to-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/assign-doctor-to-patient"
    :juxt.site/subject "https://site.test/subjects/system"
    :juxt.site/action "https://site.test/actions/assign-doctor-to-patient"
    :juxt.site/purpose nil}))

(defn assign-doctor-to-patient! [{:keys [patient doctor]}]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/assign-doctor-to-patient"
   {:patient patient
    :doctor doctor}))

(defn create-action-get-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-patient"

    :juxt.site/action-contexts
    {"https://site.test/actions/get-doctor"
     {:juxt.site/additional-where-clauses
      '[[ass ::site/type "https://site.test/types/doctor-patient-assignment"]
        [ass :patient e]
        [ass :doctor parent]]}}

    :juxt.site/rules
    '[
      ;; TODO: Performance tweak: put [subject] to hint that subject is always
      ;; bound - see @jdt for details
      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]
       [resource :juxt.site/type "https://site.test/types/patient"]
       [permission :patient :all]]

      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]
       [resource :juxt.site/type "https://site.test/types/patient"]
       [permission :patient resource]]]}))

(defn grant-permission-to-get-any-patient! [username]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/get-any-patient" username)
    :juxt.site/action "https://site.test/actions/get-patient"
    :juxt.site/user (format "https://site.test/users/%s" username)
    :patient :all
    :juxt.site/purpose nil
    }))

(defn grant-permission-to-get-patient! [username pid]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/get-patient/%s" username pid)
    :juxt.site/action "https://site.test/actions/get-patient"
    :juxt.site/user (format "https://site.test/users/%s" username)
    :patient (format "https://site.test/patients/%s" pid)
    :juxt.site/purpose nil
    }))

(defn create-action-list-patients! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/list-patients"

    ;; What if this was the resource we will target with GET?
    ;; The /patients is merely an alias.

    ;; Are actions just resources with ACL rules?
    ;; Actions are already resources.
    ;; Maybe any resource can be an action?

    ;; What are our other examples of targeting actions?
    ;; POST /actions/install-graphql-endpoint
    ;; (book_test line ~500, book line ~1197)

    :juxt.site/state
    {:juxt.site.sci/program
     (pr-str
      ;; Perform a query, using the rules in get-patient. It would be a good
      ;; idea to restrict the ability for actions to make general queries
      ;; against the database. By only exposing API functions such as
      ;; pull-allowed-resources to this SCI script, we can limit the power of
      ;; actions thereby securing them. This is preferable to limiting the
      ;; ability to deploy actions to a select group of highly authorized
      ;; individuals.
      ;;
      ;; TODO: Go through the use-cases which already make general lookups
      ;; and queries to XT and see if we can rewrite them to use a more
      ;; restricted API.
      '(juxt.site/pull-allowed-resources
        #{"https://site.test/actions/get-patient"}))}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]]]}))

(defn grant-permission-to-list-patients! [username]
  (init/do-action
    "https://site.test/subjects/system"
    "https://site.test/actions/grant-permission"
    {:xt/id (format "https://site.test/permissions/%s/list-patients" username)
     :juxt.site/action "https://site.test/actions/list-patients"
     :juxt.site/user (format "https://site.test/users/%s" username)
     :juxt.site/purpose nil}))

(defn create-action-register-patient-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-patient-measurement"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://site.test/measurements/.*"]]
     [:patient [:re "https://site.test/patients/.*"]]]

    :juxt.site/prepare
    {:juxt.site.sci/program
     (pr-str
      '(let [content-type (-> *ctx*
                              :juxt.site/received-representation
                              :juxt.http/content-type)
             body (-> *ctx*
                      :juxt.site/received-representation
                      :juxt.http/body)]
         (case content-type
           "application/edn"
           (some->
            body
            (String.)
            clojure.edn/read-string
            juxt.site.malli/validate-input
            (assoc
             :juxt.site/type "https://site.test/types/measurement"
             :juxt.site/protection-spaces #{"https://site.test/protection-spaces/bearer"})))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-patient-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-patient-measurement"
    :juxt.site/subject "https://site.test/subjects/system"
    :juxt.site/action "https://site.test/actions/register-patient-measurement"
    :juxt.site/purpose nil}))

;; Warning, this is an overly broad action! TODO: Narrow this action.
;; It permits grantees access to ALL measurements!!
(defn create-action-read-any-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/read-any-measurement"

    :juxt.site/action-contexts
    {"https://site.test/actions/get-patient"
     {:juxt.site/additional-where-clauses
      '[[e :patient parent]
        [e ::site/type "https://site.test/types/measurement"]]}}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]
       ;;[resource :juxt.site/type "https://site.test/types/measurement"]
       ]]}))

(defn grant-permission-to-read-any-measurement! [username]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/read-any-measurement" username)
    :juxt.site/action "https://site.test/actions/read-any-measurement"
    :juxt.site/user (format "https://site.test/users/%s" username)
    :juxt.site/purpose nil
    }))

(defn create-action-get-doctor! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-doctor"

    :juxt.site/params
    {:search
     {:juxt.site/additional-where-clauses
      '[[e :name doctor-name]
        [(re-seq pat doctor-name)]
        ;; Case-insensitive search
        [(str "(?i:" $ ")") regex]
        [(re-pattern regex) pat]
        ]}}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.site/user-identity id]
       [id :juxt.site/user user]
       [permission :juxt.site/user user]
       [resource :juxt.site/type "https://site.test/types/doctor"]]
      ]}))

(defn grant-permission-to-get-doctor! [username]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/get-doctor" username)
    :juxt.site/action "https://site.test/actions/get-doctor"
    :juxt.site/user (format "https://site.test/users/%s" username)
    :juxt.site/purpose nil}))

(def DOCTOR_NAMES
  {"001" "Dr. Jack Conway"
   "002" "Dr. Murillo"
   "003" "Dr. Jackson"
   "004" "Dr. Kim"})

(def PATIENT_NAMES
  {"001" "Terry Levine"
   "002" "Jeannie Finley"
   "003" "Jewel Blackburn"
   "004" "Lila Dickson"
   "005" "Angie Solis"
   "006" "Floyd Castro"
   "007" "Melanie Black"
   "008" "Beulah Leonard"
   "009" "Monica Russell"
   "010" "Sondra Richardson"
   "011" "Kim Robles"
   "012" "Mark Richard"
   "013" "Hazel Huynh"
   "014" "Francesco Casey"
   "015" "Moshe Lynch"
   "016" "Darrel Schwartz"
   "017" "Blanca Lindsey"
   "018" "Rudy King"
   "019" "Valarie Campos"
   "020" "Elisabeth Riddle"})

(defn register-doctor! [{:keys [id params] :as arg1}]
  (let [subid (get params "id")
        name (get DOCTOR_NAMES subid)]
    (when (nil? name)
      (throw (ex-info "arg1" {:arg1 arg1}))
      )
    #_(assert name (format "No name found for id: %s" subid))
    (init/do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/register-doctor"
     {:xt/id id
      :name name
      ::http/content-type "application/json"
      ::http/content (json/write-value-as-string {"name" name})})))

(defn register-patient! [{:keys [id params]}]
  (let [pid (get params "pid")
        name (get PATIENT_NAMES pid)]
    (assert name (format "No name found for pid: %s" pid))
    (init/do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/register-patient"
     {:xt/id id
      :name name
      ::http/content-type "application/json"
      ::http/content (json/write-value-as-string {"name" name})})))

(def dependency-graph
  {"https://site.test/actions/register-doctor"
   {:create #'create-action-register-doctor!
    :deps #{::init/system}}

   "https://site.test/permissions/system/register-doctor"
   {:create #'grant-permission-to-invoke-action-register-doctor!
    :deps #{::init/system}}

   "https://site.test/actions/register-patient"
   {:create #'create-action-register-patient!
    :deps #{::init/system}}

   "https://site.test/permissions/system/register-patient"
   {:create #'grant-permission-to-invoke-action-register-patient!
    :deps #{::init/system}}

   "https://site.test/actions/list-patients"
   {:create #'create-action-list-patients!
    :deps #{::init/system}}

   "https://site.test/actions/get-patient"
   {:create #'create-action-get-patient!
    :deps #{::init/system}}

   "https://site.test/actions/get-doctor"
   {:create #'create-action-get-doctor!
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/get-any-patient"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-get-any-patient! (get params "username")))
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/get-patient/{pid}"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-get-patient! (get params "username") (get params "pid")))
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/list-patients"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-list-patients! (get params "username")))
    :deps #{::init/system}}

   "https://site.test/actions/register-patient-measurement"
   {:create #'create-action-register-patient-measurement!
    :deps #{::init/system}}

   "https://site.test/permissions/system/register-patient-measurement"
   {:create #'grant-permission-to-invoke-action-register-patient-measurement!
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/get-doctor"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-get-doctor! (get params "username")))
    :deps #{::init/system}}

   "https://site.test/doctors/{id}"
   {:create (fn [args]
              (register-doctor! args))
    :deps #{::init/system
            "https://site.test/actions/register-doctor"
            "https://site.test/permissions/system/register-doctor"}}

   "https://site.test/patients/{pid}"
   {:create (fn [args]
              (register-patient! args))
    :deps #{::init/system
            "https://site.test/actions/register-patient"
            "https://site.test/permissions/system/register-patient"}}

   "https://site.test/patients"
   {:create
    (fn [_]
      (init/put!
       {:xt/id "https://site.test/patients"
        :juxt.site/methods
        {:get
         {:juxt.site/actions #{"https://site.test/actions/list-patients"}}}
        :juxt.site/protection-spaces #{"https://site.test/protection-spaces/bearer"}
        ::http/content-type "application/json"
        :juxt.site/respond
        {:juxt.site.sci/program
         (pr-str
          '(let [content (jsonista.core/write-value-as-string *state*)]
             (-> *ctx*
                 (assoc :ring.response/body content)
                 (update :ring.response/headers assoc "content-length" (count (.getBytes content)))
                 )))}}))
    :deps #{::init/system}}

   "https://site.test/actions/read-any-measurement"
   {:create #'create-action-read-any-measurement!
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/read-any-measurement"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-read-any-measurement! (get params "username")))
    :deps #{::init/system}}

   "https://site.test/actions/assign-doctor-to-patient"
   {:create #'create-action-assign-doctor-to-patient!
    :deps #{::init/system}}

   "https://site.test/permissions/system/assign-doctor-to-patient"
   {:create #'grant-permission-to-invoke-action-assign-doctor-to-patient!
    :deps #{::init/system}}

   "https://site.test/assignments/patient/{pid}/doctor/{did}"
   {:create (fn [{:keys [params]}]
              (assign-doctor-to-patient!
               {:patient (format "https://site.test/patients/%s" (get params "pid"))
                :doctor (format "https://site.test/doctors/%s" (get params "did"))}))
    :deps (fn [params _]
            (when (nil? (get params "pid"))
              (throw (ex-info "Bad params (pid)" {:params params})))
            (when (nil? (get params "did"))
              (throw (ex-info "Bad params (did)" {:params params})))
            #{::init/system
              (format "https://site.test/patients/%s" (get params "pid"))
              (format "https://site.test/doctors/%s" (get params "did"))
              "https://site.test/actions/assign-doctor-to-patient"
              "https://site.test/permissions/system/assign-doctor-to-patient"})}})

(deftest eql-with-acl-test
  (let [resources
        (->
         #{::init/system

           "https://site.test/login"
           "https://site.test/user-identities/alice"
           "https://site.test/user-identities/bob"
           "https://site.test/user-identities/carlos"

           "https://site.test/oauth/authorize"
           "https://site.test/session-scopes/default"
           "https://site.test/permissions/alice-can-authorize"
           "https://site.test/permissions/bob-can-authorize"
           "https://site.test/permissions/carlos-can-authorize"
           "https://site.test/applications/local-terminal"

           "https://site.test/protection-spaces/bearer"

           "https://site.test/actions/get-patient"
           "https://site.test/permissions/alice/get-any-patient"
           "https://site.test/permissions/bob/get-patient/004"
           "https://site.test/permissions/bob/get-patient/009"
           "https://site.test/permissions/bob/get-patient/010"
           "https://site.test/actions/list-patients"
           "https://site.test/permissions/alice/list-patients"
           "https://site.test/permissions/bob/list-patients"

           "https://site.test/actions/register-patient-measurement"
           "https://site.test/permissions/system/register-patient-measurement"

           "https://site.test/actions/get-doctor"
           "https://site.test/permissions/alice/get-doctor"
           "https://site.test/permissions/bob/get-doctor"

           "https://site.test/patients"

           "https://site.test/actions/read-any-measurement"
           "https://site.test/permissions/alice/read-any-measurement"
           "https://site.test/permissions/bob/read-any-measurement"}

         ;; Add some patients
         (into
          (for [i (range 1 (inc 20))]
            (format "https://site.test/patients/%03d" i)))

         ;; Add some doctors
         (into
          (for [i (range 1 (inc 4))]
            (format "https://site.test/doctors/%03d" i)))

         (into
          #{"https://site.test/assignments/patient/001/doctor/001"
            "https://site.test/assignments/patient/002/doctor/001"
            "https://site.test/assignments/patient/003/doctor/001"
            "https://site.test/assignments/patient/004/doctor/002"
            "https://site.test/assignments/patient/005/doctor/001"
            "https://site.test/assignments/patient/005/doctor/002"
            "https://site.test/assignments/patient/006/doctor/003"
            "https://site.test/assignments/patient/010/doctor/003"}))]

    (with-resources
      (with-meta resources
        {:dependency-graphs
         #{session-scope/dependency-graph
           user/dependency-graph
           protection-space/dependency-graph
           form-based-auth/dependency-graph
           example-users/dependency-graph
           oauth/dependency-graph
           dependency-graph}})

      ;; Create some measurements
      (init/do-action
       "https://site.test/subjects/system"
       "https://site.test/actions/register-patient-measurement"
       {:xt/id "https://site.test/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1e"
        :patient "https://site.test/patients/004"
        :reading {"heartRate" "120"
                  "bloodPressure" "137/80"}})

      (init/do-action
       "https://site.test/subjects/system"
       "https://site.test/actions/register-patient-measurement"
       {:xt/id "https://site.test/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1e"
        :patient "https://site.test/patients/006"
        :reading {"heartRate" "82"
                  "bloodPressure" "198/160"}})

      (init/do-action
       "https://site.test/subjects/system"
       "https://site.test/actions/register-patient-measurement"
       {:xt/id "https://site.test/measurements/eeda3b49-2e96-42fc-9e6a-e89e2eb68c24"
        :patient "https://site.test/patients/010"
        :reading {"heartRate" "85"
                  "bloodPressure" "120/80"}})

      (init/do-action
       "https://site.test/subjects/system"
       "https://site.test/actions/register-patient-measurement"
       {:xt/id "https://site.test/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1d"
        :patient "https://site.test/patients/010"
        :reading {"heartRate" "87"
                  "bloodPressure" "127/80"}})

      (let [alice-session-token
            (form-based-auth/login-with-form!
             *handler*
             :juxt.site/uri "https://site.test/login"
             "username" "alice"
             "password" "garden")

            {alice-access-token "access_token" error "error"}
            (oauth/authorize!
             (merge
              alice-session-token
              {"client_id" "local-terminal"
               ;; "scope" ["https://site.test/oauth/scope/read-personal-data"]
               }))
            _ (is (nil? error) (format "OAuth2 grant error: %s" error))

            bob-session-token
            (form-based-auth/login-with-form!
             *handler*
             :juxt.site/uri "https://site.test/login"
             "username" "bob"
             "password" "walrus")
            {bob-access-token "access_token"
             error "error"}
            (oauth/authorize!
             (merge
              bob-session-token
              {"client_id" "local-terminal"
               ;;"scope" ["https://site.test/oauth/scope/read-personal-data"]
               })
             )
            _ (is (nil? error) (format "OAuth2 grant error: %s" error))]

        ;; Add a /patient/XXX resource to serve an individual patient.

        ;; https://site.test/actions/get-patient must perform an XT query.

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
        ;; https://site.test/actions/get-patient action rule has the clause
        ;; '[resource :juxt.site/type
        ;; "https://site.test/types/patient"]' then it is not a permitted
        ;; action. We must separate the actions that allow access to a
        ;; uri-template'd resource and the actions that create the body
        ;; payload.

        ;; Alice can access a particular patient because she has a particularly
        ;; broad permission on the get-patient action

        (testing "Access to /patient/005"
          (let [response
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/patients/005"
                  :ring.request/headers
                  {"authorization" (format "Bearer %s" alice-access-token)
                   "accept" "application/json"}})]

            (is (= (json/write-value-as-string {"name" "Angie Solis"})
                   (String. (:ring.response/body response))))
            (is (= 200 (:ring.response/status response))))

          ;; Bob can't see the patient details of Angie Solis
          (let [response (*handler*
                          {:ring.request/method :get
                           :ring.request/path "/patients/005"
                           :ring.request/headers
                           {"authorization" (format "Bearer %s" bob-access-token)
                            "accept" "application/json"}})]
            (is (= 403 (:ring.response/status response)))))

        (testing "List patients with /patients"

          ;; Alice sees all 20 patients
          (let [response
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/patients"
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
                  :ring.request/path "/patients"
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
                (::site/subject
                 (ffirst
                  (xt/q db '{:find [(pull e [*])]
                             :where [[e ::site/token token]]
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
                          {(:patients {::site/action "https://site.test/actions/get-patient"})
                           [:xt/id
                            :name
                            ::site/type
                            {(:measurements {::site/action "https://site.test/actions/read-any-measurement"})
                             [:reading]}]}])))]

              (testing "Alice's view"
                (is (= #{{:name "Terry Levine"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/001"
                          :measurements nil}
                         {:name "Moshe Lynch"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/015"
                          :measurements nil}
                         {:name "Hazel Huynh"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/013"
                          :measurements nil}
                         {:name "Valarie Campos"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/019"
                          :measurements nil}
                         {:name "Lila Dickson"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/004"
                          :measurements nil}
                         {:name "Floyd Castro"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/006"
                          :measurements
                          [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                         {:name "Jeannie Finley"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/002"
                          :measurements nil}
                         {:name "Beulah Leonard"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/008"
                          :measurements nil}
                         {:name "Francesco Casey"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/014"
                          :measurements nil}
                         {:name "Angie Solis"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/005"
                          :measurements nil}
                         {:name "Jewel Blackburn"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/003"
                          :measurements nil}
                         {:name "Sondra Richardson"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/010"
                          :measurements
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}
                         {:name "Monica Russell"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/009"
                          :measurements nil}
                         {:name "Rudy King"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/018"
                          :measurements nil}
                         {:name "Mark Richard"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/012"
                          :measurements nil}
                         {:name "Blanca Lindsey"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/017"
                          :measurements nil}
                         {:name "Elisabeth Riddle"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/020"
                          :measurements nil}
                         {:name "Melanie Black"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/007"
                          :measurements nil}
                         {:name "Kim Robles"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/011"
                          :measurements nil}
                         {:name "Darrel Schwartz"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/016"
                          :measurements nil}}
                       (eqlc/prune-result (xt/q db q1 alice nil)))))

              (testing "Bob's view"
                (is (= #{{:name "Lila Dickson"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/004"
                          :measurements nil}
                         {:name "Sondra Richardson"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/010"
                          :measurements
                          [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                           {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}
                         {:name "Monica Russell"
                          :juxt.site/type "https://site.test/types/patient"
                          :xt/id "https://site.test/patients/009"
                          :measurements nil}}
                       (eqlc/prune-result (xt/q db q1 bob nil)))))))

          (testing "Graph query with 3 levels of nesting"
            (let [q1 (first
                      (eqlc/compile-ast
                       db
                       (eql/query->ast
                        '[{(:doctors {::site/action "https://site.test/actions/get-doctor"})
                           [:xt/id
                            :name
                            ::site/type
                            {(:patients {::site/action "https://site.test/actions/get-patient"})
                             [:xt/id
                              :name
                              ::site/type
                              {(:readings {::site/action "https://site.test/actions/read-any-measurement"})
                               [:reading]}]}]}])))]

              (testing "Alice's view"
                (is (= #{{:name "Dr. Jack Conway"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/001"
                          :patients
                          [{:name "Terry Levine"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/001"
                            :readings nil}
                           {:name "Jeannie Finley"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/002"
                            :readings nil}
                           {:name "Jewel Blackburn"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/003"
                            :readings nil}
                           {:name "Angie Solis"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/005"
                            :readings nil}]}
                         {:name "Dr. Murillo"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/002"
                          :patients
                          [{:name "Lila Dickson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/004"
                            :readings nil}
                           {:name "Angie Solis"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/005"
                            :readings nil}]}
                         {:name "Dr. Jackson"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/003"
                          :patients
                          [{:name "Floyd Castro"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/006"
                            :readings
                            [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                           {:name "Sondra Richardson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/010"
                            :readings
                            [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                             {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}
                         {:name "Dr. Kim"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/004"
                          :patients nil}}
                       (eqlc/prune-result (xt/q db q1 alice nil)))))

              (testing "Bob's view"
                (is (= #{{:name "Dr. Jack Conway"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/001"
                          :patients nil}
                         {:name "Dr. Murillo"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/002"
                          :patients
                          [{:name "Lila Dickson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/004"
                            :readings nil}]}
                         {:name "Dr. Jackson"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/003"
                          :patients
                          [{:name "Sondra Richardson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/010"
                            :readings
                            [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                             {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}
                         {:name "Dr. Kim"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/004"
                          :patients nil}}
                       (eqlc/prune-result (xt/q db q1 bob nil)))))))

          (testing "Graph query with parameters"
            ;; Get a particular doctor, by a simple search term.
            ;; Uses EQL parameters for this.
            (let [q1 (first
                      (eqlc/compile-ast
                       db
                       (eql/query->ast
                        '[{(:doctor {::site/action "https://site.test/actions/get-doctor"
                                     :search "jack"})
                           [:xt/id
                            :name
                            ::site/type
                            {(:patients {::site/action "https://site.test/actions/get-patient"})
                             [:xt/id
                              :name
                              ::site/type
                              {(:readings {::site/action "https://site.test/actions/read-any-measurement"})
                               [:reading]}]}]}])))]

              (testing "Alice's view"
                (is (= #{{:name "Dr. Jack Conway"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/001"
                          :patients
                          [{:name "Terry Levine"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/001"
                            :readings nil}
                           {:name "Jeannie Finley"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/002"
                            :readings nil}
                           {:name "Jewel Blackburn"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/003"
                            :readings nil}
                           {:name "Angie Solis"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/005"
                            :readings nil}]}
                         {:name "Dr. Jackson"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/003"
                          :patients
                          [{:name "Floyd Castro"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/006"
                            :readings
                            [{:reading {"bloodPressure" "198/160" "heartRate" "82"}}]}
                           {:name "Sondra Richardson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/010"
                            :readings
                            [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                             {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}}
                       (eqlc/prune-result (xt/q db q1 alice nil)))))

              (testing "Bob's view"
                (is (= #{{:name "Dr. Jack Conway"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/001"
                          :patients nil}
                         {:name "Dr. Jackson"
                          :juxt.site/type "https://site.test/types/doctor"
                          :xt/id "https://site.test/doctors/003"
                          :patients
                          [{:name "Sondra Richardson"
                            :juxt.site/type "https://site.test/types/patient"
                            :xt/id "https://site.test/patients/010"
                            :readings
                            [{:reading {"bloodPressure" "127/80" "heartRate" "87"}}
                             {:reading {"bloodPressure" "120/80" "heartRate" "85"}}]}]}}
                       (eqlc/prune-result (xt/q db q1 bob nil))))))))

        ;; Modelling ideas

        ;; Doctor's have patients, patients have an assigned doctor.
        ;; A measurement must be taken by a doctor or other individual.
        ;; From the doctor, you can see patients.
        ;; A patient should be able to see their own medical file.

        #_(repl/e "https://site.test/patients/014")

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
        ;; type Hospital { patients: [String] @site(action: https://site.test/actions/list-patients) }
        ;;
        ;; type Doctor { patients: [String] @site(action: https://site.test/actions/list-patients-by-doctor) }

        ;; Should https://site.test/actions/list-patients-by-doctor exist
        ;; independently or instead be a reference to
        ;; https://site.test/actions/list-patients with a join key? The former
        ;; is overly cumbersome and would require a lot of extra actions and
        ;; associated admin costs. (DONE: we have gone with the notion of an action being called in the context of another)

        ;; type Doctor {
        ;;   id ID
        ;;   patients(gender: String, costBasis: String): [Patient] @site(action: "https://site.test/actions/list-patients" join: "primary-doctor")
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

        ;; {:xt/id "list-patients" ::site/rules "get-patient"}

        ;; Idea: Break GraphQL schemas into constituent types and create
        ;; individual resources, one resource per type. Use 'set' difference to
        ;; identity types to delete. Create HTML from /graphql to show a nice
        ;; HTML page with a list of types.


        ))))

(deftest graphql-test
  (let [resources
        (->
         #{::init/system

           "https://site.test/login"
           "https://site.test/user-identities/alice"
           "https://site.test/user-identities/bob"
           "https://site.test/user-identities/carlos"

           "https://site.test/oauth/authorize"
           "https://site.test/session-scopes/default"
           "https://site.test/permissions/alice-can-authorize"
           "https://site.test/permissions/bob-can-authorize"
           "https://site.test/permissions/carlos-can-authorize"
           "https://site.test/applications/local-terminal"

           "https://site.test/actions/get-patient"
           "https://site.test/permissions/alice/get-any-patient"
           "https://site.test/permissions/bob/get-patient/004"
           "https://site.test/permissions/bob/get-patient/009"
           "https://site.test/permissions/bob/get-patient/010"
           "https://site.test/actions/list-patients"
           "https://site.test/permissions/alice/list-patients"
           "https://site.test/permissions/bob/list-patients"

           "https://site.test/actions/register-patient-measurement"
           "https://site.test/permissions/system/register-patient-measurement"

           "https://site.test/actions/get-doctor"
           "https://site.test/permissions/alice/get-doctor"
           "https://site.test/permissions/bob/get-doctor"

           "https://site.test/patients"

           "https://site.test/actions/read-any-measurement"
           "https://site.test/permissions/alice/read-any-measurement"
           "https://site.test/permissions/bob/read-any-measurement"}

         ;; Add some patients
         (into
          (for [i (range 1 (inc 20))]
            (format "https://site.test/patients/%03d" i)))

         ;; Add some doctors
         (into
          (for [i (range 1 (inc 4))]
            (format "https://site.test/doctors/%03d" i)))

         (into
          #{"https://site.test/assignments/patient/001/doctor/001"
            "https://site.test/assignments/patient/002/doctor/001"
            "https://site.test/assignments/patient/003/doctor/001"
            "https://site.test/assignments/patient/004/doctor/002"
            "https://site.test/assignments/patient/005/doctor/001"
            "https://site.test/assignments/patient/005/doctor/002"
            "https://site.test/assignments/patient/006/doctor/003"
            "https://site.test/assignments/patient/010/doctor/003"}))]

    (with-resources
      (with-meta resources
        {:dependency-graphs
         #{session-scope/dependency-graph
           user/dependency-graph
           form-based-auth/dependency-graph
           example-users/dependency-graph
           oauth/dependency-graph
           dependency-graph}})

      (let [alice-session-token
            (form-based-auth/login-with-form!
             *handler*
             :juxt.site/uri "https://site.test/login"
             "username" "alice"
             "password" "garden")
            {alice-access-token "access_token"}
            (oauth/authorize!
             (merge
              alice-session-token
              {"client_id" "local-terminal"
               ;;"scope" ["https://site.test/oauth/scope/read-personal-data"]
               }))

            bob-session-token
            (form-based-auth/login-with-form!
             *handler*
             :juxt.site/uri "https://site.test/login"
             "username" "bob"
             "password" "walrus")
            {bob-access-token "access_token"}
            (oauth/authorize!
             (merge
              bob-session-token
              {"client_id" "local-terminal"
               ;;"scope" ["https://site.test/oauth/scope/read-personal-data"]
               }))

            db (xt/db *xt-node*)

            ;; This is just a function to extract the subjects from the
            ;; database.  These subjects are then used below to test directly
            ;; against the database, rather than going via Ring .
            extract-subject-with-token
            (fn [token]
              (::site/subject
               (ffirst
                (xt/q db '{:find [(pull e [*])]
                           :where [[e ::site/token token]]
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
                          :juxt.site/type "https://site.test/types/doctor",
                          :xt/id "https://site.test/doctors/001",
                          :patients
                          [{:name "Terry Levine",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/001",
                            :readings nil}
                           {:name "Jeannie Finley",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/002",
                            :readings nil}
                           {:name "Jewel Blackburn",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/003",
                            :readings nil}
                           {:name "Angie Solis",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/005",
                            :readings nil}]}
                         {:name "Dr. Jackson",
                          :juxt.site/type "https://site.test/types/doctor",
                          :xt/id "https://site.test/doctors/003",
                          :patients
                          [{:name "Floyd Castro",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/006",
                            :readings nil}
                           {:name "Sondra Richardson",
                            :juxt.site/type "https://site.test/types/patient",
                            :xt/id "https://site.test/patients/010",
                            :readings nil}]}}
                       (eqlc/prune-result (xt/q db q alice nil))))))

          (testing "Bob's view"
            (is (= #{{:name "Dr. Jack Conway",
                      :juxt.site/type "https://site.test/types/doctor",
                      :xt/id "https://site.test/doctors/001",
                      :patients nil}
                     {:name "Dr. Jackson",
                      :juxt.site/type "https://site.test/types/doctor",
                      :xt/id "https://site.test/doctors/003",
                      :patients
                      [{:name "Sondra Richardson",
                        :juxt.site/type "https://site.test/types/patient",
                        :xt/id "https://site.test/patients/010",
                        :readings nil}]}}
                   (eqlc/prune-result (xt/q db q bob nil)))))))))
