(ns juxt.site.server
  (:require
   [jsonista.core :as json]
   juxt.vext.ring-server
   [hiccup.page :refer [html5]]
   [juxt.spin.alpha.handler :as spin.handler]
   [juxt.spin.alpha.resource :as spin.resource]
   [juxt.spin.alpha.server :as spin.server]
   [crux.api :as crux]
   [juxt.reap.alpha.decoders :as reap]
   [integrant.core :as ig]
   [clojure.java.io :as io]
   [juxt.vext.content-store :as cstore]))

(defmethod ig/init-key ::content-store [_ {:keys [vertx dir]}]
  (.mkdirs (io/file dir))
  (cstore/->VertxFileContentStore vertx (io/file dir) (io/file dir)))

(defn handler [{:keys [crux content-store]}]
  (assert crux)
  (spin.handler/handler
   (reify ;; resource interface

     ;; TODO: Implement a POST!!

     spin.resource/ResourceLocator
     (locate-resource [_ uri request]
       (let [db (crux/db crux)]
         (when-let [eid (ffirst (crux/q db {:find ['?e]
                                            :where [['?e :juxt.site/url uri]]}))]
           (crux/entity db eid))))

     spin.resource/GET
     (get-or-head [_ server resource response request respond raise]
       ;; TODO: Reply with James' holidays
       (respond
        {:status 200
         :headers {"content-type" "text/html;charset=utf8"}
         :body
         (html5
          [:form {:method "POST" :enctype "multipart/form-data"}
           (into
            [:field-set]
            (for [[att-k {:crux.schema/keys [_ label]}]
                  (:crux.schema/attributes resource)
                  :let [n (name att-k)]]
              [:div
               (when label [:label {:for n} label])
               [:input {:name n :type "text"}]]))
           [:input {:type "submit" :value "Submit"}]])}))

     spin.resource/POST
     (post [_ server resource response request respond raise]
       (let [vtxreq (:juxt.vext/request request)]
         (case ((juxt :juxt.http/type :juxt.http/subtype) (reap/content-type (get-in request [:headers "content-type"])))
           ["application" "json"]
           (.bodyHandler
            vtxreq
            (reify io.vertx.core.Handler
              (handle [_ buffer]
                (prn (json/read-value (.getBytes buffer)))
                (respond
                 {:status 200
                  :headers {"content-type" "text/plain;charset=utf8"}
                  :body "Thanks! Buon Viaggio!\n"}))))

           ["multipart" "form-data"]
           (do
             (.setExpectMultipart vtxreq true)
             (.endHandler
              vtxreq
              (reify io.vertx.core.Handler
                (handle [_ _]
                  (prn "attriubtes are" (into {} (.formAttributes vtxreq)))
                  (respond
                   {:status 200
                    :headers {"content-type" "text/plain;charset=utf8"}
                    :body "Thanks"}))))))))

     spin.resource/PUT
     (put [_ server resource response request respond raise]
       (let [ct (reap/content-type (get-in request [:headers "content-type"]))]
         (case (:juxt.http/type ct)
           "image" ;; Allow upload of static image content
           (->
            (cstore/post-content content-store (.toFlowable (:juxt.vext/request request)))
            (.subscribe
             (reify io.reactivex.functions.Consumer ;; happy path!
               (accept [_ v]
                 (respond
                  (merge
                   response
                   ;; TODO Why is this not coming through?
                   {:body "Thanks, that looks like a wonderful image!\n"}))))
             (reify io.reactivex.functions.Consumer ;; sad path!
               (accept [_ t]
                 (raise t)))))))))

   (reify
     spin.server/ServerOptions
     (server-header [_] "JUXT Site!!")
     (server-options [_]))))
