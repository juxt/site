;; Copyright © 2022, JUXT LTD.

(ns juxt.site.package
  (:require
   [clojure.edn :as edn]
   [sci.core :as sci]
   [juxt.site.locator :refer [to-regex]]
   [juxt.site.actions :as actions]
   [selmer.parser :as selmer]
   [ring.util.codec :as codec]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [postwalk]]
   [jsonista.core :as json]
   [xtdb.api :as xt]))

(defn put! [xt-node & ms]
  (->>
   (xt/submit-tx
    xt-node
    (for [m ms]
      (let [vt (:xtdb.api/valid-time m)]
        [:xtdb.api/put (dissoc m :xtdb.api/valid-time) vt])))
   (xt/await-tx xt-node)))



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
               (assert parent)
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
            (when (nil? init-data)
              (throw (ex-info "Nil init data" {:id id})))
            (conj acc (-> node (assoc :juxt.site.package/init-data init-data)))))
        [])))

(defn call-action-with-init-data! [xt-node init-data]
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
         (actions/do-action!
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
      (put! xt-node (:juxt.site/input init-data)))))

(defn converge!
  "Given a set of resource ids and a dependency graph, create resources and their
  dependencies. A resource id that is a keyword is a proxy for a set of
  resources that are included together but where there is no common dependant."
  [xt-node ids graph parameter-map]

  (assert (map? parameter-map) "Parameter map arg must be a map")
  (assert (every? (fn [[k v]] (and (string? k) (string? v))) parameter-map) "All keys in parameter map must be strings")

  (let [nodes (ids->nodes ids graph parameter-map)]
    (->> nodes
         (mapv
          (fn [{id :id
                init-data :juxt.site.package/init-data
                error :error :as node}]
            (assert id)
            (when error (throw (ex-info "Cannot proceed with error resource" {:id id :error error})))
            (when-not init-data
              (throw
               (ex-info
                "Node does not contain init-data"
                {:id id :node node})))

            (try
              (let [{:juxt.site/keys [puts] :as result}
                    (call-action-with-init-data! xt-node init-data)]
                (when (and puts (not (contains? (set puts) id)))
                  (throw (ex-info "Puts does not contain id" {:id id :puts puts})))
                {:id id :status :installed :result result})
              (catch Throwable cause
                (throw (ex-info (format "Failed to converge id: '%s'" id) {:id id} cause))
                ;;{:id id :status :error :error cause}
                )))))))


(def READERS {'juxt.pprint (fn [x] (with-out-str (pprint x)))
              'juxt.json (fn [x] (json/write-value-as-string x))})

(defn- normalize-uri-map [uri-map]
  (->> uri-map
       (mapcat (fn [[k v]]
                 (if (coll? k)
                   (zipmap k (repeat v))
                   [[k v]])))
       (into {})))

(defn apply-uri-map
  "A uri-map allows a package to be relocated to a given host or hosts. A package
  declares the uris that must be mapped to the target host. The keys to the
  uri-map argument may be individual strings or sets of strings."
  [pkg uri-map]
  (let [uri-map (normalize-uri-map uri-map)]
    (when-let [missing (seq (set/difference (set (map first (:uri-map pkg))) (set (keys uri-map))))]
      (throw (ex-info "uri-map is missing some required keys" {:missing missing})))
    (postwalk
     (fn [x]
       (cond-> x
         (string? x)
         (str/replace
          #"(https://.*?example.org)(.*)"
          (fn [[_ host path]] (str (get uri-map host host) path)))))
     (dissoc pkg :uri-map))))

;; Make this a test
(comment
  (apply-uri-map {"a" "https://data.example.org/customers/"
                  "b" "https://example.org/index.html"
                  "c" "https://other.example.org/index.html"}
                 {"https://data.example.org" "https://apis.site.test"
                  "https://example.org" "https://site.test"}))

(defn- load-dependency-graph-from-filesystem [dir metadata]
  (let [root (io/file dir)]
    (into
     {}
     (for [host-root (.listFiles root)
           f (file-seq host-root)
           :let [path (.toPath f)
                 relpath (.toString (.relativize (.toPath host-root) path))
                 [_ urlpath] (re-matches #"(.+)\.edn" relpath)]
           :when (and (.isFile f) urlpath)
           :let [urlpath (if-let [[_ stem] (re-matches #"(.*/)\{index\}" urlpath)]
                           stem
                           urlpath)]]
       [(format "https://%s/%s" (.getName host-root) urlpath)
        (try
          (->
           (edn/read-string {:readers READERS} (slurp f))
           (update-in [:install :juxt.site/input] merge metadata {:juxt.site.package/source (str f)})
           (assoc :juxt.site.package/source (str f)))
          (catch Exception e
            (throw (ex-info (format "Failed to load %s" f) {:file f} e))
            ))]))))

(defn load-package-from-filesystem [dir]
  (let [root (io/file dir)
        _ (assert (.isDirectory root) (format "%s is not a directory" root))
        index (edn/read-string {:readers READERS} (slurp (io/file root "index.edn")))]
    (assoc
     index
     :dependency-graph (load-dependency-graph-from-filesystem
                        (io/file root "installers")
                        {:juxt.site/package (:xt/id index)}))))

(defn get-package-transitive-dependencies [db pkg]
  (let [dependencies (:dependencies pkg)]
    (mapcat (fn [depid]
              (when-let [dep-pkg (xt/entity db depid)]
                (cons dep-pkg
                      (get-package-transitive-dependencies db dep-pkg))))
            dependencies)))

(defn install-package!
  "Install a package. Not that installation of packages is NOT atomic. Any errors
  that are reported during the installation of a package must be investigated
  and repaired."
  ([pkg xt-node parameter-map]
   (let [db (xt/db xt-node)]

     ;; Check all the package's dependencies exist in the database
     (doseq [dep (:dependencies pkg)]
       (when-not (xt/entity db dep)
         (throw
          (ex-info
           (format "Required dependency '%s' not installed" dep)
           {:package (:xt/id pkg)
            :dependency dep}))))

     ;; Install the package
     (put! xt-node (assoc pkg :juxt.site/type "https://meta.juxt.site/types/package"))

     ;; Install the package's resources
     (converge!
      xt-node
      (:resources pkg)
      (apply merge
             (:dependency-graph pkg)
             (map :dependency-graph (get-package-transitive-dependencies db pkg)))
      parameter-map)))
  ([pkg xt-node]
   (install-package! pkg xt-node {})))

(defn inspect-package-from-filesystem!
  [dir uri-map]
  (-> dir
      load-package-from-filesystem
      (apply-uri-map uri-map)))

(defn install-package-from-filesystem!
  [dir xt-node uri-map]
  (-> dir
      load-package-from-filesystem
      (apply-uri-map uri-map)
      (install-package! xt-node {})))

;; Commands

(defn dependency-graph
  ([pkg db]
   (apply merge
          (:dependency-graph pkg)
          (map :dependency-graph (get-package-transitive-dependencies db pkg))))
  ([db]
   (apply merge
          (map :dependency-graph
               (map first (xt/q db '{:find [(pull e [:dependency-graph])]
                                     :where [[e :juxt.site/type "https://meta.juxt.site/types/package"]]}))))))

(defn create-command-fn [program args]
  (assert program)
  (assert (map? args))
  (fn [xt-node]
    (try
      (sci/binding [sci/out *out*
                    sci/in *in*]
        (sci/eval-string
         program
         {:namespaces
          {'user
           {'*args* args

            'converge!
            (fn
              [resources parameters]
              (converge!
               xt-node
               resources
               (dependency-graph (xt/db xt-node)) ;; (pkg/dependency-graph pkg)
               parameters))

            'install-resource-with-action!
            (fn [init-data]
              (call-action-with-init-data! xt-node init-data))

            'q (fn [query & args] (apply xt/q (xt/db xt-node) query args))
            ;; TODO: Need to think about uri-map arg here
            ;;'install! (fn [dir] (install! dir {}))
            }
           'ring.util.codec
           {'form-encode codec/form-encode
            'form-decode codec/form-decode
            'url-encode codec/url-encode
            'url-decode codec/url-decode}}}))
      (catch clojure.lang.ExceptionInfo e
        (println (.getMessage e))
        (pprint (ex-data e))
        (throw (ex-info (.getMessage e) (or (ex-data (.getCause e)) {}) (.getCause e)))))))
