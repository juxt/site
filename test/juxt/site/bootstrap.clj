;; Copyright © 2022, JUXT LTD.

(ns juxt.site.bootstrap
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [juxt.site.alpha.init :refer [put! base-uri substitute-actual-base-uri converge! do-action]]
   [juxt.pass.openid :as openid]
   [juxt.pass.alpha.actions :as actions]
   [juxt.pass.session-scope :as session-scope]
   [juxt.pass.user :as user]
   [juxt.flip.alpha.core :as f]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]))

(defn install-system-subject! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-system-subject![]
      {:xt/id "https://example.org/subjects/system"
       :juxt.site.alpha/description "The system subject"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}
      ;; end::install-system-subject![]
      )))))

(defn install-system-permissions! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-system-permissions![]
      {:xt/id "https://example.org/permissions/system/bootstrap"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/permission" ; <1>
       :juxt.pass.alpha/action #{"https://example.org/actions/create-action"
                                 "https://example.org/actions/grant-permission"} ; <2>
       :juxt.pass.alpha/purpose nil      ; <3>
       :juxt.pass.alpha/subject "https://example.org/subjects/system" ; <4>
       }
      ;; end::install-system-permissions![]
      )))))

(defn install-do-action-fn! [_]
  (put! (actions/install-do-action-fn (base-uri))))

(defn grant-permission-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/grant-permission"
        {:xt/id "https://example.org/permissions/get-not-found"
         :juxt.pass.alpha/action "https://example.org/actions/get-not-found"
         :juxt.pass.alpha/purpose nil}))))))

(defn install-create-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/put!
      ;; tag::install-create-action![]
      {:xt/id "https://example.org/actions/create-action"
       :juxt.site.alpha/description "The action to create all other actions"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"

       :juxt.pass.alpha/rules
       '[
         ;; Creating actions should only be available to the most trusted
         ;; subjects. Actions can write directly to the database, if they wish.
         [(allowed? subject resource permission) ; <1>
          [permission :juxt.pass.alpha/subject subject]]]

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [juxt.site.alpha/request-body-as-edn

                (site/validate
                 [:map                  ; <2>
                  [:xt/id [:re "https://example.org/actions/(.+)"]]
                  [:juxt.pass.alpha/rules [:vector [:vector :any]]]])

                (site/set-type "https://meta.juxt.site/pass/action") ; <3>

                xtdb.api/put]))]))}}
      ;; end::install-create-action![]
      )))))

(defn create-grant-permission-action! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::create-grant-permission-action![]
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/grant-permission"
       :juxt.site.alpha/type "https://meta.juxt.site/pass/action"

       :juxt.pass.alpha/rules
       '[
         [(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject subject]]

         ;; This might be overly powerful, as a general way of granting anyone a
         ;; permission on any action! Let's comment for now
         #_[(allowed? subject resource permission)
          [subject :juxt.pass.alpha/user-identity id]
          [id :juxt.pass.alpha/user user]
          [user :role role]
          [permission :role role]]]

       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [juxt.site.alpha/request-body-as-edn
                (juxt.site.alpha/validate
                 [:map
                  [:xt/id [:re "https://example.org/permissions/(.+)"]]
                  [:juxt.pass.alpha/action [:re "https://example.org/actions/(.+)"]]
                  [:juxt.pass.alpha/purpose [:maybe :string]]])
                (f/set-at (f/dip ["https://meta.juxt.site/pass/permission" :juxt.site.alpha/type]))
                xtdb.api/put]))]))}})
     ;; end::create-grant-permission-action![]
     ))))

(defn create-action-get-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/get-not-found"
       #_:juxt.site.alpha/transact
       #_{:juxt.site.alpha.sci/program
        (pr-str
         '(do
            [[:ring.response/status 404]]))}
       :juxt.pass.alpha/rules
       [
        ['(allowed? subject resource permission)
         ['permission :xt/id]]]})))))

(defn create-action-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/install-not-found"
       :juxt.site.alpha/transact
       {:juxt.flip.alpha/quotation
        `(
          (site/with-fx-acc
            [(site/push-fx
              (f/dip
               [site/request-body-as-edn
                (site/validate
                 [:map
                  [:xt/id [:re "https://example.org/.*"]]])
                (site/set-type "https://meta.juxt.site/not-found")
                (site/set-methods
                 {:get {:juxt.pass.alpha/actions #{"https://example.org/actions/get-not-found"}}
                  :head {:juxt.pass.alpha/actions #{"https://example.org/actions/get-not-found"}}})
                xtdb.api/put]))]))}
       :juxt.pass.alpha/rules
       [
        ['(allowed? subject resource permission)
         '[permission :juxt.pass.alpha/subject subject]]]})))))

(defn grant-permission-install-not-found! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/install-not-found"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/install-not-found"
       :juxt.pass.alpha/purpose nil})))))

(defn install-not-found-resource! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (do
       (juxt.site.alpha.init/do-action
        "https://example.org/subjects/system"
        "https://example.org/actions/install-not-found"
        {:xt/id "https://example.org/_site/not-found"}))))))

;; TODO: In the context of an application, rename 'put' to 'register'
(defn create-action-register-application!
  "Install an action to register an application"
  [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/register-application"

       :juxt.site.alpha.malli/input-schema
       [:map
        [:juxt.pass.alpha/client-id [:re "[a-z-]{3,}"]]
        [:juxt.pass.alpha/redirect-uri [:re "https://"]]]

       :juxt.site.alpha/prepare
       {:juxt.site.alpha.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site.alpha/received-representation
                                 :juxt.http.alpha/content-type)
                body (-> *ctx*
                         :juxt.site.alpha/received-representation
                         :juxt.http.alpha/body)]
            (case content-type
              "application/edn"
              (let [input (some->
                           body
                           (String.)
                           clojure.edn/read-string
                           juxt.site.malli/validate-input)
                    client-id (:juxt.pass.alpha/client-id input)]
                (into
                 {:xt/id (format "%s/applications/%s" (:juxt.site.alpha/base-uri *ctx*) client-id)
                  :juxt.site.alpha/type "https://meta.juxt.site/pass/application"}
                 input)))))}

       :juxt.site.alpha/transact
       {:juxt.site.alpha.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       #_{:juxt.flip.alpha/quotation
          `(
            (site/with-fx-acc
              [(site/push-fx
                (f/dip
                 [site/request-body-as-edn
                  (site/validate
                   [:map
                    [:juxt.pass.alpha/client-id [:re "[a-z-]{3,}"]]
                    [:juxt.pass.alpha/redirect-uri [:re "https://"]]])

                  (site/set-type "https://meta.juxt.site/pass/application")
                  (f/set-at (f/dip [(pass/as-hex-str (pass/random-bytes 20)) :juxt.pass.alpha/client-secret]))

                  (f/set-at
                   (f/keep
                    [(f/of :juxt.pass.alpha/client-id) "/applications/" f/str (f/env :juxt.site.alpha/base-uri) f/str
                     :xt/id]))

                  xtdb.api/put]))]))}

       :juxt.pass.alpha/rules
       '[[(allowed? subject resource permission)
          [permission :juxt.pass.alpha/subject "https://example.org/subjects/system"]]

         [(allowed? subject resource permission)
          [id :juxt.pass.alpha/user user]
          [subject :juxt.pass.alpha/user-identity id]
          [user :role role]
          [permission :role role]]]})))))

(defn grant-permission-to-invoke-action-register-application! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.alpha.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/register-application"
       :juxt.pass.alpha/subject "https://example.org/subjects/system"
       :juxt.pass.alpha/action "https://example.org/actions/register-application"
       :juxt.pass.alpha/purpose nil})))))

(def dependency-graph
  {"https://example.org/_site/do-action"
   {:create install-do-action-fn!}

   "https://example.org/subjects/system"
   {:create install-system-subject!}

   "https://example.org/permissions/system/bootstrap"
   {:create install-system-permissions!}

   "https://example.org/permissions/get-not-found"
   {:create grant-permission-get-not-found!}

   "https://example.org/actions/create-action"
   {:create install-create-action!}

   "https://example.org/actions/grant-permission"
   {:create create-grant-permission-action!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"
            "https://example.org/permissions/system/bootstrap"}}

   "https://example.org/actions/get-not-found"
   {:create create-action-get-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/actions/install-not-found"
   {:create create-action-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/create-action"}}

   "https://example.org/permissions/system/install-not-found"
   {:create grant-permission-install-not-found!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/grant-permission"}}

   "https://example.org/_site/not-found"
   {:create install-not-found-resource!
    :deps #{"https://example.org/_site/do-action"
            "https://example.org/subjects/system"
            "https://example.org/actions/install-not-found"}}

   :juxt.site.alpha.init/system
   {:deps #{"https://example.org/_site/do-action"
            "https://example.org/_site/not-found"

            "https://example.org/subjects/system"

            "https://example.org/actions/create-action"
            "https://example.org/actions/grant-permission"
            "https://example.org/actions/install-not-found"
            "https://example.org/actions/get-not-found"

            "https://example.org/permissions/system/bootstrap"
            "https://example.org/permissions/system/install-not-found"
            "https://example.org/permissions/get-not-found"}}

   "https://example.org/actions/register-application"
   {:create create-action-register-application!
    :deps #{:juxt.site.alpha.init/system}}

   "https://example.org/permissions/system/register-application"
   {:create grant-permission-to-invoke-action-register-application!
    :deps #{:juxt.site.alpha.init/system}}})

(defn bootstrap* [opts]
  (converge!
   #{:juxt.site.alpha.init/system
     :juxt.pass.user/all-actions
     :juxt.pass.user/default-permissions
     :juxt.pass.openid/all-actions
     :juxt.pass.openid/default-permissions
     (substitute-actual-base-uri "https://example.org/permissions/system/put-session-scope")}
   (substitute-actual-base-uri
    (merge
     dependency-graph
     openid/dependency-graph
     session-scope/dependency-graph
     user/dependency-graph))
   opts))

(defn bootstrap []
  (bootstrap* {:dry-run? true :recreate? false}))

(defn bootstrap! []
  (bootstrap* {:dry-run? false :recreate? false}))

(defn bootstrap!! []
  (bootstrap* {:dry-run? false :recreate? true}))

;; Install openid connect provider, must be in .config/site/openid-client.edn
;; Or rather, POST a document to the https://example.org/actions/install-openid-issuer
;; and https://example.org/actions/install-openid-client
;; and https://example.org/actions/install-openid-login-endpoint

;; TODO: Rewrite this in terms of a converge
(defn install-openid! []
  (converge!
   #{:juxt.site.alpha.init/system
     :juxt.pass.openid/all-actions
     :juxt.pass.openid/default-permissions
     (substitute-actual-base-uri "https://example.org/openid/auth0/issuer")
     (substitute-actual-base-uri "https://example.org/openid/auth0/client-configuration")
     (substitute-actual-base-uri "https://example.org/openid/login")
     ;;(substitute-actual-base-uri "https://example.org/openid/callback")
     (substitute-actual-base-uri "https://example.org/session-scopes/openid")}

   (substitute-actual-base-uri
    (merge
     dependency-graph
     openid/dependency-graph
     session-scope/dependency-graph
     user/dependency-graph))

   {:dry-run? false :recreate? true}))

(defn install-openid-user!
  [& {:keys [name username]
      :juxt.pass.jwt.claims/keys [iss sub nickname]}]
  (user/put-user! :username username :name name)
  (user/put-openid-user-identity!
   (cond-> {:username username
            :juxt.pass.jwt.claims/iss iss}
     sub (assoc :juxt.pass.jwt.claims/sub sub)
     nickname (assoc :juxt.pass.jwt.claims/nickname nickname))))

(comment
  (install-openid-user!
   :username "mal"
   :name "Malcolm Sparks"
   :juxt.pass.jwt.claims/iss "https://juxt.eu.auth0.com/"
   :juxt.pass.jwt.claims/nickname "malcolmsparks"))

(comment
  (put!
   {:xt/id "https://site.test/actions/whoami"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
    :juxt.pass.alpha/rules
    '[
      [(allowed? subject resource permission)
       [permission :juxt.pass.alpha/subject subject]]

      [(allowed? subject resource permission)
       [subject :juxt.pass.alpha/user-identity id]
       [id :juxt.pass.alpha/user user]
       [permission :juxt.pass.alpha/user user]]]}))

(comment
  (put!
   {:xt/id "https://site.test/whoami"
    :juxt.site.alpha/methods
    {:get {:juxt.pass.alpha/actions #{"https://site.test/actions/whoami"}}}
    :juxt.http.alpha/content-type "text/plain"
    :juxt.http.alpha/content "Hello - who are you?"}))

(comment
  (put!
   {:xt/id "https://site.test/permissions/malcolm/whoami"
    :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"
    :juxt.pass.alpha/user "https://site.test/users/mal"
    :juxt.pass.alpha/action "https://site.test/actions/whoami"
    :juxt.pass.alpha/purpose nil}))
