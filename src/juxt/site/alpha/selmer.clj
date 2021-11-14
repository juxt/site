;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.selmer
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as x]
   [selmer.parser :as selmer]
   selmer.filters
   selmer.tags)
  (:import (java.net URL)))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(selmer/cache-off!)

(selmer/add-tag!
   :sitedebug
   (fn [args context-map tag-config]
     (selmer.tags/prettify-edn context-map)))

(def ^:dynamic *db* nil)

(selmer.filters/add-filter!
 :deref
 (fn [x] (or (x/entity *db* x) x)))

(selmer.filters/add-filter!
 :render-segment
 (fn [x] (if (vector? x)
           (case (first x)
             "text" (second x)
             "em" [:safe (str "<em>" (second x) "</em>")])
           (str x))))

(selmer.filters/add-filter!
 :days
 (fn [x] (when x (.toDays (java.time.Duration/parse "PT600H")))))

(selmer.filters/add-filter!
 :decode
 (fn [x] (case x
           "GB-ENG" "England & Wales"
           "DE" "Germany"
           x)))

(defn xt-template-loader [db]
  (proxy [java.net.URLStreamHandler] []
    (openConnection [url]
      (log/tracef "Open connection: url=%s" url)
      (proxy [java.net.URLConnection] [url]
        (getInputStream []
          (log/tracef "Loading template: url=%s" url)
          (let [res (x/entity db (str url))]
            (java.io.ByteArrayInputStream.
             (cond
               (::http/content res) (.getBytes (::http/content res) (or (::http/charset res) "UTF-8"))
               (::http/body res) (::http/body res)
               :else (.getBytes "(template not found)")))))))))

;; This is now deprecated but remains to support pre-existing use-cases
;; Template model production should be moved out of this

(defn render-template
  [{::site/keys [db selected-representation] :as req} template template-model]
  (let [{::site/keys []} selected-representation
        ush (xt-template-loader db)
        custom-resource-path (:selmer.util/custom-resource-path selected-representation)]

    (try
      (log/tracef "Render template: %s" (:xt/id template))
      (let [body
            (binding [*db* db]
              (selmer/render-file
               (java.net.URL. nil (:xt/id template) ush)
               template-model
               (cond-> {:url-stream-handler ush}
                 custom-resource-path
                 (assoc :custom-resource-path custom-resource-path))))]
        (assoc req
               :ring.response/body body
               ::site/template-model template-model))

      (catch Exception e
        (throw
         (ex-info
          (str "Failed to render template: " template)
          {:template template
           :exception-type (type e)} e))))))
