;; Copyright © 2021, JUXT LTD.

(ns juxt.site.deprecated.authz-test
  (:require
   [clojure.test :refer [deftest is are testing] :as t]
   [xtdb.api :as x]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-xt)

;; We'll borrow terms and definitions from Google's Zanzibar system.
;; 3.2.3 Check Evaluation
(deftest authorization-test []
  (submit-and-await!
   [
    [:xtdb.api/put
     {:xt/id "https://example.org/users/jon"
      :role "https://example.org/roles/patient"}]

    [:xtdb.api/put
     {:xt/id "https://example.org/users/mal"
      :role "https://example.org/roles/patient"}]

    [:xtdb.api/put
     {:xt/id "https://example.org/jon/heart-rate"
      :doc "https://www.hl7.org/fhir/observation-example-heart-rate.json.html"
      :hl7.fhir/resource-type :observation

      :hl7.fhir/category
      #:hl7.fhir{:text "Vital Signs"
                 :coding
                 #:hl7.fhir{:code "vital-signs"
                            :display "Vital Signs"
                            :system "http://terminology.hl7.org/CodeSystem/observation-category"}}

      :hl7.fhir/code
      #:hl7.fhir{:text "Heart rate"
                 :coding
                 #:hl7.code{:system "http://loinc.org"
                            :code "8867-4"
                            :display "Heart rate"}}

      :hl7.fhir/valueQuantity
      #:hl7.fhir{:value 44
                 :unit "beats/minute"
                 :system "http://unitsofmeasure.org"
                 :code "/min"}}]

    [:xtdb.api/put
     {:xt/id "https://example.org/mal/heart-rate"
      :doc "https://www.hl7.org/fhir/observation-example-heart-rate.json.html"
      :hl7.fhir/resource-type :observation

      :hl7.fhir/category
      #:hl7.fhir{:text "Vital Signs"
                 :coding
                 #:hl7.fhir{:code "vital-signs"
                            :display "Vital Signs"
                            :system "http://terminology.hl7.org/CodeSystem/observation-category"}}

      :hl7.fhir/code
      #:hl7.fhir{:text "Heart rate"
                 :coding
                 #:hl7.code{:system "http://loinc.org"
                            :code "8867-4"
                            :display "Heart rate"}}

      :hl7.fhir/valueQuantity
      #:hl7.fhir{:value 121
                 :unit "beats/minute"
                 :system "http://unitsofmeasure.org"
                 :code "/min"}}]

    ;; A metadata document acting as an ACL that assigns ownership between an
    ;; owner and a document. For now we'll model the tuple as a separate XT
    ;; entity although in future we might use relations that are in the object
    ;; rather than define new ones in the ACL.
    [:xtdb.api/put
     {:xt/id "https://example.org/jon-can-see-his-own-report.acl"
      ::pass/entity "https://example.org/jon/heart-rate"
      ::pass/owner "https://example.org/users/jon"}]

    [:xtdb.api/put
     {:xt/id "https://example.org/mal-can-see-his-own-report.acl"
      ::pass/entity "https://example.org/mal/heart-rate"
      ::pass/owner "https://example.org/users/mal"}]

    ;; Bob is Jon's doctor
    [:xtdb.api/put
     {:xt/id "https://example.org//users/bob"
      :name "Dr. Bob Smith"
      :role "https://example.org/roles/doctor"}]

    ;; Here is an ACL that represents Jon's consent that Bob can read vital signs
    [:xtdb.api/put
     {:xt/id "https://example.org/bob-is-jon-doctor.acl"
      ::site/type "Consent"
      ::pass/granted-by "https://example.org/users/jon"
      ::pass/entity "https://example.org/jon/heart-rate"
      ::pass/grantee "https://example.org/users/bob"
      ::pass/scopes #{:read}
      ;; But only for the purposes of a medical intervention
      ::pass/purpose :medical-intervention
      :hl7.fhir/category [#:hl7.fhir{:coding #:hl7.fhir{:code "vital-signs"}}]}]

    ;; Carl is another doctor, but Jon doesn't want this doctor accessing his
    ;; medical information.
    [:xtdb.api/put
     {:xt/id "https://example.org/users/carl"
      :name "Dr. Carl Lector"
      :role "https://example.org/roles/doctor"}]])

  (let [db (x/db *xt-node*)
        check
        ;; "Authorization checks take the form of “does user U have relation R
        ;; to object O?”"
        (fn [user doc purpose]
          (x/q
           db
           '{:find [(pull ?acl [*]) ?doc]
             :where [(check ?acl ?user ?doc ?purpose)]

             ;; These rules are analogous to a 'namespace
             ;; configuration'. Zanzibar manages over 1500 of these. Objects
             ;; are in namespaces, so an object's name indicates which
             ;; configuration should be used. In XT, the configuration can be
             ;; part of the document.

             :rules [[(check acl user obj purpose)
                      [acl ::pass/owner user]
                      [acl ::pass/entity obj]
                      [(some? purpose) _]]

                     [(check acl user obj purpose)
                      [acl ::site/type "Consent"]
                      [acl ::pass/grantee user]
                      [acl ::pass/granted-by owner]
                      [acl :hl7.fhir/category acl-cat]
                      [obj :hl7.fhir/category obj-cat]
                      [(get-in acl-cat [:hl7.fhir/coding :hl7.fhir/code]) code]
                      [(get-in obj-cat [:hl7.fhir/coding :hl7.fhir/code]) code]
                      [acl ::pass/purpose purpose]
                      ;; We need to check the ACL for the owner to see the doc
                      (check subacl owner obj purpose)
                      ]]

             :in [?user ?doc ?purpose]}
           user doc purpose))

        acl? (fn [user doc purpose] (first (check user doc purpose)))]

    ;; Jon can see his own record
    (is (acl? "https://example.org/users/jon"
              "https://example.org/jon/heart-rate"
              :medical-intervention))

    ;; Bob can see Jon's record
    (is (acl? "https://example.org/users/bob"
              "https://example.org/jon/heart-rate"
              :medical-intervention))

    ;; Bob cannot access Jon's HR for marketing
    (is (nil? (acl? "https://example.org/users/bob"
                    "https://example.org/jon/heart-rate"
                    :marketing)))

    ;; Bob cannot see Mal's HR
    (is (nil? (acl? "https://example.org/users/bob"
                    "https://example.org/mal/heart-rate"
                    :medical-intervention)))

    ;; Carl cannot see Jon's record
    (is (nil? (acl? "https://example.org/users/carl"
                    "https://example.org/jon/heart-rate"
                    :medical-intervention)))))
