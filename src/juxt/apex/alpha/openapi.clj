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
   [integrant.core :as ig]
   [json-html.core :refer [edn->html]]
   [jsonista.core :as json]
   [juxt.apex.alpha :as apex]
   [juxt.apex.alpha.parameters :refer [extract-params-from-request]]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.util :as util]
   [juxt.spin.alpha :as spin]
   [selmer.parser :as selmer]
   [selmer.util :refer [*custom-resource-path*]]))

;; TODO: Restrict where openapis can be PUT
(defn locate-resource [request db]
  ;; Do we have any OpenAPIs in the database?
  (or
   ;; The OpenAPI document
   (when (and (re-matches #"/_crux/apis/\w+/openapi.json" (:uri request))
              (not (.endsWith (:uri request) "/")))
     (or
      ;; It might exist
      (crux/entity db (:uri request))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::spin/methods #{:get :head :options :put}
       ::spin/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}}))

   (let [abs-request-path (:uri request)]
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
                 (crux/q db '{:find [openapi-eid openapi]
                              :where [[openapi-eid :juxt.apex.alpha/openapi openapi]]}))]

       ;; Yes?
       (let [paths (get openapi "paths")]
         ;; Any of these paths match the request's URL?
         (some
          (fn [[path path-item-object]]
            (let [path-params
                  (->>
                   (or
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
                  pattern (str pattern "$")

                  matcher (re-matcher (re-pattern pattern) rel-request-path)]

              (when (.find matcher)
                ;;(prn "REGION" (.regionStart matcher) (.regionEnd matcher))
                (let [path-params
                      (into
                       {}
                       (for [param path-params
                             :let [param-name (get param "name")]]
                         [param-name (.group matcher param-name)]))

                      operation-object (get path-item-object (name (:request-method request)))]

                  {:description "OpenAPI matched path"
                   ::apex/openid-path path
                   ::apex/openid-path-params path-params
                   ::spin/methods
                   (keep
                    #{:get :head :post :put :delete :options :trace :connect}
                    (let [methods (set
                                   (conj (map keyword (keys path-item-object)) :options))]
                      (cond-> methods
                        (contains? methods :get)
                        (conj :head))))

                   ::spin/representations
                   (for [[media-type media-type-object]
                         (fast-get-in path-item-object ["get" "responses" "200" "content"])]
                     {::spin/content-type media-type
                      ::spin/bytes-generator ::entity-bytes-generator})

                   ::apex/operation operation-object

                   ;; This is useful, because it is the base document for any
                   ;; relative json pointers.
                   ::apex/openapi openapi}))))

          paths))))))

(defn ->query [input params]
  (let [input (postwalk (fn [x]
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

;; Possibly promote up into site - by default we output the resource state, but
;; there may be a better rendering of collections, which can be inferred from
;; the schema being an array and use the items subschema. We can also use the
;; resource state as a
(defmethod generate-representation-body ::entity-bytes-generator [request resource representation db authorization subject]

  (let [param-defs
        (get-in resource [:juxt.apex.alpha/operation "parameters"])

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

        resource-state
        (if authorized-query
          (for [[e] (crux/q db authorized-query
                            ;; Could put some in params here
                            )]
            (crux/entity db e))
          (crux/entity db (:uri request)))]

    ;; TODO: Might want to filter out the spin metadata at some point
    (case (::spin/content-type representation)
      "application/json"
      ;; TODO: Might want to filter out the spin metadata at some point
      (.getBytes (str (json/write-value-as-string resource-state) "\r\n") "utf-8")

      "text/html;charset=utf-8"
      (let [config (get-in resource [:juxt.apex.alpha/operation "responses" "200" "content" (::spin/content-type representation)])
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
         (.getBytes "utf-8"))))))

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
               [:a {:href (format "/_crux/swagger-ui/index.html?url=%s" uri)} uri]]])]])
        (list
         [:p "These are no APIs loaded."])))
     (.getBytes "utf-8"))))

(defn put-openapi [request _ openapi-json-representation _ crux-node]

  (let [uri (:uri request)
        last-modified (java.util.Date.)
        openapi (json/read-value (java.io.ByteArrayInputStream. (::spin/bytes openapi-json-representation)))
        etag (format "\"%s\"" (subs (util/hexdigest (.getBytes (pr-str openapi) "utf-8")) 0 32))]
    (crux/submit-tx
     crux-node
     [
      [:crux.tx/put
       {:crux.db/id uri

        ;; Resource configuration
        ::spin/methods #{:get :head :put :options}
        ::spin/representations
        [(assoc
          openapi-json-representation
          ::spin/etag etag
          ::spin/last-modified last-modified)]

        ;; Resource state
        ::apex/openapi openapi}]])

    {:status 201
     ;; TODO: Add :body to describe the new resource
     }))

(defn put-json-representation
  [request resource new-representation old-representation crux-node]

  (let [date (java.util.Date.)
        last-modified date
        etag (format "\"%s\"" (-> new-representation
                                  ::spin/bytes
                                  util/hexdigest
                                  (subs 0 32)))

        representation-metadata
        {::spin/etag etag
         ::spin/last-modified last-modified}

        schema
        (get-in
         resource
         [::apex/operation "requestBody" "content" "application/json" "schema"])
        _ (assert schema)

        instance (json/read-value (::spin/bytes new-representation))
        _ (assert instance)

        openapi (:juxt.apex.alpha/openapi resource)
        _ (assert openapi)

        validation-results
        (jinx.api/validate schema instance {:base-document openapi})]

    ;; TODO: extract the title/version of the API and add to the entity (as metadata)
    #_(let [openapi (crux/entity (crux/db crux-node) (::apex/!api resource))]
        (prn "version of open-api used to put this resource is"
             (get-in openapi [::apex/openapi "info" "version"])))

    ;; TODO: Validate new-representation against the JSON schema in the openapi.

    (when-not (::jinx/valid? validation-results)
      (pprint validation-results)
      (throw
       (ex-info
        "Schema validation failed"
        {::spin/response
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
          id (:uri request)]

      ;; Since this resource is 'managed' by the locate-resource in this ns, we
      ;; don't have to worry about spin attributes - these will be provided by
      ;; the locate-resource function. We just need the resource state here.
      (crux/submit-tx
       crux-node
       [[:crux.tx/put (assoc instance :crux.db/id id)]])

      (spin/response
       (if old-representation 200 201)
       (response/representation-metadata-headers
        (merge
         representation-metadata
         ;; TODO: Source this from the openapi, proactively content-negotiation if
         ;; multiple possible (for each of 200 and 201)
         {::spin/content-type "application/json"}))
       nil
       request
       nil
       date
       (json/write-value-as-bytes instance)))))

;; TODO: This can be PUT instead of being built-in.
(defn swagger-ui []
  (let [jarpath
        (some
         #(re-matches #".*swagger-ui.*" %)
         (str/split (System/getProperty "java.class.path") #":"))
        fl (io/file jarpath)
        jar (java.util.jar.JarFile. fl)]
    (doall
     (for [je (enumeration-seq (.entries jar))
           :let [nm (.getRealName je)
                 [_ suffix] (re-matches #".*\.(.*)" nm)
                 size (.getSize je)
                 bytes (byte-array size)
                 path (second
                       (re-matches #"META-INF/resources/webjars/swagger-ui/[0-9.]+/(.*)"
                                   nm))]
           :when path
           :let [uri (format "/_crux/swagger-ui/%s" path)]
           :when (pos? size)]
       (do
         (.readFully (java.io.DataInputStream. (.getInputStream jar je)) bytes)
         [:crux.tx/put
          {:crux.db/id uri
           ::spin/methods #{:get :head :options}
           ::spin/representations [{::spin/content-type (get util/mime-types suffix "application/octet-stream")
                                    ::spin/last-modified (java.util.Date. (.getTime je))
                                    ::spin/content-length size
                                    ::spin/content-location uri
                                    ::spin/bytes bytes}]}])))))


(defn api-console []
  [[:crux.tx/put
    {:crux.db/id "/_crux/api-console"
     ::spin/methods #{:get :head :options}
     ::spin/representations
     [{::spin/content-type "text/html;charset=utf-8"
       ::spin/bytes-generator ::api-console-generator}]}]])

(defmethod ig/init-key ::module [_ {:keys [crux-node]}]
  (println "Adding OpenAPI module")
  (crux/submit-tx
   crux-node
   (concat
    ;; This should be possible to upload
    (swagger-ui)
    ;; This needs an api-console-generator, so not sure it can be uploaded
    (api-console))))
