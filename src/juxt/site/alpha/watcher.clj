(ns juxt.site.alpha.watcher
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [io.aviso.ansi :as ansi]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.site.alpha.xtdb :refer [e put! rm!]]
   [xtdb.api :as xt])
  (:import
   [io.methvin.watcher
    DirectoryChangeEvent
    DirectoryChangeEvent$EventType
    DirectoryChangeListener
    DirectoryWatcher]
   [java.nio.file Paths]))

(defn- fn->listener ^DirectoryChangeListener [f]
  (reify
    DirectoryChangeListener
    (onEvent [_this e]
      (let [path (.path ^DirectoryChangeEvent e)]
        (condp = (. ^DirectoryChangeEvent e eventType)
          DirectoryChangeEvent$EventType/CREATE   (f {:type :create :path path})
          DirectoryChangeEvent$EventType/MODIFY   (f {:type :modify :path path})
          DirectoryChangeEvent$EventType/DELETE   (f {:type :delete :path path})
          DirectoryChangeEvent$EventType/OVERFLOW (f {:type :overflow :path path}))))))

(defn- to-path [& args]
  (Paths/get ^String (first args) (into-array String (rest args))))

(defn- create
  "Creates a watcher taking a callback function `cb` that will be invoked
  whenever a file in one of the `paths` chages.

  Not meant to be called directly but use `watch` or `watch-blocking` instead."
  [cb paths]
  (-> (DirectoryWatcher/builder)
      (.paths (map to-path paths))
      (.listener (fn->listener cb))
      (.build)))

(defn watch
  "Creates a directory watcher that will invoke the callback function `cb` whenever
  a file event in one of the `paths` occurs. Watching will happen asynchronously.

  Returns a directory watcher that can be passed to `stop` to stop the watch."
  [cb & paths]
  (doto (create cb paths)
    (.watchAsync)))

(defn stop
  "Stops the watch for a given `watcher`."
  [^DirectoryWatcher watcher]
  (.close watcher))

(def graphql-watcher-dir "apis/graphql")

(def default-graphql-resource
  {:juxt.http.alpha/content-type "text/plain;charset=utf-8"
   :juxt.http.alpha/methods #{:get :head :post :put :options}
   :juxt.http.alpha/acceptable-on-put "application/graphql"
   :juxt.http.alpha/acceptable-on-post "application/json"

   ;; For handling the upsert the schema
   :juxt.site.alpha/put-fn 'juxt.site.alpha.graphql/put-handler
   :juxt.http.alpha/put-error-representations
   [{:ring.response/status 400
     :juxt.http.alpha/content-type "application/json"
     :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/put-error-json-body}
    {:ring.response/status 400
     :juxt.http.alpha/content-type "text/plain"
     :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/put-error-text-body}
    {:ring.response/status 400
     :juxt.http.alpha/content-type "text/html;charset=utf-8"
     :juxt.http.alpha/content "<h1>Error compiling schema</h1>"}]

   ;; For POSTing GraphQL queries
   :juxt.site.alpha/post-fn 'juxt.site.alpha.graphql/post-handler
   :juxt.http.alpha/post-error-representations
   [{:ring.response/status 400
     :juxt.http.alpha/content-type "text/plain"
     :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/post-error-text-body}
    {:ring.response/status 400
     :juxt.http.alpha/content-type "application/json"
     :juxt.site.alpha/body-fn 'juxt.site.alpha.graphql/post-error-json-body}]})

(defn graphql-schema-id
  "infer xt id and therefore url of the api from the file path"
  [path]
  (str (if (str/starts-with? path "apis/") "/" "/apis/") (str/replace path #"\.graphql" "")))

(defn upsert-schema
  [{::site/keys [xt-node base-uri]} path file]
  (when (seq file)
    (let [schema-id (graphql-schema-id path)
          db (xt/db xt-node)
          resource
          (or
           (e db schema-id)
           (assoc default-graphql-resource :xt/id schema-id))

          schema-resource
          (try (graphql/schema-resource resource file)
               (catch Exception e
                 (log/warn
                  (ansi/red ".graphql file in apis directory has errors")
                  path)
                 (pprint/pprint (.data e))))]
      (log/info "Updating schema for file at: " path)
      (put! xt-node schema-resource)
      (log/info (ansi/green (str "Schema updated for file at: " path ". \n Deployed to " base-uri schema-id))))))

(defn path->type
  [path-str]
  (re-find #"[.][^.]+$" path-str))

(defn file-contents
  [path]
  (try
    (let [file (.toFile path)
          path-str (.toString path)
          file-type (path->type path-str)]
      (case file-type
        ".graphql"
        (slurp file)
        (log/warn "ignoring unsupported file" path-str)))
    (catch Exception e
      (println "Error reading file " path e))))

(defn absolute-path-to-relative
  "takes an absolute path and returns a path relative to the
  apis directory on the classpath so we can slurp the files in"
  [path]
  (let [pwd-count (->> "apis"
                       clojure.java.io/file
                       .getCanonicalPath
                       count)]
    (subs path (inc pwd-count))))

(defn delete-schema
  [{::site/keys [xt-node]} path]
  (let [schema-id (graphql-schema-id path)]
    (log/info "Deleting schema for" path)
    (rm! xt-node schema-id)))

(defn handle-graphql-file-change
  [config {:keys [type path]}]
  (log/info "Handling graphql file change" type path)
  (let [file (when (#{:create :modify} type) (file-contents path))
        relativePath (cond-> (.toString path)
                      (.isAbsolute path) (absolute-path-to-relative))]
    (case type
      :create
      (upsert-schema config relativePath file)
      :modify
      (upsert-schema config relativePath file)
      :delete
      (delete-schema config relativePath)
      :overflow
      (prn "overflow file" relativePath))))

(defn init-existing-apis
  [{::site/keys [xt-node] :as config}]
  (let [all-files (file-seq (clojure.java.io/file graphql-watcher-dir))]
    (doseq [file all-files
            :when (= ".graphql" (path->type (.toString file)))
            :let [relativePath (.toString file)
                  file-contents (slurp file)
                  existing (e (xt/db xt-node) (graphql-schema-id relativePath))]
            :when (not existing)]
      (do
        (log/info "Creating schema for" relativePath)
        (upsert-schema config relativePath file-contents)))))

(defmethod ig/init-key ::watcher [_ {::site/keys [xt-node base-uri should-watch?] :as config}]
  (log/info "Initializing watcher" xt-node base-uri should-watch?)
  (init-existing-apis config)
  (when should-watch?
    (watch (fn [opts] (#'handle-graphql-file-change config opts)) graphql-watcher-dir)))

(defmethod ig/halt-key! ::watcher [_ watcher]
  (when (some? watcher)
    (log/info "Stopping watcher")
    (stop watcher)))
