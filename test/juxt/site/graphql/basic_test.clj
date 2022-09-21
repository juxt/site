;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.basic-test
  (:require
   [jsonista.core :as json]
   [sci.core :as sci]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [clojure.java.io :as io]
   [juxt.book :as book]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha.repl :as repl]
   [juxt.site.alpha.graphql.graphql-compiler :as gcompiler]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha.actions :as actions]
   [juxt.pass.alpha :as-alias pass]
   [juxt.http.alpha :as-alias http]
   [juxt.site.alpha.init :as init]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [juxt.site.alpha.graphql.graphql-query-processor :as gqp]
   [malli.core :as malli]
   [xtdb.api :as xt]))

;; TODO: Dedupe between this test ns and juxt.book-test

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(defn call-handler [request]
  (*handler* (book/with-body request (::body-bytes request))))

(use-fixtures :each with-system-xt with-handler)

(defmacro with-fixtures [& body]
  `((t/join-fixtures [with-system-xt with-handler])
    (fn [] ~@body)))

(defn create-action-register-doctor! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-doctor"

    :juxt.site.alpha/transact
    {:juxt.flip.alpha/quotation
     `(
       (site/with-fx-acc-with-checks
         [(site/push-fx
           (f/dip
            [site/request-body-as-edn
             (site/validate
              [:map
               [:xt/id [:re "https://site.test/doctors/.*"]]])

             (site/set-type "https://site.test/types/doctor")

             (site/set-methods
              {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-doctor"}}
               :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-doctor"}}
               :options {}})

             xtdb.api/put]))]))}

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-doctor! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-doctor"
    :juxt.pass.alpha/subject "https://site.test/subjects/system"
    :juxt.pass.alpha/action "https://site.test/actions/register-doctor"
    :juxt.pass.alpha/purpose nil}))

(defn create-action-register-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-patient"

    :juxt.site.alpha/transact
    {:juxt.flip.alpha/quotation
     `(
       (site/with-fx-acc-with-checks
         [(site/push-fx
           (f/dip
            [site/request-body-as-edn
             (site/validate
              [:map
               [:xt/id [:re "https://site.test/patients/.*"]]])

             (site/set-type "https://site.test/types/patient")

             (site/set-methods
              {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/get-patient"}}
               :head {:juxt.pass.alpha/actions #{"https://site.test/actions/get-patient"}}
               :options {}})

             xtdb.api/put]))]))}

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-patient"
    :juxt.pass.alpha/subject "https://site.test/subjects/system"
    :juxt.pass.alpha/action "https://site.test/actions/register-patient"
    :juxt.pass.alpha/purpose nil}))

;; A patient's record contains an attribute that indicates the set of assigned doctors.
(defn create-action-assign-doctor-to-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/assign-doctor-to-patient"

    ;; A POST to a patient URL?
    ;; What if there are a number of actions one can perform on a patient?
    ;; A PATCH to a patient????
    :juxt.site.alpha/transact
    {:juxt.site.alpha.malli/input-schema
     [:map
      [:patient [:re "https://site.test/patients/.*"]]
      [:doctor [:re "https://site.test/doctors/.*"]]]
     :juxt.site.alpha.sci/program
     (pr-str
      '(let [[_ patient-id] (re-matches #"https://site.test/patients/(.*)" (:patient *input*))
             [_ doctor-id] (re-matches #"https://site.test/doctors/(.*)" (:doctor *input*))
             id (format "https://site.test/assignments/patient/%s/doctor/%s" patient-id doctor-id)]
         [[:xtdb.api/put
           {:xt/id id
            :patient (:patient *input*)
            :doctor (:doctor *input*)}]]))}
    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]}))

(defn grant-permission-to-invoke-action-assign-doctor-to-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/assign-doctor-to-patient"
    :juxt.pass.alpha/subject "https://site.test/subjects/system"
    :juxt.pass.alpha/action "https://site.test/actions/assign-doctor-to-patient"
    :juxt.pass.alpha/purpose nil}))

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

    :juxt.pass.alpha/filters
    #{:contexts {"https://site.test/types/doctor"
                 '[[$e :doctor $parent]]}
      :args {:gender '[[$e :gender $arg-value]]}}

    :juxt.pass.alpha/rules
    '[
      ;; TODO: Performance tweak: put [subject] to hint that subject is always
      ;; bound - see @jdt for details
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]
       [resource :juxt.site.alpha/type "https://site.test/types/patient"]
       [permission :patient :all]]

      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]
       [resource :juxt.site.alpha/type "https://site.test/types/patient"]
       [permission :patient resource]]]}))

(defn grant-permission-to-get-any-patient! [username]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/get-any-patient" username)
    :juxt.pass.alpha/action "https://site.test/actions/get-patient"
    :juxt.pass.alpha/user (format "https://site.test/users/%s" username)
    :patient :all
    :juxt.pass.alpha/purpose nil
    }))

(defn grant-permission-to-get-patient! [username pid]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/get-patient/%s" username pid)
    :juxt.pass.alpha/action "https://site.test/actions/get-patient"
    :juxt.pass.alpha/user (format "https://site.test/users/%s" username)
    :patient (format "https://site.test/patients/%s" pid)
    :juxt.pass.alpha/purpose nil
    }))

(defn create-action-list-patients! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/list-patients"

    ;; What if this was the resource will target with GET?
    ;; The /patients is more simply an alias.

    ;; Are actions just resources with ACL rules?
    ;; Actions are already resources.
    ;; Maybe any resource can be an action?

    ;; What are our other examples of targeting actions?
    ;; POST /actions/install-graphql-endpoint
    ;; (book_test line ~500, book line ~1197)

    ;; An action is capable of deriving a view of state across a set of
    ;; resources.
    :juxt.site.alpha/methods
    {:get
     {:juxt.pass.alpha/actions #{"https://site.test/actions/list-patients"}}}

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]]]

    ;; While many actions perform mutations on the database, some actions
    ;; only query the database.
    :juxt.site.alpha/query
    {:juxt.flip.alpha/quotation
     `(
       ;; Perform a query, using the rules in get-patient. It would be a good
       ;; idea to restrict the ability for actions to make general queries
       ;; against the database. By only exposing API functions such as
       ;; pull-allowed-resources to Flip, we can limit the power of actions
       ;; thereby securing them. This is preferable to limiting the ability
       ;; to deploy actions to a select group of highly authorized
       ;; individuals.
       ;;
       ;; TODO: Go through the use-cases which already make general lookups
       ;; and queries to XT and see if we can rewrite them to use a more
       ;; restricted API.
       (pass/pull-allowed-resources
        {:actions #{"https://site.test/actions/get-patient"}})

       ;; TODO: This needs to be aware of the content-type of the selected
       ;; representation
       ;;(f/env :juxt.site.alpha/selected-representation)
       ;;(f/of ::http/content-type)
       ;;       jsonista.core/write-value-as-string
       ;;f/break
       )}}))

(defn grant-permission-to-list-patients! [username]
  (init/do-action
    "https://site.test/subjects/system"
    "https://site.test/actions/grant-permission"
    {:xt/id (format "https://site.test/permissions/%s/list-patients" username)
     :juxt.pass.alpha/action "https://site.test/actions/list-patients"
     :juxt.pass.alpha/user (format "https://site.test/users/%s" username)
     :juxt.pass.alpha/purpose nil}))

(defn create-action-register-patient-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/register-patient-measurement"

    :juxt.site.alpha/transact
    {:juxt.flip.alpha/quotation
     `(
       (site/with-fx-acc-with-checks
         [(site/push-fx
           (f/dip
            [site/request-body-as-edn
             (site/validate
              [:map
               [:xt/id [:re "https://site.test/measurements/.*"]]
               [:patient [:re "https://site.test/patients/.*"]]])

             (site/set-type "https://site.test/types/measurement")

             xtdb.api/put]))]))}

    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]]}))

(defn grant-permission-to-invoke-action-register-patient-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/system/register-patient-measurement"
    :juxt.pass.alpha/subject "https://site.test/subjects/system"
    :juxt.pass.alpha/action "https://site.test/actions/register-patient-measurement"
    :juxt.pass.alpha/purpose nil}))

;; Warning, this is an overly broad action! TODO: Narrow this action.
;; It permits grantees access to ALL measurements!!
(defn create-action-read-any-measurement! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/read-any-measurement"
    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]
       ;;[resource :juxt.site.alpha/type "https://site.test/types/measurement"]
       ]]}))

(defn grant-permission-to-read-any-measurement! [username]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/grant-permission"
   {:xt/id (format "https://site.test/permissions/%s/read-any-measurement" username)
    :juxt.pass.alpha/action "https://site.test/actions/read-any-measurement"
    :juxt.pass.alpha/user (format "https://site.test/users/%s" username)
    :juxt.pass.alpha/purpose nil
    }))


(def DOCTOR_NAMES
  {"001" "Dr. Conway"
   "002" "Dr. Murillo"
   "003" "Dr. Jackson"
   "004" "Dr. Kim"})

(def PATIENT_NAMES
  {"001" "Terry Levine",
   "002" "Jeannie Finley",
   "003" "Jewel Blackburn",
   "004" "Lila Dickson",
   "005" "Angie Solis",
   "006" "Floyd Castro",
   "007" "Melanie Black",
   "008" "Beulah Leonard",
   "009" "Monica Russell",
   "010" "Sondra Richardson"
   "011" "Kim Robles",
   "012" "Mark Richard",
   "013" "Hazel Huynh",
   "014" "Francesco Casey",
   "015" "Moshe Lynch",
   "016" "Darrel Schwartz",
   "017" "Blanca Lindsey",
   "018" "Rudy King",
   "019" "Valarie Campos",
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
      ::http/content (json/write-value-as-string {"name" name})
      })))

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
        :juxt.site.alpha/methods
        {:get
         {:juxt.pass.alpha/actions #{"https://site.test/actions/list-patients"}
          :juxt.flip.alpha/quotation
          ;; This should be a pattern of cond'ing on the content-type of the
          ;; selected representation.  TODO: get the action, call the query,
          ;; format the result according to the content-type, return the site
          ;; request context on the stack.

          ;; (We could use a quasiquote but that then makes this less portable
          ;; across namespaces, due to a coupling between the quotation and any
          ;; namespaces aliases declared in the ns.)
          '(
            ;; Get all the patients we can access.

            ;; This is done by getting the first permitted action entity.

            ;; TODO: This block should be factored into a common convenience
            ;; since most GET implementations will do something similar.
            (juxt.flip.alpha.core/env :juxt.pass.alpha/permitted-actions)
            juxt.flip.alpha.core/first
            (juxt.flip.alpha.core/of :juxt.pass.alpha/action)
            (juxt.flip.alpha.core/of :juxt.site.alpha/query)
            (juxt.flip.alpha.core/of :juxt.flip.alpha/quotation)
            juxt.flip.alpha.core/call

            ;; Now style according to the content-type
            ;; First we need the content-type of the selected representation
            ;;(juxt.flip.alpha.core/env :juxt.site.alpha/selected-representation)
            ;;(juxt.flip.alpha.core/of :juxt.http.alpha/content-type)

            ;; Always going to be application/json!
            jsonista.core/write-value-as-bytes

            #_(juxt.flip.alpha.core/case
                  ["application/json"

                   ({:message "TODO: Style the stack in json format"})

                   ;; TODO: This is the 'default' branch, but very ugly and needs
                   ;; improving
                   (:content-type {} juxt.flip.alpha.core/set-at
                                  "Unsupported content-type"
                                  juxt.flip.alpha.core/swap
                                  juxt.flip.alpha.core/ex-info
                                  juxt.flip.alpha.core/throw-exception)])


            )
          }}
        ::http/content-type "application/json"}))
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

(defmacro with-resources [resources & body]
  `(do
     (let [resources# ~resources]
       (init/converge!
        (conj resources# ::init/system)
        (init/substitute-actual-base-uri
         (merge init/dependency-graph book/dependency-graph dependency-graph))))
     ~@body))

;;

(with-fixtures
  (let [resources
        (->
         #{::init/system

           "https://site.test/login"
           "https://site.test/user-identities/alice/basic"
           "https://site.test/user-identities/bob/basic"

           "https://site.test/oauth/authorize"
           "https://site.test/session-scopes/oauth"
           "https://site.test/permissions/alice-can-authorize"
           "https://site.test/permissions/bob-can-authorize"
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
            "https://site.test/assignments/patient/005/doctor/002"}))]

    ;; Alice can read patients

    (with-resources
      resources

      ;; Create some measurements
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

      (init/do-action
       "https://site.test/subjects/system"
       "https://site.test/actions/register-patient-measurement"
       {:xt/id "https://site.test/measurements/5d1cfb88-cafd-4241-8c7c-6719a9451f1e"
        :patient "https://site.test/patients/004"
        :reading {"heartRate" "120"
                  "bloodPressure" "137/80"}})

      (let [alice-session-id (book/login-with-form! {"username" "alice" "password" "garden"})
            {alice-access-token "access_token" error "error"}
            (book/authorize!
             :session-id alice-session-id
             "client_id" "local-terminal"
             ;;"scope" ["https://site.test/oauth/scope/read-personal-data"]
             )
            _ (is (nil? error) (format "OAuth2 grant error: %s" error))

            bob-session-id (book/login-with-form! {"username" "bob" "password" "walrus"})
            {bob-access-token "access_token"
             error "error"}
            (book/authorize!
             :session-id bob-session-id
             "client_id" "local-terminal"
             ;;"scope" ["https://site.test/oauth/scope/read-personal-data"]
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
        ;; '[resource :juxt.site.alpha/type
        ;; "https://site.test/types/patient"]' then it is not a permitted
        ;; action. We must separate the actions that allow access to a
        ;; uri-template'd resource and the actions that create the body
        ;; payload.

        ;; Alice can access a particular patient because she has a particularly
        ;; broad permission on the get-patient action
        (let [response (*handler*
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
          (is (= 403 (:ring.response/status response))))

        ;;(repl/e "https://site.test/patients")
        ;; list-patients is going to re-use the rule for get-patient.  In this
        ;; sense, the action list-patients is a get-patient action applied
        ;; across a set.

        ;; Alice sees all patients
        #_(let [response
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/patients"
                  ;;:debug true
                  :ring.request/headers
                  {"authorization" (format "Bearer %s" alice-access-token)
                   "accept" "application/json"}})]
            (is (= 200 (:ring.response/status response)))
            ;;(is (= 200 (:ring.response/body response)))

            (is (string? (:ring.response/body response)))
            )

        ;; Alice sees all 20 patients
        (let [response
              (*handler*
               {:ring.request/method :get
                :ring.request/path "/patients"
                :ring.request/headers
                {"authorization" (format "Bearer %s" alice-access-token)
                 "accept" "application/json"}})
              body (:ring.response/body response)
              result (json/read-value body)]
          (is (= "application/json" (get-in response [:ring.response/headers "content-type"])))
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
          (is (= 3 (count result))))

        ;; We are calling juxt.pass.alpha.actions/pull-allowed-resources which
        ;; provides our query, but we want to experiment with creating our own
        ;; query with sub-queries, which we can compile to with GraphQL.

        ;; Now we have a get-patient with rules that we can bring into a sub-query

        ;; Let's start with an EQL that represents our query.
        ;; Metadata attributed to the EQL contains actions.
        ;; The EQL could be the target of the compilation of a GraphQL query.

        (let [db (xt/db *xt-node*)
              {subject ::pass/subject}
              (ffirst (xt/q db '{:find [(pull e [*])] :where [[e ::pass/token token]] :in [token]} bob-access-token))]

          ;; TODO: Generate this query

          (xt/q
           db
           `{:find ~'[(pull e [:xt/id :name]) measurements]
             :keys ~'[patient measurements]
             :where
             [
              ~'[action :xt/id "https://site.test/actions/get-patient"]
              ~'[permission ::site/type "https://meta.juxt.site/pass/permission"]
              ~'[permission ::pass/action action]
              ~'[permission ::pass/purpose purpose]
              ~'(allowed? subject e permission)

              ;; join
              [(~'q {:find ~'[(pull e [:reading])]
                     :keys ~'[node]
                     :where
                     ~'[
                        [e :patient parent]
                        [e ::site/type "https://site.test/types/measurement"]

                        [action :xt/id "https://site.test/actions/read-any-measurement"]
                        [permission ::site/type "https://meta.juxt.site/pass/permission"]
                        [permission ::pass/action action]
                        [permission ::pass/purpose purpose]
                        (allowed? subject e permission)
                        ]
                     :rules ~(actions/actions->rules db #{"https://site.test/actions/read-any-measurement"})
                     :in ~'[parent subject]}
                ~'e ~'subject)
               ~'measurements]]

             :rules ~(actions/actions->rules db #{"https://site.test/actions/get-patient"})

             :in [~'subject ~'purpose]}

           subject nil)

          )

        ;; Modelling ideas

        ;; Doctor's have patients, patients have an assigned doctor.
        ;; A measurement must be taken by a doctor or other individual.
        ;; From the doctor, you can see patients.
        ;; A patient should be able to see their own medical file.

        #_(repl/e "https://site.test/patients/014")

        #_(let [db (xt/db *xt-node*)
                {subject ::pass/subject}
                (ffirst (xt/q db '{:find [(pull e [*])] :where [[e ::pass/token token]] :in [token]} alice-access-token))]

            (xt/q
             db
             `{:find ~'[(pull e [*])]
               :where
               [
                ~'[e ::site/type "https://site.test/types/doctor"]]
               :in [~'subject ~'purpose]}
             subject nil)
            )

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
        ;; associated admin costs.

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

        ;; {:xt/id "list-patients" ::pass/rules "get-patient"}

        ;; Idea: Break GraphQL schemas into constituent types and create
        ;; individual resources, one resource per type. Use 'set' difference to
        ;; identity types to delete. Create HTML from /graphql to show a nice
        ;; HTML page with a list of types.

        ))))


#_`{:find ~'[(pull patient [:xt/id :name])]
                   :keys ~'[patient]
                   :where
                   [
                    ~'[action :xt/id "https://site.test/actions/get-patient"]
                    ~'[permission ::site/type "https://meta.juxt.site/pass/permission"]
                    ~'[permission ::pass/action action]
                    ~'[permission ::pass/purpose purpose]
                    ~'(allowed? subject patient permission)

                    ]

                   :rules ~(actions/actions->rules db #{"https://site.test/actions/get-patient"})

  :in [~'subject ~'purpose]}

(let [eql '[{:patient [:xt/id :name]}]
      reducer
      (fn [eql]
        (reduce (fn [acc prop]
                  acc
                  ) {:find []} eql))]

  (reducer eql))

#_(f/eval-quotation
   [{:patient "https://site.test/patients/001"
     :doctor "https://site.test/doctors/001"}]
   `(
     (site/with-fx-acc
       [
        ;; Validate arguments
        (f/dip
         [(site/validate
           [:map
            [:patient [:re "https://site.test/patients/.*"]]
            [:doctor [:re "https://site.test/doctors/.*"]]])])

        (f/set-at (f/dip [:input]))

        (f/set-at (f/keep [(f/of :input) (f/of :patient) site/entity :patient]))

        :patient f/swap [(f/set-at (f/keep [:bar :foo]))] f/change-at

        ;;(f/keep [(f/change-at :patient f/swap [f/drop :ok]) :new-patient])


        #_(site/entity (f/of :patient))])

     ;; The patient could be the resource undergoing a PATCH, where the
     ;; PATCH body contains the doctor id.

     ;; Or a GraphQL mutation where an action is called with the patient id
     ;; and doctor id as arguments.

     ;; It feels like this action needs to be given the arguments on its
     ;; initial stack, rather than each action pull them into from EDN.

     ;; However an action is called, let's always assume the action begins
     ;; with arguments on its stack, as any quotation would. But
     ;; conventionally, there is one argument: a map.

     )

   {::site/db (xt/db *xt-node*)
    })

#_(let [compiled-schema
        (->
         "juxt/site/graphql/basic.graphql"
         io/resource
         slurp
         gcompiler/compile-schema)]

    (gqp/graphql-query->xtdb-query
     "query { patients { name heartRate } }"
     compiled-schema
     db))
