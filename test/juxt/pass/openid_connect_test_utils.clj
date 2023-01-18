(ns juxt.pass.openid-connect-test-utils
  (:require
   [clojure.test :refer [is]]
   [jsonista.core :as json]))

(def mock-openid-configuration
  {"auth0"
   {:url
    "https://dev-14bkigf7.us.auth0.com/.well-known/openid-configuration"
    :doc
    "{\"issuer\":\"https://dev-14bkigf7.us.auth0.com/\",
      \"authorization_endpoint\":\"https://dev-14bkigf7.us.auth0.com/authorize\",
      \"token_endpoint\":\"https://dev-14bkigf7.us.auth0.com/oauth/token\",
      \"device_authorization_endpoint\":\"https://dev-14bkigf7.us.auth0.com/oauth/device/code\",
      \"userinfo_endpoint\":\"https://dev-14bkigf7.us.auth0.com/userinfo\",
      \"mfa_challenge_endpoint\":\"https://dev-14bkigf7.us.auth0.com/mfa/challenge\",
      \"jwks_uri\":\"https://dev-14bkigf7.us.auth0.com/.well-known/jwks.json\",
      \"registration_endpoint\":\"https://dev-14bkigf7.us.auth0.com/oidc/register\",
      \"revocation_endpoint\":\"https://dev-14bkigf7.us.auth0.com/oauth/revoke\",
      \"scopes_supported\":[\"openid\",\"profile\",\"offline_access\",\"name\",\"given_name\",\"family_name\",\"nickname\",\"email\",\"email_verified\",\"picture\",\"created_at\",\"identities\",\"phone\",\"address\"],
      \"response_types_supported\":[\"code\",\"token\",\"id_token\",\"code token\",\"code id_token\",\"token id_token\",\"code token id_token\"],
      \"code_challenge_methods_supported\":[\"S256\",\"plain\"],
      \"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],
      \"subject_types_supported\":[\"public\"],
      \"id_token_signing_alg_values_supported\":[\"HS256\",\"RS256\"],
      \"token_endpoint_auth_methods_supported\":[\"client_secret_basic\",\"client_secret_post\"],
      \"claims_supported\":[\"aud\",\"auth_time\",\"created_at\",\"email\",\"email_verified\",\"exp\",\"family_name\",\"given_name\",\"iat\",\"identities\",\"iss\",\"name\",\"nickname\",\"phone_number\",\"picture\",\"sub\"],
      \"request_uri_parameter_supported\":false,
      \"request_parameter_supported\":false}"}

   "aws-cognito"
   {:url
    "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6/.well-known/openid-configuration"
    :doc
    "{\"authorization_endpoint\":\"https://excel.auth.us-east-2.amazoncognito.com/oauth2/authorize\",
      \"id_token_signing_alg_values_supported\":[\"RS256\"],
      \"issuer\":\"https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6\",
      \"jwks_uri\":\"https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ccXNbbzY6/.well-known/jwks.json\",
      \"response_types_supported\":[\"code\",\"token\"],
      \"scopes_supported\":[\"openid\",\"email\",\"phone\",\"profile\"],
      \"subject_types_supported\":[\"public\"],
      \"token_endpoint\":\"https://excel.auth.us-east-2.amazoncognito.com/oauth2/token\",
      \"token_endpoint_auth_methods_supported\":[\"client_secret_basic\",\"client_secret_post\"],
      \"userinfo_endpoint\":\"https://excel.auth.us-east-2.amazoncognito.com/oauth2/userInfo\"}"}})

(defn mock-openid-configuration-req
  [provider]
  (let [{:keys [url doc]} (get mock-openid-configuration provider)]
    (fn [conf-url]
      (is (= url conf-url))
      doc)))

(defn mock-make-nonce
  [len]
  (case len
    8 "aaaaaaaa"
    12 "bbbbbbbbbbbb"
    16 "cccccccccccccccc"
    :else "x"))

(def mock-openid-jwks
  {"aws-cognito"
   "{\"keys\":[{\"alg\":\"RS256\",
                \"e\":\"AQAB\",
                \"kid\":\"HfPNX7FSg4eQfhZXeWT9vZYjzr148oIaAYS72e50xys=\",
                \"kty\":\"RSA\",
                \"n\":\"00mEv_bfufZMBPWrSjzX5x8xHiqR963pYeGud0aTXpv5ujYjWGYFchhibvQWEu0Llv3J87NPe2v3nCBWgOzZeSDBYTVbttNS96ZZZnG-N5pGC-3rGR6jbqQGcqDWX36i1c_ynqKdCkknARipbA95dsbUHsKB6YZGxqBe3gnA9twWPNMppEOKtD4ZUi78moF8JB0PNLZM_kVa0AD6fvHB26PkFu9k_YdQ4EoC-HRPJKVVO_IRvxGKvnR9PMLQ7z-Jz4Bqcbo67wR0QuouISC1iFjY07FC7mfKGywhuU8oLFDaw5ZOxuSFF8jSjA35EM7nS5ukcChTCSwzRjlUpJ0ndw\",
                \"use\":\"sig\"},
               {\"alg\":\"RS256\",
                \"e\":\"AQAB\",
                \"kid\":\"31uL7Hv7H6CMEDwLnS+qwVVsLqIghtqplWVGxIMqsmQ=\",
                \"kty\":\"RSA\",
                \"n\":\"1wXQyFM5VBnnTNmsKuIowz-XXOBGNrZXzQGrar_1tyNtsDXhBV1semfN6aKTPJn1BZ-5P3brKcYYDOaAB0TyeZ4fdBGUc8Rj07T43VyBWtFtC_xFqm5nI1UfrYjXvc7f4JN3ZNqatYuQT1C_MUn1uqlHV4_5b-ECK6ct1FYgDBANgjFLuysmoml6vXTPUpXsKENz56YgMNuhixXYT-G-0olTIwPlDyWPfXTAeazRs-YqRQk_xiALiNDjyFywivtKXqZ1mldxQarWl81sm3_kxHtc1d5pNFIGW2D-RBxDUmNUEM-JGNWP636XzQWL97E_shHryQJzxUyCn0EuM3PBFw\",
                \"use\":\"sig\"}]}\n"})

(defn mock-openid-jwks-req
  [provider]
  (let [{:keys [doc]} (get mock-openid-configuration provider)
        url (get (json/read-value doc) "jwks_uri")]
    (fn [jwks-endpoint {:keys [accept]}]
      (is (= url jwks-endpoint))
      (is (= :json accept))
      {:status 200
       :body (get mock-openid-jwks provider)})))

(def mock-openid-token
  {"aws-cognito"
   "{\"id_token\":\"eyJraWQiOiJIZlBOWDdGU2c0ZVFmaFpYZVdUOXZaWWp6cjE0OG9JYUFZUzcyZTUweHlzPSIsImFsZyI6IlJTMjU2In0.eyJhdF9oYXNoIjoia2FMbWhFeG85T3cxZlJRaXdMOV9lZyIsInN1YiI6IjIzNTM2NjFjLWU0MzItNDY0Zi05YjIyLWU5MDhkZGQ5MjBlNSIsImNvZ25pdG86Z3JvdXBzIjpbInN1cGVydXNlciJdLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMi5hbWF6b25hd3MuY29tXC91cy1lYXN0LTJfY2NYTmJielk2IiwiY29nbml0bzp1c2VybmFtZSI6ImV4Y2VsYWRtaW4iLCJub25jZSI6IjRmNjg5YTI1ODU2MDFkODA1MGRjM2NlYyIsIm9yaWdpbl9qdGkiOiJkNTlkZTY5YS03MDgxLTQ5MTgtYmY4NC04MjE4YjU1ZGZjNDciLCJhdWQiOiIzYWFvY2V0bms4ZDI5NDE1ODVrNm5iYmNrYyIsImV2ZW50X2lkIjoiNGRkYTQ2ZjQtNGU5Yi00ZWE5LTk1MzktMDY2OTFlMjNlOTJiIiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NzM4ODM4ODcsIm5hbWUiOiJFeGNlbCBBZG1pbiIsImV4cCI6MTY3Mzg4NzQ4NywiaWF0IjoxNjczODgzODg3LCJqdGkiOiJiODQ2YTYwZi01ZWM1LTQ4ZDgtYTQyZC0yNDAzMzIxNWMzNGMiLCJlbWFpbCI6ImZ3YytleGNlbGFkbWluQGp1eHQucHJvIn0.qCa3GvhAcbxy4f8sgjPQX-6OHPlmaV03shsdNuCQP6hp0u3MsuoVZIhHVv0kIBikB-94NJsFGAqqKASZ9P6Z8OClHzcQsRC2pad0WAUL1tne_-4o309AvCAdeGOTcgzcwdVk1wPFH9rBYYEF-0ou0wzpPOHx8CTRhTqHYcKAHxVz_rleDkp8LRuRUkG0vz4DcOeaF7Fj6nCDuFmYQag2ZgEHjM6iPJdovyZDYwIUjUCwl6jHNL1meNWlQEvMi0_xVlozSs4y6P58Drafhwk2P8wL9ZpTG0Et3QhhHXvdQs1xBVNQlSbmxBIIZ9jPiejkOh60kQeB8SVRSsT5HgXzLA\",
     \"access_token\":\"omitted-content\",
     \"refresh_token\":\"omitted-content\",
     \"expires_in\":3600,
     \"token_type\":\"Bearer\"}"})

(defn mock-openid-token-req
  [provider]
  (let [{:keys [doc]} (get mock-openid-configuration provider)
        url (get (json/read-value doc) "token_endpoint")]
    (fn [token-endpoint {:keys [form-params accept]}]
      (is (= url token-endpoint))
      (is (= "authorization_code" (get form-params "grant_type")))
      (is (= :json accept))
      {:status 200
       :body (get mock-openid-token provider)})))
