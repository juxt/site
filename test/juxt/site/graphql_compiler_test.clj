(ns juxt.site.graphql-compiler-test
  (:require  [clojure.test :refer [deftest testing is]]
             [juxt.site.alpha.graphql.graphql-compiler :as sut]
             [clojure.java.io :as io]
             [juxt.grab.alpha.graphql :as-alias graphql]
             [juxt.grab.alpha.document :as document]))

(def example-schema (-> "juxt/data/example.graphql"
                        io/resource
                        slurp
                        sut/compile-schema))

(deftest query-doc->actions-test
  (testing "Can extract the linked actions for a single layer query"
    (is (= #{"https://test.example.com/actions/getEmployee"}
           (sut/query-doc->actions (sut/query->query-doc
                                                          "query findEmployeePhoneNumbers { employee { juxtcode } }"
                                                          example-schema) example-schema)))
    (is (= #{"https://test.example.com/actions/getContractor"}
           (sut/query-doc->actions (sut/query->query-doc
                                                          "query findContractorPhoneNumbers { contractor { juxtcode } }"
                                                          example-schema) example-schema))))
  (testing "Can extract the linked actions for a simple multi-layer query"
    (is (= #{"https://test.example.com/actions/getEmployee" "https://test.example.com/actions/getProject"}
           (sut/query-doc->actions (sut/query->query-doc
                                                          "query findEmployeeProjects { employee { juxtcode, projects { name } } }"
                                                          example-schema) example-schema))))
  (testing "Can extract the linked actions for a query with a cycle in the type graph (and only includes the action once)"
    (is (= #{"https://test.example.com/actions/getEmployee" "https://test.example.com/actions/getProject"}
           (sut/query-doc->actions (sut/query->query-doc
                                                          "query findEmployeeProjects { employee { juxtcode, projects { name, assigned { name } } } }"
                                                          example-schema) example-schema)))))


(deftest build-query-for-selection-set-test
  (testing "Returns simple result with single local field"
    (is (=
         {:find ['e '(pull e [:juxtcode])]
          :in ['subject 'purpose]
          :rules [['(allowed? permission subject action e)]
                  ['(include? action e)]]
          :where [['e :xt/id '_]
                  ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                  ['action :xt/id "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                  ['permission :juxt.pass.alpha/action "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.pass.alpha/purpose 'purpose]
                  '(allowed? permission subject action e)
                  '(include? action e)]}
         (sut/build-query-for-selection-set
          {::document/scoped-type-name "Query"
           ::graphql/name "employee"
           ::graphql/selection-set [{::document/scoped-type-name "Employee"
                                     ::graphql/name "juxtcode"}]}
          example-schema
          [['(allowed? permission subject action e)]
           ['(include? action e)]]
          false))))

  (testing "Returns simple result with multiple local fields"
    (is (=
         {:find ['e '(pull e [:juxtcode :name])]
          :in ['subject 'purpose]
          :rules [['(allowed? permission subject action e)]
                  ['(include? action e)]]
          :where [['e :xt/id '_]
                  ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                  ['action :xt/id "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                  ['permission :juxt.pass.alpha/action "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.pass.alpha/purpose 'purpose]
                  '(allowed? permission subject action e)
                  '(include? action e)]}
         (sut/build-query-for-selection-set
          {::document/scoped-type-name "Query"
           ::graphql/name "employee"
           ::graphql/selection-set [{::document/scoped-type-name "Employee"
                                     ::graphql/name "juxtcode"}
                                    {::document/scoped-type-name "Employee"
                                     ::graphql/name "name"}]}
          example-schema
          [['(allowed? permission subject action e)]
           ['(include? action e)]]
          false)))
    (is (=
         {:find ['e '(pull e [:juxtcode :name :phonenumber])]
          :in ['subject 'purpose]
          :rules [['(allowed? permission subject action e)]
                  ['(include? action e)]]
          :where [['e :xt/id '_]
                  ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                  ['action :xt/id "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                  ['permission :juxt.pass.alpha/action "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.pass.alpha/purpose 'purpose]
                  '(allowed? permission subject action e)
                  '(include? action e)]}
         (sut/build-query-for-selection-set
          {::document/scoped-type-name "Query"
           ::graphql/name "employee"
           ::graphql/selection-set [{::document/scoped-type-name "Employee"
                                     ::graphql/name "juxtcode"}
                                    {::document/scoped-type-name "Employee"
                                     ::graphql/name "name"}
                                    {::document/scoped-type-name "Employee"
                                     ::graphql/name "phonenumber"}]}
          example-schema
          [['(allowed? permission subject action e)]
           ['(include? action e)]]
          false))))

  (testing "Returns multi-layer result with single local field"
    (is (=
         {:find ['e {} {:projects 'inner-projects}]
          :in ['subject 'purpose]
          :rules [['(allowed? permission subject action e)]
                  ['(include? action e)]]
          :where [['e :xt/id '_]
                  ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                  ['action :xt/id "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                  ['permission :juxt.pass.alpha/action "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.pass.alpha/purpose 'purpose]
                  '(allowed? permission subject action e)
                  '(include? action e)
                  ['e :projects 'projects]
                  ['(q
                     {:find [e (pull e [:name])]
                      :in [subject purpose input-id]
                      :rules [[(allowed? permission subject action e)]
                              [(include? action e)]]
                      :where [[e :xt/id input-id]
                              [action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                              [action :xt/id "https://test.example.com/actions/getProject"]
                              [permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                              [permission
                               :juxt.pass.alpha/action
                               "https://test.example.com/actions/getProject"]
                              [permission :juxt.pass.alpha/purpose purpose]
                              (allowed? permission subject action e)
                              (include? action e)]}
                     subject purpose projects)
                   'inner-projects]]}
         (sut/build-query-for-selection-set
          {::document/scoped-type-name "Query"
           ::graphql/name "employee"
           ::graphql/selection-set [{::document/scoped-type-name "Employee"
                                     ::graphql/name "projects"
                                     ::graphql/selection-set [{::document/scoped-type-name "Project"
                                                               ::graphql/name "name"}]}]}
          example-schema
          [['(allowed? permission subject action e)]
           ['(include? action e)]]
          false))))

  (testing "Returns multi-layer result with multiple local fields"
    (is (=
         {:find ['e '(pull e [:juxtcode]) {:projects 'inner-projects}]
          :in ['subject 'purpose]
          :rules [['(allowed? permission subject action e)]
                  ['(include? action e)]]
          :where [['e :xt/id '_]
                  ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                  ['action :xt/id "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                  ['permission :juxt.pass.alpha/action "https://test.example.com/actions/getEmployee"]
                  ['permission :juxt.pass.alpha/purpose 'purpose]
                  '(allowed? permission subject action e)
                  '(include? action e)
                  ['e :projects 'projects]
                  ['(q
                     {:find [e (pull e [:name :assigned])]
                      :in [subject purpose input-id]
                      :rules [[(allowed? permission subject action e)]
                              [(include? action e)]]
                      :where [[e :xt/id input-id]
                              [action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                              [action :xt/id "https://test.example.com/actions/getProject"]
                              [permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                              [permission
                               :juxt.pass.alpha/action
                               "https://test.example.com/actions/getProject"]
                              [permission :juxt.pass.alpha/purpose purpose]
                              (allowed? permission subject action e)
                              (include? action e)]}
                     subject purpose projects)
                   'inner-projects]]}
         (sut/build-query-for-selection-set
          {::document/scoped-type-name "Query"
           ::graphql/name "employee"
           ::graphql/selection-set [{::document/scoped-type-name "Employee"
                                     ::graphql/name "juxtcode"}
                                    {::document/scoped-type-name "Employee"
                                     ::graphql/name "projects"
                                     ::graphql/selection-set [{::document/scoped-type-name "Project"
                                                               ::graphql/name "name"}
                                                              {::document/scoped-type-name "Project"
                                                               ::graphql/name "assigned"}]}]}
          example-schema
          [['(allowed? permission subject action e)]
           ['(include? action e)]]
          false)))))
