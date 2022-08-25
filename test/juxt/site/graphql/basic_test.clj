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
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/register-patient"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/register-patient"
       :juxt.pass.alpha/purpose nil})))))

(defn create-action-get-patient! [_]
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-patient"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]
          [resource :juxt.site.alpha/type "https://example.org/types/patient"]
          [permission :patient :all]]

         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]
          [resource :juxt.site.alpha/type "https://example.org/types/patient"]
          [permission :patient resource]]]})))))

(defn grant-permission-to-get-any-patient! [username]
  (eval
   (init/substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id ~(format "https://example.org/permissions/%s/get-any-patient" username)
       :juxt.pass.alpha/action "https://example.org/actions/get-patient"
       :juxt.pass.alpha/user ~(format "https://example.org/users/%s" username)
       :patient :all
       :juxt.pass.alpha/purpose nil
       }))))

(defn grant-permission-to-get-patient! [username pid]
  (eval
   (init/substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id ~(format "https://example.org/permissions/%s/get-patient/%s" username pid)
       :juxt.pass.alpha/action "https://example.org/actions/get-patient"
       :juxt.pass.alpha/user ~(format "https://example.org/users/%s" username)
       :patient ~(format "https://example.org/patients/%s" pid)
       :juxt.pass.alpha/purpose nil
       }))))

(defn create-action-list-patients! [_]
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/list-patients"

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
        {:juxt.pass.alpha/actions #{"https://example.org/actions/list-patients"}}}

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [permission :juxt.pass.alpha/user user]]]

       :juxt.site.alpha/data-view
       {:juxt.flip.alpha/quotation `(f/break)}
       })))))

(defn grant-permission-to-list-patients! [username]
  (eval
   (init/substitute-actual-base-uri
    `(init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id ~(format "https://example.org/permissions/%s/list-patients" username)
       :juxt.pass.alpha/action "https://example.org/actions/list-patients"
       :juxt.pass.alpha/user ~(format "https://example.org/users/%s" username)
       :juxt.pass.alpha/purpose nil}))))

(defn create-action-read-vitals! [_]
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/read-vitals"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]
          ]]})))))

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

   "https://site.test/actions/read-vitals"
   {:create #'create-action-read-vitals!
    :deps #{::init/system}}})

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

           "https://site.test/patients"

           }
         ;; Add some users
         (into
          (for [i (range 1 (inc 20))]
            (format "https://site.test/patients/%03d" i))))]

    ;; Alice can read patients
    ;; Carlos cannot read patients

    (with-resources
      resources

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
        ;; "https://example.org/types/patient"]' then it is not a permitted
        ;; action. We must separate the actions that allow access to a
        ;; uri-template'd resource and the actions that create the body
        ;; payload.


        ;; Alice can access a particular patient because she has the permission on get-every-patient-details
        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients/005"
          :ring.request/headers
          {"authorization" (format "Bearer %s" alice-access-token)
           "accept" "application/json"}})

        #_(repl/ls)

        (*handler*
         {:ring.request/method :get
          :ring.request/path "/patients"
;;          :debug true
          :ring.request/headers
          {"authorization" (format "Bearer %s" alice-access-token)
           "accept" "application/json"}})

        ;;(repl/ls)
        ;;(repl/e "https://site.test/actions/list-patients")
        ;;(repl/e "https://site.test/patients")
        ;;(repl/e "https://site.test/permissions/alice/list-patients")

        #_(*handler*
         {:ring.request/method :get
          :ring.request/path "/patients"
          ;;:debug true
          :ring.request/headers
          {"authorization" (format "Bearer %s" bob-access-token)
           "accept" "application/json"}})

        ;;(repl/e "https://site.test/patients")

        ;; list-patients is going to re-use the rule for get-patient.  In this
        ;; sense, the action list-patients is a get-patient action applied
        ;; across a set.

        ;; TODO: Add Bob and see that Bob cannot see any patients.

        ;; TODO: Experiment with get-patient and permissions to limit Bob's view
        ;; to a handful subset of patients.

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
