;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.main
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [juxt.site.alpha.init :as init]
   [xtdb.api :as xt]
   [integrant.core :as ig]))

(def system nil)

(def profile :prod)

(let [lock (Object.)]
  (defn- load-namespaces
    [system-config]
    (locking lock
      (ig/load-namespaces system-config))))

;; There will be integrant tags in our Aero configuration. We need to
;; let Aero know about them using this defmethod.
(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config
  "Read EDN config, with the given aero options. See Aero docs at
  https://github.com/juxt/aero for details."
  []
  (log/infof "Configuration profile: %s" (name profile))
  (let [custom-config (System/getProperty "site.config")
        config-file (or (when-not (empty? custom-config) (io/file custom-config))
                        (io/file (System/getProperty "user.home") ".config/site/config.edn"))]
    (when-not (.exists config-file)
      (log/error (str "Configuration file does not exist: " (.getAbsolutePath config-file)))
      (throw (ex-info
              (str "Please copy a configuration file to " (.getAbsolutePath config-file))
              {})))
    (log/debug "Loading configuration from" (.getAbsolutePath config-file))
    (aero/read-config config-file {:profile profile})))

(def config-map (config))

(defn system-config
  "Construct a new system, configured with the given profile"
  []
  (let [system-config (:ig/system config-map)]
    (load-namespaces system-config)
    (ig/prep system-config)))

(defn -main [& _]
  (log/info "Starting system")
  (let [system-config (system-config)
        sys (ig/init system-config)
        node (:juxt.site.alpha.db/xt-node sys)
        db (xt/db node)
        open-api-path "/_site/apis/site/openapi.json"]

    (when-not (xt/entity db open-api-path)
      (init/insert-base-resources! node config))

    (log/info "System started and ready...")

    (log/trace "TRACE on")

    (Thread/setDefaultUncaughtExceptionHandler
     (reify Thread$UncaughtExceptionHandler
       (uncaughtException [_ thread ex]
         (log/error ex "Uncaught exception on" (.getName thread)))))

    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread.
      (fn []
        (ig/halt! sys))))
    (alter-var-root #'system (constantly sys)))

  @(promise))
