;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resources.form-based-auth
  (:require
   [juxt.site.session-scope :as session-scope]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [juxt.site.init :as init :refer [substitute-actual-base-uri]]
   [malli.core :as malli]
   [ring.util.codec :as codec]))

;; TODO: Rename to create-action-install-login-form-resource!
(defn create-action-create-login-resource!
  "A very specific action that creates a login form."
  ;; TODO: We could make the HTML content a parameter
  ;; Note: It helps security if the http methods remain unconfigurable.
  [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/create-login-resource"

       :juxt.site.malli/input-schema
       [:map
        [:xt/id [:re "https://example.org/.*"]]
        [:juxt.site/session-scope [:re "https://example.org/.*"]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/edn"
              (some->
               body
               (String.)
               clojure.edn/read-string
               juxt.site.malli/validate-input
               (assoc
                :juxt.site/methods
                {:get {:juxt.site/actions #{"https://example.org/actions/get-public-resource"}}
                 :head {:juxt.site/actions #{"https://example.org/actions/get-public-resource"}}
                 :post {:juxt.site/actions #{"https://example.org/actions/login"}}
                 :options {:juxt.site/actions #{"https://example.org/actions/get-options"}}}

                :juxt.http/content-type "text/html;charset=utf-8"
                :juxt.http/content "
<html>
<head>
<link rel='icon' href='data:,'>
</head>
<body>
<form method=POST>
<p>
Username: <input name=username type=text>
</p>
<p>
Password: <input name=password type=password>
</p>
<p>
<input type=submit value=Login>
</p>
</form>
</body>
</html>
\r\n")))))}

       :juxt.site/transact
       {:juxt.site.sci/program
        (pr-str
         '[[:xtdb.api/put *prepare*]])}

       :juxt.site/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]})))))

(defn grant-permission-to-create-login-resource! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     ;; tag::grant-permission-to-create-login-resource![]
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/system/create-login-resource"
       :juxt.site/subject "https://example.org/subjects/system"
       :juxt.site/action "https://example.org/actions/create-login-resource"
       :juxt.site/purpose nil
       })
     ;; end::grant-permission-to-create-login-resource![]
     ))))

(defn create-login-resource! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-login-resource"
      {:xt/id "https://example.org/login"
       :juxt.site/session-scope "https://example.org/session-scopes/default"})))))

(defn create-action-login! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/create-action"
      {:xt/id "https://example.org/actions/login"
       :juxt.site/rules
       '[
         [(allowed? subject resource permission)
          [permission :xt/id]]]

       :juxt.site.malli/input-schema
       [:map
        ["username" [:string {:min 1}]]
        ["password" [:string {:min 1}]]]

       :juxt.site/prepare
       {:juxt.site.sci/program
        (pr-str
         '(let [content-type (-> *ctx*
                                 :juxt.site/received-representation
                                 :juxt.http/content-type)
                body (-> *ctx*
                         :juxt.site/received-representation
                         :juxt.http/body)]
            (case content-type
              "application/x-www-form-urlencoded"
              {:form (-> body (String.) ring.util.codec/form-decode juxt.site.malli/validate-input)
               :subject-id (juxt.site.util/make-nonce 10)
               :session-id (juxt.site.util/make-nonce 16)
               :session-token (juxt.site.util/make-nonce 16)})))}

       ;;:juxt.site/transact juxt.book.login/login-quotation
       :juxt.site/transact
       {
        :juxt.site.sci/program
        (pr-str
         '(let [user-identity
                (juxt.site/match-identity-with-password
                 {:juxt.site/type "https://meta.juxt.site/site/basic-user-identity"
                  :juxt.site/username (clojure.string/lower-case (get-in *prepare* [:form "username"]))}
                 (get-in *prepare* [:form "password"])
                 :juxt.site/password-hash)

                subject
                (when user-identity
                  {:xt/id (str "https://example.org/subjects/" (get *prepare* :subject-id))
                   :juxt.site/type "https://meta.juxt.site/site/subject"
                   :juxt.site/user-identity user-identity})

                _ (assert subject)

                session-id (:session-id *prepare*)
                _ (assert session-id)

                session-scope-id (:juxt.site/session-scope *resource*)

                session-scope (xt/entity session-scope-id)

                _ (when-not session-scope
                    (throw (ex-info "Session scope does not exist in database" {:session-scope session-scope-id})))

                session
                (when subject
                  {:xt/id (str "https://example.org/sessions/" session-id)
                   :juxt.site/type "https://meta.juxt.site/site/session"
                   :juxt.site/subject (:xt/id subject)
                   :juxt.site/session-scope session-scope-id})

                session-token (:session-token *prepare*)
                _ (assert session-token)

                session-token-doc
                (when session
                  {:xt/id (str "https://example.org/session-tokens/" session-token)
                   :juxt.site/type "https://meta.juxt.site/site/session-token"
                   :juxt.site/session-token session-token
                   :juxt.site/session (:xt/id session)})

                cookie-name (:juxt.site/cookie-name session-scope)
                cookie-path (or (:juxt.site/cookie-path session-scope) "/")]

            (cond-> []
              subject (conj [:xtdb.api/put subject])
              session (conj [:xtdb.api/put session])
              session-token-doc (conj [:xtdb.api/put session-token-doc])

              (and session-token-doc cookie-name)
              (conj [:ring.response/headers
                     { ;;"location" return-to
                      "set-cookie"
                      (format "%s=%s; Path=%s; Secure; HttpOnly; SameSite=Lax"
                              cookie-name session-token cookie-path)}])
              #_true #_(conj [:xtdb.api/put
                          {:xt/id :result
                           :session-scope session-scope
                           :session-scope-id session-scope-id
                           :cookie-name cookie-name
                           :cookie-path cookie-path
                           }]))))}})))))

(defn grant-permission-to-invoke-action-login! [_]
  (eval
   (substitute-actual-base-uri
    (quote
     (juxt.site.init/do-action
      "https://example.org/subjects/system"
      "https://example.org/actions/grant-permission"
      {:xt/id "https://example.org/permissions/login"
       :juxt.site/action "https://example.org/actions/login"
       :juxt.site/purpose nil})))))

(def dependency-graph
  {"https://example.org/actions/login"
   {:create #'create-action-login!
    :deps #{::init/system}}

   "https://example.org/permissions/login"
   {:create #'grant-permission-to-invoke-action-login!
    :deps #{::init/system}}

   "https://example.org/actions/create-login-resource"
   {:create #'create-action-create-login-resource!
    :deps #{::init/system
            "https://example.org/actions/login"
            "https://example.org/permissions/login"}}

   "https://example.org/permissions/system/create-login-resource"
   {:create #'grant-permission-to-create-login-resource!
    :deps #{::init/system}}

   "https://example.org/login"
   {:create #'create-login-resource!
    :deps #{::init/system
            "https://example.org/actions/create-login-resource"
            "https://example.org/permissions/system/create-login-resource"
            "https://example.org/session-scopes/default"}}})

(defn login-with-form!
  "Return a session id (or nil) given a map of fields."
  [handler & {:juxt.site/keys [uri] :as args}]
  {:pre [(malli/validate
          [:map
           ;;[:juxt.site/uri [:re "https://.*"]]
           [:juxt.site/uri [:re "https://.*"]]
           ["username" [:string {:min 2}]]
           ["password" [:string {:min 6}]]] args)]}
  (let [form (codec/form-encode (dissoc args :juxt.site/uri))
        body (.getBytes form)
        req {;;:juxt.site/uri uri
             :juxt.site/uri (:juxt.site/uri args)
             :ring.request/method :post
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/x-www-form-urlencoded"}
             :ring.request/body (io/input-stream body)}
        response (handler req)
        {:strs [set-cookie]} (:ring.response/headers response)
        [_ id] (when set-cookie (re-matches #"[a-z]+=(.*?);.*" set-cookie))]
    (when-not id
      (throw
       (ex-info
        "Login failed"
        {:args args
         :response response})))
    {:juxt.site/session-token id}))
