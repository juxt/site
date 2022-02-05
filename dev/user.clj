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

#_(e "https://home.test/identities/96a65f1b-1c14-4b36-af45-42dfdd1f21ba")

#_(q '{:find [(pull acl [*])]
     :where [[session :juxt.pass.openid/sub sub]
             [session :juxt.pass.openid/iss iss]
             [ident :juxt.home/issuer iss]
             [ident :juxt.home/subject-identifier sub]
             [ident :juxt.home/person-id subject]
             (check acl subject resource)
             ]

     :in [session resource]

     ;; Here are the rules, attached to /index.html, that say that for an
     ;; INTERNAL resource, those that have been granted access to internal, can
     ;; {GET,HEAD,OPTIONS} it.
     :rules [
             ;; Anyone who has the internal role can see resources classified as INTERNAL
             [(check acl subject resource)
              [acl ::site/type "ACL"]
              [acl :juxt.home/role "https://home.test/_home/roles/internal"]
              [acl :juxt.home/person-id subject]
              [resource :juxt.pass.alpha/classification "INTERNAL"]
              ]

             ;; Role access (with ACL granting the role to the subject)
             [(check acl subject resource)
              [acl ::site/type "ACL"]
              [acl :juxt.home/person-id subject]
              [acl :juxt.home/role role]
              [role-access :juxt.home/type "RoleAccess"]
              [role-access :juxt.home/role role]
              [role-access :juxt.site/uri resource]
              ]]}

   "urn:site:session:ae3fe679a038e61fdc4cd8ef3839079a"

   "https://home.test/pages.graphql")

;; This is a grant of INTERNAL resources to Malcolm
#_(put!
 {:xt/id "https://home.test/grants/mal-has-internal"
  ::site/type "ACL"
  :juxt.home/person-id "https://home.test/people/8430214a-5b05-469a-9639-f65d6d6dfa24"
  :juxt.home/role "https://home.test/_home/roles/internal"})


;; A grant of pages to developers

#_(put!
 {:xt/id "https://home.test/grants/developer-sees-pages"
  :juxt.home/type "RoleAccess"
  :juxt.home/role "https://home.test/_home/roles/developer"
  :juxt.site/uri #{"https://home.test/pages.graphql"
                   "https://home.test/employee.graphql"}})

#_(put!
 {:xt/id "https://home.test/grants/mal-has-developer"
  ::site/type "ACL"
  :juxt.home/person-id "https://home.test/people/8430214a-5b05-469a-9639-f65d6d6dfa24"
  :juxt.home/role "https://home.test/_home/roles/developer"})


#_(e "https://home.test/grants/mal-has-internal")

#_(rm! "https://home.test/grants/mal-has-internal")

#_(e "https://home.test/people/8430214a-5b05-469a-9639-f65d6d6dfa24")

#_(e "urn:site:session:16c10e3bece6be15d05670c6748da43f")

#_(e "https://home.test/index.html")

#_(keys (e "https://home.test/pages.graphql"))
