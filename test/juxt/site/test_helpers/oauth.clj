;; Copyright © 2022, JUXT LTD.

(ns juxt.site.test-helpers.oauth
  (:require
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [juxt.site.util :refer [make-nonce]]
   [juxt.test.util :refer [*handler*]]
   [malli.core :as malli]
   [ring.util.codec :as codec]))

(defn authorize-response!
  "Authorize response"
  [{:juxt.site/keys [session-token]
    client-id "client_id"
    scope "scope"}]
  (let [state (make-nonce 10)
        request
        {:ring.request/method :get
         :juxt.site/uri "https://example.org/oauth/authorize"
         :ring.request/headers {"cookie" (format "id=%s" session-token)}
         :ring.request/query
         (codec/form-encode
          (cond->
              {"response_type" "token"
               "client_id" client-id
               "state" state}
              scope (assoc "scope" (codec/url-encode (str/join " " scope)))))}]
    (*handler* request)))

(malli/=>
 authorize-response!
 [:=> [:cat
       [:map
        ["client_id" :string]
        ["scope" {:optional true} [:sequential :string]]]]
  [:map
   ["access_token" {:optional true} :string]
   ["error" {:optional true} :string]]])

(defn authorize!
  "Authorize an application, and return decoded fragment parameters as a string->string map"
  [args]
  (let [response (authorize-response! args)
        _ (case (:ring.response/status response)
            (302 303) :ok
            400 (throw (ex-info "Client error" (assoc args :response response)))
            403 (throw (ex-info "Forbidden to authorize" (assoc args :response response)))
            (throw (ex-info "Unexpected error" (assoc args :response response))))

        location-header (-> response :ring.response/headers (get "location"))

        [_ _ encoded-access-token]
        (re-matches #"https://(.*?)/.*?#(.*)" location-header)]

    (when-not encoded-access-token
      (throw (ex-info "No access-token fragment" {:response response})))

    (codec/form-decode encoded-access-token)))

(malli/=>
 authorize!
 [:=> [:cat
       [:map
;;        ^{:doc "to authenticate with authorization server"} [:juxt.site/session-token :string]
        ["client_id" :string]
        ["scope" {:optional true} [:sequential :string]]]]
  [:map
   ["access_token" {:optional true} :string]
   ["error" {:optional true} :string]]])
