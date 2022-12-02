;; Copyright © 2021, JUXT LTD.

(ns juxt.test.util
  (:require
   [clojure.java.io :as io]
   [juxt.site.handler :as h]
   [juxt.site.main :as main]
   [juxt.site.init :as init]
   [juxt.site.bootstrap :as bootstrap]
   [xtdb.api :as xt])
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
              main/*system* {:juxt.site.db/xt-node node}]
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
                       (init/substitute-actual-base-uri
                        {:juxt.site/xt-node *xt-node*
                         :juxt.site/base-uri "https://example.org"}))]
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
   :juxt.site/description "A rule allowing access everything"
   :juxt.site/type "Rule"
   :juxt.site/target '[]
   :juxt.site/effect :juxt.site/allow})

(defmacro with-fixtures [& body]
  `((clojure.test/join-fixtures [with-system-xt with-handler])
    (fn [] ~@body)))

(defmacro with-resources [resources & body]
  `(do
     (let [resources# ~resources]
       (init/converge!
        (conj resources# ::init/system)
        (init/substitute-actual-base-uri
         (apply merge
                bootstrap/dependency-graph
                (:dependency-graphs (meta resources#))))
        {:dry-run? false :recreate? false}))
     ~@body))

(defn lookup-session-details [session-token]
  (let [db (xt/db *xt-node*)]
    (first
     (xt/q db '{:find [(pull session [*]) (pull scope [*])]
                :keys [session scope]
                :where [[e :juxt.site/type "https://meta.juxt.site/site/session-token"]
                        [e :juxt.site/session-token session-token]
                        [e :juxt.site/session session]
                        [session :juxt.site/session-scope scope]]
                :in [session-token]}
           session-token))))

(defn with-session-token [req session-token]
  (let [{:keys [scope]}
        (lookup-session-details session-token)

        {:juxt.site/keys [cookie-name]} scope]
    (assoc-in req [:ring.request/headers "cookie"] (format "%s=%s" cookie-name session-token))))

(defn with-request-body [req body-bytes]
  (-> req
      (->
       (update :ring.request/headers (fnil assoc {}) "content-length" (str (count body-bytes)))
       (assoc :ring.request/body (io/input-stream body-bytes)))))
