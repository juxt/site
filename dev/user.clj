;; Copyright © 2021, JUXT LTD.

(ns user
  (:require
   clojure.main
   [io.aviso.ansi :as ansi]
   [juxt.site.alpha.main]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl]
   [integrant.core :as ig]
   [xtdb.api :as xt]))

(apply require clojure.main/repl-requires)

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn delete-local-access-token
  "Until access tokens are stored in the database, restarting a server will clear
  tokens out of memory causing the local access token to be invalid. Delete it
  now to avoid any unhelpful 401 messages when using the CLI tool."
  []
  (let [f (io/file (System/getProperty "user.home") ".local/share/site/access-token.json")]
    (when (.exists f)
      (println "Deleting" (.getAbsolutePath f))
      (.delete f))))

(defn start []
  (println "Site by JUXT. Copyright (c) 2021, JUXT LTD.")
  (println "Compiling code, please wait...")
  (delete-local-access-token)
  (log/info "Starting development system")


  (alter-var-root #'juxt.site.alpha.main/profile (constantly :dev))
  (let [system-config (juxt.site.alpha.main/system-config)
        sys (ig/init system-config)]
    (alter-var-root #'juxt.site.alpha.main/system (constantly sys)))
  (log/info "System started and ready...")

  (println)
  (println "Welcome to Site!")
  (juxt.site.alpha.repl/status)

  (println (ansi/yellow "Enter (help) for help"))

  :ready)
