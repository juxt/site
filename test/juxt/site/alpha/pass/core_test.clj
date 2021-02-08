;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.pass.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.pass.alpha :as pass]
   [crux.api :as crux]))

;;(crux/q db query xacml-context)

(defn populate-node [node]
  (crux/submit-tx
   node
   [
    [:crux.tx/put {:crux.db/id :organization}]

    [:crux.tx/put {:crux.db/id :dealership
                   :crux/sub-type-of :organization}]

    [:crux.tx/put {:crux.db/id :dealership-superuser}]

    [:crux.tx/put {:crux.db/id :dealerX
                   :crux/type :dealership
                   :fullname "Ken's Truck Scales"}]

    [:crux.tx/put {:crux.db/id :dealerY
                   :crux/type :dealership
                   :fullname "Mary's Industrial Scales"}]

    [:crux.tx/put {:crux.db/id :companyA
                   :crux/type :company
                   :dealership :dealerX
                   :fullname "Jackson Cars"}]

    [:crux.tx/put {:crux.db/id :companyB
                   :crux/type :company
                   :dealership :dealerX
                   :fullname "Blue Cross Ltd."}]

    [:crux.tx/put {:crux.db/id :companyC
                   :crux/type :company
                   :dealership :dealerY
                   :fullname "John's Pig Farm"}]

    [:crux.tx/put {:crux.db/id :dealership-x-superuser
                   :crux/type :dealership-superuser
                   :owner :dealerX
                   :fullname "Dealer X superuser"}]

    [:crux.tx/put {:crux.db/id :dealership-y-superuser
                   :crux/type :dealership-superuser
                   :owner :dealerY
                   :fullname "Dealer Y superuser"}]

    [:crux.tx/put {:crux.db/id (java.net.URI. "/api/customers")
                   ::pass/valid-reader-role-types #{:dealership-superuser
                                                    :dealership-owner}}]

    [:crux.tx/put
     ;; TODO: We need rules for the Crux superuser!

     {:crux.db/id :rule0

      ::pass/description
      "The Crux superuser is allowed to do anything."

      ;; used to match the rule against the target
      ::pass/target
      '[
        [context ::pass/role ::pass/superuser]]

      ::pass/effect ::pass/allow

      ;; the effect of the rule 'going forward'
      ::pass/limiting-clauses '[]}]

    [:crux.tx/put
     {:crux.db/id :rule1

      ::pass/description
      "Dealership superusers can access findCompanies and findCompaniesById"

      ;; used to match the rule against the target
      ::pass/target
      '[[context ::pass/role role]
        [role :crux/type :dealership-superuser]
        [context ::pass/openapi-operation #{"findCompanies" "findCompaniesById"}]]

      ::pass/effect ::pass/allow}]

    [:crux.tx/put
     {:crux.db/id :rule1b

      ::pass/description
      "Dealership superusers can GET on resources that allow them via
      their :juxt.pass.alpha/valid-reader-role-types attribute"

      ;; used to match the rule against the target
      ::pass/target
      '[[context ::pass/role role]
        [role :crux/type :dealership-superuser]
        [context ::pass/resource resource]
        [resource ::pass/valid-reader-role-types :dealership-superuser]]

      ::pass/effect ::pass/allow

      ::pass/allow-methods #{:get :head :options}}]

    [:crux.tx/put
     {:crux.db/id :rule2

      ::pass/description
      "Dealership superusers can only view entities owned by their dealership."

      ;; used to match the rule against the target
      ::pass/target
      '[[context ::pass/role role]
        [role :crux/type :dealership-superuser]]

      ::pass/effect ::pass/allow

      ;; the effect of the rule 'going forward'
      ::pass/limiting-clauses
      '[(or
         [e :dealership dealership]
         [e :owner dealership])
        [(get context ::pass/role) role]
        [role :owner dealership]]}]

    #_[:crux.tx/put
       {:crux.db/id :policy1

        :juxt.pass.alpha/description
        "Policy for dealership superusers"

        :juxt.pass.alpha/rules #{}}]
    ])

  (crux/sync node))

(deftest policy-based-access-control-test
  (with-open [node (crux/start-node {})]

    (populate-node node)

    (let [db (crux/db node)

          query '{:find [f]
                  :where [[f :crux.db/id]
                          [f :fullname fn]]}

          fullnames-under-role
          (fn [role]
            (let [request-context {::pass/role role}
                  authorization (pdp/authorization db request-context)
                  query (pdp/authorize-query query authorization)]
              (crux/q db query request-context)))]

      (is
       (= #{[:companyA]
            [:companyB]
            [:dealership-x-superuser]}
          (fullnames-under-role :dealership-x-superuser)))

      (is
       (= #{[:companyC]
            [:dealership-y-superuser]}
          (fullnames-under-role :dealership-y-superuser)))

      (is
       (= #{[:companyA]
            [:companyB]
            [:companyC]
            [:dealerX]
            [:dealership-x-superuser]
            [:dealerY]
            [:dealership-y-superuser]}
          (fullnames-under-role ::pass/superuser))))))

(with-open [node (crux/start-node {})]

  (populate-node node)

  (let [db (crux/db node)

        #_request-context
        #_{:subject {:id :juxtmal :role :superuser :auth-method "Basic" :ip-address "0.0.0.0"}
           :resource {:url "/getUsers" :json-schema "/schema/UserList"}
           :action {:method :get :openid/operationId "getUsers" :read-or-write :read}
           :environment {:profile :staging}}

        query '{:find [e]
                :where [[e :crux.db/id]
                        [e :fullname fn]]}

        request-context
        {:user :juxtmal
         ::pass/role :dealership-x-superuser #_:dealership-y-superuser #_::pass/superuser
         ::pass/openapi-operation "findCompanies"
         ::pass/resource (java.net.URI. "/api/customers")
         :domain "WallyWorld"}

        authorization
        (pdp/authorization db request-context)

        authorized-query (pdp/authorize-query query authorization)]

    authorization

    ;;(crux/q db authorized-query request-context)
    ))
