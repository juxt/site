;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.openapi
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [crux.api :as crux]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.api :as jinx.api]
   [hiccup.core :as h]
   [hiccup.page :as hp]
   [integrant.core :as ig]
   [juxt.apex.alpha :as apex]
   [juxt.site.alpha :as site]
   [juxt.site.alpha.payload :refer [generate-representation-body]]
   [juxt.site.alpha.perf :refer [fast-get-in]]
   [juxt.site.alpha.put :refer [put-representation]]
   [juxt.jinx.alpha.vocabularies.transformation :refer [process-transformations]]
   [juxt.jinx.alpha.vocabularies.keyword-mapping :refer [process-keyword-mappings]]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.util :as util]
   [juxt.site.alpha.entity :as entity]
   [juxt.spin.alpha :as spin]
   [jsonista.core :as json])
  (:import
   (java.net URI)
   (java.util Date UUID)))

;; TODO: Restrict where openapis can be PUT
(defn locate-resource [request db]
  ;; Do we have any OpenAPIs in the database?
  (or

   ;; The OpenAPI document
   (when (and (re-matches #"/_crux/apis/\w+/openapi.json" (:uri request))
              (not (.endsWith (:uri request) "/")))

     (or
      ;; It might exist
      (crux/entity db (URI. (:uri request)))

      ;; Or it might not
      ;; This last item (:put) might be granted by the PDP.
      {::site/description
       "Resource with no representations accepting a PUT of an OpenAPI JSON document."
       ::spin/methods #{:get :head :options :put}
       ::spin/acceptable {"accept" "application/vnd.oai.openapi+json;version=3.0.2"}}))

   (let [abs-request-path (.getPath (URI. (:uri request)))]
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
                  (re-pattern
                   (str/replace
                    path
                    #"\{(\p{Alpha}+)\}"
                    (fn [[_ group]]
                      (format "(?<%s>\\w+)" group))))

                  matcher (re-matcher pattern rel-request-path)]

              (when (.find matcher)
                (let [path-params
                      (into
                       {}
                       (for [param path-params
                             :let [param-name (get param "name")]]
                         [param-name (.group matcher param-name)]))

                      operation-object (get path-item-object (name (:request-method request)))]

                  {::spin/methods
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
                      ::spin/last-modified (java.util.Date.)
                      ::spin/bytes-generator ::entity-bytes-generator
                      })

                   ::apex/operation operation-object

                   ;; This is useful, because it is the base document for any
                   ;; relative json pointers.
                   ::apex/openapi openapi}))))

          paths))))))

;; Possibly promote up into site - by default we output the resource state, but
;; there may be a better rendering of collections, which can be inferred from
;; the schema being an array and use the items subschema. We can also use the
;; resource state as a
(defmethod generate-representation-body ::entity-bytes-generator [resource representation db]
  (let [resource-state
        (reduce-kv
         (fn [acc k v]
           (cond-> acc
             (not (and (keyword? k)
                       (some->>
                        (namespace k)
                        (re-matches #"juxt\.(reap|spin|site|pick)\..*"))))
             (assoc k v)))
         {}
         representation)]
    ;; TODO: Might want to filter out the spin metadata at some point
    (case (::spin/content-type representation)
      "application/json"
      ;; TODO: Might want to filter out the spin metadata at some point
      (json/write-value-as-bytes resource-state)

      "text/html;charset=utf-8"
      (.getBytes

       (hp/html5
        [:h1 "Crux entity"]
        [:pre (with-out-str (pprint resource-state))]
        )
       ))))

(defmethod generate-representation-body ::api-console-generator [resource representation db]
  (.getBytes
   (.toString
    (doto (StringBuilder.)
      (.append "<h1>API Console</h1>\r\n")
      (.append "<ul>")
      (.append
       (apply str
              (for [[uri openapi]
                    (crux/q db '{:find [e openapi]
                                 :where [[e ::apex/openapi openapi]]})]
                (str
                 "<li>" (get-in openapi ["info" "title"])
                 "&nbsp<small>[&nbsp;"
                 (format "<a href='/_crux/swagger-ui/index.html?url=%s'>" uri)
                 "Swagger UI"
                 "&nbsp;]</small>"
                 "</a></li>"))))
      (.append "</ul>")))))


(defmethod put-representation
  "application/vnd.oai.openapi+json;version=3.0.2"
  [request _ openapi-json-representation old-representation crux-node]

  (let [uri (URI. (:uri request))
        last-modified (java.util.Date.)
        openapi (json/read-value (java.io.ByteArrayInputStream. (::spin/bytes openapi-json-representation)))
        etag (format "\"%s\"" (subs (util/hexdigest (.getBytes (pr-str openapi))) 0 32))]
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

(defmethod put-representation
  "application/json"
  [request resource new-representation old-representation crux-node]

  #_(prn "old-representation is")
  #_(println (with-out-str (pprint old-representation)))

  (let [date (java.util.Date.)
        last-modified date
        etag (format "\"%s\"" (subs (util/hexdigest (::spin/bytes new-representation)) 0 32))
        representation-metadata {::spin/etag etag
                                 ::spin/last-modified last-modified}
        schema (get-in resource [::apex/operation "requestBody" "content" "application/json" "schema"])
        _ (assert schema)
        instance (-> (json/read-value (::spin/bytes new-representation))
                     ;; If we don't add the id, we'll fail the schema validation
                     ;; check
                     (assoc "id" (:uri request)))
        _ (assert instance)
        openapi (:juxt.apex.alpha/openapi resource)
        _ (assert openapi)
        validation-results (jinx.api/validate schema instance {:base-document openapi})
        ]

    ;; TODO: extract the title/version of the API and add to the entity (as metadata)
    #_(let [openapi (crux/entity (crux/db crux-node) (::apex/!api resource))]
        (prn "version of open-api used to put this resource is"
             (get-in openapi [::apex/openapi "info" "version"])))

    ;; TODO: Validate new-representation against the JSON schema in the openapi.

    (when-not (::jinx/valid? validation-results)
      (prn "validation-results")
      (println (with-out-str (pprint validation-results)))
      (throw
       (ex-info
        "Schema validation failed"
        {::spin/response
         {:status 400
          ;; TODO: Content negotiation for error responses
          :body (with-out-str (pprint validation-results))}})))

    (let [validation (-> validation-results process-transformations process-keyword-mappings)
          instance (::jinx/instance validation)]

      (assert (:crux.db/id instance) "The doc must contain an entry for :crux.db/id")

      (crux/submit-tx
       crux-node
       [[:crux.tx/put
         (merge
          instance
          {::spin/methods #{:get :head :put :options}
           ::spin/representations
           [{::spin/content-type "application/json"
             ::spin/text "TODO: Render Crux entity as JSON"}]
           ;;::apex/openapi (::apex/openapi resource)
           })]])

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
                                   nm))
                 uri (URI. (format "/_crux/swagger-ui/%s" path))]
           :when (pos? size)]
       (do
         (.read (.getInputStream jar je) bytes 0 size)
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
    {:crux.db/id (URI. "/_crux/api-console")
     ::spin/methods #{:get :head :options}
     ::spin/representations
     [{::spin/content-type "text/html;charset=utf-8"
       ::spin/bytes-generator ::api-console-generator}]}]])

(defmethod ig/init-key ::module [_ {:keys [crux]}]
  (println "Adding OpenAPI module")
  (crux/submit-tx
   crux
   (concat
    ;; This should be possible to upload
    (swagger-ui)
    ;; This needs an api-console-generator, so not sure it can be uploaded
    (api-console))))
