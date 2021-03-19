;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [crux.api :as x]
   [hiccup.page :as hp]
   [json-html.core :refer [edn->html]]
   [jsonista.core :as json]
   [juxt.apex.alpha.parameters :refer [extract-params-from-request]]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.reap.alpha.decoders :as reap.decoders]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.util :as util]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn put-openapi
  [{::site/keys [db crux-node uri resource
                 received-representation start-date] :as req}]

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
          ::http/content-type "application/json"})]])
     (x/await-tx crux-node))

    (assoc req
           :ring.response/status
           (if (= (::site/resource-provider resource) ::openapi-empty-document-resource)
             201 204))))

(defn- received-body->json
  "Return a JSON instance from the request. Throw a 400 error if the JSON is
  invalid."
  [{::site/keys [resource received-representation] :as req}]

  (let [body (::http/body received-representation)
        schema (get-in resource [::site/request-locals ::apex/operation "requestBody" "content"
                                 "application/json" "schema"]) _ (assert schema)
        instance (json/read-value body) _ (assert instance)
        openapi (get-in resource [::site/request-locals ::apex/openapi]) _ (assert openapi)

        validation
        (jinx.api/validate schema instance {:base-document openapi})

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

        authorization
        (pdp/authorization
         db
         {'subject subject
          'resource (dissoc resource ::site/request-locals)
          ;; might change to 'action' at
          ;; this point
          'request (select-keys req [:ring.request/method])
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
                (-> req
                    (into {:ring.response/status status
                           :ring.response/body (str message "\r\n")}))))))

        exists? (x/entity db uri)

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
        (assoc :ring.response/status (if-not exists? 201 204)
               ::http/etag etag
               ::http/last-modified last-modified)
        (update :ring.response/headers assoc "location" uri))))


(defn path-entry->resource
  "From an OpenAPI path-to-path-object entry, return the corresponding resource
  if it matches the path"
  [{:ring.request/keys [method]}
   [path path-item-object] openapi rel-request-path]
  (let [path-params
        (->>
         (or
          ;; TODO: We could mandate path parameters at the
          ;; path-object level, not the path-item-object
          (get-in path-item-object [(name method) "parameters"])
          (get-in path-item-object ["parameters"]))
         (filter #(= (get % "in") "path")))

        pattern
        (str/replace
         path
         #"\{(\p{Alpha}+)\}"
         (fn [[_ group]]
           (format "(?<%s>[\\p{Alnum}-_]+)" group)))

        ;; We have to terminate with a 'end of line' otherwise we
        ;; match too eagerly. So if we had /users and /users/{id},
        ;; then '/users/foo' might match /users.
        pattern (str "^" pattern "$")

        matcher (re-matcher (re-pattern pattern) rel-request-path)]

    (when (.find matcher)

      (log/trace "Got a match!" rel-request-path)

      (let [path-params
            (into
             {}
             (for [param path-params
                   :let [param-name (get param "name")]]
               [param-name (.group matcher param-name)]))
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

            post-fn (when (= method :post) (some-> (get operation-object "juxt.site.alpha/post-fn") symbol))

            resource
            {::site/resource-provider ::openapi-path

             ;; This is useful, because it is the base document for any
             ;; relative json pointers.

             ::site/request-locals
             {::apex/openapi openapi
              ::apex/operation operation-object
              ::apex/openid-path path
              ::apex/openid-path-params path-params}

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
               ::site/access-control-allow-headers #{"authorization"}}}

             ;; TODO: Merge in any properties of a resource that is in
             ;; Crux - e.g. if this resource is a collection, what type
             ;; of collection is it? Some properties that can be used in
             ;; the PDP.
             }]

        ;; Add conditional entries to the resource
        (cond-> resource
          (seq acceptable) (assoc ::http/acceptable {"accept" acceptable})

          post-fn (assoc-in
                   ;; Use ::site/request-locals to avoid database involvement
                   [::site/request-locals ::site/post-fn]
                   (fn [req]
                     (log/debug "Calling post-fn" post-fn)
                     (let [f (requiring-resolve post-fn)]
                       (f (assoc-in
                           req [::site/request-locals ::apex/request-instance]
                           (received-body->json req))))))

          (= method :put)
          (assoc-in [::site/request-locals ::site/put-fn]
                    (fn [req]
                      (let [rep (::site/received-representation req)
                            {:juxt.reap.alpha.rfc7231/keys [type subtype parameter-map]}
                            (reap.decoders/content-type (::http/content-type rep))]
                        (case
                            ;; TODO: Depending on requestBody
                            (and
                             (.equalsIgnoreCase "application" type)
                             (.equalsIgnoreCase "json" subtype))
                            (put-json-representation req))))))))))

;; TODO: Restrict where openapis can be PUT
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
   (when (re-matches (re-pattern (format "%s/_site/apis/\\w+/openapi.json" base-uri)) uri)
     (or
      ;; It might exist
      (some-> (x/entity db uri)
              (assoc ::site/resource-provider ::openapi-document
                     ::site/request-locals {::site/put-fn put-openapi}))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/resource-provider ::openapi-empty-document-resource
       ::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::http/methods #{:get :head :options :put}
       ::http/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}
       ::site/request-locals {::site/put-fn put-openapi}}))

   (let [openapis (x/q db '{:find [openapi-eid openapi]
                            :where [[openapi-eid ::apex/openapi openapi]]})]


     (some
      (fn [[openapi-uri openapi]]
        #_(log/tracef "Checking API: %s" openapi-uri)
        (some
         (fn [server]
           #_(log/tracef "Checking server: %s" server)
           (let [server-url (get server "url")
                 server-url (cond->> server-url
                              (.startsWith server-url "/") (str base-uri))]
             (when (.startsWith uri server-url)
               (let [paths (get openapi "paths")]
                 (some
                  #(path-entry->resource req % openapi (subs uri (count server-url)))
                  paths)))))
         ;; Iterate
         (get openapi "servers")))
      ;; Iterate across all APIs in this server, looking for a match
      openapis))))

(defn ->query [input params]

  ;; Replace input with values from params
  (let [input
        (postwalk
         (fn [x]
           (if (and (map? x)
                    (contains? x "name")
                    (= (get x "in") "query"))
             (get-in params [:query (get x "name") :value]
                     (get-in params [:query (get x "name") :param "default"]))
             x))
         input)]

    ;; Perform manipulations required for each key
    (reduce
     (fn [acc [k v]]
       (assoc acc (keyword k)
              (case (keyword k)
                :find (mapv symbol v)
                :where (mapv
                        ;; We're using some inline recursion to keep things lean-ish here
                        ;; based on the assumption we can bump our stack _a little_ higher
                        ;; and that our clauses will remain fairly simple
                        (fn translate-clause [clause]
                          (cond
                            (and (vector? clause) (every? (comp not coll?) clause))
                            (mapv (fn [item txf] (txf item)) clause [symbol keyword symbol])

                            (and (vector? clause) (vector? (second clause)))
                            (cons (symbol (first clause))
                                  (map translate-clause (rest clause)))

                            (and (vector? clause) (vector? (first clause)))
                            [(seq (map symbol (first clause)))]))
                        v)

                :limit v
                :in (mapv symbol v)
                :args (mapv clojure.walk/keywordize-keys v)
                )))
     {} input)))

;; By default we output the resource state, but there may be a better rendering
;; of collections, which can be inferred from the schema being an array and use
;; the items subschema.
(defn entity-bytes-generator [{::site/keys [uri resource selected-representation db]
                               ::pass/keys [authorization subject] :as req}]

  (let [param-defs
        (get-in resource [::site/request-locals ::apex/operation "parameters"])

        in '[now subject]

        query
        (get-in resource [::site/request-locals ::apex/operation "responses" "200" "crux/query"])

        crux-query
        (when query (->query query (extract-params-from-request req param-defs)))

        authorized-query (when crux-query
                           ;; This is just temporary, in future, fail if no
                           ;; authorization. We just need to make sure there's
                           ;; an authorization for the subject.
                           (if authorization
                             (pdp/->authorized-query crux-query authorization)
                             crux-query))

        authorized-query (when authorized-query
                           (assoc authorized-query :in in))

        resource-state
        (if authorized-query
          (for [[e] (x/q db authorized-query
                         ;; time now
                         (java.util.Date.)
                         ;; subject
                         subject)]
            (x/entity db e))
          (x/entity db uri))]

    ;; TODO: Might want to filter out the http metadata at some point
    (case (::http/content-type selected-representation)
      "application/json"
      ;; TODO: Might want to filter out the http metadata at some point
      (-> resource-state
          (json/write-value-as-string (json/object-mapper {:pretty true}))
          (str "\r\n")
          (.getBytes "UTF-8"))

      "text/html;charset=utf-8"
      (let [config (get-in resource [::site/request-locals ::apex/operation "responses"
                                     "200" "content"
                                     (::http/content-type selected-representation)])]
        (->
         (hp/html5
          [:h1 (get config "title" "No title")]

          ;; Get :path-params = {"id" "owners"}

          (cond
            (= (get config "type") "edn-table")
            (list
             [:style
              (slurp (io/resource "json.human.css"))]
             (edn->html resource-state))

            (= (get config "type") "table")
            (if (seq resource-state)
              (let [fields (distinct (concat [:crux.db/id]
                                             (keys (first resource-state))))]
                [:table {:style "border: 1px solid #888; border-collapse: collapse;"}
                 [:thead
                  [:tr
                   (for [field fields]
                     [:th {:style "border: 1px solid #888; padding: 4pt; text-align: left"}
                      (pr-str field)])]]
                 [:tbody
                  (for [row resource-state]
                    [:tr
                     (for [field fields
                           :let [val (get row field)]]
                       [:td {:style "border: 1px solid #888; padding: 4pt; text-align: left"}
                        (cond
                          (uri? val)
                          [:a {:href val} val]
                          :else
                          (pr-str (get row field)))])])]])
              [:p "No results"])

            :else
            (let [fields (distinct (concat [:crux.db/id] (keys resource-state)))]
              [:dl
               (for [field fields
                     :let [val (get resource-state field)]]
                 (list
                  [:dt
                   (pr-str field)]
                  [:dd
                   (cond
                     (uri? val)
                     [:a {:href val} val]
                     :else
                     (pr-str (get resource-state field)))]))]))

          [:h2 "Debug"]
          [:h3 "Resource"]
          [:pre (with-out-str (pprint resource))]
          (when query
            (list
             [:h3 "Query"]
             [:pre (with-out-str (pprint query))]))
          (when crux-query
            (list
             [:h3 "Crux Query"]
             [:pre (with-out-str (pprint (->query query (extract-params-from-request req param-defs))))]))

          (when (seq param-defs)
            (list
             [:h3 "Parameters"]
             [:pre (with-out-str (pprint (extract-params-from-request req param-defs)))]))

          [:h3 "Resource state"]
          [:pre (with-out-str (pprint resource-state))])
         (.getBytes "UTF-8"))))))
