;; Copyright © 2021, JUXT LTD.

(ns juxt.site.init
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
   [malli.core :as malli]
   [juxt.reap.alpha.combinators :as p]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.site.locator :refer [to-regex]]
   [juxt.site.actions :as actions]
   [juxt.site.main :as main]
   [xtdb.api :as xt]))

(defn system [] main/*system*)

(defn xt-node []
  (:juxt.site.db/xt-node (system)))

(defn put! [& ms]
  (->>
   (xt/submit-tx
    (xt-node)
    (for [m ms]
      (let [vt (:xtdb.api/valid-time m)]
        [:xtdb.api/put (dissoc m :xtdb.api/valid-time) vt])))
   (xt/await-tx (xt-node))))

(defn config []
  (main/config))

(defn base-uri []
  (:juxt.site/base-uri (config)))

#_(defn make-repl-request-context [subject action edn-arg]
  (let [xt-node (xt-node)]
    (cond->
        {:juxt.site/xt-node xt-node
         :juxt.site/db (xt/db xt-node)
         :juxt.site/subject subject
         :juxt.site/action action
         ;;:juxt.site/base-uri (base-uri)
         }
      edn-arg (merge {:juxt.site/received-representation
                      {:juxt.http/content-type "application/edn"
                       :juxt.http/body (.getBytes (pr-str edn-arg))}}))))

;; TODO: Rename this to do-action! ?
#_(defn do-action!
  ([subject-id action-id]
   (do-action! subject-id action-id nil))
  ([subject-id action-id edn-arg]

   (throw (ex-info "DEPRECATED - inline instead" {}))

   (assert (or (nil? subject-id) (string? subject-id)) "Subject must a string or nil")

   (cond->
       {:juxt.site/subject-id subject-id
        :juxt.site/action-id action-id}
       edn-arg (merge {:juxt.site/received-representation
                       {:juxt.http/content-type "application/edn"
                        :juxt.http/body (.getBytes (pr-str edn-arg))}}))


   ;;
   #_(let [subject-id subject-id
           action-id action-id
           xt-node (xt-node)
           db (xt/db xt-node)
           subject (when subject-id (xt/entity db subject-id))]
       (:juxt.site/action-result
        (actions/do-action
         (make-repl-request-context
          subject action-id edn-arg))))))

(def host-parser (rfc7230.decoders/host {}))

(def base-uri-parser
  (p/complete
   (p/into {}
    (p/sequence-group
     (p/pattern-parser #"(?<scheme>https?)://" {:group {:juxt.reap.alpha.rfc7230/scheme "scheme"}})
     host-parser))))

(defn lookup [g id]
  (try
    (or
     (when-let [v (get g id)] (assoc v :id id))
     (some (fn [[k v]]
             ;;           (when-not (string? id) (throw (ex-info "DEBUG" {:id id})))
             (when-let [matches (re-matches (to-regex k) id)]
               (assoc v
                      :id id
                      :params
                      (zipmap
                       (map second (re-seq #"\{(\p{Alpha}+)\}" k))
                       (next matches)))))
           g))
    (catch Exception e
      (throw (ex-info (format "Failed to lookup %s" id) {:id id} e))
      )))

(def dependency-graph-malli-schema
  [:map-of [:or :string :keyword]
   [:map
    [:create {:optional true} :any]
    [:deps {:optional true}
     ;; :deps can be a set, but also, where necessary a function.
     [:or
      [:set [:or :string :keyword]]
      [:=> [:cat
            [:map-of :string :string]
            [:map {:juxt.site/base-uri :string}]]
       [:set [:or :string :keyword]]]]]]])

(defn ids->nodes [ids graph]
  (->> ids
       (mapcat (fn [id]
                 (->> id
                      (tree-seq some?
                                (fn [id]
                                  (let [{:keys [deps params]} (lookup graph id)]
                                    (cond
                                      (nil? deps) nil
                                      (fn? deps) (deps {:params params})
                                      (set? deps) deps
                                      :else (throw (ex-info "Unexpected deps type" {:deps deps}))))))
                      (keep (fn [id]
                              (if-let [v (lookup graph id)]
                                (when-not (keyword? id) [id v])
                                (throw (ex-info (format "No dependency graph entry for %s" id) {:id id}))))))))
       reverse distinct
       (reduce
        (fn [acc [id {:keys [create] :as v}]]
          (conj acc (cond-> {:id id}
                      create (assoc ::init-data (create v))
                      (not create) (assoc :error "No creator function in dependency graph entry"))))
        [])))

(defn do-action! [xt-node init-data]
  (let [db (xt/db xt-node)
        subject (when (:juxt.site/subject-id init-data)
                  (xt/entity db (:juxt.site/subject-id init-data)))

        _ (assert subject)]

    (try
      (:juxt.site/action-result
       (actions/do-action
        (cond->
            {:juxt.site/xt-node xt-node
             :juxt.site/db db
             :juxt.site/subject subject
             :juxt.site/action (:juxt.site/action-id init-data)}
          (:juxt.site/input init-data)
          (merge {:juxt.site/received-representation
                  {:juxt.http/content-type "application/edn"
                   :juxt.http/body (.getBytes (pr-str (:juxt.site/input init-data)))}}))))
      (catch Exception cause
        (throw (ex-info "Failed to perform action" {:init-data init-data} cause))))))

(defn substitute-base-uri [form base-uri]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://example.org" base-uri)))
   form))

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [ids graph {:keys [dry-run? recreate? base-uri]}]
  {:pre [(malli/validate [:or [:set [:or :string :keyword]] [:sequential [:or :string :keyword]]] ids)
         ;;(malli/validate dependency-graph-malli-schema graph)
         ]
   #_#_:post [(malli/validate
               [:sequential
                [:map
                 [:id :string]
                 [:status :keyword]
                 [:error {:optional true} :any]]] %)]}

  (when-not (malli/validate dependency-graph-malli-schema graph)
    (throw
     (ex-info
      "Graph failed to validate"
      {:graph graph
       :explain (malli/explain dependency-graph-malli-schema graph)})))

  (let [xt-node (xt-node)]
    (cond->> (ids->nodes ids graph)
      (not dry-run?)
      (mapv (fn [{id :id init-data :juxt.site.init/init-data}]
              (let [id (cond-> id base-uri (substitute-base-uri base-uri))]
                (cond
                  ;; Direct put
                  (:put! init-data)
                  (put! (:put! (cond-> init-data base-uri (substitute-base-uri base-uri))))

                  #_(if (or (not (xt/entity db id)) recreate?)
                      acc)

                  :else
                  (try
                    (let [{:juxt.site/keys [puts] :as result}
                          (do-action!
                           xt-node
                           (cond-> init-data base-uri (substitute-base-uri base-uri)))]
                      (when (and puts (not (contains? (set puts) id)))
                        (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                      {:id id :status :created :result result})
                    (catch Throwable cause
                      (throw (ex-info (format "Failed to converge %s" id) {:id id} cause))
                      ;;{:id id :status :error :error cause}
                      )))))))))
