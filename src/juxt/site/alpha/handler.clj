;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [crypto.password.bcrypt :as password]
   [clojure.string :as str]
   [crux.api :as crux]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.spin.alpha :as spin]
   [juxt.spin.alpha.representation :as spin.representation]
   [juxt.spin.alpha.auth :as spin.auth]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.site.alpha.put :refer [put-representation]]
   [juxt.site.alpha.locate :refer [locate-resource]]))

(defn assoc-when-some [m k v]
  (cond-> m v (assoc k v)))

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

(defmethod locate-resource *ns* [uri db]
  ;; If resource is in the database, explicitly, return it.
  (crux/entity db uri))

(defn locate-resource*
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [request db]
  (let [uri (java.net.URI. (:uri request))
        meths (methods locate-resource)]
    (some
     (fn [[_ meth]] (meth uri db))
     (let [order {*ns* 1 :else 2 :default 3}]
       (sort-by
        first
        (comparator (fn [x y]
                      (<
                       (get order x (get order :else))
                       (get order y (get order :else)))))
        meths)))))

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

(defn representation-metadata-headers [rep]
  (-> {}
      (assoc-when-some "content-type" (some-> rep ::spin/content-type))
      (assoc-when-some "content-encoding" (some-> rep ::spin/content-encoding))
      (assoc-when-some "content-language" (some-> rep ::spin/content-language))
      (assoc-when-some "content-location" (some-> rep ::spin/content-location str))
      (assoc-when-some "last-modified" (some-> rep ::spin/last-modified format-http-date))
      (assoc-when-some "etag" (some-> rep ::spin/etag))
      (assoc-when-some "vary" (some-> rep ::spin/vary))))

(defn payload-headers [rep]
  (-> {}
      (assoc-when-some "content-length" (some-> rep ::spin/content-length str))
      (assoc-when-some "content-range" (::spin/content-range rep))
      (assoc-when-some "trailer" (::spin/trailer rep))
      (assoc-when-some "transfer-encoding" (::spin/transfer-encoding rep))))

(defn GET [request resource date selected-representation db]
  (let [representation-metadata-headers (representation-metadata-headers selected-representation)]
    (spin/evaluate-preconditions! request resource representation-metadata-headers date)

    ;; This is naïve, some representations won't just have bytes ready, they'll
    ;; need to be generated somehow
    (let [{::spin/keys [payload-header-fields bytes]} selected-representation
          {::keys [path-item-object]} resource]

      (spin/response
       200
       representation-metadata-headers
       (payload-headers payload-header-fields)
       request
       nil
       date
       (cond
         bytes bytes
         path-item-object (.getBytes (get-in path-item-object ["get" "description"])))))))

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

    ;; TODO: evaluate preconditions in tx fn!
    (put-representation request resource new-representation selected-representation crux-node)))

(defn DELETE [request resource selected-representation date crux-node]
  {:status 200})

(defn OPTIONS [_ resource _ _]
  ;; TODO: Implement *
  (spin/options (::spin/methods resource)))

(defn authenticate
  "Authenticate a request. Return the request with any credentials, roles and
  entitlements added to it. The resource can be used to determine the particular
  Protection Space that it is part of, and the appropriate authentication
  scheme(s) for accessing the resource."
  [request resource db]
  (let [{::spin.auth/keys [user password]}
        (spin.auth/decode-authorization-header request)]
    (or
     (when-let [e (crux/entity db (java.net.URI. (format "/_crux/users/%s" user)))]
       (when (password/check password (:crux.site/password-hash!! e))
         (->
          (merge request e)
          (dissoc :crux.site/password-hash!!))))

     ;; Default
     request)))

(defn authorize
  "Return the resource, as it appears to the request after authorization rules
  have been applied."
  [request resource]

  (when resource

    ;; TODO: Let's load some policies and rules!

    (cond
      (and
       (.startsWith (:uri request) "/_crux/")
       (= (:crux.site/username request) "crux/admin"))
      resource

      (and
       (.startsWith (:uri request) "/_crux/")
       (not= (:crux.site/username request) "crux/admin"))
      (throw
       (ex-info
        "Authentication failed"
        {::spin/response
         {:status 401
          :headers
          {"www-authenticate"
           (spin.auth/www-authenticate
            [{::spin/authentication-scheme "Basic"
              ::spin/realm "Crux Administration"}])}
          :body "Unauthorized\r\n"}}))

      ;; When a resource outside /_crux, and admin, allow PUT of OpenAPI
      (and
       (not (.startsWith (:uri request) "/_crux/"))
       (not (.endsWith (:uri request) "/"))
       (= (:crux.site/username request) "crux/admin"))
      (-> resource
       (update ::spin/methods conj :put)
       (assoc ::spin/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}))

      :else resource)))

(defn make-handler [crux-node]
  (fn [request]
    (let [db (crux/db crux-node)]
      (spin/check-method-not-implemented! request)
      (let [resource (locate-resource* request db)
            request (authenticate request resource db)

            resource (authorize request resource)

            _ (spin/check-method-not-allowed! request resource)

            date (new java.util.Date)

            current-representations (current-representations db resource date)

            _ (when (contains? #{:get :head} (:request-method request))
                (spin/check-not-found! current-representations))

            selected-representation
            (negotiate-representation request current-representations)]

        (case (:request-method request)

          (:get :head)
          (GET request resource date selected-representation db)

          :post
          (POST request resource date crux-node)

          :put
          (PUT request resource selected-representation date crux-node)

          :delete
          (DELETE request resource selected-representation date crux-node)

          :options
          (OPTIONS request resource date {::db db}))))))

(defn wrap-exception-handler [h]
  (fn [req]
    (try
      (h req)
      (catch clojure.lang.ExceptionInfo e
        ;;          (tap> e)
        (prn e)
        (let [exdata (ex-data e)]
          (or
           (::spin/response exdata)
           {:status 500 :body "Internal Error\r\n"})))
      (catch Exception e
        (prn e)
        {:status 500 :body "Internal Error\r\n"}))))
