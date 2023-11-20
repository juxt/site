;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.openid-connect-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [xtdb.api :as xt]
   [hato.client :as hc]
   [juxt.pass.alpha.authentication :as auth]
   [juxt.pass.alpha.openid-connect :as openid]
   [juxt.pass.alpha.util :as util]
   [juxt.pass.openid-connect-test-utils :as tutils]
   [juxt.site.alpha.init :as site-init]
   [juxt.test.util :refer [*xtdb-node* *handler* with-xtdb with-handler]
    :as test-util])
  (:import
   (com.auth0.jwt JWT)
   (java.time Instant)
   (java.util Date)))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-xtdb with-handler
  test-util/allow-access-to-public-resources!)

(deftest openid-auth0-login-redirect-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "auth0")]
    (site-init/install-openid-provider! *xtdb-node* "https://dev-14bkigf7.us.auth0.com/"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "auth0"
             :issuer-id "https://dev-14bkigf7.us.auth0.com/"
             :client-id "clientidihgfedcba123456789"
             :client-secret "clientsecretihgfedcba123456789"}})

  (testing
   (let [resp (with-redefs
               [util/make-nonce tutils/mock-make-nonce]
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/_site/openid/auth0/login"}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (= "https://example.org/site-session/cccccccccccccccc" (:xt/id session)))
     (is (= "aaaaaaaa" (get session ::pass/state)))
     (is (= "bbbbbbbbbbbb" (get session ::pass/nonce)))
     (is (nil? (get session ::pass/return-to)))

     (is (= 303 (:ring.response/status resp)))
     (is (= "https://dev-14bkigf7.us.auth0.com/authorize?response_type=code&scope=openid&client_id=clientidihgfedcba123456789&redirect_uri=https%3A%2F%2Fexample.org%2F_site%2Fopenid%2Fauth0%2Fcallback&state=aaaaaaaa&nonce=bbbbbbbbbbbb&prompt=login"
            (get-in resp [:ring.response/headers "location"]))))))

(deftest openid-aws-cognito-login-redirect-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "clientid123456789abcdefghi"
             :client-secret "clientsecret123456789abcdefghi"}})

  (testing
   (let [resp (with-redefs
               [util/make-nonce tutils/mock-make-nonce]
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/_site/openid/aws-cognito/login"}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (= "https://example.org/site-session/cccccccccccccccc" (:xt/id session)))
     (is (= "aaaaaaaa" (get session ::pass/state)))
     (is (= "bbbbbbbbbbbb" (get session ::pass/nonce)))
     (is (nil? (get session ::pass/return-to)))

     (is (= 303 (:ring.response/status resp)))
     (is (= "https://excel.auth.us-east-2.amazoncognito.com/logout?response_type=code&scope=openid&client_id=clientid123456789abcdefghi&redirect_uri=https%3A%2F%2Fexample.org%2F_site%2Fopenid%2Faws-cognito%2Fcallback&state=aaaaaaaa&nonce=bbbbbbbbbbbb&prompt=login"
            (get-in resp [:ring.response/headers "location"]))))))

(deftest openid-login-with-return-to-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "clientid123456789abcdefghi"
             :client-secret "clientsecret123456789abcdefghi"}})

  (testing
   (let [resp (with-redefs
               [util/make-nonce tutils/mock-make-nonce]
                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/_site/openid/aws-cognito/login"
                  :ring.request/query "return-to=http://example.org/home.html"}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (= "https://example.org/site-session/cccccccccccccccc" (:xt/id session)))
     (is (= "aaaaaaaa" (get session ::pass/state)))
     (is (= "bbbbbbbbbbbb" (get session ::pass/nonce)))
     (is (= "http://example.org/home.html" (get session ::pass/return-to)))

     (is (= 303 (:ring.response/status resp)))
     (is (= "https://excel.auth.us-east-2.amazoncognito.com/logout?response_type=code&scope=openid&client_id=clientid123456789abcdefghi&redirect_uri=https%3A%2F%2Fexample.org%2F_site%2Fopenid%2Faws-cognito%2Fcallback&state=aaaaaaaa&nonce=bbbbbbbbbbbb&prompt=login"
            (get-in resp [:ring.response/headers "location"])))
     (is (= "id=cccccccccccccccc; Path=/; Secure; HttpOnly; SameSite=Lax"
            (get-in resp [:ring.response/headers "set-cookie"]))))))

(deftest openid-login-missing-client-id-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-secret "clientsecret123456789abcdefghi"}})

  (testing
   (let [resp (*handler*
               {:ring.request/method :get
                :ring.request/path "/_site/openid/aws-cognito/login"})]
     (is (= "No oauth client id found" (get-in resp [::site/errors :message])))
     (is (= 500 (:ring.response/status resp))))))

(deftest openid-login-missing-configuration-test
  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "clientid123456789abcdefghi"
             :client-secret "clientsecret123456789abcdefghi"}})

  (testing
   (let [resp (*handler*
               {:ring.request/method :get
                :ring.request/path "/_site/openid/aws-cognito/login"})]
     (is (= "No openid configuration found" (get-in resp [::site/errors :message])))
     (is (= 500 (:ring.response/status resp))))))

(deftest openid-aws-cognito-callback-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "3aaocetnk8d2941585k6nbbckc"
             :client-secret "clientsecret123456789abcdefghi"}})

  (test-util/submit-and-await!
   [[:crux.tx/put
     {:xt/id "https://example.org/_site/roles/superuser"
      ::site/type "Role"
      :name "superuser"
      :description "Superuser"}]])

  (auth/put-session!
   {::site/xtdb-node *xtdb-node*
    ::site/base-uri "https://example.org"
    ::site/start-date (Date.)}
   "current-session"
   {::pass/state "auth-flow-state"
    ::pass/nonce "4f689a2585601d8050dc3cec"
    :expires_in 3600})
; TODO: fails because start-date (handler.clj:1260) gets initialized with current date, not changed
; hence generated tokens always immediately expire. Need to either inject fixed test time or not expire
; login tokens
  (testing
   (let [resp (with-redefs
               [hc/get (tutils/mock-openid-jwks-req "aws-cognito")
                hc/post (tutils/mock-openid-token-req "aws-cognito")
                auth/access-token (constantly "new-session")
                openid/jwt-verifier
                (fn [algorithm issuer]
                  (.. (JWT/require algorithm)
                      (withIssuer issuer)
                      (acceptLeeway 1)
                      (build (reify com.auth0.jwt.interfaces.Clock
                               (getToday [_]
                                 ; Token is valid from 2023-01-16T15:44:47.00Z and lasts an hour
                                 ; so set time of validation to 2023-01-16T15:45:00.00Z
                                 (Date/from (Instant/parse "2023-01-16T15:45:00.00Z")))))))]

                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/_site/openid/aws-cognito/callback"
                  :ring.request/query "state=auth-flow-state&code=randomcode"
                  :ring.request/headers {"cookie" "id=current-session"}}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (not= "https://example.org/site-session/current-session" (:xt/id session)))
     (is (= "https://example.org/site-session/new-session" (:xt/id session)))
     (is (= "https://example.org/_site/users/exceladmin" (::pass/user session)))

     (is (= 303 (:ring.response/status resp)))
     (is (= "/?code=new-session"
            (get-in resp [:ring.response/headers "location"])))

     (is (= (xt/entity db "https://example.org/_site/users/exceladmin")
            {:xt/id "https://example.org/_site/users/exceladmin"
             ::site/type "User"
             ::pass/username "exceladmin"
             :name "Excel Admin"
             :email "fwc+exceladmin@juxt.pro"}))
     (is (= (xt/entity db "https://example.org/_site/users/exceladmin/oauth-credentials")
            {:xt/id "https://example.org/_site/users/exceladmin/oauth-credentials"
             ::site/type "OAuthCredentials"
             ::pass/user "https://example.org/_site/users/exceladmin"
             :juxt.pass.jwt/iss "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :juxt.pass.jwt/sub "2353661c-e432-464f-9b22-e908ddd920e5"}))
     (is (some? (xt/entity db "https://example.org/_site/roles/superuser")))
     (is (= (xt/entity db "https://example.org/_site/roles/superuser/users/exceladmin")
            {:xt/id "https://example.org/_site/roles/superuser/users/exceladmin",
             ::site/type "UserRoleMapping",
             ::pass/assignee "https://example.org/_site/users/exceladmin",
             ::pass/role "https://example.org/_site/roles/superuser"})))))

(deftest openid-aws-cognito-callback-no-role-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "3aaocetnk8d2941585k6nbbckc"
             :client-secret "clientsecret123456789abcdefghi"}})

  (auth/put-session!
   {::site/xtdb-node *xtdb-node*
    ::site/base-uri "https://example.org"
    ::site/start-date (Date.)}
   "current-session"
   {::pass/state "auth-flow-state"
    ::pass/nonce "4f689a2585601d8050dc3cec"
    :expires_in 3600})

  (testing
   (let [resp (with-redefs
               [hc/get (tutils/mock-openid-jwks-req "aws-cognito")
                hc/post (tutils/mock-openid-token-req "aws-cognito")
                auth/access-token (constantly "new-session")
                openid/jwt-verifier
                (fn [algorithm issuer]
                  (.. (JWT/require algorithm)
                      (withIssuer issuer)
                      (acceptLeeway 1)
                      (build (reify com.auth0.jwt.interfaces.Clock
                               (getToday [_]
                                 ; Token is valid from 2023-01-16T15:44:47.00Z and lasts an hour
                                 ; so set time of validation to 2023-01-16T15:45:00.00Z
                                 (Date/from (Instant/parse "2023-01-16T15:45:00.00Z")))))))]

                (*handler*
                 {:ring.request/method :get
                  :ring.request/path "/_site/openid/aws-cognito/callback"
                  :ring.request/query "state=auth-flow-state&code=randomcode"
                  :ring.request/headers {"cookie" "id=current-session"}}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (not= "https://example.org/site-session/current-session" (:xt/id session)))
     (is (= "https://example.org/site-session/new-session" (:xt/id session)))
     (is (= "https://example.org/_site/users/exceladmin" (::pass/user session)))

     (is (= 303 (:ring.response/status resp)))
     (is (= "/?code=new-session"
            (get-in resp [:ring.response/headers "location"])))

     (is (= (xt/entity db "https://example.org/_site/users/exceladmin")
            {:xt/id "https://example.org/_site/users/exceladmin"
             ::site/type "User"
             ::pass/username "exceladmin"
             :name "Excel Admin"
             :email "fwc+exceladmin@juxt.pro"}))
     (is (= (xt/entity db "https://example.org/_site/users/exceladmin/oauth-credentials")
            {:xt/id "https://example.org/_site/users/exceladmin/oauth-credentials"
             ::site/type "OAuthCredentials"
             ::pass/user "https://example.org/_site/users/exceladmin"
             :juxt.pass.jwt/iss "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :juxt.pass.jwt/sub "2353661c-e432-464f-9b22-e908ddd920e5"}))
     (is (nil? (xt/entity db "https://example.org/_site/roles/superuser")))
     (is (nil? (xt/entity db "https://example.org/_site/roles/superuser/user/exceladmin"))))))

(deftest openid-aws-cognito-full-flow-test
  (with-redefs
   [slurp (tutils/mock-openid-configuration-req "aws-cognito")]
    (site-init/install-openid-provider! *xtdb-node* "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"))

  (site-init/install-openid-resources!
   *xtdb-node*
   {::site/base-uri "https://example.org"
    :openid {:name "aws-cognito"
             :issuer-id "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :client-id "3aaocetnk8d2941585k6nbbckc"
             :client-secret "clientsecret123456789abcdefghi"}})

  (test-util/submit-and-await!
   [[:crux.tx/put
     {:xt/id "https://example.org/_site/roles/superuser"
      ::site/type "Role"
      :name "superuser"
      :description "Superuser"}]])

  (testing
   (let [login-resp
         (with-redefs
          [util/make-nonce (fn [len]
                             (case len
                               8 "aaaaaaaa"
                               12 "4f689a2585601d8050dc3cec"
                               16 "cccccccccccccccc"
                               :else "x"))]
           (*handler*
            {:ring.request/method :get
             :ring.request/path "/_site/openid/aws-cognito/login"}))
         callback-resp
         (with-redefs
          [hc/get (tutils/mock-openid-jwks-req "aws-cognito")
           hc/post (tutils/mock-openid-token-req "aws-cognito")
           auth/access-token (constantly "dddddddddddddddd")
           openid/jwt-verifier
           (fn [algorithm issuer]
             (.. (JWT/require algorithm)
                 (withIssuer issuer)
                 (acceptLeeway 1)
                 (build (reify com.auth0.jwt.interfaces.Clock
                          (getToday [_]
                            ; Token is valid from 2023-01-16T15:44:47.00Z and lasts an hour
                            ; so set time of validation to 2023-01-16T15:45:00.00Z
                            (Date/from (Instant/parse "2023-01-16T15:45:00.00Z")))))))]

           (*handler*
            {:ring.request/method :get
             :ring.request/path "/_site/openid/aws-cognito/callback"
             :ring.request/query "state=aaaaaaaa&code=randomcode"
             :ring.request/headers {"cookie" (get-in login-resp [:ring.response/headers "set-cookie"])}}))
         db (xt/db *xtdb-node*)
         sessions (test-util/query-sessions db)
         session (first sessions)]

     (is (= 1 (count sessions)))
     (is (not= "https://example.org/site-session/cccccccccccccccc" (:xt/id session)))
     (is (= "https://example.org/site-session/dddddddddddddddd" (:xt/id session)))
     (is (= "https://example.org/_site/users/exceladmin" (::pass/user session)))

     (is (= 303 (:ring.response/status callback-resp)))
     (is (= "/?code=dddddddddddddddd"
            (get-in callback-resp [:ring.response/headers "location"])))

     (is (= (xt/entity db "https://example.org/_site/users/exceladmin")
            {:xt/id "https://example.org/_site/users/exceladmin"
             ::site/type "User"
             ::pass/username "exceladmin"
             :name "Excel Admin"
             :email "fwc+exceladmin@juxt.pro"}))
     (is (= (xt/entity db "https://example.org/_site/users/exceladmin/oauth-credentials")
            {:xt/id "https://example.org/_site/users/exceladmin/oauth-credentials"
             ::site/type "OAuthCredentials"
             ::pass/user "https://example.org/_site/users/exceladmin"
             :juxt.pass.jwt/iss "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6"
             :juxt.pass.jwt/sub "2353661c-e432-464f-9b22-e908ddd920e5"}))
     (is (some? (xt/entity db "https://example.org/_site/roles/superuser")))
     (is (= (xt/entity db "https://example.org/_site/roles/superuser/users/exceladmin")
            {:xt/id "https://example.org/_site/roles/superuser/users/exceladmin",
             ::site/type "UserRoleMapping",
             ::pass/assignee "https://example.org/_site/users/exceladmin",
             ::pass/role "https://example.org/_site/roles/superuser"})))))
