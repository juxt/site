;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resource-package
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.init :as init]
   [clojure.pprint :refer [pprint]]
   [xtdb.api :as xt]))

(def READERS {'juxt.pprint (fn [x] (with-out-str (pprint x)))})

(defn- load-dependency-graph-from-filesystem [dir]
  (let [root (io/file dir)]
    (into
     {}
     (for [f (file-seq root)
           :let [path (.toPath f)
                 relpath (.toString (.relativize (.toPath root) path))
                 [_ urlpath] (re-matches #"(.+)\.edn" relpath)]
           :when (and (.isFile f) urlpath)
           :let [urlpath (if-let [[_ stem] (re-matches #"(.*/)\{index\}" urlpath)]
                           stem
                           urlpath)]]
       ;; We treat every resource as https://example.org. This ensures the graph
       ;; of dependencies is closed. The init/converge! function is responsible
       ;; for the final 'rendering' where example.org might be replaced by the
       ;; Site admin's domain name.
       [(str  "https://example.org/" urlpath)
        (edn/read-string {:readers READERS} (slurp f))]))))

(defn load-package-from-filesystem [dir]
  (let [root (io/file dir)]
    (assert (.isDirectory root))
    (->
     (edn/read-string {:readers READERS} (slurp (io/file root "index.edn")))
     (assoc
      :dependency-graph (load-dependency-graph-from-filesystem (io/file root "resources"))))))

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
  ([pkg opts]
   (let [db (xt/db (init/xt-node))]

     ;; Check all the package's dependencies exist in the database
     (doseq [dep (:dependencies pkg)]
       (when-not (xt/entity db dep)
         (throw
          (ex-info
           "Required dependency not installed"
           {:package (:xt/id pkg)
            :dependency dep}))))

     ;; Check all parameters required by the package are satisfied
     ;; (commented out because we don't have package-level parameters, they are only at the resource level)
     #_(when-let [missing-parameters
                (seq
                 (set/difference
                  (set (keys (:parameters pkg)))
                  (set (keys (:parameters opts)))))]
       (throw
        (ex-info
         "Required parameters not provided"
         {:missing-parameters missing-parameters})))

     ;; Install the package
     (init/put! (assoc pkg :juxt.site/type "https://meta.juxt.site/types/package"))

     ;; Install the package's resources
     (init/converge!
      (:resources pkg)
      (apply merge
             (:dependency-graph pkg)
             (map :dependency-graph (get-package-transitive-dependencies db pkg)))
      (merge {:dry-run? false} opts))))
  ([pkg]
   (install-package! pkg {})))

(defn install-package-from-filesystem!
  ([dir opts]
   (install-package!
    (load-package-from-filesystem dir)
    opts))
  ([dir]
   (install-package-from-filesystem! dir {})))

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

(defn install-resources!
  ([resources opts]
   (init/converge!
    resources
    (dependency-graph)
    (merge {:dry-run? false} opts)))
  ([resources]
   (install-resources! resources {})))
