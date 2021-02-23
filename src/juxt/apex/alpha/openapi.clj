;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [crux.api :as crux]
   [hiccup.page :as hp]
   [json-html.core :refer [edn->html]]
   [jsonista.core :as json]
   [juxt.apex.alpha.parameters :refer [extract-params-from-request]]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.util :as util]
   [juxt.spin.alpha :as spin]
   [selmer.parser :as selmer]
   [selmer.util :refer [*custom-resource-path*]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))

;; TODO: Restrict where openapis can be PUT
(defn locate-resource
  [db
   ;; We'd like to locate the resource based on nothing but the URI of the
   ;; request. This would avoid 'routing' based on other aspects of the request,
   ;; such as headers (e.g. authorization, which should be left to the
   ;; authorization step that follows resource location). The reason we can't
   ;; yet do this is due to path parameters (see below). If we were to put path
   ;; parameters at one level higher in the OpenAPI, and enforce that, then we
   ;; could make this change.
   request
   ]
  ;; Do we have any OpenAPIs in the database?
  (or
   ;; The OpenAPI document
   (when (and (re-matches #"/_site/apis/\w+/openapi.json" (:uri request))
              (not (.endsWith (:uri request) "/")))
     (or
      ;; It might exist
      (crux/entity db (str "https://home.juxt.site" (:uri request)))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/resource-provider ::openapi-empty-document-resource
       ::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::http/methods #{:get :head :options :put}
       ::http/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}}))

   (let [abs-request-path (:uri request)
         openapis (crux/q db '{:find [openapi-eid openapi]
                               :where [[openapi-eid :juxt.apex.alpha/openapi openapi]]})]
     (when-let [{:keys [openapi-ent openapi rel-request-path]}
                (some
                 (fn [[openapi-eid openapi]]
                   (some
                    (fn [server]
                      (let [server-url (get server "url")]
                        (when (.startsWith abs-request-path server-url)
                          {:openapi-eid openapi-eid
                           :openapi openapi
                           :rel-request-path (subs abs-request-path (count server-url))})))
                    (get openapi "servers")))
                 ;; Iterate across all APIs in this server, looking for a match
                 ;; TODO: authorization
                 openapis)]

       ;; Yes?
       (let [paths (get openapi "paths")]
         ;; Any of these paths match the request's URL?
         (or
          (some
           (fn [[path path-item-object]]
             (let [path-params
                   (->>
                    (or
                     ;; TODO: We could mandate path parameters at the
                     ;; path-object level, not the path-item-object
                     (get-in path-item-object [(name (:request-method request)) "parameters"])
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

                 (let [path-params
                       (into
                        {}
                        (for [param path-params
                              :let [param-name (get param "name")]]
                          [param-name (.group matcher param-name)]))
                       operation-object (get path-item-object (name (:request-method request)))
                       acceptable (str/join ", " (map first (get-in operation-object ["requestBody" "content"])))]

                   (cond->
                       {::site/resource-provider ::openapi-path
                        ::apex/openid-path path
                        ::apex/openid-path-params path-params

                        ::apex/operation operation-object

                        ;; This is useful, because it is the base document for any
                        ;; relative json pointers.
                        ::apex/openapi openapi

                        ::http/methods
                        (keep
                         #{:get :head :post :put :delete :options :trace :connect}
                         (let [methods (set
                                        (conj (map keyword (keys path-item-object)) :options))]
                           (cond-> methods
                             (contains? methods :get)
                             (conj :head))))

                        ::http/representations
                        (for [[media-type media-type-object]
                              (fast-get-in path-item-object ["get" "responses" "200" "content"])]
                          {::http/content-type media-type
                           ::site/body-generator ::entity-bytes-generator})

                        ;; TODO: Merge in any properties of a resource that is in
                        ;; Crux - e.g. if this resource is a collection, what type
                        ;; of collection is it? Some properties that can be used in
                        ;; the PDP.
                        }
                       (seq acceptable) (assoc ::http/acceptable {"accept" acceptable}))))))

           paths)))))))

(defn ->query [input params]
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
    (reduce
     (fn [acc [k v]]
       (assoc acc (keyword k)
              (case (keyword k)
                :find (mapv symbol v)
                :where (mapv (fn [clause]
                               (cond
                                 (and (vector? clause) (every? (comp not coll?) clause))
                                 (mapv (fn [item txf] (txf item)) clause [symbol keyword symbol])

                                 ;;(and (vector? clause) (list? (first clause)))
                                 ;;(mapv (fn [item txf] (txf item)) clause [#(fn ) symbol])
                                 ))

                             v)

                :limit v
                :in (mapv symbol v)
                :args [(reduce-kv (fn [acc k v] (assoc acc (keyword k) v)) {} (first v))]
                )))
     {} input)))

;; By default we output the resource state, but there may be a better rendering
;; of collections, which can be inferred from the schema being an array and use
;; the items subschema.
(defmethod generate-representation-body ::entity-bytes-generator [request resource representation db authorization subject]

  (log/trace "entity-bytes-generator")

  (let [param-defs
        (get-in resource [:juxt.apex.alpha/operation "parameters"])

        in '[now subject]

        query
        (get-in resource [:juxt.apex.alpha/operation "responses" "200" "crux/query"])

        crux-query
        (when query (->query query (extract-params-from-request request param-defs)))

        authorized-query (when crux-query
                           ;; This is just temporary, in future, fail if no
                           ;; authorization. We just need to make sure there's
                           ;; an authorization for crux/admin
                           (if authorization
                             (pdp/->authorized-query crux-query authorization)
                             crux-query))

        authorized-query (when authorized-query
                           (assoc authorized-query :in in))

        resource-state
        (if authorized-query
          (for [[e] (crux/q db authorized-query
                            ;; time now
                            (java.util.Date.)
                            ;; subject
                            subject

                            )]
            (crux/entity db e))
          (crux/entity db (:uri request)))

        resource-state (util/sanitize resource-state)]

    ;; TODO: Might want to filter out the http metadata at some point
    (case (::http/content-type representation)
      "application/json"
      ;; TODO: Might want to filter out the http metadata at some point
      (-> resource-state
          (json/write-value-as-string (json/object-mapper {:pretty true}))
          (str "\r\n")
          (.getBytes "UTF-8"))

      "text/html;charset=utf-8"
      (let [config (get-in resource [:juxt.apex.alpha/operation "responses" "200" "content" (::http/content-type representation)])
            ]
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
              (let [fields (distinct (concat [:crux.db/id] (keys (first resource-state))))]
                [:table {:style "border: 1px solid #888; border-collapse: collapse; "}
                 [:thead
                  [:tr
                   (for [field fields]
                     [:th {:style "border: 1px solid #888; padding: 4pt; text-align: left"} (pr-str field)])]]
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

            (= (get config "type") "template")
            (binding [*custom-resource-path*
                      (java.net.URL. "http://localhost:8082/apps/card/templates/")]
              (selmer/render-file
               (java.net.URL. "http://localhost:8082/apps/card/templates/kanban.html")
               {}
               :custom-resource-path
               (java.net.URL. "http://localhost:8082/apps/card/templates/")))

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
             [:pre (with-out-str (pprint (->query query (extract-params-from-request request param-defs))))]))

          (when (seq param-defs)
            (list
             [:h3 "Parameters"]
             [:pre (with-out-str (pprint (extract-params-from-request request param-defs)))]))

          [:h3 "Resource state"]
          [:pre (with-out-str (pprint resource-state))])
         (.getBytes "UTF-8"))))))

(defmethod generate-representation-body ::api-console-generator [request resource representation db authorization subject]
  (let [cell-attrs {:style "border: 1px solid #888; padding: 4pt; text-align: left"}
        openapis (sort-by
                  (comp str first)
                  (crux/q db '{:find [e openapi]
                               :where [[e ::apex/openapi openapi]]}))]
    (->
     (hp/html5
      [:h1 "APIs"]
      (if (pos? (count openapis))
        (list
         [:p "These APIs are loaded and available:"]
         [:table {:style "border: 1px solid #888; border-collapse: collapse; "}
          [:thead
           [:tr
            (for [field ["Path" "Title" "Description" "Contact" "Swagger UI"]]
              [:th cell-attrs field])]]
          [:tbody
           (for [[uri openapi]
                 ;; TODO: authorization
                 openapis]
             [:tr
              [:td cell-attrs
               (get-in openapi ["servers" 0 "url"])]

              [:td cell-attrs
               (get-in openapi ["info" "title"])]

              [:td cell-attrs
               (get-in openapi ["info" "description"])]

              [:td cell-attrs
               (get-in openapi ["info" "contact" "name"])]

              [:td cell-attrs
               [:a {:href (format "/_site/swagger-ui/index.html?url=%s" uri)} uri]]])]])
        (list
         [:p "These are no APIs loaded."])))
     (.getBytes "UTF-8"))))

(defn put-openapi [request resource openapi-json-representation date crux-node]

  (let [uri (str "https://home.juxt.site" (:uri request))
        openapi (json/read-value (java.io.ByteArrayInputStream. (::http/body openapi-json-representation)))
        etag (format "\"%s\"" (subs (util/hexdigest (.getBytes (pr-str openapi) "UTF-8")) 0 32))]
    (->>
     (crux/submit-tx
      crux-node
      [
       [:crux.tx/put
        {:crux.db/id uri

         ;; Resource configuration
         ::http/methods #{:get :head :put :options}
         ::http/representations
         [(assoc
           openapi-json-representation
           ::http/etag etag
           ::http/last-modified date)]

         ;; Resource state
         ::apex/openapi openapi}]])
     (crux/await-tx crux-node))

    (spin/response
     (if (zero? (count (::http/representations resource))) 201 204)
     nil request nil date nil)))

(defn put-json-representation
  [request resource received-representation date crux-node]

  (let [last-modified date
        etag (format "\"%s\"" (-> received-representation ::http/body util/hexdigest (subs 0 32)))

        schema
        (get-in
         resource
         [::apex/operation "requestBody" "content" "application/json" "schema"])
        _ (assert schema)

        instance (json/read-value (::http/body received-representation))
        _ (assert instance)

        openapi (:juxt.apex.alpha/openapi resource)
        _ (assert openapi)

        validation-results
        (jinx.api/validate schema instance {:base-document openapi})]

    (when-not (::jinx/valid? validation-results)
      (throw
       (ex-info
        "Schema validation failed"
        {::http/response
         {:status 400
          ;; TODO: Content negotiation for error responses
          :body (with-out-str (pprint validation-results))}})))


    (let [validation (-> validation-results process-transformations process-keyword-mappings)
          instance
          (->>
           (::jinx/instance validation)
           ;; Replace any remaining string keys with keyword equivalents.
           (reduce-kv
            (fn [acc k v]
              (assoc acc (cond-> k (string? k) keyword) v))
            {}))
          id (str "https://home.juxt.site" (:uri request))]

      ;; Since this resource is 'managed' by the locate-resource in this ns, we
      ;; don't have to worry about http attributes - these will be provided by
      ;; the locate-resource function. We just need the resource state here.
      (->>
       (crux/submit-tx
        crux-node
        [[:crux.tx/put (assoc instance :crux.db/id id)]])
       (crux/await-tx crux-node))

      (spin/response
       (if (zero? (count (::http/representations resource))) 201 204)
       {::http/etag etag
        ::http/last-modified last-modified}
       request nil date nil))))
