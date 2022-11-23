;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.openid-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is are testing]]
   [java-http-clj.core :as hc]
   [juxt.pass :as-alias pass]
   [juxt.pass.resources.openid :as openid]
   [juxt.pass.resources.session-scope :as session-scope]
   [juxt.site :as-alias site]
   [juxt.site.init :as init]
   [juxt.site.repl :as repl]
   [juxt.site.bootstrap :as bootstrap]
   [juxt.test.util :refer [*handler* *xt-node* with-fixtures with-resources]]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [juxt.reap.alpha.regex :as re]))

(def ISSUER "https://juxt.eu.auth0.com")

(def dependency-graph
  {ISSUER
   {:deps #{::init/system
            "https://example.org/permissions/system/install-openid-issuer"
            "https://example.org/permissions/system/fetch-jwks"}
    :create (fn [{:keys [id]}]
              (openid/install-openid-issuer! id)
              ;; TODO: We should refresh this resource periodically by attaching the
              ;; https://example.org/actions/openid/fetch-jwks action to a Site scheduler.
              (openid/fetch-jwks! id))}

   "https://example.org/openid/auth0/client"
   {:deps #{::init/system
            ISSUER
            "https://example.org/actions/install-openid-client"
            "https://example.org/permissions/system/install-openid-client"}
    :create (fn [{:keys [id]}]
              (openid/install-openid-client
               (merge
                {:xt/id id}
                (-> "user.home"
                    System/getProperty
                    (io/file ".config/site/openid-client.edn")
                    slurp
                    edn/read-string))))}

   "https://example.org/session-scopes/openid"
   {:deps #{::init/system
            "https://example.org/permissions/system/put-session-scope"}

    :create (fn [{:keys [id]}]
              (eval
               (init/substitute-actual-base-uri
                `(init/do-action
                  "https://example.org/subjects/system"
                  "https://example.org/actions/put-session-scope"
                  {:xt/id ~id
                   :juxt.pass/cookie-name "sid"
                   :juxt.pass/cookie-domain "https://example.org"
                   :juxt.pass/cookie-path "/"
                   :juxt.pass/login-uri "https://example.org/openid/login"}))))}

   "https://example.org/openid/login"
   ;;(load-resource-installer "resources/openid/login.edn") TODO
   {:deps #{::init/system
            "https://example.org/openid/auth0/client"
            "https://example.org/permissions/system/install-openid-login-endpoint"
            "https://example.org/actions/login-with-openid"
            "https://example.org/session-scopes/openid"
            "https://example.org/permissions/login-with-openid"}
    :create (fn [{:keys [id]}]
              (openid/install-openid-login-endpoint!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.pass/session-scope "https://example.org/session-scopes/openid"
                 :juxt.pass/openid-client "https://example.org/openid/auth0/client"})))}

   "https://example.org/openid/callback"
   {:deps #{::init/system
            "https://example.org/permissions/system/install-openid-callback-endpoint"
            "https://example.org/actions/openid/exchange-code-for-id-token"
            "https://example.org/permissions/openid/exchange-code-for-id-token"
            "https://example.org/openid/auth0/client"}
    :create (fn [{:keys [id]}]
              (openid/install-openid-callback-endpoint!
               (init/substitute-actual-base-uri
                {:xt/id id
                 :juxt.pass/openid-client "https://example.org/openid/auth0/client"})))}})

#_(with-fixtures
  (with-resources
    ^{:dependency-graphs
      #{openid/dependency-graph
        session-scope/dependency-graph
        dependency-graph}}
    #{"https://site.test/openid/login"
      "https://site.test/openid/callback"}

    (repl/e ISSUER)
    (repl/ls)

    #_(let [login-request
          {:ring.request/method :get
           :ring.request/path "/openid/login"
           :ring.request/query (codec/form-encode {"return-to" "/index.html"})}

          login-response (*handler* login-request)

          location (get-in login-response [:ring.response/headers "location"])

          db (xt/db *xt-node*)

          session (first
                   (for [id (repl/ls-type "https://meta.juxt.site/pass/session")]
                     (xt/entity db id)))

          session-token (first
                         (for [id (repl/ls-type "https://meta.juxt.site/pass/session-token")]
                           (xt/entity db id)))

          state (:juxt.pass/state session)

          _ (assert (= 303 (:ring.response/status login-response)))


          #_provider-response #_(hc/send
                                 {:method :get
                                  :uri location})

          cookie-header-value (format "sid=%s" (:juxt.pass/session-token session-token))

          callback-request
          {:ring.request/method :get
           :ring.request/path "/openid/callback"
           :ring.request/query (codec/form-encode {"code" "1234" "state" state})
           :ring.request/headers {"cookie" cookie-header-value}}

          callback-response (*handler* callback-request)
          ]

      ;; Return response for now
      {:headers (:ring.response/headers login-response)
       :cookie-header-value cookie-header-value
       :session session
       :session-token session-token
       :location location
       :state state
       :result (repl/e :result)
       }

;;      (repl/e "https://site.test/openid/auth0/jwks")

      callback-response

;;      (repl/e :result)



      ;;(http-client/get location)

      )

    #_(let [req {:ring.request/method :get
                 :ring.request/path "/openid/callback"
                 :ring.request/query (codec/form-encode {"return-to" "/index.html"})
                 }
            response (*handler* req)
            location (get-in response [:ring.response/headers "location"])
            ]
        ;; Return response for now
        (is (= 303 (:ring.response/status response)))

        ;;(http-client/get location)

        (let [db (xt/db *xt-node*)]
          {:headers (:ring.response/headers response)
           :resources (repl/ls)
           :session-tokens
           (vec
            (for [id (repl/ls-type "https://meta.juxt.site/pass/session-token")]
              (xt/entity db id)))
           :sessions
           (vec
            (for [id (repl/ls-type "https://meta.juxt.site/pass/session")]
              (xt/entity db id)))})




        )))

#_(with-fixtures
    (with-resources
      #{"https://site.test/actions/openid/exchange-code-for-id-token"
        "https://site.test/permissions/openid/exchange-code-for-id-token"}

      (let [xt-node *xt-node*
            db (xt/db xt-node)
            lookup #(xt/entity db %)
            action-doc (lookup "https://site.test/actions/openid/exchange-code-for-id-token")]
        (do-prepare
         {::site/xt-node xt-node
          ::site/db db
          ::pass/subject (xt/entity db "https://site.test/subjects/system")
          ::pass/action "https://site.test/actions/openid/exchange-code-for-id-token"
          ::site/base-uri (init/base-uri)
          :ring.request/query "code=RyMRjiijG_zjlhKdFiLOHNkcsn1i1gDMRPQNGtteQ5Fx9&state=9e7d5227c7c818bc"}
         action-doc
         nil

         ))
      ))


;; To bootstrap into persistent data:

;; user> (openid/install-openid-issuer "https://juxt.eu.auth0.com")
