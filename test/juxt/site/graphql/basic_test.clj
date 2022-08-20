;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.basic-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [clojure.java.io :as io]
   [juxt.book :as book]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha.repl :as repl]
   [juxt.site.alpha.graphql.graphql-compiler :as gcompiler]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :as init]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [juxt.site.alpha.graphql.graphql-query-processor :as gqp]))

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

    :juxt.flip.alpha/quotation
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

            xtdb.api/put]))]))

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

(defn create-action-get-patients! [_]
  (eval
   (init/substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-patients"
       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

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
      })))

(def dependency-graph
  {"https://site.test/actions/register-patient"
   {:create #'create-action-register-patient!
    :deps #{::init/system}}
   "https://site.test/permissions/system/register-patient"
   {:create #'grant-permission-to-invoke-action-register-patient!
    :deps #{::init/system}}
   "https://site.test/actions/get-patients"
   {:create #'create-action-get-patients!
    :deps #{::init/system}}
   "https://site.test/patients/{pid}"
   {:create #'register-patient!
    :deps #{::init/system
            "https://site.test/actions/register-patient"
            "https://site.test/permissions/system/register-patient"}}
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
           "https://site.test/graphql"
           "https://site.test/actions/get-patients"
           #_"https://site.test/actions/read-vitals"
           "https://site.test/actions/register-patient"
           "https://site.test/permissions/system/register-patient"

           }
         ;; Add some users
         (into
          (for [i (range 1 (inc 20))]
            (format "https://site.test/patients/%03d" i))))]

    ;; Alice can read patients
    ;; Carlos cannot patients


    (with-resources
      resources

      (repl/ls)

      #_(let [db (xt/db *xt-node*)
            compiled-schema
            (->
             "juxt/site/graphql/basic.graphql"
             io/resource
             slurp
             gcompiler/compile-schema)

            ]

        (gqp/graphql-query->xtdb-query
         "query { patients { name heartRate } }"
         compiled-schema
         db)))))


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
