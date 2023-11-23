;; Copyright © 2021, JUXT LTD.

(ns juxt.test.util
  (:require
   [juxt.site.alpha.handler :as h]
   [xtdb.api :as x])
  (:import
   (xtdb.api IXtdb)))

(def ^:dynamic *opts* {})
(def ^:dynamic ^IXtdb *xt-node*)
(def ^:dynamic *handler*)
(def ^:dynamic *db*)

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn with-xt [f]
  (with-open [node (x/start-node *opts*)]
    (binding [*xt-node* node]
      (f))))

(defn submit-and-await! [transactions]
  (->>
   (x/submit-tx *xt-node* transactions)
   (x/await-tx *xt-node*)))


(defn make-handler [opts]
  ((apply comp
          (remove
           #{h/wrap-ring-1-adapter}
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
  (binding [*db* (x/db *xt-node*)]
    (f)))

(defn with-open-db [f]
  (with-open [db (x/open-db *xt-node*)]
    (binding [*db* db]
      (f))))

(def access-all-areas
  {:xt/id "https://example.org/access-rule"
   ::site/description "A rule allowing access everything"
   ::site/type "Rule"
   ::pass/target '[]
   ::pass/effect ::pass/allow})

(def access-all-apis
  {:xt/id "https://example.org/access-rule"
   ::site/description "A rule allowing access to all APIs"
   ::site/type "Rule"
   ::pass/target '[[resource ::site/resource-provider ::apex/openapi-path]]
   ::pass/effect ::pass/allow})
