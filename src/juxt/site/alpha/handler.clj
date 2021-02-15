;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [crypto.password.bcrypt :as password]
   [jsonista.core :as json]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value]]
   [juxt.pass.alpha :as pass]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.reap.alpha.decoders :as reap.decoders]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.util :refer [assoc-when-some hexdigest]]
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

   {::spin/methods #{:get :head :options}}))

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


      (let [body (cond
                   content (.getBytes content (or charset "utf-8"))
                   bytes bytes
                   path-item-object (.getBytes (get-in path-item-object ["get" "description"]) "utf-8")
                   bytes-generator (generate-representation-body request resource selected-representation db authorization))]
        (spin/response
         200
         representation-metadata-headers
         (response/payload-headers payload-header-fields body)
         request
         nil
         date
         body)))))

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

;; TODO: Not sure there should be a general way of writing resources like this, without a lot of authorization checks
#_(defn put-resource
  [request _ new-representation old-representation crux-node]
  (let [new-resource
        (->
         (json/read-value (::spin/bytes new-representation) (json/object-mapper {:decode-key-fn true}))
         (update ::spin/methods #(set (map keyword %))))]
    (log/debugf "New resource: %s" (pr-str new-resource))
    (let [now (java.util.Date.)
          new-resource (assoc new-resource :crux.db/id (:uri request))]
      (->>
       (crux/submit-tx crux-node [[:crux.tx/put new-resource]])
       (crux/await-tx crux-node))
      (spin/response (if old-representation 200 201) nil nil request nil now nil))))

(defn put-static-representation
  "PUT a new representation of the target resource. All other representations are
  replaced."
  [request resource new-representation old-representation crux-node]

  (let [etag (format "\"%s\"" (subs (hexdigest (::spin/bytes new-representation)) 0 32))
        now (java.util.Date.)
        representation-metadata {::spin/etag etag
                                 ::spin/last-modified now}]

    (log/debugf "new-representation metadata is %s" (pr-str (dissoc new-representation ::spin/bytes)))

    (->>
     (crux/submit-tx
      crux-node
      [[:crux.tx/put
        (assoc resource
               :crux.db/id (:uri request)
               ::spin/representations [(merge new-representation representation-metadata)])]])
     (crux/await-tx crux-node))

    (spin/response
     (if old-representation 200 201) nil nil request nil now nil)))

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
    (let [decoded-content-type (reap.decoders/content-type (::spin/content-type new-representation))
          {:juxt.reap.alpha.rfc7231/keys [type subtype]} decoded-content-type]

      (cond
        (and
         (.equalsIgnoreCase "application" type)
         (.equalsIgnoreCase "vnd.oai.openapi+json" subtype)
         (#{"3.0.2"} (get-in decoded-content-type [:juxt.reap.alpha.rfc7231/parameter-map "version"])))
        (openapi/put-openapi
         request resource new-representation selected-representation crux-node)

        (and
         (.equalsIgnoreCase "application" type)
         (.equalsIgnoreCase "json" subtype))
        (openapi/put-json-representation
         request resource new-representation selected-representation crux-node)

        #_(and
         (.equalsIgnoreCase "application" type)
         (.equalsIgnoreCase "vnd.juxt.site-resource+json" subtype))
        #_(put-resource
         request resource new-representation selected-representation crux-node)

        (and
         (.equalsIgnoreCase "text" type)
         (.equalsIgnoreCase "html" subtype))
        (put-static-representation
         request resource new-representation selected-representation crux-node)

        :else
        (throw
         (ex-info
          "Unsupported content type in request"
          {::spin/content-type (::spin/content-type new-representation)
           ::spin/response
           {:status 415
            :body (.getBytes "Unsupported Media Type\r\n" "utf-8")}}))))))

(defn DELETE [request resource selected-representation date crux-node]
  {:status 200})

(defn OPTIONS [_ _ allow-methods _ _]
  ;; TODO: Implement *
  (spin/options allow-methods))

(defmethod transform-value "password" [_ instance]
  ;; TODO: Increase work-factor to 11 (not a reference to Spinal Tap!)
  (password/encrypt instance 4))

(defn authenticate
  "Authenticate a request. Return a pass subject, with information about user,
  roles and other credentials. The resource can be used to determine the
  particular Protection Space that it is part of, and the appropriate
  authentication scheme(s) for accessing the resource."
  [request resource db]
  (let [{::spin.auth/keys [user password]}
        (spin.auth/decode-authorization-header request)
        uid (format "/_crux/pass/users/%s" user)]
    (when-let [e (crux/entity db uid)]
      (when (password/check password (::pass/password-hash!! e))
        ;; TODO: This might be where we also add the 'on-behalf-of' info
        {::pass/user uid
         ::pass/username user}))))

(defn check-method-not-allowed!
  [request resource methods]
  (if resource
    (let [method (:request-method request)]
      (when-not (contains? (set methods) method)
        (throw
         (ex-info
          "Method not allowed"
          {::spin/response
           {:status 405
            :headers {"allow" (spin/allow-header methods)}
            :body "Method Not Allowed\r\n"}
           ::spin/resource resource}))))
    ;; We forbid POST, PUT and DELETE on a nil resource
    (when-not (methods (:request-method request))
      (throw
       (ex-info
        "Method not allowed"
        {::spin/response
         {:status 405
          :headers {"allow" (spin/allow-header #{:get :head})}
          :body "Method Not Allowed\r\n"}
         ::spin/resource resource})))))

(defn handler [{db ::crux-db crux-node ::crux-node :as request}]
  (spin/check-method-not-implemented! request)
  (let [
        ;; Having extracted our Crux database and node, we remove from the
        ;; request.
        request (dissoc request ::crux-db ::crux-node)

        resource (locate-resource request db)
        subject (authenticate request resource db)
        authorization (pdp/authorize
                       {::pass/subject subject
                        ::pass/resource resource
                        ::pass/request (dissoc request :body)
                        ::pass/environment {:db db}})

        _ (log/debugf "Authorization: %s" (pr-str authorization))

        allow-methods (set/union
                       (::spin/methods resource)
                       (::pass/allow-methods authorization))

        _ (when resource
            (check-method-not-allowed!
             request resource
             allow-methods))

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
      (OPTIONS request resource allow-methods date {::db db}))))

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
          (log/infof "%-7s %s %s %d"
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
        (log/errorf
         e "%s: %s" (.getMessage e)
         (pr-str (into {::spin/request req} (ex-data e))))
        (prn e)
        (pprint (into {::spin/request req} (ex-data e)))
        (let [exdata (ex-data e)]
          (or
           (::spin/response exdata)
           {:status 500 :body "Internal Error\r\n"})))
      (catch Exception e
        (log/error e (.getMessage e))
        (prn e)
        {:status 500 :body "Internal Error\r\n"}))))

(defn make-handler [crux-node]
  (-> #'handler
      (wrap-database-request-association crux-node)
      wrap-exception-handler
      wrap-logging))
