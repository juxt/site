;; Copyright © 2021, JUXT LTD.

(ns user
  (:require
   clojure.main
   [io.aviso.ansi :as ansi]
   [juxt.site.alpha.main :as main]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :refer :all]
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


  (alter-var-root #'main/profile (constantly :dev))
  (let [system-config (main/system-config)
        sys (ig/init system-config)]
    (alter-var-root #'main/system (constantly sys)))
  (log/info "System started and ready...")

  (println)
  (println "Welcome to Site!")
  (status)

  (println (ansi/yellow "Enter (help) for help"))

  :ready)


#_(map first
     (xt/q (db) {:find ['scope]
                 :where '[
                          (check-scope grant subject)
                          [grant :juxt.site.alpha/type "ScopeGrant"]
                          [grant :juxt.apex.alpha/scope scope]
                          ]
                 :rules [
                         '[(check-scope grant subject)
                           [session :juxt.pass.jwt/sub sub]
                           [session :juxt.pass.jwt/iss iss]
                           [ident :juxt.home/issuer iss]
                           [ident :juxt.home/subject-identifier sub]
                           [ident :juxt.home/person-id person]
                           [acl :juxt.home/person-id person]
                           [acl :juxt.site.alpha/type "ACL"]
                           [acl :juxt.home/role role]
                           [grant :juxt.home/role role]
                           ]]
                 :in '[session]}
           "urn:site:session:6e561642a82618655d1345a4981908cf"))

#_(ls-type "ACL")

#_(e "https://home.test/grants/mal-has-internal")


#_{:juxt.site.alpha/type "ACL",
 :juxt.home/person-id
 "https://home.test/people/f8c2c5c3-635d-4b66-a48d-0d831dce88aa",
 :juxt.home/role "https://home.test/_home/roles/internal",
 :xt/id "https://home.test/grants/mal-has-internal"}
