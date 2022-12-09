;; Copyright © 2022, JUXT LTD.

(ns juxt.site.bootstrap
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.edn :as edn]
   [juxt.site.resources :as resources]
   [juxt.site.init :as init :refer [converge!]]
   [juxt.site.actions :as actions]))

(defn install-system-subject! [_]
  {:juxt.site/input
   ;; tag::install-system-subject![]
   {:xt/id "https://example.org/subjects/system"
    :juxt.site/description "The system subject"
    :juxt.site/type "https://meta.juxt.site/site/subject"}
   ;; end::install-system-subject![]
   })

(defn install-system-permissions! [_]
  {:juxt.site/input
   ;; tag::install-system-permissions![]
   {:xt/id "https://example.org/permissions/system/bootstrap"
    :juxt.site/type "https://meta.juxt.site/site/permission" ; <1>
    :juxt.site/action #{"https://example.org/actions/create-action"
                        "https://example.org/actions/grant-permission"} ; <2>
    :juxt.site/purpose nil                                              ; <3>
    :juxt.site/subject "https://example.org/subjects/system"            ; <4>
    }
   ;; end::install-system-permissions![]
   })

(defn install-do-action-fn! [_]
  {:juxt.site/input (actions/install-do-action-fn)})

(defn grant-permission-get-not-found! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/get-not-found"
    :juxt.site/action "https://example.org/actions/get-not-found"
    :juxt.site/purpose nil}})

(defn create-grant-permission-action! [_]
  ;; tag::create-grant-permission-action![]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/grant-permission"
    :juxt.site/type "https://meta.juxt.site/site/action"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/permissions/(.+)"]]
     [:juxt.site/action [:re "https://example.org/actions/(.+)"]]
     [:juxt.site/purpose [:maybe :string]]]

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
             :juxt.site/type "https://meta.juxt.site/site/permission")))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.site/subject subject]]

      ;; This might be overly powerful, as a general way of granting anyone a
      ;; permission on any action! Let's comment for now
      #_[(allowed? subject resource permission)
         [subject :juxt.site/user-identity id]
         [id :juxt.site/user user]
         [user :role role]
         [permission :role role]]]}}
  ;; end::create-grant-permission-action![]
  )

;; TODO: Is this actually tested anywhere? Test a 404
(defn create-action-get-not-found! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/get-not-found"
    #_:juxt.site/transact
    #_{:juxt.site.sci/program
       (pr-str
        '(do
           [[:ring.response/status 404]]))}
    :juxt.site/rules
    [
     ['(allowed? subject resource permission)
      ['permission :xt/id]]]}})

(defn create-action-install-not-found! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/install-not-found"

    :juxt.site.malli/input-schema
    [:map
     [:xt/id [:re "https://example.org/.*"]]]

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
             :juxt.site/type "https://meta.juxt.site/not-found"
             :juxt.site/methods
             {:get {:juxt.site/actions #{"https://example.org/actions/get-not-found"}}
              :head {:juxt.site/actions #{"https://example.org/actions/get-not-found"}}})))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    [
     ['(allowed? subject resource permission)
      '[permission :juxt.site/subject subject]]]}})

(defn grant-permission-install-not-found! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/system/install-not-found"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/install-not-found"
    :juxt.site/purpose nil}})

(defn install-not-found-resource! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/install-not-found"
   :juxt.site/input
   {:xt/id "https://example.org/_site/not-found"}})

(defn create-action-register-application!
  "Install an action to register an application"
  [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/create-action"
   :juxt.site/input
   {:xt/id "https://example.org/actions/register-application"

    :juxt.site.malli/input-schema
    [:map
     [:juxt.site/client-id [:re "[a-z-]{3,}"]]
     [:juxt.site/redirect-uri [:re "https://"]]]

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
           (let [input (some->
                        body
                        (String.)
                        clojure.edn/read-string
                        juxt.site.malli/validate-input)
                 client-id (:juxt.site/client-id input)]
             (into
              {:xt/id (format "https://example.org/applications/%s" client-id)
               :juxt.site/type "https://meta.juxt.site/site/application"}
              input)))))}

    :juxt.site/transact
    {:juxt.site.sci/program
     (pr-str
      '[[:xtdb.api/put *prepare*]])}

    :juxt.site/rules
    '[[(allowed? subject resource permission)
       [permission :juxt.site/subject "https://example.org/subjects/system"]]

      [(allowed? subject resource permission)
       [id :juxt.site/user user]
       [subject :juxt.site/user-identity id]
       [user :role role]
       [permission :role role]]]}})

(defn grant-permission-to-invoke-action-register-application! [_]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/grant-permission"
   :juxt.site/input
   {:xt/id "https://example.org/permissions/system/register-application"
    :juxt.site/subject "https://example.org/subjects/system"
    :juxt.site/action "https://example.org/actions/register-application"
    :juxt.site/purpose nil}})

(def dependency-graph
  {"https://meta.juxt.site/do-action"
   {:create install-do-action-fn!}

   "https://example.org/subjects/system"
   {:create install-system-subject!}

   "https://example.org/permissions/system/bootstrap"
   {:create install-system-permissions!}

   "https://example.org/permissions/get-not-found"
   {:create grant-permission-get-not-found!}

   "https://example.org/actions/grant-permission"
   {:create create-grant-permission-action!
    :deps #{"https://meta.juxt.site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"
            "https://example.org/permissions/system/bootstrap"}}

   "https://example.org/actions/get-not-found"
   {:create create-action-get-not-found!
    :deps #{"https://meta.juxt.site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/actions/install-not-found"
   {:create create-action-install-not-found!
    :deps #{"https://meta.juxt.site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/permissions/system/install-not-found"
   {:create grant-permission-install-not-found!
    :deps #{"https://meta.juxt.site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/grant-permission"}}

   "https://example.org/_site/not-found"
   {:create install-not-found-resource!
    :deps #{"https://meta.juxt.site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/install-not-found"}}

   "https://example.org/actions/register-application"
   {:create create-action-register-application!}

   "https://example.org/permissions/system/register-application"
   {:create grant-permission-to-invoke-action-register-application!}})

(defn resource-file-resolver [dir]
  (fn resource-file-resolver* [id]
    (let [[_ path] (re-matches #"https://example.org/(.+)" id)
          resource-file (io/file dir (str path ".edn"))]
      (when
        (.exists resource-file)
        (-> (edn/read-string {:readers resources/READERS} (slurp resource-file))
            (assoc :id id))))))

(defn load-dependency-graph-from-filesystem [dir base-uri]
  (let [root (io/file dir)]
    (into {}
          (for [f (file-seq root)
                :let [path (.toPath f)
                      relpath (.toString (.relativize (.toPath root) path))
                      [_ urlpath] (re-matches #"(.+)\.edn" relpath)]
                :when (.isFile f)
                ]
            [(str base-uri "/" urlpath)
             (edn/read-string {:readers resources/READERS} (slurp f))]))))

(def CORE_RESOURCE_SET
  #{"https://meta.juxt.site/do-action"
    "https://example.org/_site/not-found"

    "https://example.org/subjects/system"

    "https://example.org/actions/create-action"
    "https://example.org/actions/grant-permission"
    "https://example.org/actions/install-not-found"
    "https://example.org/actions/get-not-found"

    "https://example.org/permissions/system/bootstrap"
    "https://example.org/permissions/system/install-not-found"
    "https://example.org/permissions/get-not-found"})

(defn add-implicit-dependencies
  "For each entry in the given map, associate the core dependencies such that a
  core dependency is always created before the entry."
  [graph]
  (->> graph
       (map (fn update-deps [[k v]]
              [k
               (cond-> v
                 (not (contains? CORE_RESOURCE_SET k))
                 (update :deps (fn merge-core-resources [x]
                                 (cond
                                   ;; Wrap in middleware that adds the core resources
                                   (fn? x) (fn [args] (set/union CORE_RESOURCE_SET (set (x args))))
                                   :else
                                   (set/union CORE_RESOURCE_SET (set x))))))]))
       (into {})))

(defn bootstrap-resources! [resources opts]
  (init/converge!
   (concat CORE_RESOURCE_SET resources)
   (add-implicit-dependencies
    (merge
     dependency-graph
     (load-dependency-graph-from-filesystem
      "resources"
      ;; When bootstrapping, we treat every resource as
      ;; https://example.org. This ensures the graph of dependencies is
      ;; reliable. The init/converge! function is responsible for the final
      ;; 'rendering' where example.org might be replaced by the Site admin's
      ;; domain name.
      "https://example.org")
     (:graph opts)))
   opts))

(defn bootstrap* [opts]
  (bootstrap-resources!
   #{
     ;;"https://example.org/actions/put-user"
     ;;"https://example.org/actions/put-openid-user-identity"
     ;;"https://example.org/permissions/system/put-user"
     ;;"https://example.org/permissions/system/put-openid-user-identity"
     ;;:juxt.site.openid/all-actions
     ;;:juxt.site.openid/default-permissions
     ;; "https://example.org/permissions/system/put-session-scope"
     }
   opts))

(defn bootstrap
  ([] (bootstrap {}))
  ([opts]
   (bootstrap*
    (merge
     {:dry-run? true
      :recreate? false
      :base-uri "https://site.test"}
     opts))))

(defn bootstrap!
  ([] (bootstrap! nil))
  ([opts]
   (bootstrap (merge opts {:dry-run? false}))))

(defn bootstrap!!
  ([] (bootstrap!! nil))
  ([opts] (bootstrap! (merge opts {:recreate? true}))))

;; Install openid connect provider, must be in .config/site/openid-client.edn
;; Or rather, POST a document to the https://example.org/actions/install-openid-issuer
;; and https://example.org/actions/install-openid-client
;; and https://example.org/actions/install-openid-login-endpoint

#_(defn install-openid! []
    (converge!
     #{:juxt.site.init/system
       :juxt.site.openid/all-actions
       :juxt.site.openid/default-permissions
       "https://example.org/openid/auth0/issuer"
       "https://example.org/openid/auth0/client-configuration"
       "https://example.org/openid/login"
       ;;"https://example.org/openid/callback"
       "https://example.org/session-scopes/openid"}

     (merge
      dependency-graph
      openid/dependency-graph
      session-scope/dependency-graph
      user/dependency-graph)

     {:dry-run? false :recreate? true}))

#_(defn install-openid-user!
  [& {:keys [name username]
      :juxt.site.jwt.claims/keys [iss sub nickname]}]
  (user/put-user! :username username :name name)
  (openid/put-openid-user-identity!
   (cond-> {:username username
            :juxt.site.jwt.claims/iss iss}
     sub (assoc :juxt.site.jwt.claims/sub sub)
     nickname (assoc :juxt.site.jwt.claims/nickname nickname))))

#_(comment
  (install-openid-user!
   :username "mal"
   :name "Malcolm Sparks"
   :juxt.site.jwt.claims/iss "https://juxt.eu.auth0.com/"
   :juxt.site.jwt.claims/nickname "malcolmsparks"))
