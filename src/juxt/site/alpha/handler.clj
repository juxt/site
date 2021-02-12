;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [crypto.password.bcrypt :as password]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value]]
   [juxt.pass.alpha :as pass]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.util :refer [assoc-when-some]]
   [juxt.spin.alpha :as spin]
   [juxt.spin.alpha.auth :as spin.auth]
   [juxt.spin.alpha.representation :as spin.representation]))

;; This deviates from Spin, but we want to upgrade Spin accordingly in the near
;; future. When that is done, this version can be removed and the function in
;; juxt.spin.alpha.negotiation used directly.
(defn negotiate-representation [request current-representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  (let [{selected-representation :juxt.pick.alpha/representation
         vary :juxt.pick.alpha/vary}
        (when (seq current-representations)
          ;; Call into pick which does the actual negotiation for us.
          (pick
           request
           (for [r current-representations]
             (assoc r :juxt.pick.alpha/representation-metadata
                    (-> {}
                        (assoc-when-some "content-type" (::spin/content-type r))
                        (assoc-when-some "content-encoding" (::spin/content-encoding r))
                        (assoc-when-some "content-language" (::spin/content-language r)))))
           {:juxt.pick.alpha/vary? true}))

        ;; Check for a 406 Not Acceptable
        _ (when (contains? #{:get :head} (:request-method request))
            (spin/check-not-acceptable! selected-representation))]

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc ::spin/vary (str/join ", " vary)))))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [request db]
  (or
   (when-let [resource (openapi/locate-resource request db)]
     resource)

   (crux/entity db (:uri request))

   {::site/description
    "Backstop resource indicating exhausted attempts to locate a resource."
    ::spin/methods #{:get :head :options}
    ::spin/representations []}))

(defn current-representations [db resource date]
  (or
   ;; Strategy 1: This resource could be manufactured by locate-resource. Allow
   ;; this to contain ::spin/representations
   (::spin/representations resource)

   ;; Strategy 2: Try looking up via 'mapping' entities in Crux
   (seq
    (map
     (comp #(crux/entity db %) first)
     (crux/q
      db
      '{:find [representation-id]
        :in [uri]
        :where [[resource ::spin/resource uri]
                [resource ::spin/representation representation-id]]}
      (:crux.db/id resource))))))

(defn GET [request resource date selected-representation db authorization]
  (let [representation-metadata-headers (response/representation-metadata-headers selected-representation)]
    (spin/evaluate-preconditions! request resource representation-metadata-headers date)

    ;; This is naïve, some representations won't just have bytes ready, they'll
    ;; need to be generated somehow
    (let [{::spin/keys [payload-header-fields bytes bytes-generator content charset]} selected-representation
          {::keys [path-item-object]} resource]

      (spin/response
       200
       representation-metadata-headers
       (response/payload-headers payload-header-fields)
       request
       nil
       date
       (cond
         content (.getBytes content (or charset "utf-8"))
         bytes bytes
         path-item-object (.getBytes (get-in path-item-object ["get" "description"]))
         bytes-generator (generate-representation-body request resource selected-representation db authorization))))))

(defn receive-representation [request resource date]
  (let [{metadata ::spin/representation-metadata
         payload ::spin/payload-header-fields
         :as rep}
        (spin.representation/receive-representation request resource date)]
    (-> rep
        (assoc-when-some ::spin/content-type (get metadata "content-type"))
        (assoc-when-some ::spin/content-encoding (get  metadata "content-encoding"))
        (assoc-when-some ::spin/content-language (get  metadata "content-language"))
        (assoc-when-some ::spin/content-length (some-> payload (get "content-length") Long/parseLong))
        ;; TODO: content-range?
        (dissoc ::spin/representation-metadata)
        (dissoc ::spin/payload-header-fields))))

(defn POST [request resource date crux-node]
  (let [new-representation (receive-representation request resource date)]
    (assert new-representation)
    ;; TODO: dispatch on something
    ))

(defn PUT [request resource selected-representation date crux-node]
  (let [new-representation (receive-representation request resource date)]

    (assert new-representation)

    ;; TODO: Promote into spin
    (when (get-in request [:headers "content-range"])
      (throw
       (ex-info
        "Content-Range header not allowed on a PUT request"
        {::spin/response
         {:status 400
          :body "Bad Request\r\n"}})))

    ;; TODO: evaluate preconditions in tx fn!
    (case (::spin/content-type new-representation)

      "application/vnd.oai.openapi+json;version=3.0.2"
      (openapi/put-openapi
       request resource new-representation selected-representation crux-node)

      "application/json"
      (openapi/put-json-representation
       request resource new-representation selected-representation crux-node)

      (throw
       (ex-info
        "Unsupported content type in request body"
        {::spin/content-type (::spin/content-type new-representation)
         ::spin/response
         {:status 400
          :body (.getBytes "Bad Request\r\n")}})))))

(defn DELETE [request resource selected-representation date crux-node]
  {:status 200})

(defn OPTIONS [_ resource _ _]
  ;; TODO: Implement *
  (spin/options (::spin/methods resource)))

(defmethod transform-value "password" [_ instance]
  ;; TODO: Increase work-factor to 11 (not a reference to Spinal Tap!)
  (password/encrypt instance 4))

(defn authenticate
  "Authenticate a request. Return the request with any credentials, roles and
  entitlements added to it. The resource can be used to determine the particular
  Protection Space that it is part of, and the appropriate authentication
  scheme(s) for accessing the resource."
  [request resource db]
  (let [{::spin.auth/keys [user password]}
        (spin.auth/decode-authorization-header request)
        uid (format "/_crux/pass/users/%s" user)]
    (or
     (when-let [e (crux/entity db uid)]
       (when (password/check password (::pass/password-hash!! e))
         ;; TODO: This might be where we also add the 'on-behalf-of' info
         (-> request
             (assoc ::pass/user uid ::pass/username user))))

     ;; Default
     request)))

(defn handler [{db ::crux-db crux-node ::crux-node :as request}]
  (spin/check-method-not-implemented! request)
  (let [
        ;; Having extracted our Crux database and node, we remove from the
        ;; request.
        request (dissoc request ::crux-db ::crux-node)

        resource (locate-resource request db)
        request (authenticate request resource db)
        {:keys [resource authorization]} (pdp/authorize request resource db)

        ;; TODO: Promote this when guard to spin's demo
        _ (when resource (spin/check-method-not-allowed! request resource))

        date (new java.util.Date)

        current-representations (current-representations db resource date)

        _ (when (contains? #{:get :head} (:request-method request))
            (spin/check-not-found! current-representations))

        selected-representation
        (negotiate-representation request current-representations)]

    (case (:request-method request)

      (:get :head)
      (GET request resource date selected-representation db authorization)

      :post
      (POST request resource date crux-node)

      :put
      (PUT request resource selected-representation date crux-node)

      :delete
      (DELETE request resource selected-representation date crux-node)

      :options
      (OPTIONS request resource date {::db db}))))

(defn wrap-database-request-association [h crux-node]
  (fn [req]
    (h (assoc req
              ::crux-node crux-node
              ::crux-db (crux/db crux-node)))))

(defn wrap-logging [h]
  (fn [req]
    (let [t0 (System/currentTimeMillis)]
      (try
        (org.slf4j.MDC/put "uri" (:uri req))
        (let [res (h req)]
          (org.slf4j.MDC/remove "uri")
          (org.slf4j.MDC/put "duration" (str (- (System/currentTimeMillis) t0) "ms"))
          (log/infof
           "%s %s %s %d"
           (str/upper-case (name (:request-method req)))
           (:uri req)
           (:protocol req)
           (:status res))
          res)
        (finally
          (org.slf4j.MDC/clear))))))

(defn wrap-exception-handler [h]
  (fn [req]
    (try
      (h req)
      (catch clojure.lang.ExceptionInfo e
        ;;          (tap> e)
        (prn e)
        (pprint (into {::spin/request req} (ex-data e)))
        (let [exdata (ex-data e)]
          (or
           (::spin/response exdata)
           {:status 500 :body "Internal Error\r\n"})))
      (catch Exception e
        (prn e)
        {:status 500 :body "Internal Error\r\n"}))))

(defn make-handler [crux-node]
  (-> #'handler
      (wrap-database-request-association crux-node)
      wrap-exception-handler
      wrap-logging))
