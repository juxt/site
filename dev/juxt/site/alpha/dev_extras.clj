;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.dev-extras
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [crux.api :as crux]
   [crypto.password.bcrypt :as password]
   [dev-extras :refer :all]
   [jsonista.core :as json]
   [juxt.site.alpha.util :as util])
  (:import (java.io DataInputStream FileInputStream)))

(alias 'dave (create-ns 'juxt.dave.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn config []
  integrant.repl.state/config)

(defn local-uri []
  (format "http://localhost:%d" (get-in (config) [:juxt.site.alpha.server/server :port])))

(defn to-local [s]
  (if-let [[_ path] (re-matches #"https://home.juxt.site(.*)" s)]
    (str (local-uri) path)
    s))

(defn grep [re coll]
  (filter #(re-matches (re-pattern re) %) coll))

(defn crux-node []
  (:juxt.site.alpha.db/crux-node system))

(defn db []
  (crux/db (crux-node)))

(defn e [id]
  (crux/entity (db) id))

(defn put [& ms]
  (->>
   (crux/submit-tx
    (crux-node)
    (for [m ms]
      [:crux.tx/put m]))
   (crux/await-tx (crux-node))))

(defn rm [id]
  (->>
   (crux/submit-tx
    (crux-node)
    [[:crux.tx/delete id]])
   (crux/await-tx (crux-node))))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn q [query & args]
  (apply crux/q (db) query args))

(defn t [t]
  (map
   first
   (crux/q (db) '{:find [e] :where [[e ::site/type t]] :in [t]} t)))

(defn t* [t]
  (map
   first
   (crux/q (db) '{:find [e] :where [[e :type t]] :in [t]} t)))

(defn ls
  ([]
   (sort-by
    str
    (map first
         (q '{:find [e] :where [[e :crux.db/id]]}))))
  ([t]
   (sort-by
    str
    (map first
         (q '{:find [(eql/project e [*])]
              :where [[e :crux.db/id]
                      [e ::site/type t]]
              :in [t]} t)))))

(defn rules []
  (sort-by
   str
   (map first
        (q '{:find [(eql/project e [*])] :where [[e ::site/type "Rule"]]}))))

(defn uuid [s]
  (cond
    (string? s) (java.util.UUID/fromString s)
    (uuid? s) s))

(defn uri [s]
  (cond
    (string? s) (java.net.URI. s)
    (uri? s) s))

(defn we
  "Lookup a 'web entity'"
  [u]
  (e (uri u)))

(defn wes
  "List all web entities"
  []
  (sort
   (for [[e m] (q '{:find [e (distinct m)] :where [[e ::http/methods m]]})
         :let [ent (crux/entity (db) e)]]
     [(str e) m (count (::http/representations ent))])))

(defn slurp-file-as-bytes [f]
  (let [f (io/file f)]
    (.readAllBytes (FileInputStream. f))))

(defn init-db [webmaster-password]
  (println "Initializing Site Database")

  ;; Create the webmaster.
  (put
   {:crux.db/id "https://home.juxt.site/_site/users/webmaster"
    ::http/methods #{:get :head :options}
    ::http/representations []
    ::pass/password-hash!! (password/encrypt webmaster-password)}

   ;; Add rule that allows the webmaster to do everything, at least during the
   ;; bootstrap phase of a deployment. This can be deleted after the initial
   ;; users/roles have been populated, if required.
   {:crux.db/id "https://home.juxt.site/_site/rules/webmaster-allow-read-all"
    :description "The webmaster has read access to everything"
    ::site/type "Rule"
    ::pass/target '[[subject :juxt.pass.alpha/username "webmaster"]]
    ::pass/effect ::pass/allow
    ::pass/allow-methods #{:get :head :options}})

  ;; Resources classified as PUBLIC should be readable (but not writable). We'll
  ;; need at least one PUBLIC resource to provide a login page.
  (put
   {:crux.db/id "https://home.juxt.site/_site/rules/public-resources"
    ::site/type "Rule"
    ::site/description "PUBLIC resources are accessible to GET"
    ::pass/target '[[request :request-method #{:get :head :options}]
                    [resource ::pass/classification "PUBLIC"]]
    ::pass/effect ::pass/allow})

  ;; Add the home page, with classification of PUBLIC. If you're not logged in,
  ;; you'll get a login form.
  (put
   {:crux.db/id "https://home.juxt.site/index.html"
    ::http/methods #{:get :head :options}
    ::http/representations
    [{::http/content-type "text/html;charset=utf-8"
      ::site/body-generator :juxt.site.alpha.home/home-page}]
    ::pass/classification "PUBLIC"})

  ;; The login form will require some additional PUBLIC resources
  (apply
   put
   (for [f ["juxt-logo-on-white.svg" "juxt-logo-on-black.svg"]]
     {:crux.db/id (str "https://home.juxt.site/" f)
      ::http/methods #{:get :head :option}
      ::http/representations
      [(let [bytes (slurp-file-as-bytes (str "resources/" f))]
         {::http/content-type "image/svg+xml"
          ::http/content-length (count bytes)
          ::http/body bytes})]
      ::pass/classification "PUBLIC"}))

  ;; The login form is styled with Tailwind CSS
  (put
   {:crux.db/id "https://home.juxt.site/css/tailwind/styles.css"
    ::http/methods #{:get :head :option}
    ::http/representations
    [(let [bytes (slurp-file-as-bytes "style/target/styles.css")]
       {::http/content-type "text/css"
        ::http/content-length (count bytes)
        ::http/body bytes})
     (let [bytes (slurp-file-as-bytes "style/target/styles.css.gz")]
       {::http/content-type "text/css"
        ::http/content-encoding "gzip"
        ::http/content-length (count bytes)
        ::http/body bytes})
     (let [bytes (slurp-file-as-bytes "style/target/styles.css.br")]
       {::http/content-type "text/css"
        ::http/content-encoding "br"
        ::http/content-length (count bytes)
        ::http/body bytes})]
    ::pass/classification "PUBLIC"})

  ;; A login post handler will also be required, along with a special rule to
  ;; allow anyone to POST to it.
  (put
   {:crux.db/id "https://home.juxt.site/_site/login"
    ::http/methods #{:post}
    ::http/acceptable "application/x-www-form-urlencoded"
    ::site/purpose ::site/post-login-credentials
    ::pass/expires-in (* 3600 24 30)}

   {:crux.db/id "https://home.juxt.site/_site/rules/anyone-can-post-login-credentials"
    ::site/type "Rule"
    ::site/description "The login POST handler must be accessible by all"
    ::pass/target '[[request :request-method #{:post}]
                    [resource ::site/purpose ::site/post-login-credentials]]
    ::pass/effect ::pass/allow})

  ;; After the webmaster has logged in, they may want to add new users. For
  ;; this, they need at least need access to the Swagger UI.
  (let [jarpath
        (some
         #(re-matches #".*swagger-ui.*" %)
         (str/split (System/getProperty "java.class.path") #":"))
        fl (io/file jarpath)
        jar (java.util.jar.JarFile. fl)]
    (doseq
        [je (enumeration-seq (.entries jar))
         :let [nm (.getRealName je)
               [_ suffix] (re-matches #".*\.(.*)" nm)
               size (.getSize je)
               path (second
                     (re-matches #"META-INF/resources/webjars/swagger-ui/[0-9.]+/(.*)"
                                 nm))]
         :when path
         :let [uri (format "https://home.juxt.site/_site/swagger-ui/%s" path)]
         ;; TODO: Do we still need this to defend against 0 size web entries?
         :when (pos? size)]
        (let [body (.readAllBytes (.getInputStream jar je))]
          (put
           {:crux.db/id uri
            ::http/methods #{:get :head :options}
            ::http/representations [{::http/content-type (get util/mime-types suffix "application/octet-stream")
                                     ::http/last-modified (java.util.Date. (.getTime je))
                                     ::http/content-length (count body)
                                     ::http/content-location uri
                                     ::http/body body}]}))))

  ;; Add a Site API
  (let [f (io/file "src/juxt/site/alpha/openapi.edn")
        json (json/write-value-as-string (edn/read-string (slurp f)))
        openapi (json/read-value json)
        bytes (.getBytes json "UTF-8")]
    (put
     {:crux.db/id "https://home.juxt.site/_site/apis/site/openapi.json"
      ::http/methods #{:get :head :options}
      ::http/representations
      [{::http/content-type "application/vnd.oai.openapi+json;version=3.0.2"
        ::http/last-modified (java.util.Date. (.lastModified f))
        ::http/content-length (count bytes)
        ::http/body bytes}]
      ::site/type "OpenAPI"
      :juxt.apex.alpha/openapi openapi}))

  ;; Redirect from / to /index.html
  (put
   {:crux.db/id "https://home.juxt.site/"
    ::http/redirect "/index.html"})

  #_(put
     {:crux.db/id "https://home.juxt.site/_site/pass/rules/users-can-post-their-own-home-pages"
      ::site/type "Rule"
      ::pass/target '[[subject ::pass/user user]
                      [resource ::owner user]]
      ::pass/effect ::pass/allow})

  (put
   (let [bytes (.readAllBytes (io/input-stream (io/file "resources/favicon.ico")))]
     {:crux.db/id "https://home.juxt.site/favicon.ico"
      ::pass/classification "PUBLIC"
      ::http/methods #{:get :head :options}
      ::http/representations
      [{::http/content-type "image/x-icon"
        ::http/content-length (count bytes)
        ::http/body bytes}]}))

  ;; Authentication resources

  (let [token-endpoint "https://home.juxt.site/_site/token"
        grant-types #{"client_credentials"}]
    (put
     {:crux.db/id token-endpoint
      ::http/methods #{:post}
      ::http/acceptable "application/x-www-form-urlencoded"
      ::pass/expires-in 60})

    (let [content
          (str
           (json/write-value-as-string
            {"issuer" "https://juxt.site" ; draft
             "token_endpoint" token-endpoint
             "token_endpoint_auth_methods_supported" ["client_secret_basic"]
             "grant_types_supported" (vec grant-types)}
            (json/object-mapper
             {:pretty true}))
           "\r\n")]
      (put
       {:crux.db/id "https://home.juxt.site/.well-known/openid-configuration"

        ;; OpenID Connect Discovery documents are publically available
        ::pass/classification "PUBLIC"

        ::http/methods #{:get :head :options}
        ::http/representations
        [{::http/content-type "application/json"
          ::http/last-modified (java.util.Date.)
          ::http/etag (subs (util/hexdigest (.getBytes content)) 0 32)
          ::http/content content}]})))


  (put
   {:crux.db/id "https://home.juxt.site/~webmaster/"
    :juxt.site.alpha.home/owner "https://home.juxt.site/_site/users/webmaster"
    ::http/methods #{:get :head :options}
    ::http/representations
    [{::http/content-type "text/html;charset=utf-8"
      ::site/body-generator :juxt.site.alpha.home/user-home-page}]})

  #_(put
     {:crux.db/id "https://home.juxt.site/_site/api-console"
      ::http/methods #{:get :head :options}
      ::http/representations
      [{::http/content-type "text/html;charset=utf-8"
        ::site/bytes-generator ::api-console-generator}]})

  )

#_[[:crux.tx/put
    {:crux.db/id "/contacts/roger.edn"
     :type "Contact"
     :name "Roger Farringdon"
     :email "roger@example.com"
     :address "13 Pickets Way, Colchester"
     :tel "01 232 7321"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/jeremy.edn"
     :type "Contact"
     :name "Jeremy Taylor"
     :email "jdt@juxt.pro"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/malcoml.edn"
     :type "Contact"
     :name "Jeremy Taylor"
     :email "jdt@juxt.pro"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/ben.edn"
     :type "Contact"
     :name "Ben Harvy"
     :email "ben@example.com"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/tim.edn"
     :type "Contact"
     :name "Tim Greene"
     :email "tim@example.com"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/contacts/chris.edn"
     :type "Contact"
     :name "Chris Roberts"
     :email "ben@example.com"
     ::http/methods #{:get :head :options :put}
     ::http/acceptable "application/edn"
     ::http/representations
     [{::http/content-type "application/edn"
       ::http/last-modified last-modified
       ::site/bytes-generator ::contact}]}]

   [:crux.tx/put
    {:crux.db/id "/projects/internal.edn"
     ::site/type "Project"
     :name "Internal projects"}]

   [:crux.tx/put
    {:crux.db/id "/contacts/"
     :description "A collection"
     ::http/methods #{:get :head :options :propfind :mkcol}
     ::http/options {"DAV" "1"}
     ::dave/query '[[e :type "Contact"]]}]]
