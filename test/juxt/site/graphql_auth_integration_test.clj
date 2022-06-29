(ns juxt.site.graphql-auth-integration-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [juxt.test.util :refer [*xt-node* *handler*] :as tutil]
             [juxt.site.alpha.repl :as repl]
             [juxt.site.alpha :as-alias site]
             [xtdb.api :as xt]
             [clojure.java.io :as io]
             [juxt.site.alpha.graphql.graphql-query-processor :as graphql-proc]
             [juxt.site.alpha.graphql.graphql-compiler :as gcompiler]))

(def site-prefix "https://test.example.com")

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri site-prefix
              ::site/uri-prefix site-prefix})]
    (f)))

(defn make-user
  [user-id]
  {:xt/id (str site-prefix "/users/" user-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/user"})

(defn make-identity
  [user-id]
  {:xt/id (str site-prefix "/identities/" user-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
   :juxt.pass.alpha/user (str site-prefix "/users/" user-id)})

(defn make-subject
  [user-id subject-id]
  {:xt/id (str site-prefix "/subjects/" subject-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
   :juxt.pass.alpha/identity (str site-prefix "/identities/" user-id)})

(defn make-permission
  [action-id]
  {:xt/id (str site-prefix "/permissions/" action-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
   :juxt.pass.alpha/action (str site-prefix "/actions/" action-id)
   :juxt.pass.alpha/purpose nil})

(defn with-site-helpers
  [f]
  (repl/put! (merge (make-user "host") {:name "Test Host User"}))
  (repl/put! (make-identity "host"))
  (repl/put! (make-subject "host" "host-test"))
  (repl/put! (merge (make-permission "create-action") { :juxt.pass.alpha/user (str site-prefix "/users/host") }))
  (f))

(def fixtures [tutil/with-system-xt with-handler with-site-helpers])

(defn make-employee
  [juxtcode]
  {:xt/id (str site-prefix "/employee/" juxtcode)
   :juxt.site.alpha/type (str site-prefix "/employee")
   :juxtcode juxtcode})

(defn make-repository
  [repository-name]
  {:xt/id (str site-prefix "/repository/" repository-name)
   :juxt.site.alpha/type (str site-prefix "/repository")
   :name repository-name
   :url (str site-prefix "/git/" repository-name)})

(defn make-project
  [prj-code]
  {:xt/id (str site-prefix "/project/" prj-code)
   :juxt.site.alpha/type (str site-prefix "/project")
   :name prj-code})

(defn make-client
  [client-name]
  {:xt/id (str site-prefix "/client/" client-name)
   :juxt.site.alpha/type (str site-prefix "/client")
   :name client-name})

(defn make-action
  [action-id]
  {:xt/id (str site-prefix "/actions/" action-id)
   :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
   :juxt.pass.alpha/scope "read:resource"
   :juxt.pass.alpha/rules
   [['(allowed? permission subject action resource)
     ['permission :xt/id]]]})

(def example-compiled-schema (-> "juxt/data/example.graphql"
                                 io/resource
                                 slurp
                                 gcompiler/compile-schema))

(apply (partial t/use-fixtures :each) fixtures)

(defn setup-db
  []
  (let [prj-alpha (make-project "alpha")
           prj-bravo (make-project "bravo")
           cwi (assoc (make-employee "cwi")
                      :phonenumber "1234"
                      :projects (:xt/id prj-alpha))
           repository-a (make-repository "repository-a")
           repository-b (make-repository "repository-b")]
       (repl/put! repository-a)
       (repl/put! repository-b)
       (repl/put! (assoc prj-alpha
                         :assigned #{(:xt/id cwi)}
                         :repositories #{(:xt/id repository-a)
                                         (:xt/id repository-b)}
                         ))
       (repl/put! prj-bravo)
       (repl/put! (assoc (make-client "acme-corp") :projects #{(:xt/id prj-alpha)
                                                               (:xt/id prj-bravo)}))
       (repl/put! cwi)
       (repl/put! (assoc (make-employee "bob") :phonenumber "5678" :manager (:xt/id cwi)))
       (repl/put! (assoc (make-employee "ali") :phonenumber "91011" :juxt.site.alpha/type "https://test.example.com/contractor"))
       (repl/put! (update (make-action "getEmployee")
                          :juxt.pass.alpha/rules conj ['(include? action e)
                                                       ['e :juxt.site.alpha/type "https://test.example.com/employee"]]))
       (repl/put! (make-permission "getEmployee"))
       (repl/put! (update (make-action "getContractor")
                          :juxt.pass.alpha/rules conj ['(include? action e)
                                                       ['e :juxt.site.alpha/type "https://test.example.com/contractor"]]))
       (repl/put! (make-permission "getContractor"))
       (repl/put! (update (make-action "getProject")
                          :juxt.pass.alpha/rules conj ['(include? action e)
                                                       ['e :juxt.site.alpha/type "https://test.example.com/project"]]))
       (repl/put! (make-permission "getProject"))
       (repl/put! (update (make-action "getRepository")
                          :juxt.pass.alpha/rules conj ['(include? action e)
                                                       ['e :juxt.site.alpha/type "https://test.example.com/repository"]]))
       (repl/put! (make-permission "getRepository"))
       (repl/put! (update (make-action "getClient")
                          :juxt.pass.alpha/rules conj ['(include? action e)
                                                       ['e :juxt.site.alpha/type "https://test.example.com/client"]]))
       (repl/put! (make-permission "getClient"))))

(deftest graphql->xtdb-test

  (setup-db)

  (testing "Can return basic result with only one result"
    (is (=
         #{{:name "acme-corp"}}
         (graphql-proc/run
           "query findEmployeephonenumbers {client { name } }"
           (repl/db)
           example-compiled-schema
           "subject"
           nil))))

  (testing "Can return multiple-fields in result with only one result"
    (is (=
         #{{:name "acme-corp"
            :projects #{"https://test.example.com/project/alpha"
                        "https://test.example.com/project/bravo"}}}
         (graphql-proc/run
           "query findEmployeephonenumbers {client { name, projects } }"
           (repl/db)
           example-compiled-schema
           "subject"
           nil))))

  (testing "Can return basic result with multiple results"
    (is (=
         #{{:juxtcode "bob"}
           {:juxtcode "cwi"}}
         (graphql-proc/run
           "query findEmployeephonenumbers {employee { juxtcode } }"
           (repl/db)
           example-compiled-schema
           "subject"
           nil))))

  (testing "Can return multi-layer result"
    (is (=
         #{{:juxtcode "bob"
            :manager #{{:juxtcode "cwi"}}}}
         (graphql-proc/run
           "query findEmployeephonenumbers {employee {juxtcode,  manager { juxtcode } } }"
           (repl/db)
           example-compiled-schema
           "subject"
           nil))))

  (testing "Can return multi-layer result with multiple results"
    (is (= #{{:name "acme-corp"
              :projects #{{:name "alpha"}
                          {:name "bravo"}}}}
           (graphql-proc/run
             "query findEmployeephonenumbers {client {name,  projects { name } } }"
             (repl/db)
             example-compiled-schema
             "subject"
             nil))))

  (testing "Can return multi-layer result with no local fields"
    (is (= #{{:juxtcode "bob"
              :manager #{{:juxtcode "cwi"
                          :projects #{{:assigned #{{:juxtcode "cwi"}}}}}}}}
           (graphql-proc/run
             "query findEmployeePhoneNumbers { employee { juxtcode, manager { juxtcode, projects { assigned { juxtcode } } } } }"
             (repl/db)
             example-compiled-schema
             "subject"
             nil)))
    (is (= #{{:juxtcode "bob"
              :manager #{{:projects #{{:assigned #{{:juxtcode "cwi"}}}}}}}}
           (graphql-proc/run
             "query findEmployeePhoneNumbers { employee { juxtcode, manager { projects { assigned { juxtcode } } } } }"
             (repl/db)
             example-compiled-schema
             "subject"
             nil)))))