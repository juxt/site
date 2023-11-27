;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [xtdb.api :as x]
   [jsonista.core :as json]
   [juxt.apex.alpha.representation-generation :refer [entity-bytes-generator]]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.reap.alpha.decoders :as reap.decoders]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.util :as util])
  (:import (java.net URLDecoder)))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn put-openapi
  [{::site/keys [xtdb-node uri resource received-representation start-date] :as req}]

  (let [body (json/read-value
              (java.io.ByteArrayInputStream.
               (::http/body received-representation)))

        openapi (get body "openapi")

        etag (format "\"%s\""
                     (subs
                      (util/hexdigest
                       (.getBytes (pr-str openapi) "UTF-8")) 0 32))]

    (->>
     (x/submit-tx
      xtdb-node
      [[:xtdb.tx/put
        (merge
         {:xt/id uri
          ::http/methods #{:get :head :options :put}
          ::http/etag etag
          ::http/last-modified start-date
          ::site/type "OpenAPI"
          ::apex/openapi openapi
          ::http/body (json/write-value-as-string openapi)
          ::http/content-type "application/json"
          :title (get-in openapi ["info" "title"])
          :version (get-in openapi ["info" "version"])
          :description (get-in openapi ["info" "description"])})]])
     (x/await-tx xtdb-node))

    (assoc req
           :ring.response/status
           (if (= (::site/resource-provider resource) ::openapi-empty-document-resource)
             201 204))))

(defmulti received-body->resource-state
  "Convert the request payload into the proposed new state of a resource."
  (fn [req]
    (let [rep (::site/received-representation req)
          {:juxt.reap.alpha.rfc7231/keys [type subtype]}
          (reap.decoders/content-type (::http/content-type rep))]
      ;; TODO: Check that the OpenAPI supports the content-type of the request
      ;; payload, if not, throw a 415.
      (format "%s/%s" type subtype))))

(defmethod received-body->resource-state :default [req]
  ;; Regardless of whether the OpenAPI declares it can read the content-type, we
  ;; can't process it. TODO: Should have some way of passing it 'raw' to some
  ;; processing function that is able to turn it into resource state (a Crux
  ;; resource)?
  (throw
   (ex-info
    "Unsupported media type"
    (into req {:ring.response/status 415}))))

(defmethod received-body->resource-state "application/edn"
  [{::site/keys [received-representation resource db] :as req}]
  ;; The assumption here is that EDN resource is 'good to go' as resource
  ;; state. But authorization rules will be run by put-resource-state that will
  ;; determine whether it is allowed in.
  received-representation)

(defn pick-validation-failures
  [{::jinx/keys [errors keyword schema valid? subschemas instance property] :as validation}]
  (cond
    (and (not valid?)
         (not-empty subschemas))
    (let [subschema-failures
          (->> (mapv pick-validation-failures subschemas)
               (filterv some?)
               (reduce concat))]
      {:errors errors
       :property property
       :schema schema
       :keyword keyword
       :instance instance
       :valid? valid?
       :required (get schema "required")
       :subschemas subschema-failures})

    (and (not valid?)
         (empty? subschemas))
    {:errors errors
     :property property
     :schema schema
     :keyword keyword
     :instance instance
     :valid? valid?
     :required (get schema "required")}))

(defmethod received-body->resource-state "application/json"
  [{::site/keys [received-representation resource db] :as req}]
  (let [schema (get-in resource [::apex/operation "requestBody" "content"
                                 "application/json" "schema"])
        _ (assert schema)
        body (::http/body received-representation)
        instance (try
                   (json/read-value body)
                   (catch Exception _
                     (throw
                      (ex-info
                       "Bad JSON in input"
                       (into req {:ring.response/status 400})))))
        _ (assert instance)
        openapi-ref (get resource ::apex/openapi) _ (assert openapi-ref)
        openapi-resource (x/entity db openapi-ref) _ (assert openapi-resource)

        validation
        (jinx.api/validate
         schema instance
         {:base-document (::apex/openapi openapi-resource)})

        _ (when-not (::jinx/valid? validation)
            (throw
             (ex-info
              "Schema validation failed"
              (-> req
                  (into {:ring.response/status 400
                         :ring.response/body (pick-validation-failures validation)
                         ::jinx/validation-results validation})))))

        validation (-> validation
                       process-transformations process-keyword-mappings)

        path-params (get-in req [::site/resource ::apex/openapi-path-params])

        instance (::jinx/instance validation)

        ;; Replace any remaining string keys with keyword equivalents.
        instance (reduce-kv
                  (fn [acc k v] (assoc acc (cond-> k (string? k) keyword) v))
                  {} instance)

        ;; Inject path parameter values into body, this is a feature, enabled by
        ;; the x-juxt-site-inject-property OpenAPI extension keyword.
        instance (reduce-kv
                  (fn [acc _ v]
                    (let [inject-property (get v :inject-property)]
                      (cond-> acc
                        inject-property
                        (assoc (keyword inject-property) (:value v)))))
                  instance path-params)]
    instance))

(defn put-resource-state
  "Put some new resource state into Crux, if authorization checks pass. The new
  resource state should be a valid Crux entity, with a :xt/id"
  [{::site/keys [received-representation start-date resource db xtdb-node]
    ::pass/keys [subject]
    :as req}
   new-resource-state]

  (assert new-resource-state)

  (let [id (:xt/id new-resource-state)
        _ (assert id)
        authorization
        (pdp/authorization
         db
         {'subject subject
          'resource resource
          ;; might change to 'action' at
          ;; this point
          'request (select-keys req [:ring.request/method :ring.request/path ::site/base-uri])
          'environment {}
          'new-state new-resource-state})

        _ (when-not (= (::pass/access authorization) ::pass/approved)
            (log/debug "Unauthorized OpenAPI JSON instance"
                       new-resource-state authorization)
            (let [status (if-not (::pass/user subject) 401 403)
                  message (case status
                            401 "Unauthorized"
                            403 "Forbidden")]
              (throw
               (ex-info
                message
                (into req {:ring.response/status status})))))

        already-exists? (x/entity db id)

        last-modified start-date
        etag (format "\"%s\"" (-> received-representation
                                  ::http/body util/hexdigest (subs 0 32)))]

    ;; Since this resource is 'managed' by the locate-resource in this ns, we
    ;; don't have to worry about http attributes - these will be provided by
    ;; the locate-resource function. We just need the resource state here.

    ;; TODO: Although this resource is managed, shouldn't we put a
    ;; representation into the db anyway, if only to store the last-modified and
    ;; etag validators?

    (->> (x/submit-tx
          xtdb-node
          [[:xtdb.tx/put new-resource-state]])
         (x/await-tx xtdb-node))

    (-> req
        (assoc :ring.response/status (if-not already-exists? 201 204)
               ::http/etag etag
               ::http/last-modified last-modified
               ::apex/instance new-resource-state)
        (update :ring.response/headers assoc "location" id))))

(defn- path-parameter-defs->map [path-param-defs]
  (reduce
   (fn [acc p]
     (let [location (get p "in")]
       (if (= location "path")
         (if (contains? acc location)
           ;; "The list MUST NOT include duplicated parameters" (OpenAPI 3.0.2)
           ;; TODO: Throw a proper 500 with the request context
           (throw (ex-info "OpenAPI error, the list of parameters cannot contain duplicates" {}))
           (assoc acc (get p "name") p))
         acc)))
   {}
   path-param-defs))

(defn path-entry->resource
  "From an OpenAPI path-to-path-object entry, return the corresponding resource if
  it matches the path. Path matching also considers path-parameters."
  [{:ring.request/keys [method]}
   [path path-item-object] openapi openapi-uri rel-request-path]
  (let [path-param-defs
        (merge
         ;; Path level
         (path-parameter-defs->map
          (get-in path-item-object ["parameters"]))
         ;; Operation level. From OpenAPI Spec 3.0.2: "These parameters can be
         ;; overridden at the operation level, but cannot be removed there."
         (path-parameter-defs->map
          (get-in path-item-object [(name method) "parameters"])))

        pattern
        (str/replace
         path
         #"\{([^\}]+)\}"                ; e.g. {id}
         (fn replacer [[_ group]]
           ;; Instead of using the regex of path parameter's schema, we use a
           ;; weak regex that includes anything except forward slashes, question
           ;; marks, or hashes (as described in the Path Templating section of
           ;; the OpenAPI Specification (version 3.0.2). We are just locating
           ;; the resource in this step, if the regex were too strong and we
           ;; reject the path then locate-resource would yield nil, and we might
           ;; consequently end up creating a static resource (or whatever other
           ;; resource locators are tried after this one). That might surprise
           ;; the user so instead (prinicple of least surprise) we are more
           ;; liberal in what we accept at this stage and leave validation
           ;; against the path parameter to later (potentially yielding a 400).
           (format "(?<%s>[^/#\\?]+)" group)))

        ;; We have to terminate with a 'end of line' otherwise we
        ;; match too eagerly. So if we had /users and /users/{id},
        ;; then '/users/foo' might match /users.
        pattern (str "^" pattern "$")

        matcher (re-matcher (re-pattern pattern) rel-request-path)]

    (when (.find matcher)

      (let [path-params
            (into
             {}
             (for [[param-name param-def] path-param-defs
                   :let [schema (get param-def "schema")
                         param-value
                         (-> (.group matcher param-name)
                             ;; This is the point we decode any encoded
                             ;; forward-slashes, question-marks or hashes (or
                             ;; indeed any other encoded 'reserved' character --
                             ;; see RFC 3986). Such characters will have had to
                             ;; have been percent-encoded to have been
                             ;; 'tunnelled' through to this point.
                             URLDecoder/decode)
                         validation
                         (jinx.api/validate schema param-value)
                         inject-property
                         (get param-def "x-juxt-site-inject-property")]]

               [param-name (cond-> {:value param-value ::jinx/valid? (::jinx/valid? validation)}
                             (not (::jinx/valid? validation))
                             (assoc :validation validation)
                             inject-property
                             (assoc :inject-property inject-property))]))

            operation-object (get path-item-object (name method))

            acceptable (str/join ", " (map first (get-in operation-object ["requestBody" "content"])))

            methods (set
                     (keep
                      #{:get :head :post :put :delete :options :trace :connect}
                      (let [methods (set
                                     (conj (map keyword (keys path-item-object)) :options))]
                        (cond-> methods
                          (contains? methods :get)
                          (conj :head)))))

            post-fn-sym (when (= method :post) (some-> (get operation-object "juxt.site.alpha/post-fn") symbol))

            put-fn-sym (when (= method :put) (some-> (get operation-object "juxt.site.alpha/put-fn") symbol))

            get-fn-sym (when (= method :get)
                         (some-> operation-object
                                 (get "juxt.site.alpha/get-fn")
                                 symbol))

            resource
            {::site/resource-provider ::apex/openapi-path

             ;; This is useful, because it is the base document for any relative
             ;; json pointers.
             ;; TODO: Search for instances of apex/openapi and ensure they do (x/entity)
             ::apex/openapi openapi-uri

             ::apex/operation operation-object
             ::apex/openapi-path path
             ::apex/openapi-path-params path-params

             ::http/methods methods

             ::http/representations
             (for [[media-type media-type-object]
                   (fast-get-in path-item-object ["get" "responses" "200" "content"])]
               {::http/content-type media-type
                ;; Wait a second, if this doesn't get logged then we
                ;; can use a 'proper' function right?
                ::site/body-fn `entity-bytes-generator})

             ;; TODO: The allowed origins ought to be specified in the top-level
             ;; of the openapi document, or under the security section. This is
             ;; just for testing.
             ::site/access-control-allow-origins
             {"http://localhost:8000"
              {::site/access-control-allow-methods #{:get :put :post :delete}
               ::site/access-control-allow-headers #{"authorization" "content-type"}
               ::site/access-control-allow-credentials true}}

             ;; TODO: Merge in any properties of a resource that is in
             ;; Crux - e.g. if this resource is a collection, what type
             ;; of collection is it? Some properties that can be used in
             ;; the PDP.
             }]

        ;; Add conditional entries to the resource
        (cond-> resource
          (seq acceptable) (assoc ::http/acceptable {"accept" acceptable})

          get-fn-sym
          (assoc
           ::site/get-fn
           (fn get-fn-proxy [req]
             (log/debug "Calling get-fn" get-fn-sym)
             (let [f (try
                       (requiring-resolve get-fn-sym)
                       (catch IllegalArgumentException _
                         (throw
                          (ex-info
                           (str "Failed to find get-fn: " get-fn-sym)
                           (into req
                                 {:ring.response/status 500})))))]
               (f req))))

          post-fn-sym
          (assoc
           ::site/post-fn
           (fn post-fn-proxy [req]
             (log/debug "Calling post-fn" post-fn-sym)
             (let [f (try
                       (requiring-resolve post-fn-sym)
                       (catch IllegalArgumentException _
                         (throw
                          (ex-info
                           (str "Failed to find post-fn: " post-fn-sym)
                           (into req
                                 {:ring.response/status 500})))))]
               (f req))))

          put-fn-sym
          (assoc
           ::site/put-fn
           (fn put-fn-proxy [req]
             (log/debug "Calling put-fn" post-fn-sym)
             (let [f (try
                       (requiring-resolve put-fn-sym)
                       (catch IllegalArgumentException _
                         (throw
                          (ex-info
                           (str "Failed to find put-fn: " post-fn-sym)
                           (into req
                                 {:ring.response/status 500})))))]
               (f req))))

          (and (not put-fn-sym)
               (= method :put))
          (assoc
           ::site/put-fn
           (fn put-fn [req]
             (if-let [put-fn-sym (some-> (get operation-object "juxt.site.alpha/put-fn") symbol)]
               (do (log/debug "Calling put-fn" put-fn-sym)
                   (let [f (try
                             (requiring-resolve put-fn-sym)
                             (catch IllegalArgumentException _
                               (throw
                                (ex-info
                                 (str "Failed to find put-fn: " put-fn-sym)
                                 (into req
                                       {:ring.response/status 500})))))]
                     (f req)))
               (put-resource-state
                req
                (-> req
                    received-body->resource-state
                    ;; Since this is a PUT, we add
                    (assoc :xt/id (::site/uri req))))))))))))

(defn locate-resource
  [db uri
   ;; We'd like to locate the resource based on nothing but the URI of the
   ;; request. This would avoid 'routing' based on other aspects of the request,
   ;; such as headers (e.g. authorization, which should be left to the
   ;; authorization step that follows resource location). The reason we can't
   ;; yet do this is due to path parameters (see below). If we were to put path
   ;; parameters at one level higher in the OpenAPI, and enforce that, then we
   ;; could make this change.
   {::site/keys [base-uri] :as req}]

  ;; Do we have any OpenAPIs in the database?
  (or
   ;; The OpenAPI document

   ;; TODO: In future, we should relax the locations where APIs can live to make
   ;; it easier to develop APIs with WebDav.
   (when (re-matches (re-pattern (format "%s/_site/apis/\\w+/openapi.json" base-uri)) uri)
     (or
      ;; It might exist
      (some-> (x/entity db uri)
              (assoc ::site/resource-provider ::openapi-document
                     ::site/put-fn put-openapi))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/resource-provider ::openapi-empty-document-resource
       ::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::http/methods #{:get :head :options :put}
       ::http/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}
       ::site/put-fn put-openapi}))

   (let [openapis (filter (comp #(str/starts-with? % base-uri) first)
                          (x/q db '{:find [openapi-uri openapi]
                                    :where [[openapi-uri ::apex/openapi openapi]]}))
         matches
         (for [[openapi-uri openapi] openapis
               server (get openapi "servers")
               :let [server-url (get server "url")
                     abs-server-url
                     (cond->> server-url
                       (or (.startsWith server-url "/")
                           (= server-url ""))
                       (str base-uri))]
               :when (.startsWith uri abs-server-url)
               :let [paths (get openapi "paths")]
               path paths
               :let [resource (path-entry->resource req path openapi openapi-uri (subs uri (count abs-server-url)))]
               :when resource]
           resource)]

     (when (pos? (count matches))
       ;; This was the last chance we could return to allow another resource
       ;; locator a turn. Now we're committed to either one of the resources or
       ;; throwing a 400 to report a kind of 'near-miss' with getting the URI
       ;; right.

       (if (= (count matches) 1)        ; this is the most common case
         (let [openapi-resource (first matches)
               resource (merge openapi-resource (x/entity db uri))]
           (if (every? ::jinx/valid? (vals (::apex/openapi-path-params openapi-resource)))
             resource
             (throw
              (ex-info
               "One or more of the path-parameters in the request did not validate against the required schema"
               (into req {::site/resource resource
                          :ring.response/status 400
                          :ring.response/body "Bad Request\r\n"})))))

         ;; Select one of the matches, and throw an error if this proves
         ;; impossible.
         (let [resources
               (filter
                #(every? ::jinx/valid?
                         (vals (get % ::apex/openapi-path-params)))
                matches)]
           (if (= (count resources) 1)
             (first resources)
             ;; TODO: There is the information in each path-param's validation
             ;; to construct a much more helpful error message.
             (throw
              (ex-info
               "Throwing Multiple API paths match"
               (into req {::multiple-matched-resources resources}))))))))))
