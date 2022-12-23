;; Copyright © 2021, JUXT LTD.

(ns juxt.site.init
  (:require
   [clojure.walk :refer [postwalk]]
   [clojure.tools.logging :as log]
   [selmer.parser :as selmer]
   [juxt.site.locator :refer [to-regex]]
   [juxt.site.actions :as actions]
   [juxt.site.main :as main]
   [xtdb.api :as xt]
   [ring.util.codec :as codec]))

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

(defn lookup [id graph]
  (or
   (when-let [v (get graph id)] (assoc v :id id))
   (some (fn [[k v]]
           (when-let [matches (re-matches (to-regex k) id)]
             (assoc v
                    :id id
                    :params
                    (zipmap
                     (map second (re-seq #"\{(\p{Alpha}+)\}" k))
                     (map codec/url-decode (next matches))))))
         graph)))

(defn render-form-templates [form params]
  (postwalk (fn [x]
              (cond-> x
                (string? x) (selmer/render params))) form))

(defn- node-dependencies
  "Return the dependency ids for the given node, with any parameters expanded
  out."
  [node parameter-map]
  (let [{:keys [deps params]} node]
    (when (seq deps)
      ;; Dependencies may be templates, with parameters
      ;; that correspond to the uri-template pattern of
      ;; the id.
      (map #(selmer/render % (merge parameter-map params)) deps))))

(defn ids->nodes [ids graph parameter-map]
  (assert (every? some? ids) (format "Some ids were nil: %s" (pr-str ids)))
  (->> ids
       (mapcat
        (fn [id]
          (let [root (lookup id graph)]
            (when-not root
              (throw
               (ex-info
                (format "Resource identified by '%s' could not be resolved" id)
                {:id id
                 :ids ids
                 :graph graph
                 :parameter-map parameter-map})))
            ;; From each id, find all descendants
            (tree-seq
             some?
             (fn [parent]
               (for [child-id (node-dependencies parent parameter-map)
                     :let [child (lookup child-id graph)
                           _ (when-not child
                               (throw
                                (ex-info
                                 (format "Unsatisfied dependency between '%s' and '%s'" (:juxt.site.package/source parent) child-id)
                                 {:dependant parent
                                  :dependency child-id
                                  :graph (keys graph)})))]]
                 child))
             root))))
       ;; to get depth-first order
       reverse
       ;; to dedupe
       distinct
       ;; (perf: note the reduce is done after the distinct to avoid duplicating
       ;; work)
       (reduce
        (fn [acc {:keys [id install params] :as node}]
          (let [init-data (render-form-templates install (assoc (merge parameter-map params) "$id" id))]
            (conj acc (-> node (assoc ::init-data init-data)))))
        [])))

(defn install! [xt-node init-data]
  (when-not init-data (throw (ex-info "No init data" {})))
  (if-let [subject-id (:juxt.site/subject-id init-data)]

    (let [db (xt/db xt-node)
          _ (assert (:juxt.site/subject-id init-data))
          _ (log/infof
             "Calling action %s by subject %s: input id %s"
             (:juxt.site/action-id init-data)
             subject-id
             (:xt/id init-data))

          subject (when (:juxt.site/subject-id init-data)
                    (xt/entity db (:juxt.site/subject-id init-data)))

          _ (when-not subject
              (throw
               (ex-info
                (format "No subject found in database for %s" subject-id)
                {:subject-id subject-id})))]

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
          (throw (ex-info "Failed to perform action" {:init-data init-data} cause)))))

    ;; Go direct!
    (do
      (assert (get-in init-data [:juxt.site/input :xt/id]))
      (log/infof
       "Installing id %s"
       (get-in init-data [:juxt.site/input :xt/id]))
      (put! (:juxt.site/input init-data)))))

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [ids graph parameter-map]

  (assert (map? parameter-map) "Parameter map arg must be a map")
  (assert (every? (fn [[k v]] (and (string? k) (string? v))) parameter-map) "All keys in parameter map must be strings")

  (let [nodes (ids->nodes ids graph parameter-map)
        xt-node (xt-node)]
    (->> nodes
         (mapv
          (fn [{id :id init-data :juxt.site.init/init-data error :error}]
            (assert id)
            (when error (throw (ex-info "Cannot proceed with error resource" {:id id :error error})))
            (try
              (let [{:juxt.site/keys [puts] :as result}
                    (install! xt-node init-data)]
                (when (and puts (not (contains? (set puts) id)))
                  (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                {:id id :status :installed :result result})
              (catch Throwable cause
                (throw (ex-info (format "Failed to converge id: '%s'" id) {:id id} cause))
                ;;{:id id :status :error :error cause}
                )))))))
