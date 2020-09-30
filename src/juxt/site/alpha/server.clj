;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.server
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :refer [run-jetty]]
   [juxt.site.alpha.handler :refer [make-handler wrap-exception-handler]]))

(defmethod ig/init-key ::server [_ {:keys [crux port dynamic?]}]
  (run-jetty
   (if dynamic?
     (fn [req]
       ((->
         (make-handler crux)
         wrap-exception-handler) req))
     (->
         (make-handler crux)
         wrap-exception-handler))
   {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ s]
  (when s
    (.stop s)))
