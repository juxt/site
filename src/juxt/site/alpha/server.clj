;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.server
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :refer [run-jetty]]
   [juxt.site.alpha.handler :refer [make-handler]]))

(alias 'site (create-ns 'juxt.site.alpha))

(defmethod ig/init-key ::server [_ {::site/keys [crux-node port base-uri] :as opts}]
  (run-jetty (make-handler opts) {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ s]
  (when s
    (.stop s)))
