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

  (demo/demo-create-action-put-graphql-resource!)
  (demo/demo-grant-permission-to-invoke-action-put-graphql-resource!)

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

  (let [db (::site/db req)
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
                   (condp =
                       [(get-in args [:object-type ::g/name])
                        (get-in args [:field-name])]
                       ["Query" "foo"]
                       (pr-str
                        (authz/pull-allowed-resources
                         db
                         ;; TODO: A better error would be to detect the missing action
                         #{"https://site.test/actions/list-accounts"}
                         {::pass/subject :malcolm  #_(::pass/subject req)
                          }))
                       ))})]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string result))
        (update :ring.response/headers assoc "content-type" "application/json"))))

((t/join-fixtures [with-system-xt with-setup with-handler])
 (fn []

   (repl/put! {:xt/id "https://site.test/account/jamie"
               :owner :jamie
               :type "Account"})
   (repl/put! {:xt/id "https://site.test/account/malcolm"
               :owner :malcolm
               :type "Account"})

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
                  [resource :owner subject]
                  [permission ::pass/subject subject]
                  ]

                 [(allowed? permission subject action resource)
                  [resource :type "Account"]
                  [subject :xt/id :malcolm]
                  [permission ::pass/subject subject]
                  ]]
               })

   (repl/put! {:xt/id "https://site.test/permissions/jamie/list-accounts"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-accounts"
               ::pass/purpose nil
               ::pass/subject :jamie
               })

   (repl/put! {:xt/id "https://site.test/permissions/malcolm/list-accounts"
               ::site/type "https://meta.juxt.site/pass/permission"
               ::pass/action "https://site.test/actions/list-accounts"
               ::pass/purpose nil
               ::pass/subject :malcolm
               })

   (repl/put! {:xt/id :jamie})
   (repl/put! {:xt/id :malcolm})

   (authz/pull-allowed-resources
    (xt/db *xt-node*)
    ;; TODO: A better error would be to detect the missing action
    #{"https://site.test/actions/list-accounts"}
    {::pass/subject :jamie
     })

   ;; TODO: Get the subject from the access-token

   #_(repl/do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/put-graphql-resource"
      {:xt/id "https://site.test/graphql"
       ::http/methods {:post {:juxt.pass.alpha/actions #{"https://site.test/actions/get-public-resource"}}}
       :juxt.site.alpha/post-fn 'juxt.site.graphql-test/post-handler})

   #_(repl/e "https://site.test/graphql")

   #_(let [body (json/write-value-as-bytes {"query" "query { foo }"})
           response
           (*handler*
            {:ring.request/method :post
             :ring.request/path "/graphql"
             :ring.request/body (java.io.ByteArrayInputStream. body)
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/json"}})]
       (json/read-value (:ring.response/body response))
       )))


;; Action: list-accounts
;; Action: list-transactions
;; Join between accounts and transactions
