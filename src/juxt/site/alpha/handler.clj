;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
   [clojure.instant :refer [read-instant-date]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [crypto.password.bcrypt :as password]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.dave.alpha :as dave]
   [juxt.dave.alpha.methods :as dave.methods]
   [juxt.jinx.alpha.vocabularies.transformation :refer [transform-value]]
   [juxt.pass.alpha.http-authentication :as http-authn]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.pass.alpha.session-scope :as session-scope]
   [juxt.pick.alpha.core :refer [rate-representation]]
   [juxt.pick.alpha.ring :refer [decode-maybe]]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.reap.alpha.regex :as re]
   [juxt.reap.alpha.ring :refer [headers->decoded-preferences]]
   [juxt.site.alpha.cache :as cache]
   [juxt.site.alpha.conditional :as conditional]
   [juxt.site.alpha.content-negotiation :as conneg]
   [juxt.site.alpha.locator :as locator]
   [juxt.site.alpha.util :as util]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.triggers :as triggers]
   [juxt.site.alpha.rules :as rules]
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [juxt.reap.alpha.rfc7230 :as-alias rfc7230])
  (:import (java.net URI)))

(defn join-keywords
  "Join method keywords into a single comma-separated string. Used for the Allow
  response header value, and others."
  [methods upper-case?]
  (->>
   methods
   seq
   distinct
   (map (comp (if upper-case? str/upper-case identity) name))
   (str/join ", ")))

(defn receive-representation
  "Check and load the representation enclosed in the request message payload."
  [{::site/keys [resource start-date] :ring.request/keys [method] :as req}]

  (let [content-length
        (try
          (some->
           (get-in req [:ring.request/headers "content-length"])
           (Long/parseLong))
          (catch NumberFormatException e
            (throw
             (ex-info
              "Bad content length"
              {::site/request-context (assoc req :ring.response/status 400)}
              e))))]

    (when (nil? content-length)
      (throw
       (ex-info
        "No Content-Length header found"
        {::site/request-context (assoc req :ring.response/status 411)})))

    ;; Protects resources from PUTs that are too large. If you need to
    ;; exceed this limitation, explicitly declare ::spin/max-content-length in
    ;; your resource.
    (when-let [max-content-length (get resource ::http/max-content-length (Math/pow 2 24))] ;;16MB
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          {::site/request-context (assoc req :ring.response/status 413)}))))

    (when-not (:ring.request/body req)
      (throw
       (ex-info
        "No body in request"
        {::site/request-context (assoc req :ring.response/status 400)})))

    (let [decoded-representation
          (decode-maybe

           ;; See Section 3.1.1.5, RFC 7231 as to why content-type defaults
           ;; to application/octet-stream
           (cond-> {::http/content-type "application/octet-stream"}
             (contains? (:ring.request/headers req) "content-type")
             (assoc ::http/content-type (get-in req [:ring.request/headers "content-type"]))

             (contains? (:ring.request/headers req) "content-encoding")
             (assoc ::http/content-encoding (get-in req [:ring.request/headers "content-encoding"]))

             (contains? (:ring.request/headers req) "content-language")
             (assoc ::http/content-language (get-in req [:ring.request/headers "content-language"]))))]

      ;; TODO: Someday there should be a functions that could be specified to
      ;; handle conversions as described in RFC 7231 Section 4.3.4

      ;; TODO: Add tests for content type (and other axes of) acceptance of PUT
      ;; and POST

      (when-let [acceptable
                 (get-in resource [::site/methods method ::site/acceptable])]

        (let [prefs (headers->decoded-preferences acceptable)
              request-rep (rate-representation prefs decoded-representation)]

          (when (or (get prefs "accept") (get prefs "accept-charset"))
            (cond
              (= (:juxt.pick.alpha/content-type-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-type of the request payload is not supported by the resource"
                {::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::site/request-context (assoc req :ring.response/status 415)}))

              (and
               (= "text" (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/type]))
               (get prefs "accept-charset")
               (not (contains? (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map]) "charset")))
              (throw
               (ex-info
                "The Content-Type header in the request is a text type and is required to specify its charset as a media-type parameter"
                {::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::site/request-context (assoc req :ring.response/status 415)}))

              (= (:juxt.pick.alpha/charset-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The charset of the Content-Type header in the request is not supported by the resource"
                {::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::site/request-context (assoc req :ring.response/status 415)}))))

          (when (get prefs "accept-encoding")
            (cond
              (= (:juxt.pick.alpha/content-encoding-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-encoding in the request is not supported by the resource"
                {::acceptable acceptable
                 ::content-encoding (get-in req [:ring.request/headers "content-encoding"] "identity")
                 ::site/request-context (assoc req :ring.response/status 409)}))))

          (when (get prefs "accept-language")
            (cond
              (not (contains? (:ring.response/headers req) "content-language"))
              (throw
               (ex-info
                "Request must contain Content-Language header"
                {::acceptable acceptable
                 ::content-language (get-in req [:ring.request/headers "content-language"])
                 ::site/request-context (assoc req :ring.response/status 409)}))

              (= (:juxt.pick.alpha/content-language-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-language in the request is not supported by the resource"
                {::acceptable acceptable
                 ::content-language (get-in req [:ring.request/headers "content-language"])
                 ::site/request-context (assoc req :ring.response/status 415)}))))))

      (when (get-in req [:ring.request/headers "content-range"])
        (throw
         (ex-info
          "Content-Range header not allowed on a PUT request"
          {::site/request-context (assoc req :ring.response/status 400)})))

      (with-open [in (:ring.request/body req)]
        (let [body (.readNBytes in content-length)
              content-type (:juxt.reap.alpha.rfc7231/content-type decoded-representation)]

          (merge
           decoded-representation
           {::http/content-length content-length
            ::http/last-modified start-date}

           (if (and
                (= (:juxt.reap.alpha.rfc7231/type content-type) "text")
                (nil? (get decoded-representation ::http/content-encoding)))
             (let [charset
                   (get-in decoded-representation
                           [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map "charset"])]
               (merge
                {::http/content (new String body (or charset "utf-8"))}
                (when charset {::http/charset charset})))

             {::http/body body})))))))

(defn GET [req]
  (conditional/evaluate-preconditions! req)
  (-> req
      (assoc :ring.response/status 200)
      (response/add-payload)))

(defn post-variant [{::site/keys [xt-node db uri]
                     ::apex/keys [request-payload]
                     :as req}]
  (let [location
        (str uri (hash (select-keys request-payload [::site/resource ::site/variant])))
        existing (xt/entity db location)]

    (->> (xt/submit-tx
          xt-node
          [[:xtdb.api/put (merge {:xt/id location} request-payload)]])
         (xt/await-tx xt-node))

    (into req {:ring.response/status (if existing 204 201)
               :ring.response/headers {"location" location}})))

(defn post-redirect [{::site/keys [xt-node db]
                      :as req}]
  (let [resource-state (::apex/request-payload (openapi/validate-request-payload req))
        {::site/keys [resource]} resource-state
        existing (xt/entity db resource)]
    (->> (xt/submit-tx
          xt-node
          [[:xtdb.api/put
            (merge
             {:xt/id resource}
             ;; ::site/resource = :xt/id, no need to duplicate
             (dissoc resource-state ::site/resource))]])
         (xt/await-tx xt-node))

    (into req {:ring.response/status (if existing 204 201)})))

(defn POST [{::site/keys [xt-node resource]
             ::pass/keys [subject]
             :ring.request/keys [method]
             :as req}]
  (let [rep (receive-representation req)

        content-type (get-in req [:ring.request/headers "content-type"] "application/octet-stream")

        body-as-value
        (case content-type
          "application/x-www-form-urlencoded"
          (ring.util.codec/form-decode (String. (::http/body rep)))
          )

        ;; TODO: Should we fail if more than one permitted action available?
        permitted-action (::pass/action (first (::pass/permitted-actions req)))

        pass-ctx {::pass/subject (:xt/id subject)
                  ::site/resource (:xt/id resource)}

        ;; Default
        req (assoc req :ring.response/status 200)

        req


        req

        #_post-fn #_(::site/post-fn resource)
        #_post
        #_(cond
            (fn? post-fn) post-fn

            (symbol? post-fn)
            (try
              (or
               (requiring-resolve post-fn)
               (throw
                (ex-info
                 (format "Requiring resolve of %s returned nil" post-fn)
                 {:post-fn post-fn
                  ::site/request-context (assoc req :ring.response/status 500)})))
              (catch Exception e
                (throw
                 (ex-info
                  (format "post-fn '%s' is not resolvable" post-fn)
                  {::post-fn post-fn
                   ::site/request-context (assoc req :ring.response/status 500)}
                  e))))

            (nil? post-fn)
            (throw
             (ex-info
              "Resource allows POST but doesn't have a post-fn function"
              {::site/request-context (assoc req :ring.response/status 500)}))

            :else
            (throw
             (ex-info
              (format "post-fn is neither a function or a symbol, but type '%s'" (type post-fn))
              {::site/request-context (assoc req :ring.response/status 500)})))


        ]

    (try
      (authz/do-action req pass-ctx (:xt/id permitted-action) body-as-value)
      (catch Exception e
        (throw
         (ex-info
          (.getMessage e)
          {::site/request-context
           (merge (assoc req :ring.response/status 500)
                  (ex-data e))
           :permitted-action permitted-action
           :received-representation rep
           :body-as-value body-as-value}
          e))))

    ;;(assert post)

    ;; TODO: Depending on the context, it might be applicable to reveal the URI
    ;; of any newly created resource with a 201 and Location header. This
    ;; determination perhaps ought to be configured into the Action doing the
    ;; creation.
    ;;(assoc req :ring.response/status 200)
    ;;(post req)
    ))

(defn PUT [{::site/keys [resource] :as req}]
  (let [rep (receive-representation req) _ (assert rep)
        req (assoc req ::site/received-representation rep)
        put-fn (::site/put-fn resource)]

    (log/tracef "PUT, put-fn is %s" put-fn)
    (let [put
          (cond
            (fn? put-fn)
            put-fn

            (symbol? put-fn)
            (try
              (or
               (requiring-resolve put-fn)
               (throw (ex-info (format "Requiring resolve of %s returned nil" put-fn) {:put-fn put-fn})))
              (catch Exception e
                (throw
                 (ex-info
                  (format "put-fn '%s' is not resolvable" put-fn)
                  {::put-fn put-fn
                   ::site/request-context (assoc req :ring.response/status 500)}
                  e))))
            (nil? put-fn)
            (throw
             (ex-info
              "Resource allows PUT but doesn't contain a put-fn function"
              {::site/request-context (assoc req :ring.response/status 500)}))

            :else
            (throw
             (ex-info
              (format "put-fn is neither a function or a symbol, but type '%s'" (type put-fn))
              {::site/request-context (assoc req :ring.response/status 500)})))]
      (if-let [response (put req)]
        response
        (throw
         (ex-info
          "put-fn returned a nil response"
          {:put-fn put-fn
           ::site/request-context (assoc req :ring.response/status 500)}))))))

(defn PATCH [{::site/keys [resource] :as req}]
  (let [rep (receive-representation req) _ (assert rep)
        req (assoc req ::site/received-representation rep)
        patch-fn (::site/patch-fn resource)]

    ;; TODO: evaluate preconditions (in tx fn)
    (cond
      (fn? patch-fn) (patch-fn req)
      :else
      (throw
       (ex-info
        "Resource allows PATCH but doesn't contain have a patch-fn function"
        {::site/request-context (assoc req :ring.response/status 500)})))))

(defn DELETE [{::site/keys [xt-node uri] :as req}]
  (let [tx (xt/submit-tx xt-node [[:xtdb.api/delete uri]])]
    (xt/await-tx xt-node tx)
    (into req {:ring.response/status 204})))

(defn OPTIONS [{::site/keys [resource allowed-methods] :as req}]
  ;; TODO: Implement *
  (let [{::site/keys [access-control-allow-origins]} resource
        request-origin (get-in req [:ring.request/headers "origin"])

        [resource-origin allow-origin]
        (or
         (when-let [ro (get access-control-allow-origins request-origin)]
           [ro request-origin])
         (when-let [ro (get access-control-allow-origins "*")]
           [ro "*"]))

        access-control-allow-methods
        (get resource-origin ::site/access-control-allow-methods)
        access-control-allow-headers
        (get resource-origin ::site/access-control-allow-headers)
        access-control-allow-credentials
        (get resource-origin ::site/access-control-allow-credentials)]

    (log/trace "In OPTIONS")

    (cond-> (into req {:ring.response/status 200})

      true (update :ring.response/headers
                   merge
                   (::http/options resource)
                   {"allow" (join-keywords allowed-methods true)
                    ;; TODO: Shouldn't this be a situation (a missing body) detected
                    ;; by middleware, which can set the content-length header
                    ;; accordingly?
                    "content-length" "0"})

      access-control-allow-methods
      (update
       :ring.response/headers
       (fn [headers]
         (cond-> headers
           allow-origin (assoc "access-control-allow-origin" allow-origin)
           access-control-allow-methods (assoc "access-control-allow-methods" (join-keywords access-control-allow-methods true))
           access-control-allow-headers (assoc "access-control-allow-headers" (join-keywords access-control-allow-headers false))
           access-control-allow-credentials (assoc "access-control-allow-credentials" access-control-allow-credentials)))))))

(defn PROPFIND [req]
  (dave.methods/propfind req))

(defn MKCOL [{::site/keys [xt-node uri]}]
  (let [tx (xt/submit-tx
            xt-node
            [[:xtdb.api/put
              {:xt/id uri
               ::dave/resource-type :collection
               ::site/methods {:get {} :head {} :options {} :propfind {}}
               ::http/content-type "text/html;charset=utf-8"
               ::http/content "<h1>Index</h1>\r\n"
               ::http/options {"DAV" "1"}}]])]
    (xt/await-tx xt-node tx))
  {:ring.response/status 201
   :ring.response/headers {}})

(defmethod transform-value "password" [_ instance]
  (password/encrypt instance 11))

(defmethod transform-value "inst" [_ instance]
  (read-instant-date instance))

(defn wrap-method-not-implemented? [h]
  (fn [{:ring.request/keys [method] :as req}]
    (when-not (contains?
               #{:get :head :post :put :delete :options
                 :patch
                 :mkcol :propfind} method)
      (throw
       (ex-info
        "Method not implemented"
        {::site/request-context (assoc req :ring.response/status 501)})))
    (h req)))

(defn wrap-locate-resource [h]
  (fn [req]
    (let [res (locator/locate-resource req)]
      (log/debugf "Resource provider: %s" (::site/resource-provider res))
      (h (assoc req ::site/resource res)))))

(defn wrap-redirect [h]
  (fn [{::site/keys [resource] :ring.request/keys [method] :as req}]
    (when (= (::site/type resource) "Redirect")
      (let [status (case method (:get :head) 302 307)]
        (throw
         (ex-info
          "Redirect"
          {:location (::site/location resource)
           ::site/request-context
           (-> req
               (assoc :ring.response/status status)
               (update :ring.response/headers
                       assoc "location" (::site/location resource)))}))))
    (h req)))

(defn wrap-find-current-representations
  [h]
  (fn [{:ring.request/keys [method] :as req}]
    (if (#{:get :head :put} method)
      (let [cur-reps (seq (conneg/current-representations req))]
        (when (and (#{:get :head} method) (empty? cur-reps))
          (throw
           (ex-info
            "Not Found"
            {::site/request-context (assoc req :ring.response/status 404)})))
        (h (assoc req ::site/current-representations cur-reps)))
      (h req))))

(defn wrap-negotiate-representation [h]
  (fn [req]
    (let [cur-reps (::site/current-representations req)]
      (h (cond-> req
           (seq cur-reps)
           (assoc
            ::site/selected-representation
            (conneg/negotiate-representation req cur-reps)))))))

(defn wrap-http-authenticate [h]
  (fn [req]
    (h (http-authn/authenticate req))))

#_(defn wrap-authorize-with-acls [h]
    (fn [{::pass/keys [session] ::site/keys [resource] :as req}]
      (when (::pass/authorization resource)
        (log/trace "Already authorized")
        )
      (h (cond-> req
           ;; Only authorize if not already authorized
           (not (::pass/authorization resource))
           authz/authorize-resource))))

#_(defn wrap-authorize-with-pdp
  ;; Do authorization as late as possible (in order to have as much data
  ;; as possible to base the authorization decision on. However, note
  ;; Section 8.5, RFC 4918 states "the server MUST do authorization checks
  ;; before checking any HTTP conditional header.".
  [h]
  (fn [{:ring.request/keys [method] ::site/keys [db resource] ::pass/keys [subject session] :as req}]
    (if (::pass/authorization resource)
      ;; If authorization already established, this is sufficient
      (do
        (log/tracef "pre-authorized: %s" (::pass/authorization resource))
        (h req))
      ;; Otherwise, authorize with PDP
      (let [request-context
            {'subject subject
             'resource (dissoc resource ::http/body ::http/content)
             'request (select-keys
                       req
                       [:ring.request/headers :ring.request/method :ring.request/path
                        :ring.request/query :ring.request/protocol :ring.request/remote-addr
                        :ring.request/scheme :ring.request/server-name :ring.request/server-post
                        :ring.request/ssl-client-cert
                        ::site/uri])
             'representation (dissoc resource ::http/body ::http/content)
             'environment {}}

            authz (when (not= method :options)
                    (pdp/authorization db request-context))

            req (cond-> req
                  true (assoc ::pass/request-context request-context)
                  authz (update ::site/resource assoc ::pass/authorization authz)
                  ;; If the max-content-length has been modified, update that in the
                  ;; resource
                  (::http/max-content-length authz)
                  (update ::site/resource
                          assoc ::http/max-content-length (::http/max-content-length authz)))]

        (when (and (not= method :options)
                   (not= (::pass/access authz) ::pass/approved))
          (let [status
                (if (or
                     (::pass/user subject)
                     ;; This is temporarily coupled to the new authz in order to
                     ;; generate a correct 403 response. We need to rework this
                     ;; to give these two authorization mechanisms independence
                     ;; from each other.
                     (:juxt.pass.jwt/sub session))
                  403
                  401)]
            (throw
             (ex-info
              (case status 401  "Unauthorized" 403 "Forbidden")
              {::site/request-context (assoc req :ring.response/status status)}))))
        (h req)))))

(defn wrap-method-not-allowed? [h]
  (fn [{::site/keys [resource] :ring.request/keys [method] :as req}]
    (when-not (map? (::site/methods resource))
      (throw (ex-info "Resource :juxt.site.alpha/methods must be a map"
                      {:resource resource
                       ::site/request-context req})))
    (if resource
      (let [allowed-methods (set (keys (::site/methods resource)))]
        (when-not (contains? allowed-methods method)
          (throw
           (ex-info
            "Method not allowed"
            {:method method
             ::site/allowed-methods allowed-methods
             ::site/request-context
             (into
              req
              {:ring.response/status 405
               :ring.response/headers {"allow" (join-keywords allowed-methods true)}})})))
        (h (assoc req ::site/allowed-methods allowed-methods)))
      (h req))))

(defn wrap-invoke-method [h]
  (fn [{:ring.request/keys [method] :as req}]
    (h (case method
         (:get :head) (GET req)
         :post (POST req)
         :put (PUT req)
         :patch (PATCH req)
         :delete (DELETE req)
         :options (OPTIONS req)
         :propfind (PROPFIND req)
         :mkcol (MKCOL req)))))

(defn wrap-triggers [h]
  ;; Site-specific step: Check for any observers and 'run' them TODO:
  ;; Perhaps effects need to run against happy and sad paths - i.e. errors
  ;; - this should really be in a 'finally' block.
  (fn [{::site/keys [xt-node] ::pass/keys [subject] :as req}]

    (let [db (xt/db xt-node) ; latest post-method db
          result (h req)

          triggers
          (map first
               (xt/q db '{:find [rule]
                         :where [[rule ::site/type "Trigger"]]}))

          request-context
          {'subject subject
           'request (select-keys
                     req
                     [:ring.request/headers :ring.request/method :ring.request/path
                      :ring.request/query :ring.request/protocol :ring.request/remote-addr
                      :ring.request/scheme :ring.request/server-name :ring.request/server-post
                      :ring.request/ssl-client-cert
                      ::site/uri])
           'environment {}}]

      (try
        ;; TODO: Can we use the new refreshed db here to save another call to xt/db?
        (let [actions (rules/eval-triggers (xt/db xt-node) triggers request-context)]
          (when (seq actions)
            (log/tracef "Triggered actions are %s" (pr-str actions)))
          (doseq [action actions]
            (log/tracef "Running action: %s" (get-in action [:trigger ::site/action]))
            (triggers/run-action! req action)))
        (catch clojure.lang.ExceptionInfo e
          (log/error e (format "Failed to run trigger/action: %s" (pr-str (ex-data e)))))
        (catch Exception e
          (log/error e "Failed to run trigger/action")))

      result)))

(defn wrap-security-headers [h]
  (fn [req]
    ;; TODO - but see wrap-cors-headers middleware
    ;;        Strict-Transport-Security: max-age=31536000; includeSubdomains
    ;;        X-Frame-Options: DENY
    ;;        X-Content-Type-Options: nosniff
    ;;        X-XSS-Protection: 1; mode=block
    ;;        X-Download-Options: noopen
    ;;        X-Permitted-Cross-Domain-Policies: none

    (let [res (h req)]
      (cond-> res
        ;; Don't allow Google to track your site visitors. Disable FLoC.
        true (assoc-in [:ring.response/headers "permissions-policy"] "interest-cohort=()")))))

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

(defn redact [req]
  (-> req
      (update :ring.request/headers
              (fn [headers]
                (cond-> headers
                  (contains? headers "authorization")
                  (assoc "authorization" "(redacted)"))))))

(defn ->storable [{::site/keys [request-id db] :as req}]
  (-> req
      (into (select-keys db [:xtdb.api/valid-time :xtdb.api/tx-id]))
      (assoc :xt/id request-id ::site/type "Request")
      redact
      (dissoc ::site/xt-node ::site/db :ring.request/body :ring.response/body)
      (util/deep-replace
       (fn [form]
         (cond-> form
           (and (string? form) (>= (count form) 1024))
           (subs 0 1024)

           (and (vector? form) (>= (count form) 64))
           (subvec 0 64)

           (and (list? form) (>= (count form) 64))
           (#(take 64 %)))))))

(defn log-request! [{:ring.request/keys [method] :as req}]
  (assert method)
  (log/infof
   "%-7s %s %s %d"
   (str/upper-case (name method))
   (:ring.request/path req)
   (:ring.request/protocol req)
   (:ring.response/status req)))

(defn respond
  [{::site/keys [selected-representation start-date base-uri request-id]
    :ring.request/keys [method]
    :ring.response/keys [body]
    :as req}]

  (assert req)
  (assert start-date)

  (let [end-date (java.util.Date.)
        req (assoc req
                   ::site/end-date end-date
                   ::site/date end-date
                   ::site/duration-millis (- (.getTime end-date)
                                             (.getTime start-date)))]
    (cond->
        (update req
                :ring.response/headers
                assoc "date" (format-http-date start-date))

        request-id
        (update :ring.response/headers
                assoc "site-request-id"
                (cond-> request-id
                  ;; Not sure I like this shortening, it's inconvenient to have
                  ;; to prepend the base-uri each time
                  #_(.startsWith request-id base-uri)
                  #_(subs (count base-uri))))

        selected-representation
        (update :ring.response/headers
                representation-headers selected-representation body)

        (= method :head) (dissoc :ring.response/body))))

(defn wrap-initialize-response [h]
  (fn [req]
    (assert req)
    (let [response (h req)]
      (assert response)
      (respond response))))

(defn access-control-match-origin [allow-origins origin]
  (some (fn [[pattern result]]
          (when (re-matches (re-pattern pattern) origin)
            result))
        allow-origins))

(defn wrap-cors-headers [h]
  (fn [req]
    (let [{::site/keys [resource] :as req}
          (h req)

          request-origin (get-in req [:ring.request/headers "origin"])
          {::site/keys [access-control-allow-origins]} resource

          access-control
          (when request-origin
            (access-control-match-origin access-control-allow-origins request-origin))]

      (cond-> req
        access-control
        (assoc-in [:ring.response/headers "access-control-allow-origin"] request-origin)

        (contains? access-control ::site/access-control-allow-methods)
        (assoc-in [:ring.response/headers "access-control-allow-methods"]
                  (join-keywords (get access-control ::site/access-control-allow-methods) true))
        (contains? access-control ::site/access-control-allow-headers)
        (assoc-in [:ring.response/headers "access-control-allow-headers"]
                  (join-keywords (get access-control ::site/access-control-allow-headers) false))
        (contains? access-control ::site/access-control-allow-credentials)
        (assoc-in [:ring.response/headers "access-control-allow-credentials"]
                  (str (get access-control ::site/access-control-allow-credentials)))))))

(defn status-message [status]
  (case status
    200 "OK"
    201 "Created"
    202 "Accepted"
    204 "No Content"
    302 "Found"
    307 "Temporary Redirect"
    304 "Not Modified"
    400 "Bad Request"
    401 "Unauthorized"
    403 "Forbidden"
    404 "Not Found"
    405 "Method Not Allowed"
    409 "Conflict"
    411 "Length Required"
    412 "Precondition Failed"
    413 "Payload Too Large"
    415 "Unsupported Media Type"
    500 "Internal Server Error"
    501 "Not Implemented"
    503 "Service Unavailable"
    "Error"))

(defn errors-with-causes
  "Return a collection of errors with their messages and causes"
  [e]
  (let [cause (.getCause e)]
    (cons
     (cond->
         {:message (.getMessage e)
          :stack-trace (.getStackTrace e)}
       (instance? clojure.lang.ExceptionInfo e)
       (assoc :ex-data (dissoc (ex-data e) ::site/request-context)))
     (when cause (errors-with-causes cause)))))

(defn put-error-representation
  "If method is PUT"
  [{::site/keys [resource] :ring.response/keys [status] :as req} e]
  (let [{::http/keys [put-error-representations]} resource
        put-error-representations
        (filter
         (fn [rep]
           (if-some [applies-to (:ring.response/status rep)]
             (= applies-to status)
             true)) put-error-representations)]

    (when (seq put-error-representations)
      (some-> (conneg/negotiate-representation req put-error-representations)
              ;; Content-Location is not appropriate for errors.
              (dissoc ::http/content-location)))))

(defn post-error-representation
  "If method is POST"
  [{::site/keys [resource] :ring.response/keys [status] :as req} e]
  (let [{::http/keys [post-error-representations]} resource
        post-error-representations
        (filter
         (fn [rep]
           (if-some [applies-to (:ring.response/status rep)]
             (= applies-to status)
             true)) post-error-representations)]

    (when (seq post-error-representations)
      (some-> (conneg/negotiate-representation req post-error-representations)
              ;; Content-Location is not appropriate for errors.
              (dissoc ::http/content-location)))))

;; TODO: I'm beginning to think that a resource should include the handling of
;; all possible errors and error representations. So there shouldn't be such a
;; thing as an 'error resource'. In this perspective, site-wide 'common'
;; policies, such as a common 404 page, can be merged into the resource by the
;; resource locator. Whether a user-agent is given error information could be
;; subject to policy which is part of a common configuration. But the resource
;; itself should be ignorant of such policies. Additionally, this is more
;; aligned to OpenAPI's declaration of per-resource errors.

(defn error-resource
  "Locate an error resource. Currently only uses a simple database lookup of an
  'ErrorResource' entity matching the status. In future this could use rules to
  use the environment (dev vs. prod), subject (developer vs. customer) or other
  variables to determine the resource to use."
  [{::site/keys [db]} status]
  (when-let [res (ffirst
            (xt/q db '{:find [(pull er [*])]
                      :where [[er ::site/type "ErrorResource"]
                              [er :ring.response/status status]]
                      :in [status]} status))]
    (log/tracef "ErrorResource found for status %d: %s" status res)
    res))

(defn error-resource-representation
  "Experimental. Not sure this is a good idea to have a 'global' error
  resource. Better to merge error handling into each resource (using the
  resource locator)."
  [req e]
  (let [{:ring.response/keys [status]} req]

    (when-let [er (error-resource req (or status 500))]
      (let [
            ;; Allow errors to be transmitted to developers
            er
            (assoc er
                   ::site/access-control-allow-origins
                   {"http://localhost:8000"
                    {::site/access-control-allow-methods #{:get :put :post :delete}
                     ::site/access-control-allow-headers #{"authorization" "content-type"}
                     ::site/access-control-allow-credentials true}})

            er
            (-> er
                #_(update ::site/template-model assoc
                          "_site" {"status" {"code" status
                                             "message" (status-message status)}
                                   "error" {"message" (.getMessage e)}
                                   "uri" (::site/uri req)}))

            error-representations
            (conneg/current-representations
             (assoc req
                    ::site/resource er
                    ::site/uri (:xt/id er)))]

        (when (seq error-representations)
          (some-> (conneg/negotiate-representation req error-representations)
                  ;; Content-Location is not appropriate for errors.
                  (dissoc ::http/content-location)))))))

(defn respond-internal-error [{::site/keys [request-id] :as req} e]
  (log/error e (str "Internal Error: " (.getMessage e)))
  ;; TODO: We should allow an ErrorResource for 500 errors

  ;; TODO: Negotiate a better format for internal server errors

  (let [default-body
        (str "<body>\r\n"
             (cond-> "<h1>Internal Server Error</h1>\r\n"
               request-id (str (format "<p><a href=\"%s\" target=\"_site_error\">%s</a></p>\r\n" request-id "Error")))
             "</body>\r\n")]
    (respond
     (into
      req
      {:ring.response/status 500
       :ring.response/body default-body
       ::site/errors (errors-with-causes e)
       ::site/selected-representation
       {::http/content-type "text/html;charset=utf-8"
        ::http/content-length (count default-body)
        :ring.response/body default-body}
       }))))

(defn error-response
  "Respond with the given error"
  [req e]

  (assert (::site/start-date req))

  (let [{:ring.response/keys [status]
         :ring.request/keys [method]
         ::site/keys [request-id]} req

        representation
        (or
         (when (= method :put) (put-error-representation req e))
         (when (= method :post) (post-error-representation req e))
         (error-resource-representation req e)

         ;; Some default representations for errors
         (some->
          (conneg/negotiate-representation
           req
           [(let [content
                  ;; We don't want to provide much information here, we don't
                  ;; know much about the recipient, only that they're probably
                  ;; using a web browser. We provide a link to the error
                  ;; resource, because that will be subject to authorization
                  ;; checks. So authorized users get to see extensive error
                  ;; information, unauthorized users don't.
                  (cond-> (str (status-message status) "\r\n")
                    request-id (str (format "<a href=\"%s\" target=\"_site_error\">%s</a>\r\n" request-id "Error")))]
              {::http/content-type "text/html;charset=utf-8"
               ::http/content-length (count content)
               ::http/content content})
            (let [content (str (status-message status) "\r\n")]
              {::http/content-type "text/plain;charset=utf-8"
               ::http/content-length (count content)
               ::http/content content})])

          ;; This is an error, it won't be cached, it isn't negotiable 'content'
          ;; so the Vary header isn't deemed applicable. Let's not set it.
          (dissoc ::http/vary)))

        error-resource (merge
                        {:ring.response/status 500
                         ::site/errors (errors-with-causes e)}
                        (dissoc req ::site/request-context)
                        ;; For the error itself
                        {::site/selected-representation representation})

        error-resource (assoc
                        error-resource
                        ::site/status-message (status-message (:ring.response/status error-resource)))

        response (try
                   (cond-> error-resource
                       (not= method :head) response/add-payload)
                   (catch Exception e
                     (respond-internal-error req e)))]

    (respond response)))


(defn wrap-error-handling
  "Return a handler that constructs proper Ring responses, logs and error
  handling where appropriate."
  [h]
  (fn [{::site/keys [request-id] :as req}]
    (org.slf4j.MDC/put "reqid" request-id)
    (try
      (h req)
      (catch clojure.lang.ExceptionInfo e
        ;; When throwing ex-info, try to add the ::site/request-context key,
        ;; using the request of the cause as the base. In this way, the cause
        ;; request (which is more recent), predominates but a catcher can always
        ;; override aspects, such as the ring.response/status.

        (log/errorf e "wrap-error-handling, ex-data: %s" (pr-str (ex-data e)))

        (let [ex-data
              (-> (ex-data e)
                  (update-in [::site/request-context :ring.response/status]
                             (fn [x] (or x 500))))
              rc (or (::site/request-context ex-data) req)
              status (:ring.response/status rc)]

          (if (::site/start-date rc)
            (do
              ;; Don't log exceptions which are used to escape (e.g. 302, 401).
              (when (or (not (integer? status)) (>= status 500))
                (let [ex-data (->storable ex-data)]
                  (log/errorf e "%s: %s" (.getMessage e) (pr-str (dissoc ex-data ::site/request-context)))))

              (assert (::site/start-date rc))
              (error-response rc e))

            (error-response
             req
             (ex-info "ExceptionInfo caught, but with an invalid request-context attached" {::site/request-context rc} e)))))

      (catch Throwable t (respond-internal-error req t))
      (finally (org.slf4j.MDC/clear)))))

(defn wrap-check-error-handling
  "Ensure that on exceptions slip through the net."
  [h]
  (fn [req]
    (try
      (h req)
      (catch Throwable e
        ;; We're expecting the error handling to have fully resolved any
        ;; errors, it should not throw
        (log/error e "ERROR NOT HANDLED!")
        (throw e)))))

;; See https://portswigger.net/web-security/host-header and similar TODO: Have a
;; 'whitelist' in the config to check against - but this would require a reboot,
;; or some mechanism of freshening the whitelist without restart.
(def host-header-parser (rfc7230.decoders/host {}))

(defn assert-host [host]
  (try
    (host-header-parser (re/input host))
    (catch Exception e
      (throw (ex-info "Illegal host format" {:host host} e)))))

(defn new-request-id [base-uri]
  (str base-uri "/_site/requests/"
       (subs (util/hexdigest
              (.getBytes (str (java.util.UUID/randomUUID)) "US-ASCII")) 0 24)))

(defn normalize-path
  "Normalize path prior to constructing URL used for resource lookup. This is to
  avoid two equivalent URLs pointing to two different XTDB entities."
  [path]
  (cond
    (str/blank? path) "/"
    :else (-> path
              ;; Normalize (remove dot-segments from the path, see Section 6.2.2.3 of
              ;; RFC 3986)
              ((fn [path] (.toString (.normalize (URI. path)))))
              ;; TODO: Upcase any percent encodings: e.g. %2f -> %2F (as per Sections
              ;; 2.1 and 6.2.2.1 of RFC 3986)

              ;; TODO: Replace each space with '+'

              ;; TODO: Decode any non-reserved (:/?#[]@!$&'()*+,;=) percent-encoded
              ;; octets (see Section 6.2.2.2 of RFC 3986)
              )))

(defn http-scheme-normalize
  "Return a scheme-based normalized host, as per Section 6.2.3 of RFC 3986."
  [s scheme]
  (case scheme
    :http (if-let [[_ x] (re-matches #"(.*):80" s)] x s)
    :https (if-let [[_ x] (re-matches #"(.*):443" s)] x s)))

(defn wrap-initialize-request
  "Initialize request state."
  [h {::site/keys [xt-node base-uri uri-prefix] :as opts}]
  (assert xt-node)
  (assert base-uri)
  (fn [{:ring.request/keys [scheme] :as req}]
    (let [db (xt/db xt-node)
          req-id (new-request-id base-uri)
          scheme+authority
          (or uri-prefix
              (->
               (let [{::rfc7230/keys [host]}
                     (host-header-parser
                      (re/input
                       (or
                        (get-in req [:ring.request/headers "x-forwarded-host"])
                        (get-in req [:ring.request/headers "host"]))))]
                 (str (or (get-in req [:ring.request/headers "x-forwarded-proto"])
                          (name scheme))
                      "://" host))
               (str/lower-case)         ; See Section 6.2.2.1 of RFC 3986
               (http-scheme-normalize scheme)))

          ;; The scheme+authority is already normalized (by transforming to
          ;; lower-case). The path, however, needs to be normalized here.
          uri (str scheme+authority (normalize-path (:ring.request/path req)))

          req (into req (merge
                         {::site/start-date (java.util.Date.)
                          ::site/request-id req-id
                          ::site/uri uri
                          ::site/db db}
                         (dissoc opts ::site/uri-prefix)))]

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
              :query-string :ring.request/query
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
           {} req)
          (h)
          (select-keys
           [:ring.response/status
            :ring.response/headers
            :ring.response/body])
          (set/rename-keys {:ring.response/status :status
                            :ring.response/headers :headers
                            :ring.response/body :body})))))

(defn wrap-store-request-in-request-cache [h]
  (fn [req]
    (let [req (h req)]
      (when-let [req-id (::site/request-id req)]
        (cache/put! cache/requests-cache req-id (->storable req)))
      req)))

(defn wrap-store-request [h]
  (fn [req]
    (let [req (h req)]
      (when-let [req-id (::site/request-id req)]
        (let [{:ring.request/keys [method] ::site/keys [xt-node]} req]
          (when (or (= method :post) (= method :put))
            (xt/submit-tx
             xt-node
             [[:xtdb.api/put (-> req ->storable
                                (select-keys [:juxt.pass.alpha/subject
                                              ::site/date
                                              ::site/uri
                                              :ring.request/method
                                              :ring.response/status])
                                (assoc :xt/id req-id
                                       ::site/type "Request"))]]))))
      req)))

(defn wrap-log-request [h]
  (fn [req]
    (let [req (h req)]
      (assert req)
      (log-request! req)
      req)))

(defn wrap-healthcheck
  [h]
  (fn [req]
    (if (= "/_site/healthcheck" (:ring.request/path req))
      {:ring.response/status 200 :ring.response/body "Site OK!\r\n"}
      (h req))))

(defn service-available? [req]
  true)

(defn wrap-service-unavailable?
  ""
  [h]
  (fn [req]
    (when-not (service-available? req)
      (throw
       (ex-info
        "Service unavailable"
        {::site/request-context
         (-> req
             (into {:ring.response/status 503})
             (assoc-in [:ring.response/headers "retry-after"] "120"))})))
    (h req)))

(defn make-pipeline
  "Make a pipeline of Ring middleware. Note, that each Ring middleware designates
  a processing stage. An interceptor chain (perhaps using Pedestal (pedestal.io)
  or Sieppari (https://github.com/metosin/sieppari) could be used. This is
  currently a synchronous chain but async could be supported in the future."
  [opts]
  [
   ;; Switch Ring requests/responses to Ring 2 namespaced keywords
   wrap-ring-1-adapter

   ;; Optional, helpful for AWS ALB
   wrap-healthcheck

   ;; Initialize the request by merging in some extra data
   #(wrap-initialize-request % opts)

   wrap-service-unavailable?

   ;; Logging and store request
   wrap-log-request
   wrap-store-request
   wrap-store-request-in-request-cache

   ;; Security
   wrap-cors-headers
   wrap-security-headers

   ;; Error handling
   wrap-check-error-handling
   wrap-error-handling

   ;; 501
   wrap-method-not-implemented?

   ;; Authenticate

   ;; Rewrite to work against a cookie-scope, which may use JWTs or sessions.
   ;; wrap-process-cookies
   ;;oldsession/wrap-associate-session
   session-scope/wrap-session-scopes

   wrap-http-authenticate

   ;; Locate resources
   wrap-locate-resource
   wrap-redirect

   ;; 405
   wrap-method-not-allowed?

   ;; We authorize the resource, prior to finding representations.
   authz/wrap-authorize-with-actions

   ;; Find representations and possibly do content negotiation
   wrap-find-current-representations
   wrap-negotiate-representation

   ;; Authorize - deprecated
   #_wrap-authorize-with-acls
   #_wrap-authorize-with-pdp

   ;; Custom middleware for Site
   #_wrap-triggers

   ;; Create initial response
   wrap-initialize-response

   ;; Methods (GET, PUT, POST, etc.)
   wrap-invoke-method])

(defn make-handler [opts]
  ((apply comp (make-pipeline opts)) identity))
