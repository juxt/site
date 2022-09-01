;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.basic-test
  (:require
   [jsonista.core :as json]
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

(defn create-action-get-patient! [_]
  (init/do-action
   "https://site.test/subjects/system"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/get-patient"
    :juxt.pass.alpha/rules
    '[
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
        {:actions #{"https://site.test/actions/get-patient"}}))}}))

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

(def PATIENT_NAMES
  {
   "001" "Terry Levine",
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
   "020" "Elisabeth Riddle"
   })

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
  {"https://site.test/actions/register-patient"
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

   "https://site.test/patients/{pid}"
   {:create #'register-patient!
    :deps #{::init/system
            "https://site.test/actions/register-patient"
            "https://site.test/permissions/system/register-patient"}}

   "https://site.test/patients"
   {:create
    (fn [_]
      (init/put!
       {:xt/id "https://site.test/patients"
        ::site/methods
        {:get
         {::pass/actions #{"https://site.test/actions/list-patients"}}}
        ::http/content-type "application/json"}))
    :deps #{::init/system}}

   "https://site.test/actions/read-any-measurement"
   {:create #'create-action-read-any-measurement!
    :deps #{::init/system}}

   "https://site.test/permissions/{username}/read-any-measurement"
   {:create (fn [{:keys [params]}]
              (grant-permission-to-read-any-measurement! (get params "username")))
    :deps #{::init/system}}
   })

(defmacro with-resources [resources & body]
  `(do
     (let [resources# ~resources]
       (init/converge!
        (conj resources# ::init/system)
        (init/substitute-actual-base-uri
         (merge init/dependency-graph book/dependency-graph dependency-graph))))
     ~@body))

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

           ;;"https://site.test/actions/install-api-resource"
           ;;"https://site.test/permissions/system/install-api-resource"

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

         ;; Add some users
         (into
          (for [i (range 1 (inc 20))]
            (format "https://site.test/patients/%03d" i)))

         )]

    ;; Alice can read patients
    ;; Carlos cannot read patients

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

      (let [alice-session-id (book/login-with-form! {"username" "alice" "password" "garden"})
            {alice-access-token "access_token"
             error "error"}
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

        ;; Let's be careful before treating 'api resources' differently from
        ;; 'normal' Site resources. They're the same thing really.
        #_(juxt.site.alpha.init/do-action
           "https://site.test/subjects/system"
           "https://site.test/actions/install-api-resource"
           ;; This is the API resource representing a given patient
           {:xt/id "https://site.test/patient/{pid}"
            ::site/uri-template true
            ;; TODO: Scope can be specified here
            ::site/methods
            {:get {::pass/actions #{"https://site.test/actions/perform-api-operation"}
                   ;; TODO: Scope can be specified here. For example, only
                   ;; read-only API operations may be granted to an application.
                   :juxt.site.openapi/description "Get a patient"
                   :actions #{"https://site.test/actions/get-patient"}}
             :head {::pass/actions #{"https://site.test/actions/perform-get-operation"}}
             :options {}}
            ::http/content-type "application/json"
            })

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
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients/005"
          :ring.request/headers
          {"authorization" (format "Bearer %s" alice-access-token)
           "accept" "application/json"}})

        ;; Bob can't see the patient details of Angie Solis
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients/005"
          :ring.request/headers
          {"authorization" (format "Bearer %s" bob-access-token)
           "accept" "application/json"}})

        ;;(repl/e "https://site.test/patients")
        ;; list-patients is going to re-use the rule for get-patient.  In this
        ;; sense, the action list-patients is a get-patient action applied
        ;; across a set.

        ;; Alice sees all patients
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients"
          ;;:debug true
          :ring.request/headers
          {"authorization" (format "Bearer %s" alice-access-token)
           "accept" "application/json"}})

        ;; Bob sees a handful of patients
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients"
          ;;:debug true
          :ring.request/headers
          {"authorization" (format "Bearer %s" bob-access-token)
           "accept" "application/json"}})

        ;; We are calling juxt.pass.alpha.actions/pull-allowed-resources which
        ;; provides our query, but we want to experiment with creating our own
        ;; query with sub-queries, which we can compile to with GraphQL.

        #_(xt/q
           (xt/db *xt-node*)
           '{:find [(pull resource [*])]
             :where [
                     [resource :juxt.site.alpha/type "https://site.test/types/patient"]]})

        (let [db (xt/db *xt-node*)

              {subject ::pass/subject}
              (ffirst (xt/q db '{:find [(pull e [*])] :where [[e ::pass/token token]] :in [token]} bob-access-token))]

          (xt/q
           db
           `{:find ~'[patient (pull action [:xt/id ::pass/pull]) permission measurement]
             :keys ~'[patient action permission measurement]
             :where
             [
              ~'[action :xt/id "https://site.test/actions/get-patient"]
              ~'[permission ::site/type "https://meta.juxt.site/pass/permission"]
              ~'[permission ::pass/action action]
              ~'[permission ::pass/purpose purpose]
              ~'(allowed? subject patient permission)

              ;; join
              [(~'q {:find ~'[(pull m [:reading])]
                     :where
                     ~'[
                        [m :patient p]
                        [m ::site/type "https://site.test/types/measurement"]

                        [action :xt/id "https://site.test/actions/read-any-measurement"]
                        [permission ::site/type "https://meta.juxt.site/pass/permission"]
                        [permission ::pass/action action]
                        [permission ::pass/purpose purpose]
                        (allowed? sub m permission)
                        ]
                     :rules ~(actions/actions->rules db #{"https://site.test/actions/read-any-measurement"})
                     :in ~'[p sub]}
                ~'patient ~'subject)
               ~'measurement]]

             :rules ~(actions/actions->rules db #{"https://site.test/actions/get-patient"})

             :in [~'subject ~'purpose]}

           subject nil)

          )


        ;;(repl/e (format "https://site.test/access-tokens/%s" bob-access-token))


        #_(juxt.site.alpha.init/do-action
           "https://site.test/subjects/system"
           "https://site.test/actions/install-api-resource"
           {:xt/id "https://site.test/patients"
            ::site/methods
            {:get {::pass/actions #{"https://site.test/actions/get-patients"}}
             :head {::pass/actions #{"https://site.test/actions/get-patients"}}
             :options {}}
            })

        #_(*handler*
           {:ring.request/method :get
            :ring.request/path "/patients"
            :ring.request/headers
            {"authorization" (format "Bearer %s" access-token)
             "accept" "application/edn"}}))

      #_(repl/ls)
      #_(repl/e "https://site.test/patients/010")

      #_(let [db (xt/db *xt-node*)]

          (xt/q
           db
           '{:find [(pull e [:name])]
             :where [[e ::site/type "https://site.test/types/patient"]]
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
               db))))


    ))

#_{:find [e (pull e [:name :heartRate])],
 :where
 [[e :xt/id _]
  [action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
  [action :xt/id #{"https://site.test/actions/get-patients"}]
  [permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
  [permission
   :juxt.pass.alpha/action
   #{"https://site.test/actions/get-patients"}]
  [permission :juxt.pass.alpha/purpose purpose]
  (allowed? permission subject action e)
  (include? action e)],
 :rules
 [[(allowed? subject resource permission)
   [permission :xt/id]
   [action :xt/id "https://site.test/actions/get-patients"]]],
   :in [subject purpose]}

#_(into
 #{::init/system
   "https://site.test/graphql"
   "https://site.test/actions/get-patients"
   #_"https://site.test/actions/read-vitals"
   "https://site.test/actions/register-patient"
   "https://site.test/permissions/system/register-patient"
   "https://site.test/patients/001"
   "https://site.test/patients/002"

   ;; Add some users
   ;; Alice can read patients
   ;; Carlos cannot patients
   }
 (for [i (range 20)] (format "https://site.test/patients/%03d" i)))
