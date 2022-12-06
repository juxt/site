;; Copyright © 2021, JUXT LTD.

(ns user
  (:require
   clojure.main
   [io.aviso.ansi :as ansi]
   [juxt.site.main :as main]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [juxt.site.repl :refer :all]
   [juxt.site.bootstrap :refer [bootstrap bootstrap! bootstrap!!]]
   [juxt.site.init :as init :refer [config base-uri xt-node system put!]]
   [integrant.core :as ig]
   [xtdb.api :as xt]
   malli.dev.pretty
   [malli.dev :as md]
   juxt.site.schema
   xtdb.query
   fipp.ednize
   [juxt.clojars-mirrors.nippy.v3v1v1.taoensso.nippy :as nippy]
   ))

(nippy/extend-freeze
 clojure.lang.Atom :juxt.site.nippy/atom [x data-output]
 (.writeUTF data-output "<atom>"))

(nippy/extend-thaw
 :juxt.site.nippy/atom [data-input]
 (.readUTF data-input)
 nil)

(nippy/extend-freeze
 xtdb.query.QueryDatasource :juxt.site.nippy/db [x data-output]
 (.writeUTF data-output "<db>"))

(nippy/extend-thaw
 :juxt.site.nippy/db [data-input]
 (.readUTF data-input)
 nil)

(apply require clojure.main/repl-requires)

;; This ensures that Fipp doesn't attempt to ednize the entire database upon
;; pretty printing.
(extend-type xtdb.query.QueryDatasource
  fipp.ednize/IOverride
  fipp.ednize/IEdn
  (-edn [db] (pr-str db)))

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
  (println "Site by JUXT. Copyright (c) 2021-2022, JUXT LTD.")
  (println "Compiling code, please wait...")
  (delete-local-access-token)
  (log/info "Starting development system")
  (alter-var-root #'main/profile (constantly :dev))
  (let [system-config (main/system-config)
        system (ig/init system-config)]
    (alter-var-root #'main/*system* (constantly system)))

  (println "Starting Malli development instrumentation")
  (md/start!
   {:report
    (fn [type data]
      (throw (ex-info (format "Malli validation failure: %s" type)
                      {:type type
                       ;; Sometimes this can include the whole db!
                       ;;:data data
                       })))})

  (log/info "System started and ready...")

  (println)
  (println "Welcome to Site!")

  (println (ansi/yellow "Enter (help) for help"))

  :ready)
