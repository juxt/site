;; Copyright © 2022, JUXT LTD.

(ns juxt.site.bootstrap
  (:require
   [juxt.site.resource-package :as pack]
   [clojure.java.io :as io]
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [juxt.site.init :as init]))

;; TODO: Test a 404

(defn
  ^{:deprecated "install-bootstrap-package"}
  bootstrap-resources! [resources opts]
  (let [bootstrap-resource-set (some-> (pack/load-package-from-filesystem (io/file "resources/bootstrap")) :resources set)]
    (init/converge!
     (concat bootstrap-resource-set resources)
     (pack/add-bootstrap-resources-as-implicit-dependencies
      (merge
       (:dependency-graph (pack/load-package-from-filesystem "resources/bootstrap"))
       (:graph opts)))
     opts)))

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

(comment
  (spit "resources/login.yaml"
        (yaml/generate-string
         (edn/read-string (slurp (io/file "resources/login.edn")))
         :dumper-options {:flow-style :block}
         )))

(comment
  (yaml/parse-string (slurp (io/file "resources/login.yaml"))))
