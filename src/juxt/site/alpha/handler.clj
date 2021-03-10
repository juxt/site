;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [crux.api :as x]
   [crypto.password.bcrypt :as password]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.dave.alpha :as dave]
   [juxt.dave.alpha.methods :as dave.methods]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value]]
   [juxt.pass.alpha.authentication :as authn]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.pick.alpha.ring :refer [pick]]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.reap.alpha.regex :as re]
   [juxt.site.alpha.locator :as locator]
   [juxt.site.alpha.util :as util]
   [juxt.site.alpha.representation :refer [receive-representation]]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'rfc7230 (create-ns 'juxt.reap.alpha.rfc7230))

(defn allow-header
  "Return the Allow response header value, given a set of method keywords."
  [methods]
  (->>
   methods
   seq
   distinct
   (map (comp str/upper-case name))
   (str/join ", ")))

(defn negotiate-representation [request current-representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  (log/debug "current-representations" (map (fn [rep] (dissoc rep ::http/body ::http/content)) current-representations))

  (let [{selected-representation ::pick/representation
         vary ::pick/vary}
        (when (seq current-representations)
          ;; TODO: Pick must upgrade to ring headers
          (pick (assoc request :headers (:ring.request/headers request))
                current-representations {::pick/vary? true}))]

    (when (contains? #{:get :head} (:ring.request/method request))
      (when-not selected-representation
        (throw
         (ex-info
          "Not Acceptable"
           ;; TODO: Must add list of available representations
          {:ring.response/status 406
           :ring.response/body "Not Acceptable\r\n"}))))

    (log/debug "result of negotiate-representation" (dissoc selected-representation ::http/body ::http/content))

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc ::http/vary (str/join ", " vary)))))

(defn etag [representation]
  (format
   "\"%s\""
   (subs
    (util/hexdigest
     (cond
       (::http/body representation)
       (::http/body representation)
       (::http/content representation)
       (.getBytes (::http/content representation)
                  (get representation ::http/charset "UTF-8")))) 0 32)))

(defn put-static-resource
  "PUT a new representation of the target resource. All other representations are
  replaced."
  [{::site/keys [uri db received-representation start-date crux-node] :as req}]
  (let [existing? (x/entity db uri)]
    (->>
     (x/submit-tx
        crux-node
        [[:crux.tx/put
          (merge
           {:crux.db/id uri
            ::site/type "StaticRepresentation"
            ::pass/classification "PUBLIC"
            ::http/methods #{:get :head :options :put :patch}
            ::http/etag (etag received-representation)
            ::http/last-modified start-date}
           received-representation)]])

     (x/await-tx crux-node))

    (into req {:ring.response/status (if existing? 204 201)})))

(defn patch-static-resource
  [{::site/keys [uri received-representation start-date crux-node] :as req}]
  (throw (ex-info "TODO: patch" {:incoming received-representation})))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [{::site/keys [db uri canonical-host] :as req}]
  (let [prefix (format "https://%s" canonical-host)]
    (or
     ;; We call OpenAPI location here, because a resource can be defined in
     ;; OpenAPI, and exits in Crux, simultaneously.
     (openapi/locate-resource db uri req)

     ;; Is it in Crux?
     (when-let [r (x/entity db uri)]
       (cond-> (assoc r ::site/resource-provider ::db)
         (= (get r ::site/type) "StaticRepresentation")
         (update ::site/request-locals
                 assoc
                 ::site/put-fn put-static-resource
                 ::site/patch-fn patch-static-resource)))

     ;; Is it found by any resource locators registered in the database?
     (locator/locate-with-locators db req)

     ;; Is it a redirect?
     (when-let [[r loc] (first
                         (x/q db '{:find [r loc]
                                   :where [[r ::site/resource uri]
                                           [r ::site/location loc]
                                           [r ::site/type "Redirect"]]
                                   :in [uri]}
                              uri))]
       {::site/uri uri
        ::site/methods #{:get :head :options}
        ::site/resource-provider r
        ::http/redirect (cond-> loc (.startsWith loc prefix)
                                (subs (count prefix)))})

     ;; Return a back-stop resource
     {::site/resource-provider ::default-empty-resource
      ::http/methods #{:get :head :options :put :post}
      ::site/request-locals
      {::site/put-fn put-static-resource}})))

(defn current-representations [{::site/keys [resource uri db]}]
  (or
   ;; This is not common in the Crux DB, but allows 'dynamic' resources to
   ;; declare multiple representations.
   (::http/representations resource)

   ;; See if there are variants
   (let [variants (x/q db '{:find [r]
                            :where [[v ::site/type "Variant"]
                                    [v ::site/resource uri]
                                    [v ::site/variant r]]
                            :in [uri]
                            } uri)]
     (when (pos? (count variants))
       (log/tracef "found %d extra variants for uri %s" (count variants) uri)
       (cond-> (for [[v] variants
                     :let [rep (x/entity db v)]
                     :when rep]
                 (assoc rep ::http/content-location v))
         (::http/content-type resource)
         (conj resource))))

   ;; Most resources have a content-type, which indicates there is only one
   ;; variant.
   (when (::http/content-type resource)
     [(-> resource
          (dissoc ::site/request-locals))])

   ;; No representations. On a GET, this would yield a 404.
   []))

(defn GET [{::site/keys [selected-representation] :as req}]
  #_(spin/evaluate-preconditions! request resource selected-representation date)

  (let [{::http/keys [body content charset]} selected-representation
        {::site/keys [body-fn]} selected-representation
        body (cond
               content (.getBytes content (or charset "utf-8"))
               body body

               body-fn
               (let [f (requiring-resolve body-fn)]
                 (log/debugf "Calling body-fn: %s" body-fn)
                 (let [body (f req)]
                   (cond-> body
                     (string? body) (.getBytes (or charset "UTF-8"))))))]

    ;; TODO: Fix this!
    (assoc req
           :ring.response/status 200
           :ring.response/body body)))

(defn post-variant [{::site/keys [crux-node db uri request-locals] :as req}]

  (let [request-instance (get request-locals ::apex/request-instance)
        location
        (str uri (hash (select-keys request-instance [::site/resource ::site/variant])))
        existing (x/entity db location)]

    (->> (x/submit-tx
          crux-node
          [[:crux.tx/put (merge {:crux.db/id location} request-instance)]])
         (x/await-tx crux-node))

    (into req {:ring.response/status (if existing 204 201)
               :ring.response/headers {"location" location}})))

(defn post-redirect [{::site/keys [crux-node db uri request-locals] :as req}]
  (let [request-instance (get request-locals ::apex/request-instance)
        location (str uri (hash (select-keys request-instance [::site/resource ::site/location])))
        existing (x/entity db location)]
    (->> (x/submit-tx
          crux-node
          [[:crux.tx/put (merge {:crux.db/id location} request-instance)]])
         (x/await-tx crux-node))

    (into req {:ring.response/status (if existing 204 201)
               :ring.response/headers {"location" location}})))

(defn POST [{::site/keys [resource request-id] :as req}]
  (let [rep (->
             (receive-representation req)
             (assoc ::site/request request-id))
        req (assoc req ::site/received-representation rep)
        post-fn (get-in resource [::site/request-locals ::site/post-fn])]

    (cond
      (fn? post-fn) (post-fn req)

      :else (case (::site/purpose resource)
              ::site/acquire-token (authn/token-response req)
              ::site/login (authn/login-response req)
              ::site/logout (authn/logout-response req)
              (throw
               (ex-info
                "POST not handled, returning 404"
                (into req
                      {:ring.response/status 404
                       :ring.response/body "Not Found\r\n"})))))))

(defn PUT [{::site/keys [resource] :as req}]
  (let [rep (receive-representation req) _ (assert rep)
        req (assoc req ::site/received-representation rep)
        put-fn (get-in resource [::site/request-locals ::site/put-fn])]

    ;; TODO: evaluate preconditions (in tx fn)
    (cond
      (fn? put-fn) (put-fn req)
      :else
      (throw
       (ex-info "Resource allows PUT but doesn't contain have a put-fn function"
                (into req
                      {:ring.response/status 500
                       :ring.response/body "Internal Error\r\n"}))))))

(defn PATCH [{::site/keys [resource] :as req}]
  (let [rep (receive-representation req) _ (assert rep)
        req (assoc req ::site/received-representation rep)
        patch-fn (get-in resource [::site/request-locals ::site/patch-fn])]

    ;; TODO: evaluate preconditions (in tx fn)
    (cond
      (fn? patch-fn) (patch-fn req)
      :else
      (throw
       (ex-info "Resource allows PATCH but doesn't contain have a patch-fn function"
                (into req
                      {:ring.response/status 500
                       :ring.response/body "Internal Error\r\n"}))))))

(defn DELETE [{::site/keys [crux-node uri] :as req}]
  (->>
   (x/submit-tx crux-node [[:crux.tx/delete uri]])
   (x/await-tx crux-node))

  (into req
        {:ring.response/status 415
         :ring.response/body "Unsupported Media Type\r\n"}))

(defn OPTIONS [{::site/keys [resource allowed-methods] :as req}]
  ;; TODO: Implement *
  (-> req
      (into {:ring.response/status 200})
      (update :ring.response/headers
              merge
              (::http/options resource)
              {"allow" (allow-header allowed-methods)
               ;; TODO: Shouldn't this be a situation (a missing body) detected
               ;; by middleware, which can set the content-length header
               ;; accordingly?
               "content-length" "0"})))

(defn PROPFIND [req]
  (dave.methods/propfind req))

(defn MKCOL [{::site/keys [crux-node uri]}]
  (let [tx (x/submit-tx
            crux-node
            [[:crux.tx/put
              {:crux.db/id uri
               ::dave/resource-type :collection
               ::http/methods #{:get :head :options :propfind}
               ::http/content-type "text/html;charset=utf-8"
               ::http/content "<h1>Index</h1>\r\n"
               ::http/options {"DAV" "1"}}]])]
    (x/await-tx crux-node tx))
  {:ring.response/status 201
   :ring.response/headers {}
   :ring.response/body "Collection created\r\n"})

(defmethod transform-value "password" [_ instance]
  (password/encrypt instance 11))

(defmethod transform-value "inst" [_ instance]
  (java.util.Date/from (java.time.Instant/parse instance)))

(defn inner-handler [{:ring.request/keys [method]
                      ::site/keys [uri db] :as req}]

  (when-not (contains?
             #{:get :head :post :put :delete :options
               :patch
               :mkcol :propfind} method)
    (throw
     (ex-info "Method not implemented"
              (merge req {:ring.response/status 501
                          :ring.response/body "Not Implemented\r\n"}))))

  (let [res (locate-resource req)

        _ (log/debug "resource provider" (::site/resource-provider res))

        req (assoc req ::site/resource res)

        _ (when-let [location (::http/redirect res)]
            (throw
             (ex-info "Redirect"
                      (-> req
                          (assoc :ring.response/status
                                 (case method (:get :head) 302 307))
                          (update :ring.response/headers
                                  assoc "location" location)))))

        cur-reps
        (when (#{:get :head} method) (current-representations req))

        sel-rep
        (when (seq cur-reps)
          (negotiate-representation req cur-reps))
        req (assoc req ::site/selected-representation sel-rep)

        _ (when (and (#{:get :head} method) (empty? cur-reps))
            (throw
             (ex-info "Not Found"
                      (merge req
                             {:ring.response/status 404
                              :ring.response/body "Not Found\r\n"}))))

        ;; Do authorization as late as possible (in order to have as much data
        ;; as possible to base the authorization decision on. However, note
        ;; Section 8.5, RFC 4918 states "the server MUST do authorization checks
        ;; before checking any HTTP conditional header.".

        sub (authn/authenticate req)
        req (assoc req ::pass/subject sub)

        ;;_ (when subject (org.slf4j.MDC/put "user" (::pass/username subject)))

        ;; Could the scope of this request context be expanded to be what is
        ;; ultimately returned (in the case of 2xx/3xx) or thrown (3xx/4xx/5xx)?
        request-context {'subject sub
                         ;; ::site/request-locals is used to avoid database involvement
                         'resource (dissoc res ::site/request-locals ::http/body ::http/content)
                         'request (select-keys req [:ring.request/method])
                         'representation (dissoc res ::site/request-locals ::http/body ::http/content)
                         'environment {}}

        authz (pdp/authorization db request-context)

        req (assoc req
                   ::pass/authorization authz
                   ::pass/request-context request-context)

        ;; If the max-content-length has been modified, update that in the resource
        req (cond-> req
              (::http/max-content-length authz)
              (update ::site/resource
                      assoc ::http/max-content-length (::http/max-content-length authz)))

        _ (when-not (= (::pass/access authz) ::pass/approved)
            (let [status (if-not (::pass/user sub) 401 403)
                  msg (case status 401  "Unauthorized" 403 "Forbidden")]
              (throw
               (ex-info msg
                        (into req
                              {:ring.response/status status
                               :ring.response/body (str msg "\r\n")})))))

        allowed-methods (set (::http/methods res))
        req (assoc req ::site/allowed-methods allowed-methods)]

    (when res
      (when-not (contains? allowed-methods method)
        (throw
         (ex-info
          "Method not allowed"
          (into req
                {:ring.response/status 405
                 :ring.response/headers {"allow" (allow-header allowed-methods)}
                 :ring.response/body "Method Not Allowed\r\n"})))))

    (case method
      (:get :head) (GET req)
      :post (POST req)
      :put (PUT req)
      :patch (PATCH req)
      :delete (DELETE req)
      :options (OPTIONS req)
      :propfind (PROPFIND req)
      :mkcol (MKCOL req))))

(defn representation-headers [acc rep body]
  (letfn [(assoc-when-some [m k v]
            (cond-> m v (assoc k v)))]
    (-> acc
        (assoc-when-some "content-type"
                         (some-> rep ::http/content-type))
        (assoc-when-some "content-encoding"
                         (some-> rep ::http/content-encoding))
        (assoc-when-some "content-language"
                         (some-> rep ::http/content-language))
        (assoc-when-some "content-location"
                         (some-> rep ::http/content-location str))
        (assoc-when-some "last-modified"
                         (some-> rep ::http/last-modified format-http-date))
        (assoc-when-some "etag" (some-> rep ::http/etag))
        (assoc-when-some "vary" (some-> rep ::http/vary))
        (assoc-when-some "content-length"
                         (or
                          (some-> rep ::http/content-length str)
                          (when (counted? body)
                            (some-> body count str))))
        (assoc-when-some "content-range" (::http/content-range rep))
        (assoc-when-some "trailer" (::http/trailer rep))
        (assoc-when-some "transfer-encoding" (::http/transfer-encoding rep)))))

(defn minimize-response [response]
  (-> response
      (dissoc :ring.response/body
              :ring.request/body
              ::site/crux-node ::site/db
              ;; Also, this is the crux.db/id so don't repeat
              ::site/request-id)

      (update :ring.response/headers dissoc "set-cookie")

      ;; ::site/request-locals is used to avoid database involvement
      (update ::site/resource dissoc ::site/request-locals ::http/body ::http/content)
      (update ::site/selected-representation dissoc ::http/body ::http/content)
      (update ::site/received-representation dissoc ::http/body ::http/content)
      (update :ring.request/headers
              (fn [headers]
                (cond-> headers
                  (contains? headers "authorization")
                  (assoc "authorization" "REDACTED"))))))

;; TODO: Split into logback logger, Crux logger and response finisher. This will
;; make it easier to disable one or both logging strategies.
(defn outer-handler
  [{::site/keys [crux-node db request-id start-date host]
    :ring.request/keys [method]
    :as req}]

  (org.slf4j.MDC/put "reqid" request-id)

  (let [respond
        (fn [{::site/keys [selected-representation body] :as response}]

          (log/infof "%-7s %s %s %d"
                     (str/upper-case (name (:ring.request/method req)))
                     (:ring.request/path req)
                     (:ring.request/protocol req)
                     (:ring.response/status response))
          (try
            (x/submit-tx
             crux-node
             [[:crux.tx/put
               (merge
                (minimize-response response)

                (select-keys db [:crux.db/valid-time :crux.tx/tx-id])

                (let [end-date (java.util.Date.)]
                  {:crux.db/id request-id
                   ::site/type "Request"
                   ::site/end-date end-date
                   ::site/duration-millis (- (.getTime end-date)
                                             (.getTime start-date))}))]])
            (catch Exception e
              (log/error e "Failed to log request, recovering")))

          (cond->
              (update response
                      :ring.response/headers
                      assoc "date" (format-http-date start-date))

              request-id
              (update :ring.response/headers
                      assoc "site-request-id"
                      (cond-> request-id
                        (.startsWith request-id (str "https://" host))
                        (subs (count (str "https://" host)))))

              selected-representation
              (update :ring.response/headers
                      representation-headers selected-representation body)

              (= method :head) (dissoc :ring.response/body)))]

    (try
      (respond (inner-handler req))
      (catch clojure.lang.ExceptionInfo e
        (let [{:ring.response/keys [status] :as exdata} (ex-data e)]
          (when (or (nil? status) (>= status 500))
            (let [exdata (minimize-response exdata)]
              (prn (.getMessage e))
              (pprint exdata)
              (log/errorf e "%s: %s" (.getMessage e) (pr-str exdata))))
          (respond
           (-> (into
                {:ring.response/status 500
                 :ring.response/body "Internal Error\r\n"
                 ::site/error (.getMessage e)
                 ::site/error-stack-trace (.getStackTrace e)}
                exdata)))))
      (catch Throwable e
        (log/error e (.getMessage e))
        (prn e)
        (respond
         (into req
               {:ring.response/status 500
                :ring.response/body "Internal Error\r\n"})))
      (finally
        (org.slf4j.MDC/clear)))))

(def host-header-parser (rfc7230.decoders/host {}))

(defn assert-host [host]
  (try
    (host-header-parser (re/input host))
    (catch Exception e
      (throw (ex-info "Illegal host format" {:host host} e)))))

(defn new-request-id [canonical-host]
  (str "https://" canonical-host "/_site/requests/"
       (subs (util/hexdigest
              (.getBytes (str (java.util.UUID/randomUUID)) "US-ASCII")) 0 24)))

(defn wrap-initialize-state [h crux-node host-map]

  ;; Validate host-map
  (try
    (doseq [[k v] host-map]
      (assert-host k)
      (assert-host v))
    (catch Exception e
      (throw (ex-info "Invalid host-map format" {::site/host-map host-map} e))))

  (assert crux-node)

  (fn [req]
    (let [db (x/db crux-node)
          {::site/keys [canonical-host]}
          (x/entity db ::site/init-settings) _ (assert canonical-host)
          req-id (new-request-id canonical-host)

          {::rfc7230/keys [host]}
          (host-header-parser
           (re/input (get-in req [:ring.request/headers "host"])))
          _ (assert host)
          host (get host-map host host)
          uri (str "https://" host (:ring.request/path req))
          req (into req {::site/start-date (java.util.Date.)
                         ::site/request-id req-id
                         ::site/uri uri
                         ::site/host host
                         ::site/crux-node crux-node
                         ::site/db db
                         ::site/canonical-host canonical-host})]

      ;; The Ring request map becomes the container for all state collected
      ;; along the request processing pathway.
      (h req))))

(defn wrap-ring-1-adapter
  "Given the presence of keywords from different origins, it helps that we
  distinguish Ring keywords from application keywords. Ring 2.0 provides a good
  set of namespaced keywords. This Ring middleware adapts a Ring 1.0 adapter to
  this set of keywords. See
  https://github.com/ring-clojure/ring/blob/2.0/SPEC-2.md for full details."
  [h]
  (fn [req]
    (let [mp {:body :ring.request/body
              :headers :ring.request/headers
              :request-method :ring.request/method
              :uri :ring.request/path
              :query :ring.request/query
              :protocol :ring.request/protocol
              :remote-addr :ring.request/remote-addr
              :scheme :ring.request/scheme
              :server-name :ring.request/server-name
              :server-port :ring.request/server-port
              :ssl-client-cert :ring.request/ssl-client-cert}]

      (-> (reduce-kv
           (fn [acc k v]
             (let [k2 (get mp k)]
               (cond-> acc k2 (assoc k2 v))))
           (sorted-map) req)
          (h)
          (select-keys
           [:ring.response/status
            :ring.response/headers
            :ring.response/body])
          (set/rename-keys {:ring.response/status :status
                            :ring.response/headers :headers
                            :ring.response/body :body})))))

(defn make-handler [crux-node host-map]
  (-> #'outer-handler
      (wrap-initialize-state crux-node host-map)
      (wrap-ring-1-adapter)))
