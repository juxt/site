;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.main
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [clojure.tools.logging :as log]))

(def system nil)

(let [lock (Object.)]
  (defn- load-namespaces
    [system-config]
    (locking lock
      (ig/load-namespaces system-config))))

(defn config
  "Read EDN config, with the given aero options. See Aero docs at
  https://github.com/juxt/aero for details."
  []
  (let [config-file (io/file (System/getProperty "user.home") ".config/site/config.edn")]
    (when-not (.exists config-file)
      (log/error (str "Configuration file does not exist: " (.getAbsolutePath config-file)))
      (throw (ex-info
              (str "Please copy a configuration file to " (.getAbsolutePath config-file))
              {})))
    (log/debug "Loading configuration from" (.getAbsolutePath config-file))
    (ig/read-string (slurp config-file))))

(defn system-config
  "Construct a new system, configured with the given profile"
  []
  (let [config (config)
        system-config (:ig/system config)]
    (load-namespaces system-config)
    (ig/prep system-config)))

(defn -main [& _]
  (log/info "Starting production system")
  (let [system-config (system-config)
        sys (ig/init system-config)]
    (log/infof "Configuration: %s" (pr-str system-config))

    (log/info "System started and ready...")

    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread.
      (fn []
        (ig/halt! sys))))
    (alter-var-root #'system (constantly sys)))
  @(promise))
