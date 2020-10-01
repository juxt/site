(ns juxt.site.server
  (:require
   [jsonista.core :as json]
   juxt.vext.ring-server
   [hiccup.page :refer [html5]]
   [juxt.spin.alpha.handler :as spin.handler]
   [juxt.spin.alpha.resource :as spin.resource]
   [juxt.spin.alpha.server :as spin.server]
   [crux.api :as crux]
   [juxt.reap.alpha.decoders :as reap])
  )

(defn handler [{:keys [crux]}]
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
         :body (html5 [:form {:method "POST" :enctype "multipart/form-data"}
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
       (juxt.vext.ring-server/handle-body
        request
        (fn [buffer]
          ;; TODO: Put this into the database!
          (case ((juxt :juxt.http/type :juxt.http/subtype) (reap/content-type (get-in request [:headers "content-type"])))
            ["application" "json"]
            (prn (json/read-value (.getBytes buffer)))
            ["multipart" "form-data"]
            (println "TODO!"))

          (respond
           (merge
            response
            ;; TODO Why is this not coming through
            {:body "Thanks! Buon Viaggio!\n"}))))))

   (reify
     spin.server/ServerOptions
     (server-header [_] "JUXT Site!!")
     (server-options [_]))))
