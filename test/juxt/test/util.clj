;; Copyright © 2021, JUXT LTD.

(ns juxt.test.util
  (:require
   [juxt.site.alpha.handler :as h]
   [xtdb.api :as xt]
   [juxt.site.alpha.main :as main]
   [juxt.pass.alpha.actions :as authz]
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.mail.alpha :as-alias mail]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site])
  (:import
   (xtdb.api IXtdb)))

(def ^:dynamic *opts* {})
(def ^:dynamic ^IXtdb *xt-node*)
(def ^:dynamic *handler*)
(def ^:dynamic *db*)

(defn with-xt [f]
  (with-open [node (xt/start-node *opts*)]
    (binding [*xt-node* node]
      (f))))

(defn with-system-xt [f]
  (with-open [node (xt/start-node *opts*)]
    (binding [*xt-node* node
              main/*system* {:juxt.site.alpha.db/xt-node node}]
      (f))))

(defn submit-and-await! [transactions]
  (->>
   (xt/submit-tx *xt-node* transactions)
   (xt/await-tx *xt-node*)))


(defn make-handler [opts]
  ((apply comp
          (remove
           #{h/wrap-healthcheck
             h/wrap-ring-1-adapter}
           (h/make-pipeline opts)))
   identity))

(defn with-handler [f]
  (binding [*handler* (make-handler
                       {::site/xt-node *xt-node*
                        ::site/base-uri "https://example.org"
                        ::site/uri-prefix "https://example.org"})]
    (f)))

(defn with-timing [f]
  (let [t0 (System/nanoTime)
        result (f)
        t1 (System/nanoTime)]
    {:result result
     :duration-µs (/ (- t1 t0) 1000.0)}))

(defn with-db [f]
  (binding [*db* (xt/db *xt-node*)]
    (f)))

(defn with-open-db [f]
  (with-open [db (xt/open-db *xt-node*)]
    (binding [*db* db]
      (f))))

;; Deprecated
(def access-all-areas
  {:xt/id "https://example.org/access-rule"
   ::site/description "A rule allowing access everything"
   ::site/type "Rule"
   ::pass/target '[]
   ::pass/effect ::pass/allow})

;; Deprecated
(def access-all-apis
  {:xt/id "https://example.org/access-rule"
   ::site/description "A rule allowing access to all APIs"
   ::site/type "Rule"
   ::pass/target '[[resource ::site/resource-provider ::apex/openapi-path]]
   ::pass/effect ::pass/allow})

;; TODO: Rename this, it isn't very descriptive!
(defn install-test-resources! []
  (submit-and-await!
   [
    ;; A tester
    [::xt/put {:xt/id :tester}]

    ;; A test action
    [::xt/put
     {:xt/id :test
      ::site/type "Action"
      ::pass/rules
      '[
        [(allowed? subject resource permission)
         [permission ::pass/subject subject]
         [(nil? resource)]]
        [(allowed? subject resource permission)
         [permission ::pass/subject subject]
         [resource :xt/id]]]}]

    ;; A permission between them
    [::xt/put
     {:xt/id :permission
      ::site/type "Permission"
      ::pass/subject :tester
      ::pass/action :test
      ::pass/purpose nil}]

    [::xt/put (authz/install-do-action-fn)]]))
