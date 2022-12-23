;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resource-package
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [postwalk]]
   [juxt.site.init :as init]
   [xtdb.api :as xt]))

(def READERS {'juxt.pprint (fn [x] (with-out-str (pprint x)))})

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

(defn- load-dependency-graph-from-filesystem [dir]
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
       ;; We treat every resource as https://example.org. This ensures the graph
       ;; of dependencies is closed. The init/converge! function is responsible
       ;; for the final 'rendering' where example.org might be replaced by the
       ;; Site admin's domain name.
       ;;
       [(format "https://%s/%s" (.getName host-root) urlpath)
        (->
         (edn/read-string {:readers READERS} (slurp f))
         (assoc :juxt.site.package/source f))]))))

(defn load-package-from-filesystem [dir]
  (let [root (io/file dir)
        _ (assert (.isDirectory root) (format "%s is not a directory" root))]
    (->
     (edn/read-string {:readers READERS} (slurp (io/file root "index.edn")))
     (assoc
      :dependency-graph (load-dependency-graph-from-filesystem (io/file root "builders"))))))

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
  ([pkg parameter-map]
   (let [db (xt/db (init/xt-node))]

     ;; Check all the package's dependencies exist in the database
     (doseq [dep (:dependencies pkg)]
       (when-not (xt/entity db dep)
         (throw
          (ex-info
           (format "Required dependency '%s' not installed" dep)
           {:package (:xt/id pkg)
            :dependency dep}))))

     ;; Install the package
     (init/put! (assoc pkg :juxt.site/type "https://meta.juxt.site/types/package"))

     ;; Install the package's resources
     (init/converge!
      (:resources pkg)
      (apply merge
             (:dependency-graph pkg)
             (map :dependency-graph (get-package-transitive-dependencies db pkg)))
      parameter-map)))
  ([pkg]
   (install-package! pkg {})))

(defn inspect-package-from-filesystem!
  [dir uri-map]
  (-> dir
      load-package-from-filesystem
      (apply-uri-map uri-map)))

(defn install-package-from-filesystem!
  [dir uri-map]
  (-> dir
      load-package-from-filesystem
      (apply-uri-map uri-map)
      (install-package! {})))

(comment
  (-> (io/file "packages/core")
      load-package-from-filesystem
      (apply-uri-map {"https://example.org" "https://site.test"})))

(defn dependency-graph
  ([pkg]
   (let [db (xt/db (init/xt-node))]
     (apply merge
            (:dependency-graph pkg)
            (map :dependency-graph (get-package-transitive-dependencies db pkg)))))
  ([]
   (let [db (xt/db (init/xt-node))]
     (apply merge
            (map :dependency-graph
                 (map first (xt/q db '{:find [(pull e [:dependency-graph])]
                                       :where [[e :juxt.site/type "https://meta.juxt.site/types/package"]]})))))))

;; TODO: If this is going to be used outside of tests, it will need to take and
;; process a parameter map
(defn install-resources!
  [resources]
  (init/converge! resources (dependency-graph) {}))
