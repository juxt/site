;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.handler
  (:require
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
   [juxt.pick.alpha.core :refer [rate-representation]]
   [juxt.pick.alpha.ring :refer [pick decode-maybe]]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.decoders.rfc7230 :as rfc7230.decoders]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.reap.alpha.regex :as re]
   [juxt.reap.alpha.rfc7231 :as rfc7231]
   [juxt.reap.alpha.rfc7232 :as rfc7232]
   [juxt.reap.alpha.ring :refer [headers->decoded-preferences]]
   [juxt.site.alpha.locator :as locator]
   [juxt.site.alpha.util :as util]
   [juxt.site.alpha.triggers :as triggers]
   [taoensso.nippy.utils :refer [freezable?]]
   [juxt.site.alpha.rules :as rules])
  (:import (java.net URI)))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'rfc7230 (create-ns 'juxt.reap.alpha.rfc7230))

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

(defn receive-representation
  "Check and load the representation enclosed in the request message payload."
  [{::site/keys [resource start-date] :as req}]

  (let [content-length
        (try
          (some->
           (get-in req [:ring.request/headers "content-length"])
           (Long/parseLong))
          (catch NumberFormatException e
            (throw
             (ex-info
              "Bad content length"
              (into req {:ring.response/status 400
                         :ring.response/body "Bad Request\r\n"})
              e))))]

    (when (nil? content-length)
      (throw
       (ex-info
        "No Content-Length header found"
        (into req {:ring.response/status 411
                   :ring.response/body "Length Required\r\n"}))))

    ;; Spin protects resources from PUTs that are too large. If you need to
    ;; exceed this limitation, explicitly declare ::spin/max-content-length in
    ;; your resource.
    (when-let [max-content-length (get resource ::http/max-content-length (Math/pow 2 16))]
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          (into req
                {:ring.response/status 413
                 :ring.response/body "Payload Too Large\r\n"})))))

    (when-not (:ring.request/body req)
      (throw
       (ex-info
        "No body in request"
        (into req {:ring.response/status 400
                   :ring.response/body "Bad Request\r\n"}))))

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

      (when-let [acceptable (::http/acceptable resource)]

        (let [prefs (headers->decoded-preferences acceptable)
              request-rep (rate-representation prefs decoded-representation)]

          (when (or (get prefs "accept") (get prefs "accept-charset"))
            (cond
              (= (:juxt.pick.alpha/content-type-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-type of the request payload is not supported by the resource"
                (into req
                      {:ring.response/status 415
                       :ring.response/body "Unsupported Media Type\r\n"
                       ::acceptable acceptable
                       ::content-type (get request-rep "content-type")})))

              (and
               (= "text" (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/type]))
               (get prefs "accept-charset")
               (not (contains? (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map]) "charset")))
              (throw
               (ex-info
                "The Content-Type header in the request is a text type and is required to specify its charset as a media-type parameter"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-type (get request-rep "content-type")})))

              (= (:juxt.pick.alpha/charset-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The charset of the Content-Type header in the request is not supported by the resource"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-type (get request-rep "content-type")})))))

          (when (get prefs "accept-encoding")
            (cond
              (= (:juxt.pick.alpha/content-encoding-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-encoding in the request is not supported by the resource"
                (into req {:ring.response/status 409
                           :ring.response/body "Conflict\r\n"
                           ::acceptable acceptable
                           ::content-encoding (get-in req [:ring.request/headers "content-encoding"] "identity")})))))

          (when (get prefs "accept-language")
            (cond
              (not (contains? (:ring.response/headers req) "content-language"))
              (throw
               (ex-info
                "Request must contain Content-Language header"
                (into req {:ring.response/status 409
                           :ring.response/body "Conflict\r\n"
                           ::acceptable acceptable
                           ::content-language (get-in req [:ring.request/headers "content-language"])})))

              (= (:juxt.pick.alpha/content-language-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-language in the request is not supported by the resource"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-language (get-in req [:ring.request/headers "content-language"])})))))))

      (when (get-in req [:ring.request/headers "content-range"])
        (throw
         (ex-info
          "Content-Range header not allowed on a PUT request"
          (into req
                {:ring.response/status 400
                 :ring.response/body "Bad Request\r\n"}))))

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

(defn evaluate-if-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource. If the precondition is found to be false, an exception is thrown
  with ex-data containing the proper response."
  [{::site/keys [current-representations] :as req}]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (when-let [header-field (reap/if-match (get-in req [:ring.request/headers "if-match"]))]
    (log/tracef "evaluate-if-match! %s" header-field)
    (cond
      ;; "If the field-value is '*' …"
      (and (map? header-field)
           (::rfc7232/wildcard header-field)
           (empty? current-representations))
      ;; "… the condition is false if the origin server does not have a current
      ;; representation for the target resource."
      (throw
       (ex-info
        "If-Match precondition failed"
        (into req
              {:ring.response/status 412
               :ring.response/body "Precondition Failed\r\n"})))

      (sequential? header-field)
      (do
        (log/tracef "evaluate-if-match! sequential? true, header-field: %s" header-field)
        (let [matches (for [rep current-representations
                            :let [rep-etag (some-> (get rep ::http/etag) reap/entity-tag)]
                            etag header-field
                            ;; "An origin server MUST use the strong comparison function
                            ;; when comparing entity-tags"
                            :let [_ (log/tracef "evaluate-if-match! - compare %s with %s" etag rep-etag)]
                            :when (rfc7232/strong-compare-match? etag rep-etag)]
                        etag)]
          (log/tracef "matches: %d: %s" (count matches) matches)
          (when-not (seq matches)

              ;; TODO: "unless it can be determined that the state-changing
              ;; request has already succeeded (see Section 3.1)"
              (throw
               (ex-info
                "No strong matches between if-match and current representations"
                (into req {:ring.response/status 412
                           :ring.response/body "Precondition Failed\r\n"})))))))))

;; TODO: See Section 4.1, RFC 7232:
;;
(defn evaluate-if-none-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource and, when applicable, the representation metadata of the selected
  representation. If the precondition is found to be false, an exception is
  thrown with ex-data containing the proper response."
  [{::site/keys [selected-representation] :as req}]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (let [header-field (reap/if-none-match (get-in req [:ring.request/headers "if-none-match"]))]
    (cond
      (sequential? header-field)
      (when-let [rep-etag (some-> (get selected-representation ::http/etag) reap/entity-tag)]
        ;; "If the field-value is a list of entity-tags, the condition is false
        ;; if one of the listed tags match the entity-tag of the selected
        ;; representation."
        (doseq [etag header-field]
          ;; "A recipient MUST use the weak comparison function when comparing
          ;; entity-tags …"
          (when (rfc7232/weak-compare-match? etag rep-etag)
            (throw
             (if (#{:get :head} (:ring.request/method req))
               (ex-info
                "Not modified"
                (into req {:ring.response/status 304
                           :ring.response/body "Not Modified\r\n"
                           ::matching-entity-tag etag}))
               ;; "… or 412 (Precondition Failed) status code for all other
               ;; request methods."
               (ex-info
                "If-None-Match precondition failed"
                (into req {:ring.response/status 412
                           :ring.response/body "Precondition Failed\r\n"
                           ::matching-entity-tag etag})))))))

      ;; "If-None-Match can also be used with a value of '*' …"
      (and (map? header-field) (::rfc7232/wildcard header-field))
      ;; "… the condition is false if the origin server has a current
      ;; representation for the target resource."
      (when selected-representation
        (throw
         (ex-info
          "At least one representation already exists for this resource"
          (into req
                (if (#{:get :head} (:ring.request/method req))
                  ;; "the origin server MUST respond with either a) the 304 (Not
                  ;; Modified) status code if the request method is GET or HEAD
                  ;; …"
                  {:ring.response/status 304
                   :ring.response/body "Not Modified\r\n"}
                  ;; "… or 412 (Precondition Failed) status code for all other
                  ;; request methods."
                  {:ring.response/status 412
                   :ring.response/body "Precondition Failed\r\n"}))))))))

(defn evaluate-if-unmodified-since! [{::site/keys [selected-representation] :as req}]
  (let [if-unmodified-since-date
        (-> req
            (get-in [:ring.request/headers "if-unmodified-since"])
            reap/http-date
            ::rfc7231/date)]
    (when (.isAfter
           (.toInstant (get selected-representation ::http/last-modified (java.util.Date.)))
           (.toInstant if-unmodified-since-date))
      (throw
       (ex-info
        "Precondition failed"
        (into req {:ring.resposne/status 304
                   :ring.response/body "Precondition Failed\r\n"}))))))

(defn evaluate-if-modified-since! [{::site/keys [selected-representation] :as req}]
  (let [if-modified-since-date
        (-> req
            (get-in [:ring.request/headers "if-modified-since"])
            reap/http-date
            ::rfc7231/date)]

    (when-not (.isAfter
               (.toInstant (get selected-representation ::http/last-modified (java.util.Date.)))
               (.toInstant if-modified-since-date))
      (throw
       (ex-info
        "Not modified"
        (into req {:ring.response/status 304
                   :ring.response/body "Not Modified\r\n"}))))))

(defn evaluate-preconditions!
  "Implementation of Section 6 of RFC 7232. Arguments are the (Ring) request, a
  resource (map, typically with Spin namespaced entries, representation metadata
  of the selected representation and the response message origination date."
  [req]
  ;; "… a server MUST ignore the conditional request header fields … when
  ;; received with a request method that does not involve the selection or
  ;; modification of a selected representation, such as CONNECT, OPTIONS, or
  ;; TRACE." -- Section 5, RFC 7232
  (when (not (#{:connect :options :trace} (:ring.request/method req)))
    (if (get-in req [:ring.request/headers "if-match"])
      ;; Step 1
      (evaluate-if-match! req)
      ;; Step 2
      (when (get-in req [:ring.request/headers "if-unmodified-since"])
        (evaluate-if-unmodified-since! req)))
    ;; Step 3
    (if (get-in req [:ring.request/headers "if-none-match"])
      (evaluate-if-none-match! req)
      ;; Step 4, else branch: if-none-match is not present
      (when (#{:get :head} (:ring.request/method req))
        (when (get-in req [:ring.request/headers "if-modified-since"])
          (evaluate-if-modified-since! req))))
    ;; (Step 5 is handled elsewhere)
    ))

(defn put-static-resource
  "PUT a new representation of the target resource. All other representations are
  replaced."
  [{::site/keys [uri db received-representation start-date crux-node base-uri] :as req}]
  (let [existing? (x/entity db uri)
        classification (get-in req [:ring.request/headers "site-classification"])
        new-rep (merge
                 (cond->
                     {:crux.db/id uri
                      ::site/type "StaticRepresentation"
                      ::http/methods #{:get :head :options :put :patch}
                      ::http/etag (etag received-representation)
                      ::http/last-modified start-date}
                     classification (assoc ::pass/classification classification))
                 received-representation)]

    ;; Currently we cannot tell whether a submitted tx has been successful,
    ;; see https://github.com/juxt/crux/issues/1480. As a workaround, we do
    ;; the conditional checks here. In the future, we'll call into separate
    ;; tx fns.
    (evaluate-preconditions! req)

    (->> (x/submit-tx
          crux-node
          [[:crux.tx/put new-rep]])
         (x/await-tx crux-node))

    (into req {:ring.response/status (if existing? 204 201)})))

(defn patch-static-resource
  [{::site/keys [received-representation] :as req}]
  (throw (ex-info "TODO: patch" (into req {::incoming received-representation}))))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [{::site/keys [db uri base-uri] :as req}]
  (or
   ;; We call OpenAPI location here, because a resource can be defined in
   ;; OpenAPI, and exits in Crux, simultaneously.
   (openapi/locate-resource db uri req)

   ;; Is it in Crux?
   (when-let [r (x/entity db uri)]
     (cond-> (assoc r ::site/resource-provider ::db)
       (= (get r ::site/type) "StaticRepresentation")
       (assoc ::site/put-fn put-static-resource
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
      ::http/redirect (cond-> loc (.startsWith loc base-uri)
                              (subs (count base-uri)))})

   ;; Return a back-stop resource
   {::site/resource-provider ::default-empty-resource
    ::http/methods #{:get :head :options :put :post}
    ::site/put-fn put-static-resource}))

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
       (cond-> (for [[v] variants
                     :let [rep (x/entity db v)]
                     :when rep]
                 (assoc rep ::http/content-location v))
         (::http/content-type resource)
         (conj resource))))

   ;; Most resources have a content-type, which indicates there is only one
   ;; variant.
   (when (::http/content-type resource)
     [resource])

   ;; No representations. On a GET, this would yield a 404.
   []))

(defn GET [{::site/keys [selected-representation resource] :as req}]
  (evaluate-preconditions! req)

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

    (cond-> (assoc req :ring.response/status 200)
      body (assoc :ring.response/body body))))

(defn post-variant [{::site/keys [crux-node db uri]
                     ::apex/keys [request-instance]
                     :as req}]

  (let [location
        (str uri (hash (select-keys request-instance [::site/resource ::site/variant])))
        existing (x/entity db location)]

    (->> (x/submit-tx
          crux-node
          [[:crux.tx/put (merge {:crux.db/id location} request-instance)]])
         (x/await-tx crux-node))

    (into req {:ring.response/status (if existing 204 201)
               :ring.response/headers {"location" location}})))

(defn post-redirect [{::site/keys [crux-node db uri]
                      ::apex/keys [request-instance]
                      :as req}]
  (let [location (str uri (hash (select-keys request-instance [::site/resource ::site/location])))
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
        post-fn (::site/post-fn resource)]

    (cond
      (fn? post-fn) (post-fn req)

      :else
      (case (::site/purpose resource)
        ::site/acquire-token (authn/token-response req)
        ::site/login (authn/login-response req)
        ::site/logout (authn/logout-response req)
        (throw
         (ex-info
          "Resource allows POST but doesn't contain have a post-fn function"
          (into req
                {:ring.response/status 500
                 :ring.response/body "Internal Error\r\n"})))))))

(defn PUT [{::site/keys [resource] :as req}]
  (let [rep (receive-representation req) _ (assert rep)
        req (assoc req ::site/received-representation rep)
        put-fn (::site/put-fn resource)]

    (log/tracef "PUT, put-fn is %s" put-fn)
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
        patch-fn (::site/patch-fn resource)]

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

(defn wrap-method-not-implemented? [h]
  (fn [{:ring.request/keys [method] :as req}]
    (when-not (contains?
             #{:get :head :post :put :delete :options
               :patch
               :mkcol :propfind} method)
      (throw
       (ex-info "Method not implemented"
                (into req {:ring.response/status 501
                           :ring.response/body "Not Implemented\r\n"}))))
    (h req)))

(defn wrap-locate-resource [h]
  (fn [req]
    (let [res (locate-resource req)]
      (log/debugf "Resource provider: %s" (::site/resource-provider res))
      (h (assoc req ::site/resource res)))))

(defn wrap-redirect [h]
  (fn [{::site/keys [resource] :ring.request/keys [method] :as req}]
    (when-let [location (::http/redirect resource)]
      (throw
       (ex-info "Redirect"
                (-> req
                    (assoc :ring.response/status
                           (case method (:get :head) 302 307))
                    (update :ring.response/headers
                            assoc "location" location)))))
    (h req)))

(defn wrap-find-current-representations
  ;; Step 5 (GET/HEAD/PUT only). Determine the resource's current
  ;; representations, or 404
  ;; Step 6 (GET/HEAD only). Maybe throw a 404
  [h]
  (fn [{:ring.request/keys [method] :as req}]
    (if (#{:get :head :put} method)
      (let [cur-reps (seq (current-representations req))]
        (when (and (#{:get :head} method) (empty? cur-reps))
          (throw
           (ex-info
            "Not Found"
            (into req
                  {:ring.response/status 404
                   :ring.response/body "Not Found\r\n"}))))
        (h (assoc req ::site/current-representations cur-reps)))
      (h req))))

(defn wrap-negotiate-representation [h]
  ;; Step 7. (GET/HEAD only). Select a representation
  (fn [req]
    (let [cur-reps (::site/current-representations req)]
      (h (cond-> req
           (seq cur-reps)
           (assoc
            ::site/selected-representation
            (negotiate-representation req cur-reps)))))))

(defn wrap-authenticate [h]
  ;; Step 8. Authenticate
  (fn [{:ring.request/keys [method] :as req}]
    (let [sub (when-not (= method :options) (authn/authenticate req))]
      (h (cond-> req sub (assoc ::pass/subject sub))))))

(defn wrap-authorize
  ;; Step 9. Authorize

  ;; Do authorization as late as possible (in order to have as much data
  ;; as possible to base the authorization decision on. However, note
  ;; Section 8.5, RFC 4918 states "the server MUST do authorization checks
  ;; before checking any HTTP conditional header.".
  [h]
  (fn [{:ring.request/keys [method] ::site/keys [db resource] ::pass/keys [subject] :as req}]
    (let [request-context
          {'subject subject
           'resource (dissoc resource ::http/body ::http/content)
           'request (select-keys
                     req
                     [:ring.request/headers :ring.request/method :ring.request/path
                      :ring.request/query :ring.request/protocol :ring.request/remote-addr
                      :ring.request/scheme :ring.request/server-name :ring.request/server-post
                      :ring.request/ssl-client-cert])
           'representation (dissoc resource ::http/body ::http/content)
           'environment {}}

          authz (when (not= method :options)
                  (pdp/authorization db request-context))

          req (cond-> req
                true (assoc ::pass/request-context request-context)
                authz (assoc ::pass/authorization authz)
                ;; If the max-content-length has been modified, update that in the
                ;; resource
                (::http/max-content-length authz)
                (update ::site/resource
                        assoc ::http/max-content-length (::http/max-content-length authz)))]

      (when (and (not= method :options)
                 (not= (::pass/access authz) ::pass/approved))
        (let [status (if-not (::pass/user subject) 401 403)
              msg (case status 401  "Unauthorized" 403 "Forbidden")]
          (throw
           (ex-info msg
                    (into req
                          {:ring.response/status status
                           :ring.response/body (str msg "\r\n")})))))
      (h req))))

(defn wrap-method-not-allowed? [h]
  ;; Step 10. Check method allowed
  (fn [{::site/keys [resource] :ring.request/keys [method] :as req}]
    (if resource
      (let [allowed-methods (set (::http/methods resource))]
        (when-not (contains? allowed-methods method)
          (throw
           (ex-info
            "Method not allowed"
            (into req
                  {:ring.response/status 405
                   :ring.response/headers {"allow" (join-keywords allowed-methods true)}
                   :ring.response/body "Method Not Allowed\r\n"}))))
        (h (assoc req ::site/allowed-methods allowed-methods)))
      (h req))))

(defn wrap-invoke-method [h]
  ;; Step 11. Invoke method
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

(defn wrap-refresh-db
  ;; Refresh the db, now all steps after this are in terms of the 'new'
  ;; db.
  [h]
  (fn [req]
    (h (assoc req ::site/db (x/db (::site/crux-node req))))))

(defn wrap-triggers [h]
  ;; Site-specific step: Check for any observers and 'run' them TODO:
  ;; Perhaps effects need to run against happy and sad paths - i.e. errors
  ;; - this should really be in a 'finally' block.
  (fn [{::site/keys [db crux-node] ::pass/keys [subject] :as req}]
    (let [triggers
          (map first
               (x/q db '{:find [rule]
                         :where [[rule ::site/type "Trigger"]]}))
          _ (log/tracef "Triggers are %s" (pr-str triggers))

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
        ;; TODO: Can we use the new refreshed db here to save another call to x/db?
        (let [actions (rules/eval-triggers (x/db crux-node) triggers request-context)]
          (log/tracef "Triggered actions are %s" (pr-str actions))
          (doseq [action actions]
            (log/tracef "Running action: %s" (get-in action [:trigger ::site/action]))
            (triggers/run-action! req action)))
        (catch clojure.lang.ExceptionInfo e
          (log/error e (format "Failed to run trigger/action: %s" (pr-str (ex-data e)))))
        (catch Exception e
          (log/error e "Failed to run trigger/action")))

      (h req))))

(defn wrap-security-headers [h]
  (fn [req]
    ;; Step 12. Set security headers, including CORS
    ;; TODO - but see wrap-cors-headers middleware

    ;;        Strict-Transport-Security: max-age=31536000; includeSubdomains
    ;;        X-Frame-Options: DENY
    ;;        X-Content-Type-Options: nosniff
    ;;        X-XSS-Protection: 1; mode=block
    ;;        X-Download-Options: noopen
    ;;        X-Permitted-Cross-Domain-Policies: none

    (h req)))

(defn handler []
  (-> (fn [req] req)
      wrap-security-headers
      wrap-triggers
      wrap-refresh-db
      wrap-invoke-method
      wrap-method-not-allowed?
      wrap-authorize
      wrap-authenticate
      wrap-negotiate-representation
      wrap-find-current-representations
      wrap-redirect
      wrap-locate-resource
      wrap-method-not-implemented?))

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

(defn redact [ctx]
  (-> ctx
      (update :ring.request/headers
              (fn [headers]
                (cond-> headers
                  (contains? headers "authorization")
                  (assoc "authorization" "(redacted)"))))))

(defn ->serializable [{::site/keys [request-id db] :as ctx}]
  (-> ctx
      (into (select-keys db [:crux.db/valid-time :crux.tx/tx-id]))
      (assoc :crux.db/id request-id ::site/type "Request")
      redact
      ;; Dissoc known unfreezables
      (dissoc ::site/crux-node ::site/db :ring.request/body)
      (util/deep-replace
       (fn [form]
         (cond-> form
           (and (string? form) (>= (count form) 64))
           (subs 0 64)

           (and (vector? form) (>= (count form) 64))
           (subvec 0 64)

           (and (list? form) (>= (count form) 64))
           (#(take 64 %))

           (not (freezable? form))
           ((fn [form] (format "(unfreezable %s)" (type form)))))))))

(defn log-context! [{:ring.request/keys [method] :as ctx}]
  (assert method)
  (log/infof
   "%-7s %s %s %d"
   (str/upper-case (name method))
   (:ring.request/path ctx)
   (:ring.request/protocol ctx)
   (:ring.response/status ctx)))

(defn store-context!
  [{::site/keys [crux-node] :as ctx}]
  (assert crux-node)
  (let [blob (->serializable ctx)]
    (try
      (x/submit-tx
       crux-node
       [[:crux.tx/put blob]])
      (catch Exception e
        (log/error e "Failed to log request, recovering")))))

(defn respond [{::site/keys [selected-representation body start-date base-uri request-id uri]
                :ring.request/keys [method]
                :as ctx}]

  (let [end-date (java.util.Date.)
        ctx (assoc ctx
                   ::site/end-date end-date
                   ::site/duration-millis (- (.getTime end-date)
                                             (.getTime start-date)))]
    (log-context! ctx)
    (store-context! ctx)

    (cond->
        (update ctx
                :ring.response/headers
                assoc "date" (format-http-date start-date))

        request-id
        (update :ring.response/headers
                assoc "site-request-id"
                (cond-> request-id
                  (.startsWith request-id base-uri)
                  (subs (count base-uri))))

        selected-representation
        (update :ring.response/headers
                representation-headers selected-representation body)

        (= method :head) (dissoc :ring.response/body))))

(defn wrap-responder [h]
  (fn [req]
    (respond (h req))))

(defn wrap-cors-headers [h]
  (fn [ctx]
    (let [{::site/keys [resource] :as ctx} (h ctx)
          request-origin (get-in ctx [:ring.request/headers "origin"])
          {::site/keys [access-control-allow-origins]} resource
          allow-origin
          (when request-origin
            (or
             (when (contains? access-control-allow-origins request-origin)
               request-origin)
             (when (contains? access-control-allow-origins "*")
               "*")))
          cors-headers (when allow-origin (get access-control-allow-origins allow-origin))
          ]
      (cond-> ctx
        allow-origin
        (assoc-in [:ring.response/headers "access-control-allow-origin"] allow-origin)
        (contains? cors-headers ::site/access-control-allow-methods)
        (assoc-in [:ring.response/headers "access-control-allow-methods"]
                  (join-keywords (get cors-headers ::site/access-control-allow-methods) true))
        (contains? cors-headers ::site/access-control-allow-headers)
        (assoc-in [:ring.response/headers "access-control-allow-headers"]
                  (join-keywords (get cors-headers ::site/access-control-allow-headers) false))
        (contains? cors-headers ::site/access-control-allow-credentials)
        (assoc-in [:ring.response/headers "access-control-allow-credentials"]
                  (str (get cors-headers ::site/access-control-allow-credentials)))))))

(defn wrap-error-handling
  "Return a handler that constructs proper Ring responses, logs and error
  handling where appropriate."
  [h]
  (fn [{::site/keys [request-id] :as req}]

    (org.slf4j.MDC/put "reqid" request-id)

    (try
      (h req)
      (catch clojure.lang.ExceptionInfo e
        (let [{:ring.response/keys [status] :as exdata} (ex-data e)]
          ;; Don't log exceptions which are used to escape (e.g. 302, 401).
          (when (or (not (integer? status)) (>= status 500))
            (let [exdata (->serializable exdata)]
              (log/errorf e "%s: %s" (.getMessage e) (pr-str exdata))))

          (respond
           (-> (merge
                ;; If this is a Clojure exception, but not a Site exception,
                ;; let's start with the ctx as a base.
                (when-not (::site/start-date exdata) req)
                {:ring.response/status 500
                 :ring.response/body "Internal Error\r\n"
                 ::site/error (.getMessage e)
                 ::site/error-stack-trace (.getStackTrace e)}
                exdata)))))

      (catch Throwable e
        (log/error e (.getMessage e))
        (respond
         (into req
               {:ring.response/status 500
                :ring.response/body "Internal Error\r\n"})))
      (finally
        (org.slf4j.MDC/clear)))))

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
  avoid two equivalent URLs pointing to two different Crux entities."
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
  "Initialize request context."
  [h {::site/keys [crux-node base-uri uri-prefix]}]
  (assert crux-node)
  (assert base-uri)
  (fn [{:ring.request/keys [scheme] :as req}]
    (let [db (x/db crux-node)
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

          req (into req {::site/start-date (java.util.Date.)
                         ::site/request-id req-id
                         ::site/uri uri
                         ::site/crux-node crux-node
                         ::site/db db
                         ::site/base-uri base-uri})]

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
           (sorted-map) req)
          (h)
          (select-keys
           [:ring.response/status
            :ring.response/headers
            :ring.response/body])
          (set/rename-keys {:ring.response/status :status
                            :ring.response/headers :headers
                            :ring.response/body :body})))))

(defn wrap-healthcheck
  [h]
  (fn [req]
    (if (= "/_site/healthcheck" (:uri req))
      {:status 200 :body "Site OK!\r\n"}
      (h req))))

(defn make-handler [opts]
  (-> (#'handler)
      (wrap-responder)
      (wrap-cors-headers)
      (wrap-error-handling)
      (wrap-initialize-request opts)
      (wrap-ring-1-adapter)
      (wrap-healthcheck)))
