;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [crux.api :as x]
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
  [{::site/keys [crux-node uri resource received-representation start-date] :as req}]

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
      crux-node
      [[:crux.tx/put
        (merge
         {:crux.db/id uri
          ::http/methods #{:get :head :options :put}
          ::http/etag etag
          ::http/last-modified start-date
          ::site/type "OpenAPI"
          ::apex/openapi openapi
          ::http/body (json/write-value-as-string openapi)
          ::http/content-type "application/json"
          :title (get-in openapi ["info" "title"])
          :version (get-in openapi ["info" "version"])
          :description (get-in openapi ["info" "description"])
          })]])
     (x/await-tx crux-node))

    (assoc req
           :ring.response/status
           (if (= (::site/resource-provider resource) ::openapi-empty-document-resource)
             201 204))))

(defn- received-body->json
  "Return a JSON instance from the request. Throw a 400 error if the JSON is
  invalid."
  [{::site/keys [resource received-representation db] :as req}]

  (let [body (::http/body received-representation)
        schema (get-in resource [::apex/operation "requestBody" "content"
                                 "application/json" "schema"])
        _ (assert schema)
        instance (json/read-value body) _ (assert instance)
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
                         ;; TODO: Content negotiation for error responses
                         :ring.response/body "Bad Request\r\n"
                         ::jinx/validation-results validation})))))

        validation (-> validation
                       process-transformations process-keyword-mappings)

        instance (::jinx/instance validation)]

    ;; Replace any remaining string keys with keyword equivalents.
    (reduce-kv
     (fn [acc k v] (assoc acc (cond-> k (string? k) keyword) v))
     {} instance)))

(defn put-json-representation
  [{::site/keys [received-representation start-date resource uri db crux-node]
    ::pass/keys [subject]
    :as req}]

  (let [instance (received-body->json req)

        path-params (get-in req [::site/resource ::apex/openapi-path-params])

        ;; Inject path parameter values into body, this is a feature, enabled by
        ;; the x-juxt-site-inject-property OpenAPI extension keyword.
        instance (reduce-kv
                  (fn [acc _ v]
                    (let [inject-property (get v :inject-property)]
                      (cond-> acc
                        inject-property
                        (assoc (keyword inject-property) (:value v)))))
                  instance path-params)

        authorization
        (pdp/authorization
         db
         {'subject subject
          'resource resource
          ;; might change to 'action' at
          ;; this point
          'request (select-keys req [:ring.request/method :ring.request/path])
          'environment {}
          'new-state instance})

        _ (when-not (= (::pass/access authorization) ::pass/approved)
            (log/debug "Unauthorized OpenAPI JSON instance"
                       instance authorization)
            (let [status (if-not (::pass/user subject) 401 403)
                  message (case status
                            401 "Unauthorized"
                            403 "Forbidden")]
              (throw
               (ex-info
                message
                (into req {:ring.response/status status})))))


        already-exists? (x/entity db uri)

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
          crux-node
          [[:crux.tx/put (assoc instance :crux.db/id uri)]])
         (x/await-tx crux-node))

    (-> req
        (assoc :ring.response/status (if-not already-exists? 201 204)
               ::http/etag etag
               ::http/last-modified last-modified
               ::apex/instance instance)
        (update :ring.response/headers assoc "location" uri))))

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
              {::site/access-control-allow-methods #{:get}
               ::site/access-control-allow-headers #{"authorization"}
               ::site/access-control-allow-credentials true}}

             ;; TODO: Merge in any properties of a resource that is in
             ;; Crux - e.g. if this resource is a collection, what type
             ;; of collection is it? Some properties that can be used in
             ;; the PDP.
             }]

        ;; Add conditional entries to the resource
        (cond-> resource
          (seq acceptable) (assoc ::http/acceptable {"accept" acceptable})

          post-fn-sym
          (assoc
           ::site/post-fn
           (fn post-fn-proxy [req]
             (log/debug "Calling post-fn" post-fn-sym)
             (let [rep (::site/received-representation req)
                   {:juxt.reap.alpha.rfc7231/keys [type subtype]}
                   (reap.decoders/content-type (::http/content-type rep))
                   f (try
                       (requiring-resolve post-fn-sym)
                       (catch IllegalArgumentException _
                         (throw
                          (ex-info
                           (str "Failed to find post-fn: " post-fn-sym)
                           (into req
                                 {:ring.response/status 500})))))]
               (f (assoc
                   req
                   ::apex/request-instance
                   (cond
                     (and
                      (.equalsIgnoreCase "application" type)
                      (.equalsIgnoreCase "json" subtype))
                     (received-body->json req)
                     :else rep))))))

          (= method :put)
          (assoc
           ::site/put-fn
           (fn put-fn [req]
             (let [rep (::site/received-representation req)
                   {:juxt.reap.alpha.rfc7231/keys [type subtype]}
                   (reap.decoders/content-type (::http/content-type rep))]
               (case
                   ;; TODO: Depending on requestBody
                   (and
                    (.equalsIgnoreCase "application" type)
                    (.equalsIgnoreCase "json" subtype))
                   (put-json-representation req))))))))))

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

   (let [openapis (x/q db '{:find [openapi-uri openapi]
                            :where [[openapi-uri ::apex/openapi openapi]]})
         ;; TODO: unnecessary let binding above
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
         (let [openapi-resource (first matches)]
           (let [resource (merge openapi-resource (x/entity db uri))]
             (if (every? ::jinx/valid? (vals (::apex/openapi-path-params openapi-resource)))
               resource
               (throw
                (ex-info
                 "One or more of the path-parameters in the request did not validate against the required schema"
                 (into req {::site/resource resource
                            :ring.response/status 400
                            :ring.response/body "Bad Request\r\n"}))))))

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
