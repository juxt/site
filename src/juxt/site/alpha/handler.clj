;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [crypto.password.bcrypt :as password]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.dave.alpha :as dave]
   [juxt.dave.alpha.methods :as dave.methods]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.pass.alpha.authentication :as authn]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.reap.alpha.decoders :as reap.decoders]
   [juxt.site.alpha.function :as site.function]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.home :as home]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.util :refer [hexdigest]]
   [juxt.spin.alpha.representation :as spin.representation]
   [juxt.spin.alpha.representation :refer [receive-representation]]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

;; This deviates from Spin, but we want to upgrade Spin accordingly in the near
;; future. When that is done, this version can be removed and the function in
;; juxt.spin.alpha.negotiation used directly.
(defn negotiate-representation [request current-representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  (log/debug "current-representations" current-representations)

  (let [{selected-representation ::pick/representation
         vary ::pick/vary}
        (when (seq current-representations)
          (pick request current-representations {::pick/vary? true}))]

    (when (contains? #{:get :head} (:request-method request))
      (spin/check-not-acceptable! selected-representation))

    (log/debug "result of negotiate-representation" selected-representation)

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc ::http/vary (str/join ", " vary)))))

(defn uri
  "Return the full URI of the request."
  ;; At some point we should move to the Ring 2.0 namespace which has more
  ;; precise naming.
  [req]
  (str "https://home.juxt.site" (:uri req)))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [request db]
  (or
   (openapi/locate-resource db request)

   (when-let [e (crux/entity db (uri request))]
     (assoc e ::site/resource-provider ::crux))

   (home/locate-resource db request)

   {::site/resource-provider ::default-empty-resource
    ::http/methods #{:get :head :options}}))

(defn current-representations [db resource date]
  ;; TODO: Reintroduce content-locations
  (::http/representations resource))

(defn GET [request resource date selected-representation db authorization subject]
  (log/trace "GET")
  #_(spin/evaluate-preconditions! request resource selected-representation date)
  (let [{::http/keys [body content charset]} selected-representation
        {::site/keys [body-generator]} selected-representation
        body (cond
               content (.getBytes content (or charset "utf-8"))
               body body
               body-generator
               (let [body (generate-representation-body
                           request resource selected-representation db authorization subject)]
                 (cond-> body
                   (string? body) (.getBytes (or charset "UTF-8")))))]
    (spin/response
     200
     selected-representation
     request
     nil
     date
     body)))

(defn POST [request resource date crux-node db subject]
  (let [posted-representation (receive-representation request resource date)]
    (assert posted-representation)

    (let [raw-body (slurp (::spin/bytes posted-representation))
          body (case (some-> posted-representation
                             ::spin/content-type
                             (str/split #";")
                             first)
                 "application/edn" (clojure.edn/read-string raw-body)
                 "application/json" (jsonista.core/read-value raw-body)
                 raw-body)
          service-function (some-> resource
                                   (get-in [::apex/operation "responses" "200" "site/service-function"])
                                   keyword)]

      (cond
        service-function
        (site.function/invoke-service-function
         {::site/service-function service-function
          :request request
          :resource resource
          :body body})

        (= "/_site/token" (:uri request))
        (authn/token-response resource date posted-representation subject)

      (= (:uri request) "/_site/login")
      (authn/login-response resource date posted-representation db)

      (re-matches #"/~(\p{Alpha}[\p{Alnum}_-]*)/" (:uri request))
      (do
        (home/create-user-home-page request crux-node subject)
        (throw
         (ex-info
          "Redirect to home page"
          {::spin/response
           {:status 303
            :headers {"location" (:uri request)}}})))

        :else
        (throw
         (ex-info
          "POST not handled, returning 404"
          {::spin/response
           {:status 404
            :body "Not Found\r\n"}}))))))

(defn etag [representation]
  (format
   "\"%s\""
   (subs
    (hexdigest
     (cond
       (::http/body representation)
       (::http/body representation)
       (::http/content representation)
       (.getBytes (::http/content representation)
                  (::http/charset representation)))) 0 32)))

(defn put-static-representation
  "PUT a new representation of the target resource. All other representations are
  replaced."
  [request resource received-representation date crux-node]

  (->>
   (crux/submit-tx
    crux-node
    [[:crux.tx/put
      (assoc resource
             :crux.db/id (:uri request)
             ::http/representations
             [(assoc received-representation
                     ::http/etag (etag received-representation)
                     ::http/last-modified date)])]])
   (crux/await-tx crux-node)))

(defn PUT [request resource date crux-node]
  (let [received-representation (receive-representation request resource date)]
    (assert received-representation)

    ;; TODO: evaluate preconditions in tx fn!
    (let [decoded-content-type (reap.decoders/content-type (::http/content-type received-representation))
          {:juxt.reap.alpha.rfc7231/keys [type subtype]} decoded-content-type]

      (cond
        (and
         (.equalsIgnoreCase "application" type)
         (.equalsIgnoreCase "vnd.oai.openapi+json" subtype)
         (#{"3.0.2"} (get-in decoded-content-type [:juxt.reap.alpha.rfc7231/parameter-map "version"])))
        (openapi/put-openapi
         request resource received-representation date crux-node)

        (and
         (.equalsIgnoreCase "application" type)
         (.equalsIgnoreCase "json" subtype))
        (openapi/put-json-representation
         request resource received-representation date crux-node)

        (and
         (.equalsIgnoreCase "text" type)
         (.equalsIgnoreCase "html" subtype))
        (put-static-representation
         request resource received-representation date crux-node)

        :else
        (throw
         (ex-info
          "Unsupported content type in request"
          {::http/content-type (::http/content-type received-representation)
           ::spin/response
           {:status 415
            :body (.getBytes "Unsupported Media Type\r\n" "utf-8")}}))))))

(defn DELETE [request resource date crux-node]
  (->>
   (crux/submit-tx
    crux-node
    [[:crux.tx/delete (str "https://home.juxt.site" (:uri request))]])
   (crux/await-tx crux-node))
  (spin/response 204 nil nil nil date nil))

(defn OPTIONS [_ resource allow-methods _ _]
  ;; TODO: Implement *
  (->
   (spin/options allow-methods)
   (update :headers merge (::http/options resource))))

(defn PROPFIND [request resource date crux-node authorization subject]
  (dave.methods/propfind request resource date crux-node authorization subject))

(defn MKCOL [request resource date crux-node]
  (let [tx (crux/submit-tx
            crux-node
            [[:crux.tx/put
              {:crux.db/id (:uri request)
               ::dave/resource-type :collection
               ::http/methods #{:get :head :options :propfind}
               ::http/representations
               [{::http/content-type "text/html;charset=utf-8"
                 ::http/content "<h1>Index</h1>\r\n"}]
               ::http/options {"DAV" "1"}}]])]
    (crux/await-tx crux-node tx))
  {:status 201
   :headers {}
   :body "Collection created\r\n"})

(defmethod transform-value "password" [_ instance]
  (password/encrypt instance 11))

(defmethod transform-value "inst" [_ instance]
  (java.util.Date/from (java.time.Instant/parse instance)))

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
  (spin/check-method-not-implemented!
   request
   #{:get :head :post :put :delete :options
     :mkcol :propfind})

  (let [;; Having extracted our Crux database and node, we remove from the
        ;; request.
        request (dissoc request ::crux-db ::crux-node)

        resource (locate-resource request db)

        _ (when-let [redirect (::http/redirect resource)]
            (throw
             (ex-info
              "Redirect"
              {::spin/response
               {:status (case (:request-method request)
                          (:get :head) 302
                          307)
                :headers {"location" redirect}}})))

        _ (log/trace
           "Result of locate-resource"
           (pr-str (->  resource
                        (update ::http/representations count)
                        (dissoc :juxt.apex.alpha/openapi))))

        date (new java.util.Date)

        current-representations
        (when (contains? #{:get :head} (:request-method request))
          (current-representations db resource date))

        selected-representation
        (when (seq current-representations)
          (negotiate-representation request current-representations))

        _ (when (contains? #{:get :head} (:request-method request))
            (spin/check-not-found! current-representations))

        ;; Do authorization as late as possible (in order to have as much data
        ;; as possible to base the authorization decision on. However, note
        ;; Section 8.5, RFC 4918 states "the server MUST do authorization checks
        ;; before checking any HTTP conditional header.".

        subject (authn/authenticate request resource date db)
        _ (when subject (org.slf4j.MDC/put "user" (::pass/username subject)))

        request-context {'subject subject
                         'resource resource
                         'request (dissoc request :body)
                         'representation selected-representation
                         'environment {}}

        authorization (pdp/authorization db request-context)

        _ (log/debugf "Result of authorization with resource-context %s is %s"
                      (pr-str request-context)
                      (pr-str authorization))

        _ (when-not (= (::pass/access authorization) ::pass/approved)
            (throw
             (if (::pass/user subject)
               (ex-info
                "Forbidden"
                {::spin/response
                 {:status 403
                  :body "Forbidden\r\n"}})

               (ex-info
                "Unauthorized"
                {::spin/response
                 {:status 401
                  :headers {}
                  :body "Unauthorized\r\n"}}))))

        allow-methods (set/union
                       (::http/methods resource)
                       (::pass/allow-methods authorization))]

    (when resource
      (check-method-not-allowed!
       request resource
       allow-methods))

    (case (:request-method request)

      (:get :head)
      (GET request resource date selected-representation db authorization subject)

      :post
      (POST request resource date crux-node db subject)

      :put
      (PUT request resource date crux-node)

      :delete
      (DELETE request resource date crux-node)

      :options
      (OPTIONS request resource allow-methods date db)

      :propfind
      (PROPFIND request resource date crux-node authorization subject)

      :mkcol
      (MKCOL request resource date crux-node))))

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
        (org.slf4j.MDC/put "method" (str/upper-case (name (:request-method req))))
        (log/debug "Request received" (pr-str (dissoc req :body)))

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
        (let [exdata (ex-data e)]
          (when-not (::spin/response exdata)
            (pprint (into {::spin/request req} (ex-data e)))
            (prn e)
            (log/errorf
             e "%s: %s" (.getMessage e)
             (pr-str (into {::spin/request req} exdata))))
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
