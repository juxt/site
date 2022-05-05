;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.demo :as demo]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db* *handler*] :as tutil]
   [xtdb.api :as xt]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.grab.alpha.parser :as parser]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]
   [juxt.http.alpha :as-alias http]
   [juxt.grab.alpha.graphql :as-alias g]
   [jsonista.core :as json]
   [clojure.tools.logging :as log]))

(defn with-setup [f]
  (demo/demo-put-user!)
  (demo/demo-put-user-identity!)
  (demo/demo-put-subject!)
  (demo/demo-install-create-action!)
  (demo/demo-install-do-action-fn!)
  (demo/demo-permit-create-action!)
  (demo/demo-create-grant-permission-action!)
  (demo/demo-permit-grant-permission-action!)

  (demo/demo-create-action-put-immutable-public-resource!)
  (demo/demo-grant-permission-to-invoke-action-put-immutable-public-resource!)
  (demo/demo-create-action-get-public-resource!)
  (demo/demo-grant-permission-to-invoke-get-public-resource!)
  (demo/demo-create-hello-world-resource!)

  ;;(demo/demo-create-action-put-graphql-resource!)
  ;;(demo/demo-grant-permission-to-invoke-action-put-graphql-resource!)

  (f))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-setup with-handler)

(defn post-handler [req]

  (let [subject-doc (::pass/subject req)
        subject (:xt/id subject-doc)
        db (::site/db req)
        schema (schema/compile-schema (parser/parse (slurp (io/resource "juxt/site/simple.graphql"))))

        body (some-> req ::site/received-representation ::http/body (String.))

        _ (log/infof "body is %s" body)

        {query "query"
         operation-name "operationName"
         variables "variables"} (some-> body json/read-value)

        _ (log/infof "query is %s" query)

        parsed-query (parser/parse query)
        compiled-query (document/compile-document parsed-query schema)
        result (execute-request
                {:schema schema :document compiled-query
                 :field-resolver
                 (fn [args]
                   (def args args)
                   (condp =

                       [(get-in args [:object-type ::g/name])
                        (get-in args [:field-name])]

                       ["Query" "accounts"]
                       (authz/pull-allowed-resources
                        db
                        ;; TODO: A better error would be to detect the missing action
                        #{"https://site.test/actions/list-accounts"}
                        {::pass/subject subject
                         })

                       ["Account" "transactions"]
                       (authz/pull-allowed-resources
                        db
                        #{"https://site.test/actions/list-transactions"}
                        {::pass/subject subject})

                       ["Transaction" "amount"]
                       (let [tx (get-in args [:object-value])]
                         (:amount tx))
                       ))})]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string result))
        (update :ring.response/headers assoc "content-type" "application/json"))))

((t/join-fixtures [with-system-xt with-setup with-handler])
 (fn []

   (repl/put! {:xt/id "https://site.test/account/alice"
               :owner :alice
               :type "Account"})

   (repl/put! {:xt/id "https://site.test/account/alice/transactions/1"
               :account "https://site.test/account/alice"
               :type "Transaction"
               :amount "$100"})

   (repl/put! {:xt/id "https://site.test/account/alice/transactions/2"
               :account "https://site.test/account/alice"
               :type "Transaction"
               :amount "$150"})

   (repl/put! {:xt/id "https://site.test/account/bob"
               :owner :bob
               :type "Account"})

   (repl/put! {:xt/id "https://site.test/account/bob/transactions/1"
               :account "https://site.test/account/bob"
               :type "Transaction"
               :amount "$99"})

   ;;(repl/e "https://site.test/account/3")
   ;;(xt/entity (xt/db *xt-node*) "https://site.test/account/1")

   (repl/put! {:xt/id "https://site.test/actions/list-accounts"
               ::site/type "https://meta.juxt.site/pass/action"
               ::pass/pull '[*]
               ::pass/rules
               '[
                 ;; Allow traders to see their own trades
                 [(allowed? permission subject action resource)
                  [resource :type "Account"]
                  [resource :owner user]
                  [permission ::pass/user user]
                  [subject ::pass/identity id]
                  [id ::pass/user user]]]})

   (repl/put! {:xt/id "https://site.test/permissions/alice/list-accounts"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-accounts"
               ::pass/purpose nil
               ::pass/user :alice})

   (repl/put! {:xt/id "https://site.test/permissions/bob/list-accounts"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-accounts"
               ::pass/purpose nil
               ::pass/user :bob})

   (repl/put! {:xt/id "https://site.test/actions/list-transactions"
               ::site/type "https://meta.juxt.site/pass/action"
               ::pass/pull '[*]
               ::pass/rules
               '[
                 ;; Allow traders to see their own trades
                 [(allowed? permission subject action resource)
                  [resource :type "Transaction"]
                  [resource :account account]
                  [account :owner user]
                  [permission ::pass/user user]
                  [subject ::pass/identity id]
                  [id ::pass/user user]]]})

   (repl/put! {:xt/id "https://site.test/permissions/alice/list-transactions"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-transactions"
               ::pass/purpose nil
               ::pass/user :alice})

   (repl/put! {:xt/id "https://site.test/permissions/bob/list-transactions"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-transactions"
               ::pass/purpose nil
               ::pass/user :bob})

   (repl/put! {:xt/id :alice})
   (repl/put! {:xt/id :bob})

   (repl/put!
    {:xt/id "https://site.test/graphql"
     ::http/methods
     {:post
      {:juxt.pass.alpha/actions #{"https://site.test/actions/get-public-resource"}}}
     :juxt.site.alpha/post-fn 'juxt.site.graphql-test/post-handler})

   #_(repl/e "https://site.test/graphql")

   ;; Alice the identity
   (repl/put!
    {:xt/id "https://site.test/identities/alice"
     :juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
     :juxt.pass.alpha/user :alice})

   (repl/put!
    {:xt/id "https://site.test/identities/bob"
     :juxt.site.alpha/type "https://meta.juxt.site/pass/identity"
     :juxt.pass.alpha/user :bob})

   ;; Alice the subject
   (repl/put!
    {:xt/id "https://site.test/subjects/alice"
     :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
     :juxt.pass.alpha/identity "https://site.test/identities/alice"})

   (repl/put!
    {:xt/id "https://site.test/subjects/bob"
     :juxt.site.alpha/type "https://meta.juxt.site/pass/subject"
     :juxt.pass.alpha/identity "https://site.test/identities/bob"})

   #_(authz/pull-allowed-resources
      (xt/db *xt-node*)
      ;; TODO: A better error would be to detect the missing action
      #{"https://site.test/actions/list-accounts"}
      {::pass/subject "https://site.test/subjects/alice"
       })

   (repl/put!
    (assoc
     (repl/make-access-token-doc
      :token "i-am-alice"
      :prefix "https://site.test/access-tokens/"
      :subject "https://site.test/subjects/alice"
      :application "https://site.test/applications/local-terminal"
      :scope "read:admin"
      :expires-in-seconds (* 5 60))
     ::site/type "https://meta.juxt.site/pass/access-token"
     ))

   (repl/put!
    (assoc
     (repl/make-access-token-doc
      :token "i-am-bob"
      :prefix "https://site.test/access-tokens/"
      :subject "https://site.test/subjects/bob"
      :application "https://site.test/applications/local-terminal"
      :scope "read:admin"
      :expires-in-seconds (* 5 60))
     ::site/type "https://meta.juxt.site/pass/access-token"
     ))

   ;; Access token
   #_{:xt/id "https://site.test/access-tokens/i-am-alice",
      :juxt.pass.alpha/subject "https://site.test/subjects/alice",
      :juxt.pass.alpha/application "https://site.test/applications/local-terminal",
      :juxt.pass.alpha/scope "read:admin",
      :juxt.pass.alpha/token "i-am-alice",
      :juxt.pass.alpha/expiry #inst "2022-05-05T16:40:51.856-00:00"}

   (xt/entity (xt/db *xt-node*) "https://site.test/access-tokens/i-am-alice")
   (xt/entity (xt/db *xt-node*) "https://site.test/access-tokens/i-am-bob")

   #_(xt/q (xt/db *xt-node*)
           '{:find [(pull sub [*])]
             :keys [subject]
             :where [[at ::pass/token tok]
                     [at ::site/type "https://meta.juxt.site/pass/access-token"]
                     [at ::pass/subject sub]
                     [sub ::site/type "https://meta.juxt.site/pass/subject"]]
             :in [tok]} "i-am-alice")

   (let [body (json/write-value-as-bytes {"query" "query { accounts { transactions { amount } } }"})
           response
           (*handler*
            {:ring.request/method :post
             :ring.request/path "/graphql"
             :ring.request/body (java.io.ByteArrayInputStream. body)
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/json"
              "authorization" "Bearer i-am-alice"}})]
       (json/read-value (:ring.response/body response))
       )))


;; Action: list-accounts
;; Action: list-transactions
;; Join between accounts and transactions
