;; Copyright © 2021, JUXT LTD.

(ns juxt.site.init
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
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

(defn config []
  (main/config))

(defn base-uri []
  (:juxt.site/base-uri (config)))

(defn lookup [g id]
  (try
    (or
     (when-let [v (get g id)] (assoc v :id id))
     (some (fn [[k v]]
             (when-not (string? id) (throw (ex-info "DEBUG" {:id id})))
             (when-let [matches (re-matches (to-regex k) id)]
               (assoc v
                      :id id
                      :params
                      (zipmap
                       (map second (re-seq #"\{(\p{Alpha}+)\}" k))
                       (map codec/url-decode (next matches))))))
           g))
    (catch Exception e
      (throw (ex-info (format "Failed to lookup %s" id) {:id id} e)))))

(defn- substitute-base-uri [form base-uri]
  (postwalk
   (fn [s]
     (cond-> s
       (string? s) (str/replace "https://example.org" base-uri)))
   form))

(defn render-form-templates [form params]
  (postwalk (fn [x]
              (cond-> x
                (string? x) (selmer/render params))) form))

(defn ids->nodes [ids graph {:keys [base-uri parameters]}]
  (->> ids
       (mapcat
        (fn [id]
          (->> id
               ;; From each id, find all descendants
               (tree-seq some?
                         (fn [id]
                           (let [{:keys [deps params]} (lookup graph id)]
                             (cond
                               (nil? deps) nil
                               (fn? deps) (deps {:params (merge parameters params)})
                               (set? deps) (->> deps
                                                ;; Dependencies may be templates, with parameters
                                                ;; that correspond to the uri-template pattern of
                                                ;; the id.
                                                (map #(selmer/render % (merge parameters params))))
                               :else (throw (ex-info "Unexpected deps type" {:deps deps}))))))
               (keep (fn [id]
                       (if-let [v (lookup graph id)]
                         [id v]
                         (throw (ex-info (format "No dependency graph entry for %s" id) {:id id}))
                         ))))))
       reverse distinct
       (reduce
        (fn [acc [id {:keys [create params] :as v}]]
          (conj acc (cond->
                        (cond-> {:id id}
                          (fn? (cond-> create (var? create) deref)) (assoc ::init-data (create v))
                          (map? create) (assoc
                                         ::init-data
                                         (render-form-templates (:create v) (assoc (merge parameters params) "$id" id)))
                          (not create) (assoc :error "No creator function in dependency graph entry"))
                        base-uri (substitute-base-uri base-uri))))
        [])))

(defn enact-create! [xt-node init-data]
  (when-not init-data (throw (ex-info "No init data" {})))
  (if-let [subject-id (:juxt.site/subject-id init-data)]

    (let [db (xt/db xt-node)
          _ (assert (:juxt.site/subject-id init-data))
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
      (put! (:juxt.site/input init-data)))))

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [ids graph {:keys [dry-run? recreate? base-uri] :as opts}]
  (let [xt-node (xt-node)]
    (cond->> (ids->nodes ids graph opts)
      (not dry-run?)
      (mapv (fn [{id :id init-data :juxt.site.init/init-data error :error}]
              (when error (throw (ex-info "Cannot proceed with error resource" {:id id :error error})))
              (let [id (cond-> id base-uri (substitute-base-uri base-uri))]
                (try
                  (let [{:juxt.site/keys [puts] :as result}
                        (enact-create! xt-node init-data)]
                    (when (and puts (not (contains? (set puts) id)))
                      (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                    {:id id :status :created :result result})
                  (catch Throwable cause
                    (throw (ex-info (format "Failed to converge %s" id) {:id id} cause))
                    ;;{:id id :status :error :error cause}
                    ))))))))
