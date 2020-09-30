;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.openapi
  (:require
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.site.alpha.put :refer [put-representation]]
   [juxt.site.alpha.util :refer [hexdigest]]
   [juxt.spin.alpha :as spin]
   [jsonista.core :as json]))

(defmethod put-representation
  "application/vnd.oai.openapi+json;version=3.0.2"
  [request resource new-representation old-representation crux-node]

  (let [uri (java.net.URI. (:uri request))
        last-modified (java.util.Date.)
        new-resource-uri (java.net.URI. (str (:uri request) "/openapi.json"))
        openapi (json/read-value (java.io.ByteArrayInputStream. (::spin/bytes new-representation)))
        etag (format "\"%s\"" (subs (hexdigest (.getBytes (pr-str openapi))) 0 32))]
    (crux/submit-tx
     crux-node
     [[:crux.tx/put
       {:crux.db/id uri
        ::spin/methods #{:get :head :put :options}
        ::spin/representations
        [{::spin/content-type "text/plain"
          ::spin/etag etag
          ::spin/last-modified last-modified
          ::spin/bytes  (.getBytes (str (get-in openapi ["info" "title"]) "\r\n"))}

         {::spin/content-type "text/html;charset=utf-8"
          ::spin/etag etag
          ::spin/last-modified last-modified
          ::spin/bytes (.getBytes (str "<h1>" (get-in openapi ["info" "title"]) "</h1>\r\n"))}]

        :openapi openapi}]

      [:crux.tx/put
       {:crux.db/id new-resource-uri
        ::spin/methods #{:get :head :options}
        ::spin/representations
        [(assoc new-representation
                ::spin/etag etag ::spin/last-modified last-modified)]}]])

    {:status 201
     :headers {"location" (str new-resource-uri)}
     ;; Add :body to describe the new resource
     }))


(defmethod ig/init-key ::module [_ _]
  (println "Adding OpenAPI module"))
