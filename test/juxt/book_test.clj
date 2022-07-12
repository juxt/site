;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.flip.alpha.core :as f]
   [juxt.flip.clojure.core :as-alias fc]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.pass.alpha.http-authentication :as authn]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.init :as init :refer [put! do-action]]
   [juxt.site.alpha.repl :as repl]
   [juxt.test.util :refer [with-system-xt *xt-node* *handler*] :as tutil]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-handler)

(deftest not-found-test
  (init/bootstrap!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))

;;ndeftest public-resource-test

((t/join-fixtures [with-system-xt with-handler])
 (fn []
   (init/bootstrap!)
   (book/setup-hello-world!)

   (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")) ;; Assert the entity exists in the db
   (is (not (xt/entity (xt/db *xt-node*) "https://site.test/not-hello"))) ;; Assert that out 404 entity is not in the db

   (let [req {:ring.request/method :get
              :ring.request/path "/hello"}]

     (testing "Can retrieve a public immutable resource"
       (let [{:ring.response/keys [status body] :as response} (*handler* req)]
         response
         #_(is (= 200 status))
         #_(is (= "Hello World!\r\n" body))))

     #_(testing "Receive 405 when method not allowed"
       (let [invalid-req (assoc req :ring.request/method :put)
             {:ring.response/keys [status]} (*handler* invalid-req)]
         (is (= 405 status))))

     #_(testing "Receive 404 when resource does not exist"
       (let [invalid-req (assoc req :ring.request/path "/not-hello")
             {:ring.response/keys [status]} (*handler* invalid-req)]
         (is (= 404 status))))))
 )



(defn encode-basic-authorization [user password]
  (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" user password))))))

(deftest protected-resource-with-http-basic-auth-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (book/protection-spaces-preliminaries!)

  (book/create-resource-protected-by-basic-auth!)
  (book/grant-permission-to-resource-protected-by-basic-auth!)
  (book/put-basic-protection-space!)

  (book/users-preliminaries!)
  (book/create-action-put-basic-user-identity!)
  (book/grant-permission-to-invoke-action-put-basic-user-identity!)
  (book/put-basic-user-identity-alice!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected-by-basic-auth/document.html"))

  (is (= 1 (count (authn/protection-spaces (xt/db *xt-node*) "https://site.test/protected-by-basic-auth/document.html"))))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-basic-auth/document.html"}

        request-with-good-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "garden")})

        request-with-bad-creds
        (assoc request :ring.request/headers {"authorization" (encode-basic-authorization "alice" "gradne")})]

    (let [response (*handler* request)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-bad-creds)]
      (is (= 401 (:ring.response/status response)))
      (is (= "Basic realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))

    (let [response (*handler* request-with-good-creds)]
      (is (= 200 (:ring.response/status response)))
      (is (nil? (get-in response [:ring.response/headers "www-authenticate"]))))))

(deftest session-scope-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)

  (book/session-scopes-preliminaries!)

  (book/create-resource-protected-by-session-scope!)
  (book/grant-permission-to-resource-protected-by-session-scope!)
  (book/create-session-scope!)

  (let [uri (some :juxt.pass.alpha/login-uri
                  (session-scope/session-scopes (xt/db *xt-node*) "https://site.test/protected-by-session-scope/document.html"))]
    (is (string? uri)))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-session-scope/document.html"}]
    (testing "Redirect"
      (let [response (*handler* request)]
        (is (= 302 (:ring.response/status response)))
        (is (.startsWith
             (get-in response [:ring.response/headers "location"])
             "https://site.test/login?return-to="))))))

(deftest protected-resource-with-http-bearer-auth-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (book/protection-spaces-preliminaries!)

  (book/applications-preliminaries!)

  (let [log-entry (book/setup-application!)
        db (xt/db *xt-node*)
        lookup (fn [id] (xt/entity db id))
        bearer-token (-> log-entry ::pass/puts (get 0) lookup ::pass/token)]

    (book/create-resource-protected-by-bearer-auth!)
    (book/grant-permission-to-resource-protected-by-bearer-auth!)
    (book/put-bearer-protection-space!)

    (is (xt/entity (xt/db *xt-node*) "https://site.test/protected-by-bearer-auth/document.html"))

    (let [request {:ring.request/method :get
                   :ring.request/path "/protected-by-bearer-auth/document.html"}]

      (testing "Cannot be accessed without a bearer token"
        (let [response (*handler* request)]
          response
          (is (= 401 (:ring.response/status response)))
          (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))
          ))

      (testing "Can be accessed with a valid bearer token"
        (let [response (*handler*
                        (assoc
                         request
                         :ring.request/headers
                         {"authorization" (format "Bearer %s" bearer-token)}))]
          (is (= 200 (:ring.response/status response)))
          (is (nil? (get-in response [:ring.response/headers "www-authenticate"])))))

      (testing "Cannot be accessed with an invalid bearer token"
        (let [response (*handler*
                        (assoc
                         request
                         :ring.request/headers
                         {"authorization" "Bearer not-test-access-token"}))]
          (is (= 401 (:ring.response/status response)))
          (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"]))))))))

(deftest user-directory-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (book/applications-preliminaries!)

  (let [log-entry (book/setup-application!)
        db (xt/db *xt-node*)
        lookup (fn [id] (xt/entity db id))
        bearer-token (-> log-entry ::pass/puts (get 0) lookup ::pass/token)]

    (do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/create-action"
     {:xt/id "https://site.test/actions/put-user-owned-content"
      :juxt.pass.alpha/scope "write:user-content"
      :juxt.pass.alpha/rules
      [
       '[(allowed? subject resource permission)
         [permission ::pass/user user]
         [subject ::pass/user-identity id]
         [id ::pass/user user]
         [resource :owner user]]]})

    (do-action
     "https://site.test/subjects/system"
     "https://site.test/actions/grant-permission"
     {:xt/id "https://site.test/permissions/put-user-owned-content"
      :juxt.pass.alpha/action "https://site.test/actions/put-user-owned-content"
      :juxt.pass.alpha/user "https://site.test/users/alice"
      :juxt.pass.alpha/purpose nil})

    ;; Both bob and alice have user directories
    (doseq [user #{"bob" "alice"}]
      (juxt.site.alpha.init/put!
       {:xt/id (format "https://site.test/~%s/{path}" user)
        ;; This needs to return a resource 'owned' by the user, then the action can
        ;; unify on the subject's user and the resource's owner.
        :owner (format "https://site.test/users/%s" user)
        ::site/uri-template true
        ::site/methods
        {:get {::pass/actions #{"https://site.test/actions/get-not-found"}}
         :put {::pass/actions #{"https://site.test/actions/put-user-owned-content"}}}}))

    ;; 404 on GET, doesn't exist yet!
    (let [req {:ring.request/method :get
               :ring.request/path "/~bob/index.html"}]
      (is (= 404 (:ring.response/status (*handler* req)))))

    ;; Alice can't write to Bob's area
    (let [req {:ring.request/method :put
               :ring.request/path "/~bob/index.html"
               :ring.request/headers
               {"authorization" (format "Bearer %s" bearer-token)}}]
      (is (= 403 (:ring.response/status (*handler* req)))))

    ;; When Alice writing to Alice's user directory, we get through security.
    (let [req {:ring.request/method :put
               :ring.request/path "/~alice/index.html"
               :ring.request/headers
               {"authorization" (format "Bearer %s" bearer-token)}}]
      (is (= 411 (:ring.response/status (*handler* req)))))))

;; This is a test just to check that
;; https://site.test/actions/put-immutable-protected-resource functions
;; properly.
(deftest put-protected-resource-test
  (init/bootstrap!)
  (book/protected-resource-preliminaries!)
  (is (=
       {:juxt.pass.alpha/subject "https://site.test/subjects/system"
        :juxt.site.alpha/type "https://meta.juxt.site/site/action-log-entry"
        :juxt.pass.alpha/action
        "https://site.test/actions/put-immutable-protected-resource"
        :juxt.pass.alpha/puts
        ["https://site.test/protected-by-session-scope/document.html"]
        :juxt.pass.alpha/deletes []}
       (select-keys
        (book/create-resource-protected-by-session-scope!)
        [:juxt.pass.alpha/subject
         :juxt.site.alpha/type
         :juxt.pass.alpha/action
         :juxt.pass.alpha/puts
         :juxt.pass.alpha/deletes]))))

;;deftest login-test

((t/join-fixtures [with-system-xt with-handler])
 (fn []
   (init/bootstrap!)
   (book/protected-resource-preliminaries!)
   (book/create-resource-protected-by-session-scope!)
   (book/session-scopes-preliminaries!)
   (book/grant-permission-to-resource-protected-by-session-scope!)
   (book/create-session-scope!)

   (book/create-action-create-login-resource!)
   (book/grant-permission-to-create-login-resource!)
   (book/create-login-resource!)
   (book/create-action-login!)
   (book/grant-permission-to-invoke-action-login!)

   (book/users-preliminaries!)

   (book/create-action-put-basic-user-identity!)
   (book/grant-permission-to-invoke-action-put-basic-user-identity!)

   (book/put-user-alice!)
   (book/put-basic-user-identity-alice!)

   (let [uri (some :juxt.pass.alpha/login-uri
                   (session-scope/session-scopes (xt/db *xt-node*) "https://site.test/protected-by-session-scope/document.html"))]
     (is (string? uri)))

   ;; Test that session scope exists
   (let [uri (some
              :juxt.pass.alpha/login-uri
              (session-scope/session-scopes
               (xt/db *xt-node*)
               "https://site.test/protected-by-session-scope/document.html"))]
     (is (string? uri)))

   ;; There is no cookie at present, so no session, so we're expecting a
   ;; redirect to a login form.
   (let [request {:ring.request/method :get
                  :ring.request/path "/protected-by-session-scope/document.html"}]

     (testing "Redirect"
       (let [response (*handler* request)]
         (is (= 302 (:ring.response/status response)))
         (is (.startsWith
              (get-in response [:ring.response/headers "location"])
              "https://site.test/login"))))

     ;; POST to the /login handler, which call the login action.
     ;; After this there should be a set-cookie escalation

     (let [body (.getBytes
                 (codec/form-encode
                  ;; usernames are case-insensitive - testing this
                  {"username" "aliCe"
                   "password" "garden"}))
           login-request
           {:ring.request/method :post
            :ring.request/path "/login"
            :ring.request/headers
            {"content-length" (str (count body))
             "content-type" "application/x-www-form-urlencoded"}
            :ring.request/query "return-to=/document.html"
            :ring.request/body (io/input-stream body)}
           response (time (*handler* login-request))
           session-token (get-in response [:ring.response/headers "set-cookie"])]

       (is (string? session-token)) ;; TODO: Check for a correct set-cookie header
       #_(is (= 302 (:ring.response/status response)))

       (let [cookie-value (get-in response [:ring.response/headers "set-cookie"])
             location (get-in response [:ring.response/headers "location"])
             token (when cookie-value (second (re-matches #"id=(.*?);.*" cookie-value)))
             db (xt/db *xt-node*)
             ;; Check the database for evidence a session has been created
             [e]
             (first (when token
                      (xt/q db '{:find [(pull e [*])]
                                 :where [[e ::site/type "https://meta.juxt.site/pass/session-token"]
                                         [e ::pass/session-token tok]]
                                 :in [tok]}
                            token)))]
         (is cookie-value)
         (is token "Cookie value doesn't contain an id")
         (is (> (count token) 20))
         (is e)
         (is (= "https://meta.juxt.site/pass/session-token" (:juxt.site.alpha/type e)))

         (is (= "/document.html" location))

         (testing "Access protected document with cookie"
           (let [request (assoc-in request [:ring.request/headers "cookie"] (format "id=%s" token))
                 response (*handler* request)]
             (is (= 200 (:ring.response/status response)))
             (is (= "<p>This is a protected message that is only visible when sending the correct session header.</p>"
                    (:ring.response/body response))))))))))

;;deftest acquire-access-token-test
((t/join-fixtures [with-system-xt with-handler])
 (fn []
   (init/bootstrap!)

   ;; Create an authorization server (this can be promoted later)
   (book/protected-resource-preliminaries!)

   ;; Here's the authorize action
   (init/put!
    {:xt/id "https://site.test/actions/oauth/authorize"
     :juxt.site.alpha/type "https://meta.juxt.site/pass/action"
     :juxt.flip.alpha/quotation
     `(
       ;; Do the implicit grant
       (site/with-fx-acc
         [(site/push-fx
           (f/dip [(site/set-status 201)]))]))

     :juxt.pass.alpha/rules
     '[
       [(allowed? subject resource permission)
        [subject :juxt.pass.alpha/user-identity id]
        [id :juxt.pass.alpha/user user]
        [permission :juxt.pass.alpha/user user]]]})

   (init/put!
    {:juxt.http.alpha/content-type "text/html;charset=utf-8",
     :juxt.http.alpha/content "<p>Welcome to the Site authorization server.</p>",
     :juxt.site.alpha/methods
     {:get #:juxt.pass.alpha{:actions #{"https://site.test/actions/oauth/authorize"}}}
     :xt/id "https://site.test/authorize"})

   ;; Create a user Alice, with her identity
   (book/users-preliminaries!)
   (book/put-user-alice!)
   (book/create-action-put-basic-user-identity!)
   (book/grant-permission-to-invoke-action-put-basic-user-identity!)
   (book/put-basic-user-identity-alice!)

   ;; Log her in
   (book/create-action-login!)
   (book/grant-permission-to-invoke-action-login!)
   (let [login-log-entry
         (authz/do-action
          (let [xt-node *xt-node*
                body (.getBytes
                      (codec/form-encode
                       ;; usernames are case-insensitive - testing this
                       {"username" "aliCe"
                        "password" "garden"}))]
            {::site/xt-node xt-node
             ::site/db (xt/db xt-node)
             ::pass/subject nil      ; there is no subject at the point of login
             ::pass/action "https://site.test/actions/login"
             ::site/base-uri "https://site.test"
             ::site/received-representation
             {::http/content-type "application/x-www-form-urlencoded"
              ::http/body body}}))

         cookies
         (as-> {} %
           (authz/apply-request-context-operations
            %
            (-> login-log-entry ::pass/action-result ::site/apply-to-request-context-ops))
           (:ring.response/headers %)
           (keep (fn [[k v]] (when (= k "set-cookie") (next (re-matches #"([a-z]+?)=(.*?);.*" v)))) %)
           (map vec %)
           (into {} %))

         ;; GET on https://site.test/authorize
         req {:ring.request/method :get
              :ring.request/path "/authorize"
              :ring.request/headers {"cookie" (apply str (interpose ";" (map (fn [[id v]] (format "%s=%s" id v)) cookies)))}
              ;;:ring.request/query "return-to=/document.html"
              }

         response-pre-grant (*handler* req)

         _ (authz/do-action
            (let [xt-node *xt-node*]
              {::site/xt-node xt-node
               ::site/db (xt/db xt-node)
               ::pass/subject "https://site.test/subjects/system"
               ::pass/action "https://site.test/actions/grant-permission"
               ::site/base-uri "https://site.test"
               ::site/received-representation
               {::http/content-type "application/edn"
                ::http/body
                (.getBytes
                 (pr-str
                  {:xt/id "https://site.test/permissions/alice-can-authorize"
                   ::pass/action "https://site.test/actions/oauth/authorize"
                   ::pass/user "https://site.test/users/alice"
                   ::pass/purpose nil}))}}))

         response-post-grant (*handler* req)]

     (is (not= 200 (:ring.response/status response-pre-grant)))
     (is (= 200 (:ring.response/status response-post-grant)))

     ;; Now, run the implicit grant

     response-post-grant

     ;;(repl/e "https://site.test/authorize")

     )))
