;; Copyright © 2021, JUXT LTD.

(ns juxt.test.util
  (:require
   [clojure.java.io :as io]
   [juxt.site.handler :as h]
   [juxt.site.main :as main]
   [juxt.site.bootstrap :as bootstrap]
   [xtdb.api :as xt])
  (:import
   (xtdb.api IXtdb)))

(def ^:dynamic *opts* {})
(def ^:dynamic ^IXtdb *xt-node*)
(def ^:dynamic *handler*)
(def ^:dynamic *db*)
(def ^:dynamic *resource-dependency-graph* nil)

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
                       {:juxt.site/xt-node *xt-node*
                        :juxt.site/base-uri "https://example.org"})]
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

(defmacro with-resource-graph [dependency-graph & body]
  `(binding [*resource-dependency-graph* ~dependency-graph]
     ~@body))

(defmacro with-resources [resources & body]
  `(do
     (bootstrap/bootstrap-resources!
      ~resources
      (merge
       (cond-> {:dry-run? false
                :recreate? false}
         ;; Allows the caller to specify an additional custom dependency graph
         *resource-dependency-graph* (assoc :graph *resource-dependency-graph*))
       (meta ~resources)))
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

(defn assoc-session-token [req session-token]
  (let [{:keys [scope]}
        (lookup-session-details session-token)
        {:juxt.site/keys [cookie-name]} scope]
    (when-not cookie-name
      (throw (ex-info "No cookie name determined for session-token" {:session-token session-token})))
    (assoc-in req [:ring.request/headers "cookie"] (format "%s=%s" cookie-name session-token))))

(defmacro with-session-token [token & body]
  `(let [dlg# *handler*
         token# ~token]
     (when-not token#
       (throw (ex-info "with-session-token called without a valid session token" {})))
     (binding [*handler*
               (fn [req#]
                 (dlg# (assoc-session-token req# token#)))]
       ~@body)))

(defn assoc-bearer-token [req token]
  (update-in
   req
   [:ring.request/headers "authorization"]
   (fn [existing-value]
     (let [new-value (format "Bearer %s" token)]
       (when (and existing-value (not= existing-value new-value))
         (throw
          (ex-info
           "To avoid confusion when debugging, assoc-bearer-token will not override an already set authorization header"
           {:new-value {"authorization" new-value}
            :existing-value {"authorization" existing-value}})))
       new-value))))

(defmacro with-bearer-token [token & body]
  `(let [dlg# *handler*
         token# ~token]
     (when-not token#
       (throw (ex-info "with-bearer-token called without a bearer token" {})))
     (binding [*handler*
               (fn [req#]
                 (dlg# (assoc-bearer-token req# token#)))]
       ~@body)))

(defn assoc-request-payload
  "Add a body payload onto the request. If the content-type is of type 'text',
  e.g. text/plain, then give the body as a string."
  [req content-type body]
  (let [body-bytes
        (cond
          (re-matches #"text/.+" content-type)
          (.getBytes body)
          :else body)]
    (-> req
        (->
         (update :ring.request/headers (fnil assoc {})
                 "content-type" content-type
                 "content-length" (str (count body-bytes)))
         (assoc :ring.request/body (io/input-stream body-bytes))))))
