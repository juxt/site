;; Copyright © 2022, JUXT LTD.

(ns juxt.book-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.book :as book]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db* *handler*] :as tutil]
   [juxt.site.alpha.repl :refer [put!]]
   [xtdb.api :as xt]
   [juxt.pass.alpha.cookie-scope :as cookie-scope]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.procedure :as proc]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.pass.alpha.http-authentication :as authn]
   [clojure.string :as str]
   [malli.util :as mu]
   [xtdb.api :as x]))

(defn with-handler [f]
  (binding [*handler*
            (tutil/make-handler
             {::site/xt-node *xt-node*
              ::site/base-uri "https://site.test"
              ::site/uri-prefix "https://site.test"})]
    (f)))

(use-fixtures :each with-system-xt with-handler)

(deftest not-found-test
  (book/preliminaries!)
  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}
        invalid-req (assoc req :ring.request/path "/not-hello")]
    (is (= 404 (:ring.response/status (*handler* invalid-req))))))

(deftest public-resource-test
  (book/preliminaries!)
  (book/setup-hello-world!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/hello")) ;; Assert the entity exists in the db
  (is (not (xt/entity (xt/db *xt-node*) "https://site.test/not-hello"))) ;; Assert that out 404 entity is not in the db

  (let [req {:ring.request/method :get
             :ring.request/path "/hello"}]

    (testing "Can retrieve a public immutable resource"
      (let [{:ring.response/keys [status body]} (*handler* req)]
        (is (= 200 status))
        (is (= "Hello World!\r\n" body))))

    (testing "Receive 405 when method not allowed"
      (let [invalid-req (assoc req :ring.request/method :put)
            {:ring.response/keys [status]} (*handler* invalid-req)]
        (is (= 405 status))))

    (testing "Receive 404 when resource does not exist"
      (let [invalid-req (assoc req :ring.request/path "/not-hello")
            {:ring.response/keys [status]} (*handler* invalid-req)]
        (is (= 404 status))))))

(defn encode-basic-authorization [user password]
  (format "Basic %s" (String. (.encode (java.util.Base64/getEncoder) (.getBytes (format "%s:%s" user password))))))

(deftest protected-resource-with-http-basic-auth-test
  (book/preliminaries!)
  (book/protected-resource-preliminaries!)
  (book/protection-spaces-preliminaries!)

  (book/book-create-resource-protected-by-basic-auth!)
  (book/book-grant-permission-to-resource-protected-by-basic-auth!)
  (book/book-put-basic-protection-space!)

  (book/book-put-basic-auth-user-identity!)

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

(deftest cookie-scope-test
  (book/preliminaries!)
  (book/protected-resource-preliminaries!)

  (book/cookies-scopes-preliminaries!)

  (book/book-create-resource-protected-by-cookie-scope!)
  (book/book-grant-permission-to-resource-protected-by-cookie-scope!)
  (book/book-create-cookie-scope!)

  (let [uri (some :juxt.pass.alpha/login-uri
                  (cookie-scope/cookie-scopes (xt/db *xt-node*) "https://site.test/protected-by-cookie-scope/document.html"))]
    (is (string? uri)))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-cookie-scope/document.html"}]
    (testing "Redirect"
      (let [response (*handler* request)]
        (is (= 302 (:ring.response/status response)))
        (is (= "https://site.test/login.html" (get-in response [:ring.response/headers "location"]))))))


  ;; POST to a login resource which we create a session - this is done in session
  )

(deftest protected-resource-with-http-bearer-auth-test
  (book/preliminaries!)
  (book/protected-resource-preliminaries!)
  (book/protection-spaces-preliminaries!)

  (book/applications-preliminaries!)
  (book/setup-application!)

  (book/book-create-resource-protected-by-bearer-auth!)
  (book/book-grant-permission-to-resource-protected-by-bearer-auth!)
  (book/book-put-bearer-protection-space!)

  (is (xt/entity (xt/db *xt-node*) "https://site.test/protected-by-bearer-auth/document.html"))

  (let [request {:ring.request/method :get
                 :ring.request/path "/protected-by-bearer-auth/document.html"}]

    (testing "Cannot be accessed without a bearer token"
      (let [response (*handler* request)]
        (is (= 401 (:ring.response/status response)))
        (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))))

    (testing "Can be accessed with a valid bearer token"
      (let [response (*handler*
                      (assoc
                       request
                       :ring.request/headers
                       {"authorization" "Bearer test-access-token"}))]
        (is (= 200 (:ring.response/status response)))
        (is (nil? (get-in response [:ring.response/headers "www-authenticate"])))))

    (testing "Cannot be accessed with an invalid bearer token"
      (let [response (*handler*
                      (assoc
                       request
                       :ring.request/headers
                       {"authorization" "Bearer not-test-access-token"}))]
        (is (= 401 (:ring.response/status response)))
        (is (= "Bearer realm=Wonderland" (get-in response [:ring.response/headers "www-authenticate"])))))))

(deftest user-directory-test
  (book/preliminaries!)
  (book/protected-resource-preliminaries!)
  (book/applications-preliminaries!)
  (book/setup-application!)
  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/create-action"
   {:xt/id "https://site.test/actions/put-user-owned-content"
    :juxt.pass.alpha/scope "write:user-content"
    :juxt.pass.alpha/rules
    [
     '[(allowed? permission subject action resource)
       [permission ::pass/user user]
       [subject ::pass/user-identity id]
       [id ::pass/user user]
       [resource :owner user]]]})

  (repl/do-action
   "https://site.test/subjects/repl-default"
   "https://site.test/actions/grant-permission"
   {:xt/id "https://site.test/permissions/put-user-owned-content"
    :juxt.pass.alpha/action "https://site.test/actions/put-user-owned-content"
    :juxt.pass.alpha/user "https://site.test/users/alice"
    :juxt.pass.alpha/purpose nil})

  ;; Both bob and alice have user directories
  (doseq [user #{"bob" "alice"}]
    (repl/put!
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
             {"authorization" "Bearer test-access-token"}}]
    (is (= 403 (:ring.response/status (*handler* req)))))

  ;; When Alice writing to Alice's user directory, we get through security.
  (let [req {:ring.request/method :put
             :ring.request/path "/~alice/index.html"
             :ring.request/headers
             {"authorization" "Bearer test-access-token"}}]
    (is (= 411 (:ring.response/status (*handler* req))))))

(comment
  ((t/join-fixtures [with-system-xt with-handler])
   (fn []
     (book/preliminaries!)
     (book/protected-resource-preliminaries!)

     (book/cookies-scopes-preliminaries!)

     (book/book-create-resource-protected-by-cookie!)
     (book/book-grant-permission-to-resource-protected-by-cookie!)
     (book/book-create-cookie-scope!)

     (book/book-put-basic-auth-user-identity!)

     (let [uri (some :juxt.pass.alpha/login-uri
                     (cookie-scope/cookie-scopes (xt/db *xt-node*) "https://site.test/protected-by-cookie/document.html"))]
       (is (string? uri)))

     ;; There is no cookie at present, so no session, so we're expecting a
     ;; redirect to a login form.
     (let [request {:ring.request/method :get
                    :ring.request/path "/protected-by-cookie/document.html"}]

       (testing "Redirect"
         (let [response (*handler* request)]
           (is (= 302 (:ring.response/status response)))
           (is (= "https://site.test/login.html" (get-in response [:ring.response/headers "location"]))))))

     ;; Create a new resource /login resource
     ;; TODO: Put in an action
     (put! {:xt/id "https://site.test/login"
            ::site/methods
            {:post {::site/acceptable
                    {"accept" "application/x-www-form-urlencoded"}
                    ::pass/actions
                    #{ ;; We must create this action
                      "https://site.test/actions/login"}}}})

     ;; Grant permission for anyone to access the login handler
     (repl/do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/grant-permission"
      {:xt/id "https://site.test/permissions/login"
       :juxt.pass.alpha/action "https://site.test/actions/login"
       :juxt.pass.alpha/purpose nil})

     ;; Create login action
     (repl/do-action
      "https://site.test/subjects/repl-default"
      "https://site.test/actions/create-action"
      {:xt/id "https://site.test/actions/login"

       ;; TODO: Replace with 'cold' and 'hot' steps - cold steps run before
       ;; head-of-line, hot steps run /at/ head-of-line
       :juxt.pass.alpha/procedure
       [
        [::proc/validate
         [:map
          ["username" [:string {:min 1}]]
          ["password" [:string {:min 1}]]]]

        [::proc/nest :input]

        [::proc/find-matching-identity-on-password
         :juxt.pass.alpha/identity
         {:username-in-identity-key :juxt.pass.alpha/username
          :path-to-username [:input "username"]
          :password-hash-in-identity-key :juxt.pass.alpha/password-hash
          :path-to-password [:input "password"]}]

        [::proc/merge
         {:juxt.site.alpha/type "https://meta.juxt.site/pass/subject"}]

        ^{:doc "Add an id"}
        [::proc/merge {:xt/id "https://juxt.site/subjects/alice438348348"}]

        ^{:doc "Strip input"}
        [::proc/dissoc :input]

        [::proc/validate
         [:map {:closed true}
          [:xt/id [:string {:min 1}]]
          [:juxt.pass.alpha/identity :string]
          [:juxt.site.alpha/type [:= "https://meta.juxt.site/pass/subject"]]]]

        [::proc/db-single-put]

        ]

       :juxt.pass.alpha/rules
       '[
         [(allowed? permission subject action resource)
          [permission :xt/id]]]})


     ;; POST to the /login handler, which call the login action.
     ;; After this there should be a set-cookie escalation
     (let [body (.getBytes (ring.util.codec/form-encode {"username" "alice" "password" "garden"}))
             request {:ring.request/method :post
                      :ring.request/path "/login"
                      :ring.request/headers
                      {"content-length" (str (count body))
                       "content-type" "application/x-www-form-urlencoded"}
                      :ring.request/body (io/input-stream body)
                      }]
       (*handler* request)
       ;; TODO: Check for a correct set-cookie header
       )

     ;; TODO: Check the database for evidence a session has been created


     )))
