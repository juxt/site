;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.debug
  (:require
   [jsonista.core :as json]
   [xtdb.api :as x]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'rfc7230 (create-ns 'juxt.reap.alpha.rfc7230))

(def whitelisted-nses
  #{"juxt.site.alpha"
    "juxt.pass.alpha"
    "ring.request"
    "ring.response"})

;; Representations of a 'request', useful for debugging
(defn json-representation-of-request [req request-to-show]
  {::http/content-type "application/json"
   ::http/etag "\"v1\""
   ::http/last-modified (:juxt.site.alpha/start-date request-to-show)
   ::site/body-fn
   (fn [req]
     (-> (sorted-map)
         (into request-to-show)
         (->> (filter (fn [[k v]]
                        (contains? whitelisted-nses (namespace k)))))
         json/write-value-as-string
         (str "\r\n")
         (.getBytes)))
   ;; TODO: use Cache-Control: immutable - see
   ;; https://www.keycdn.com/blog/cache-control-immutable and others
   })

(defn html-representation-of-request [{::site/keys [db base-uri]} request-to-show]
  (let [template (str base-uri "/_site/templates/debug-request.html")]
    (when (x/entity db template)
      ;; Do we see a template?
      {::http/content-type "text/html;charset=utf-8"
       ;; Hmm. This should really be the more recent of the request-object AND template rendering it
       ;;::http/last-modified (:juxt.site.alpha/start-date request-to-show)
       ::site/type "TemplatedRepresentation"
       ::site/template template
       ::site/template-model request-to-show

       #_#_::site/body-fn
       (fn [req]
         "<h1>Coming Soon...</h1>")
       ;; TODO: use Cache-Control: immutable - see
       ;; https://www.keycdn.com/blog/cache-control-immutable and others
       })))
