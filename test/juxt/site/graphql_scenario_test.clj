;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql-scenario-test
  (:require
   [juxt.site.logging :refer [with-logging]]
   [juxt.grab.alpha.schema :as gs]
   [clojure.test :refer [deftest is use-fixtures]]
   [juxt.site.resources :as resources]
   [juxt.site.test-helpers.login :as login]
   [juxt.site.repl :as repl]
   [juxt.test.util :refer [with-system-xt with-resources with-fixtures *handler*
                           with-handler
                           assoc-request-payload
                           with-session-token with-bearer-token]]))

(use-fixtures :each with-system-xt with-handler)

(def dependency-graph
  {"https://example.org/actions/get-graphql-type"
   {:deps #{:juxt.site.init/system}
    :create
    (fn [{:keys [id]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/create-action"
       :juxt.site/input
       {:xt/id id
        ;; TODO: Do a view
        :juxt.site/rules
        '[
          [(allowed? subject resource permission)
           [subject :juxt.site/user-identity id]
           [id :juxt.site/user user]
           [permission :juxt.site/user user]]]
        }})}

   "https://example.org/permissions/{username}/get-graphql-type"
   {:deps #{:juxt.site.init/system
            "https://example.org/actions/get-graphql-type"}
    :create
    (fn [{:keys [id params]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/grant-permission"
       :juxt.site/input
       {:xt/id id
        :juxt.site/action "https://example.org/actions/get-graphql-type"
        :juxt.site/purpose nil
        :juxt.site/user (format "https://example.org/users/%s" (get params "username"))}})}

   "https://example.org/actions/delete-graphql-type"
   {:deps #{:juxt.site.init/system}
    :create
    (fn [{:keys [id]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/create-action"
       :juxt.site/input
       {:xt/id id
        ;; TODO: The reason we want a transact here is so we can recompile the
        ;; GraphQL schema, after deleting the type. Our handler doesn't support
        ;; this yet.
        :juxt.site/transact
        {:juxt.site.sci/program
         (pr-str
          '(do
             ;; We must recompile

             (let [schema-id (:juxt.site/graphql-schema *resource*)

                   _ (when-not schema-id
                       (throw
                        (ex-info
                         "This action requires that the resource contain a :juxt.site/graphql-schema entry"
                         {:resource *resource*})))

                   name-to-delete (get-in *resource* [:juxt.grab/type-definition :juxt.grab.alpha.graphql/name])

                   _ (when-not name-to-delete
                       (throw
                        (ex-info
                         "This action requires that the resource contain a named type definition"
                         {:resource *resource*})))

                   types (->
                          (into {} (map (juxt :juxt.grab.alpha.graphql/name identity) (grab.parsed-types schema-id)))
                          (dissoc name-to-delete)
                          vals)

                   compile-output (grab.compile-schema types)

                   compile-output-id (str schema-id "compile-status")]

               (into
                [[:xtdb.api/put
                  {:xt/id compile-output-id
                   :juxt.site/type "https://meta.juxt.site/site/graphql-compile-status"
                   :juxt.site/graphql-schema schema-id
                   :juxt.grab/compile-status compile-output}]
                 [:xtdb.api/delete (:xt/id *resource*)]
                 [:ring.response/status 204]]))))}

        :juxt.site/rules
        '[
          [(allowed? subject resource permission)
           [subject :juxt.site/user-identity id]
           [id :juxt.site/user user]
           [permission :juxt.site/user user]]]}})}

   "https://example.org/permissions/{username}/delete-graphql-type"
   {:deps #{:juxt.site.init/system
            "https://example.org/actions/delete-graphql-type"}
    :create
    (fn [{:keys [id params]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/grant-permission"
       :juxt.site/input
       {:xt/id id
        :juxt.site/action "https://example.org/actions/delete-graphql-type"
        :juxt.site/purpose nil
        :juxt.site/user (format "https://example.org/users/%s" (get params "username"))}})}

   "https://example.org/actions/install-graphql-type"
   {:deps #{:juxt.site.init/system}
    :create
    (fn [{:keys [id]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/create-action"
       :juxt.site/input
       {:xt/id id

        :juxt.site/prepare
        {:juxt.site.sci/program
         (pr-str
          '(->> (get-in *ctx* [:juxt.site/received-representation :juxt.http/content])
                grab.parse
                (mapv
                 (fn [typedef]
                   (let [type-name (:juxt.grab.alpha.graphql/name typedef)]
                     {:xt/id (str (:xt/id *resource*) "types/" type-name)
                      :juxt.site/type "https://meta.juxt.site/site/graphql-type"
                      :juxt.site/methods
                      {:get {:juxt.site/actions #{"https://example.org/actions/get-graphql-type"}}
                       :delete {:juxt.site/actions #{"https://example.org/actions/delete-graphql-type"}}}
                      :juxt.site/graphql-schema (:xt/id *resource*)
                      :juxt.grab/type-definition typedef
                      ;; Inherit the protection space of the resource
                      :juxt.site/protection-spaces (:juxt.site/protection-spaces *resource*)
                      })))))}

        :juxt.site/transact
        {:juxt.site.sci/program
         (pr-str
          '(do
             (let [compile-output
                   (grab.compile-schema
                    (->>
                     ;; Attempt to combine any existing types in the database
                     ;; with the new/replacement ones brought in here.
                     (concat
                      (map (juxt :juxt.grab.alpha.graphql/name identity) (grab.parsed-types (:xt/id *resource*)))
                      (map (fn [typ] [(:juxt.grab.alpha.graphql/name :juxt.grab/type-definition typ)
                                      (:juxt.grab/type-definition typ)]) *prepare*))
                     (into {})
                     vals))

                   compile-output-id (str (:xt/id *resource*) "compile-status")]

               (into
                [[:xtdb.api/put
                  {:xt/id compile-output-id
                   :juxt.site/type "https://meta.juxt.site/site/graphql-compile-status"
                   :juxt.site/graphql-schema (:xt/id *resource*)
                   :juxt.grab/compile-status compile-output}]
                 [:ring.response/headers {"location" compile-output-id}]
                 [:ring.response/status 201]]
                (mapv (fn [x] [:xtdb.api/put x]) *prepare*)))))}

        :juxt.site/rules
        '[
          [(allowed? subject resource permission)
           [subject :juxt.site/user-identity id]
           [id :juxt.site/user user]
           [permission :juxt.site/user user]]]}})}

   "https://example.org/permissions/{username}/install-graphql-type"
   {:deps #{:juxt.site.init/system
            "https://example.org/actions/install-graphql-type"}
    :create
    (fn [{:keys [id params]}]
      {:juxt.site/subject-id "https://example.org/subjects/system"
       :juxt.site/action-id "https://example.org/actions/grant-permission"
       :juxt.site/input
       {:xt/id id
        :juxt.site/action "https://example.org/actions/install-graphql-type"
        :juxt.site/purpose nil
        :juxt.site/user (format "https://example.org/users/%s" (get params "username"))}})}

   "https://example.org/graphql/schema/"
   {:deps #{:juxt.site.init/system
            "https://example.org/protection-spaces/bearer"}
    :create
    (fn [{:keys [id]}]
      {:put!                            ; TODO: install remotely
       {:xt/id id
        :juxt.site/uri-template true
        :juxt.site/methods
        {:post
         {:juxt.site/actions #{"https://example.org/actions/install-graphql-type"}
          :juxt.site/acceptable {"accept" "text/plain"}}}
        :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}})}

   #_#_"https://example.org/graphql/schema-compilation"
   {:deps #{:juxt.site.init/system
            "https://example.org/protection-spaces/bearer"}
    :create
    (fn [{:keys [id]}]
      (init/put! ;; TODO: install remotely
       {:xt/id id
        :juxt.site/graphql-schema "https://example.org/graphql/schema/"
        :juxt.site/methods
        {:post
         {:juxt.site/actions #{"https://example.org/actions/compile-graphql-schema"}}}
        :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}))}



   #_"https://example.org/graphql"
   #_{:deps #{:juxt.site.init/system
              "https://example.org/actions/query-with-graphql"
              "https://example.org/protection-spaces/bearer"}
      :create (fn [{:keys [id]}]
                (init/put! ;; install-graphql-endpoint
                 {:xt/id id
                  :juxt.site/methods
                  {:get {:juxt.site/actions #{"https://example.org/actions/whoami"}}}
                  :juxt.site/protection-spaces #{"https://example.org/protection-spaces/bearer"}}))}

   })

(comment
  (with-fixtures
    (with-resources
      ^{:dependency-graphs
        #{(resources/load-dependency-graph "juxt/site/session-scope.edn")
          (resources/load-dependency-graph "juxt/site/user.edn")
          (resources/load-dependency-graph "juxt/site/form-based-auth.edn")
          (resources/load-dependency-graph "juxt/site/oauth.edn")
          (resources/load-dependency-graph "juxt/site/protection-space.edn")
          (resources/load-dependency-graph "juxt/site/example-users.edn")
          (resources/load-dependency-graph "juxt/site/example-applications.edn")
          (resources/load-dependency-graph "juxt/site/example-protection-spaces.edn")
          dependency-graph}}

      #{"https://example.org/user-identities/alice" ; Alice
        "https://example.org/login"         ; a way Alice can identity herself
        "https://example.org/applications/test-app" ; an app
        :juxt.site.oauth/authorization-server              ; a way of authorizing the app
        "https://example.org/permissions/alice-can-authorize" ; which Alice can use

        "https://example.org/actions/install-graphql-type"
        "https://example.org/permissions/alice/install-graphql-type"

        "https://example.org/actions/delete-graphql-type"
        "https://example.org/permissions/alice/delete-graphql-type"

        "https://example.org/actions/get-graphql-type"
        "https://example.org/permissions/alice/get-graphql-type"

        "https://example.org/graphql/schema/"}

      (let [login-result
            (login/login-with-form!
             *handler*
             "username" "alice"
             "password" "garden"
             :juxt.site/uri "https://example.org/login")

            _ (assert (:juxt.site/session-token login-result))

            {access-token "access_token" :as authorize-response}
            (with-session-token (:juxt.site/session-token login-result)
              (oauth/authorize!
               {"client_id" "test-app"}))]

        (when-not access-token
          (throw
           (ex-info
            "Error on authorize"
            {:authorize-response authorize-response})))

        ;; To install a GraphQL schema, we POST to the schema resource. We can
        ;; build up the schema incrementally, posting any type or types which will
        ;; add to the schema. If we want to start over, we can DELETE the schema.
        (with-bearer-token access-token
          (let [response
                (*handler*
                 (-> {:ring.request/method :post
                      :ring.request/path "/graphql/schema/"}
                     (assoc-request-payload
                      "text/plain"
                      "
type Patient {
  name: String
  age: Int
  medicalRecords: [MedicalRecord]
}

type MedicalRecord {
  description: String
}")))]
            (assert (= 201 (:ring.response/status response))))

          ;; Second interaction, we fix one of the problems by adding another type
          ;; (Query)

          ;; In this way, we allow an incremental approach to working with GraphQL
          ;; schema. This could be used by a GraphQL schema builder tool.

          (let [response
                (*handler*
                 (-> {:ring.request/method :post
                      :ring.request/path "/graphql/schema/"}
                     (assoc-request-payload
                      "text/plain"
                      "type Query { patients: [Patient] }")))]
            (assert (= 201 (:ring.response/status response)))))

        (repl/e "https://example.org/graphql/schema/compile-status")

        (with-bearer-token access-token
          (*handler*
           {:ring.request/method :delete
            :ring.request/path "/graphql/schema/types/MedicalRecord"}))

        (repl/e "https://example.org/graphql/schema/compile-status")
        ;;(repl/e "https://example.org/graphql/schema/types/Query")

        ;;(repl/ls)

        ;; Call an action to compile the schema?
        ;; The compilation status is held in /graphql/schema-compilation-status
        ;; The POST to the /graphql/schema-compilation-status causes the schema to be compiled, with gs/compile-schema over a vector of the types in /graphql/schema/*
        ))))


#_(juxt.grab.alpha.parser/parse
  "

type Patient {
  name: String
  age: Int
  picture: Url
}

type Project {
  name: String
  tagline: String
  contributors: [User]
}")

#_(gs/compile-schema
   (juxt.grab.alpha.parser/parse
  "

type Patient {
  name: String
  age: Int
  picture: Url
}

type Project {
  name: String
  tagline: String
  contributors: [User]
}"))

#_(juxt.grab.alpha.parser/parse
  "

type Patient {
  name: String
  age: Int
  picture: Url
}

type Project {
  name: String
  tagline: String
  contributors: [User]
}")
