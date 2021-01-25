;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.db
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [crux.api :as crux]
   [crypto.password.bcrypt :as password]
   [integrant.core :as ig]
   [jsonista.core :as json]
   [juxt.site.alpha.util :refer [hexdigest]]
   [juxt.spin.alpha :as spin])
  (:import
   (java.net URI)
   (java.util Date UUID)))

(def mime-types
  {"html" "text/html;charset=utf-8"
   "js" "application/javascript"
   "map" "application/json"
   "css" "text/css"
   "png" "image/png"})

(defn sanitize [m]
  (->> m
       (remove (fn [[k v]] (.endsWith (name k) "!!")))
       (into {})))

(defn new-data-resource [uri resource-state]
  (let [last-modified (Date.)
        ;; As a hash of the state of the resource, the etag is shared across
        ;; representations. The Vary header will ensure that a cache will use
        ;; the content-type as a secondary key.
        etag
        ;; TODO: Use a proper reap encoder to ensure we don't create invalid
        ;; etags
        (format "\"%s\"" (subs (hexdigest (.getBytes (pr-str resource-state))) 0 16))]
    (concat
     [ ;; Resource
      [:crux.tx/put
       (into
        resource-state
        {:crux.db/id uri
         ::spin/methods #{:get :head :options}})]]

     ;; EDN representation and mapping
     (let [rep-id (UUID/randomUUID)]
       [
        (let [bytes (.getBytes (str (with-out-str (pprint (sanitize resource-state)) "\r\n")) "utf-8")]
          [:crux.tx/put
           {:crux.db/id rep-id
            ::spin/content-type "application/edn"
            ::spin/etag etag
            ::spin/last-modified last-modified
            ::spin/content-length (count bytes)
            ::spin/bytes bytes}])

        [:crux.tx/put
         {:crux.db/id (UUID/randomUUID)
          :juxt.spin.alpha/resource uri
          :juxt.spin.alpha/representation rep-id}]])

     ;; JSON representation and mapping
     (let [rep-id  (UUID/randomUUID)]
       [
        (let [bytes (.getBytes (str (json/write-value-as-string (sanitize resource-state)) "\r\n") "utf-8")]
          [:crux.tx/put
           {:crux.db/id rep-id
            ::spin/content-type "application/json"
            ::spin/etag etag
            ::spin/last-modified last-modified
            ::spin/content-length (count bytes)
            ::spin/bytes bytes}])

        ;; Mapping between resource and representation
        [:crux.tx/put
         {:crux.db/id (UUID/randomUUID)
          :juxt.spin.alpha/resource uri
          :juxt.spin.alpha/representation rep-id}]]))))

(defn user-entity [username password]
  (new-data-resource
   (URI. (format "/_crux/users/%s" username))
   {:crux.site/username "crux/admin"
    :crux.site/password-hash!! (password/encrypt password)}))

;; TODO: This can be PUT instead of being built-in.
(defn swagger-ui []
  (let [last-modified (Date.)
        dir (io/file (System/getProperty "user.home") "Downloads/swagger-ui-3.40.0/dist")]
    (mapcat
     seq
     (for [fl (.listFiles dir)
           :let [[_ suffix] (re-matches #".*\.(.*)" (.getName fl))
                 uri (URI. (format "/_crux/swagger-ui/%s" (.getName fl)))
                 rep-id (UUID/randomUUID)]]
       [;; Resource
        [:crux.tx/put
         {:crux.db/id uri
          ::spin/methods #{:get :head :options}}]

        ;; Representation
        [:crux.tx/put
         {:crux.db/id rep-id
          ::spin/content-type (get mime-types suffix "application/octet-stream")
          ::spin/last-modified last-modified
          ::spin/content-length (.length fl)
          ::spin/content-location uri
          ::spin/bytes fl}]

        ;; Mapping between resource and representation
        [:crux.tx/put
         {:crux.db/id (UUID/randomUUID)
          :juxt.spin.alpha/resource uri
          :juxt.spin.alpha/representation rep-id}]]))))

(defn api-console []
  [[:crux.tx/put
    {:crux.db/id (URI. "/_crux/api-console")
     ::spin/methods #{:get :head :options}
     ::spin/representations
     [{::spin/content-type "text/html;charset=utf-8"
       ::spin/bytes-generator :api-console-generator}]}]])

(defn seed-database! [crux-node]
  (crux/submit-tx
   crux-node
   (concat
    (user-entity "crux/admin" "FunkyForest") ; The crux/admin user
    (swagger-ui)
    (api-console)))

  (crux/sync crux-node))

;;(password/encrypt "password")

(defmethod ig/init-key ::crux [_ _]
  (println "Starting Crux node")
  (let [node (crux/start-node {})]
    (seed-database! node)
    node))

(defmethod ig/halt-key! ::crux [_ node]
  (.close node)
  (println "Closed Crux node"))
