;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [jsonista.core :as json]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.pass.alpha.v3.authorization :as authz]
   [juxt.reap.alpha.decoders :as reap.decoders]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.return :refer [return]]
   [juxt.site.alpha.util :as util]
   [juxt.http.alpha :as-alias http]
   [juxt.apex.alpha :as-alias apex]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [ring.util.codec :refer [form-decode]])
  (:import (java.net URLDecoder)))

(defn put-openapi
  [{::site/keys [xt-node uri resource received-representation start-date] :as req}]

  (let [body (json/read-value
              (java.io.ByteArrayInputStream.
               (::http/body received-representation)))

        openapi (get body "openapi")

        etag (format "\"%s\""
                     (subs
                      (util/hexdigest
                       (.getBytes (pr-str openapi) "UTF-8")) 0 32))]

    (->>
     (xt/submit-tx
      xt-node
      [[:xtdb.api/put
        (merge
         {:xt/id uri
          ::http/methods {:get {} :head {} :options {} :put {}}
          ::http/etag etag
          ::http/last-modified start-date
          ::site/type "OpenAPI"
          ::apex/openapi openapi
          ::http/body (json/write-value-as-string openapi)
          ::http/content-type "application/json"
          :title (get-in openapi ["info" "title"])
          :version (get-in openapi ["info" "version"])
          :description (get-in openapi ["info" "description"])})]])
     (xt/await-tx xt-node))

    (assoc req
           :ring.response/status
           (if (= (::site/resource-provider resource) ::openapi-empty-document-resource)
             201 204))))

(defmulti validate-request-payload
  "Convert the request payload into the proposed new state of a resource."
  (fn [req]
    (let [rep (::site/received-representation req)
          {:juxt.reap.alpha.rfc7231/keys [type subtype]}
          (reap.decoders/content-type (::http/content-type rep))]
      ;; TODO: Check that the OpenAPI supports the content-type of the request
      ;; payload, if not, throw a 415.
      (format "%s/%s" type subtype))))

(defmethod validate-request-payload :default [req]
  ;; Regardless of whether the OpenAPI declares it can read the content-type, we
  ;; can't process it.
  (throw
   (ex-info
    "Unsupported media type"
    {::site/request-context (assoc req :ring.response/status 415)})))

(defmethod validate-request-payload "application/edn"
  [{::site/keys [received-representation resource db] :as req}]
  ;; The assumption here is that EDN resource is 'good to go' as resource
  ;; state. But authorization rules will be run by put-resource-state that will
  ;; determine whether it is allowed in.
  (assoc req ::apex/request-payload received-representation))

(defn validate-instance [req instance schema base-document]
  (let [validation
        (jinx.api/validate
         schema instance
         {:base-document base-document})

        _ (when-not (::jinx/valid? validation)
            (throw
             (ex-info
              "Schema validation failed"
              {::jinx/validation-results validation
               ::site/request-context (assoc req :ring.response/status 400)})))

        validation (-> validation
                       process-transformations process-keyword-mappings)]

    (::jinx/instance validation)))

(defmethod validate-request-payload "application/json"
  [{::site/keys [received-representation resource db] :as req}]
  (let [
        body (::http/body received-representation)

        instance (try
                   (json/read-value body)
                   (catch Exception _
                     (throw
                      (ex-info
                       "Bad JSON in input"
                       {::site/request-context (assoc req :ring.response/status 400)}))))
        _ (assert instance)

        schema (get-in resource [::apex/operation "requestBody" "content"
                                 "application/json" "schema"])
        _ (assert schema)

        openapi-ref (get resource ::apex/openapi)
        _ (assert openapi-ref)

        openapi-resource (xt/entity db openapi-ref)
        _ (assert openapi-resource)

        instance (validate-instance req instance schema (::apex/openapi openapi-resource))

        ;; Replace any remaining string keys with keyword equivalents.
        instance (reduce-kv
                  (fn [acc k v] (assoc acc (cond-> k (string? k) keyword) v))
                  {} instance)

        ;; Inject path parameter values into body, this is a feature, enabled by
        ;; the x-juxt-site-inject-property OpenAPI extension keyword.
        instance (let [path-params (get-in req [::site/resource ::apex/openapi-path-params])]
                   (reduce-kv
                    (fn [acc _ v]
                      (let [inject-property (get v :inject-property)]
                        (cond-> acc
                          inject-property
                          (assoc (keyword inject-property) (:value v)))))
                    instance path-params))]

    (assoc req ::apex/request-payload instance)))

(defmethod validate-request-payload "application/x-www-form-urlencoded"
  [{::site/keys [received-representation resource db] :as req}]
  (let [
        body (::http/body received-representation)

        instance (try
                   (form-decode (String. body))
                   (catch Exception e
                     (throw
                      (ex-info
                       "Bad input"
                       {::site/request-context (assoc req :ring.response/status 400)}
                       e))))
        _ (assert instance)

        ;; Unfilled fields will be presented as empty strings. We want to nil
        ;; these out because if they are present they will be validated, even if
        ;; the field itself is optional. So an empty string "" means that the
        ;; field is not provided.
        instance (reduce-kv
                  (fn [acc k v]
                    (cond-> acc (not (str/blank? v)) (assoc k v)))
                  {} instance)

        _ (log/tracef "form instance is %s" instance)

        schema (get-in resource [::apex/operation "requestBody" "content"
                                 "application/x-www-form-urlencoded" "schema"])
        _ (assert schema)

        openapi-ref (get resource ::apex/openapi)
        _ (assert openapi-ref)

        openapi-resource (xt/entity db openapi-ref)
        _ (assert openapi-resource)

        instance (validate-instance req instance schema (::apex/openapi openapi-resource))]

    (assoc req ::apex/request-payload instance)))

(condp (fn [pred els] (pred (count els))) []
  zero? :zero
  #(= 1 %) :one
  #(< 1 %) :many)


(defn put-resource-state
  "Put some new resource state into XT, if authorization checks pass. The new
  resource state should be a valid XT entity, with a :xt/id"
  [{::site/keys [received-representation start-date resource db xt-node request-id]
    ::pass/keys [subject]
    :as req}
   new-resource-state]

  (assert new-resource-state)

  (let [id (:xt/id new-resource-state)
        _ (assert id)

        already-exists? (xt/entity db id)

        last-modified start-date
        etag (format "\"%s\"" (-> received-representation
                                  ::http/body util/hexdigest (subs 0 32)))

        ;; Link the resource state with the request that supplied it, for audit
        ;; and other purposes.
        new-resource-state (assoc new-resource-state ::site/request request-id)

        pass-ctx (select-keys req [::pass/subject ::pass/purpose
                                   ;; Wait, won't this be the resource xt/id? i.e. not the whole resource?
                                   ::pass/resource
                                   ])
        action (get-in req [::site/resource ::apex/operation-action])
        action-result (authz/do-action xt-node pass-ctx action new-resource-state)
        _ (log/tracef "action result is %s" action-result)]

    ;; Since this resource is 'managed' by the locate-resource in this ns, we
    ;; don't have to worry about http attributes - these will be provided by
    ;; the locate-resource function. We just need the resource state here.

    ;; TODO: Although this resource is managed, shouldn't we put a
    ;; representation into the db anyway, if only to store the last-modified and
    ;; etag validators?

    ;; Handle any error
    (when-let [{:keys [message ex-data] :as error} (::site/error action-result)]
      (return req (:ring.response/status error 500) message ex-data))

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
  [{:ring.request/keys [method] ::site/keys [base-uri] :as req}
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
            action (get operation-object "x-juxt-site-action")

            acceptable (str/join ", " (map first (get-in operation-object ["requestBody" "content"])))

            ;; These methods are derived from the operations declared for each
            ;; path, and the actions required.
            methods
            (let [methods
                  (into
                   {}
                   (for [[mth operation-object] path-item-object]
                     [(keyword mth)
                      ;; We fish out the action and make the call conditional on
                      ;; permission to call it. This will mean that permissions
                      ;; will be searched for twice, once by the authorization
                      ;; middlware, once in the do-action transaction. The
                      ;; latter is necessary as it's in the tx-fn and defends
                      ;; against inconsistency. Perhaps we don't need to
                      ;; duplicate here, but then we need some other way of
                      ;; ushering the request through the authorization
                      ;; middleware.
                      {::pass/actions #{(get operation-object "x-juxt-site-action")}}]))]
              (cond-> methods
                (contains? methods :get)
                (conj [:head (get methods :get)])
                (not (contains? methods :options))
                (conj [:options
                       {::pass/actions
                        ;; The ability to request OPTIONS is required
                        ;; for a number of use-cases (CORS pre-flight
                        ;; requests) and should be separately
                        ;; authorized. Here, we use a well-known
                        ;; action.
                        #{(format "%s/_site/actions/http-options" base-uri)}}])))

            post-fn (when (= method :post)
                      (let [post-fn-sym (some-> (get operation-object "juxt.site.alpha/post-fn") symbol)
                            _ (when-not post-fn-sym
                                (return req 500 "A POST method operation definition must include a juxt.site.alpha/post-fn" {:operation-object operation-object}))
                            post-fn (try
                                      (requiring-resolve post-fn-sym)
                                      (catch IllegalArgumentException _
                                        (return req 500 "Failed to resolve post-fn: %s" post-fn-sym)))]
                        (when-not post-fn
                          (return req 500 "The post-fn for the operation is nil" {:operation-object operation-object}))
                        post-fn))

            ;; TODO: Replace this convention with something more robust.
            #_required-scope
            #_(or
             (some-> (get-in operation-object ["security" "oauth"]) set)
             ;; If the security entry is an empty map, it means 'no scopes'
             (when (= {} (get-in operation-object ["security"])) #{})
             ;; otherwise, nil means no authorization (which means 'deny access')
             )

            resource
            (cond-> {::site/resource-provider ::apex/openapi-path

                     ;; This is useful, because it is the base document for any relative
                     ;; json pointers.
                     ;; TODO: Search for instances of apex/openapi and ensure they do (x/entity)
                     ::apex/openapi openapi-uri

                     ::apex/operation operation-object
                     ::apex/openapi-path path
                     ::apex/openapi-path-params path-params
                     ::apex/operation-action action

                     ::http/methods methods

                     ;; TODO: Pull out the scopes from the document, find a grant for
                     ;;each scope. The scope doesn't necessarily provide access to the
                     ;;data, only the operation.

                     ::http/representations
                     (for [[media-type media-type-object]
                           (fast-get-in path-item-object ["get" "responses" "200" "content"])]
                       (into
                        media-type-object
                        {::http/content-type media-type
                         ;; Wait a second, if this doesn't get logged then we
                         ;; can use a 'proper' function right?
                         ::site/body-fn 'juxt.apex.alpha.representation-generation/entity-bytes-generator}))

                     ;; TODO: The allowed origins ought to be specified in the top-level
                     ;; of the openapi document, or under the security section. This is
                     ;; just for testing.
                     ::site/access-control-allow-origins
                     {"http://localhost:8000"
                      {::site/access-control-allow-methods #{:get :put :post :delete}
                       ::site/access-control-allow-headers #{"authorization" "content-type"}
                       ::site/access-control-allow-credentials true}}

                     ;; TODO: Merge in any properties of a resource that is in
                     ;; XT - e.g. if this resource is a collection, what type
                     ;; of collection is it? Some properties that can be used in
                     ;; the PDP.
                     }
              ;;required-scope (assoc ::apex/required-scope required-scope)
              ;;true (authorize req)
              )]

        ;; Add conditional entries to the resource
        (cond-> resource
          (seq acceptable) (assoc ::http/acceptable {"accept" acceptable})

          post-fn (assoc ::site/post-fn post-fn)
          ;; We no longer validate-request-payload so post-fn functions should do that
          #_post-fn-sym
          #_(assoc
             ::site/post-fn
             (fn post-fn-proxy [req]
               (assert ::site/start-date req)
               (log/debug "Calling post-fn" post-fn-sym)
               (let [f (try
                         (requiring-resolve post-fn-sym)
                         (catch IllegalArgumentException _
                           (throw
                            (ex-info
                             (str "Failed to find post-fn: " post-fn-sym)
                             {::site/request-context (assoc req :ring.response/status 500)}))))]
                 (f (validate-request-payload req)))))

          (= method :put)
          (assoc
           ::site/put-fn
           (fn put-fn [req]
             (let [rep (::site/received-representation req)
                   {:juxt.reap.alpha.rfc7231/keys [type subtype]}
                   (reap.decoders/content-type (::http/content-type rep))]
               (put-resource-state
                req
                (-> req
                    validate-request-payload
                    ::apex/request-payload
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

  (assert uri)
  (assert base-uri)
  (assert db)

  ;; Do we have any OpenAPIs in the database?
  (or
   ;; The OpenAPI document

   ;; TODO: In future, we should relax the locations where APIs can live to make
   ;; it easier to develop APIs with WebDav.
   (when (re-matches (re-pattern (format "%s/_site/apis/\\w+/openapi.json" base-uri)) uri)
     (or
      ;; It might exist
      (some-> (xt/entity db uri)
              (assoc ::site/resource-provider ::openapi-document
                     ::site/put-fn put-openapi))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/resource-provider ::openapi-empty-document-resource
       ::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::http/methods {:get {} :head {} :options {} :put {}}
       ::http/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.1.0"}
       ::site/put-fn put-openapi}))

   (let [openapis (xt/q db '{:find [openapi-uri openapi]
                            :where [[openapi-uri ::apex/openapi openapi]]})
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
               resource (merge openapi-resource (xt/entity db uri))]
           (if (every? ::jinx/valid? (vals (::apex/openapi-path-params openapi-resource)))
             resource
             (throw
              (ex-info
               "One or more of the path-parameters in the request did not validate against the required schema"
               {::site/resource resource
                ::site/request-context
                (into req {:ring.response/status 400
                           :ring.response/body "Bad Request\r\n"})}))))

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
               {::multiple-matched-resources resources
                ::site/request-context (assoc req :ring.response/status 500)})))))))))
