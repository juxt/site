;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.selmer
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [selmer.parser :as selmer]
   selmer.filters
   selmer.tags))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(selmer/cache-off!)

(selmer.filters/add-filter!
 :pretty
 (fn [x]
   ;; prettify-edn already does escaping on content so we're OK to declare this
   ;; output as safe
   [:safe (selmer.tags/prettify-edn x)]))

(def ^:dynamic *db* nil)

(selmer.filters/add-filter!
 :deref
 (fn [x] (or (xt/entity *db* x) x)))

(selmer.filters/add-filter!
 :render-segment
 (fn [x] (if (vector? x)
           (case (first x)
             "text" (second x)
             "em" [:safe (str "<em>" (second x) "</em>")])
           (str x))))

(defn render-file [template-id template-model db custom-resource-path template-loader]
  (binding [*db* db]
    (selmer/render-file
     (java.net.URL. nil template-id template-loader)
     template-model
     (cond-> {:url-stream-handler template-loader}
       custom-resource-path
       (assoc :custom-resource-path custom-resource-path)))))

(defn render-template
  [{::site/keys [db selected-representation template-loader] :as req} template template-model]
  (assert template-loader)
  (let [{::site/keys []} selected-representation
        custom-resource-path (:selmer.util/custom-resource-path selected-representation)]

    (try
      (let [body
            (render-file (:xt/id template) template-model db custom-resource-path template-loader)]
        (assoc req
               :ring.response/body body
               ::site/template-model template-model))

      (catch Exception e
        (throw
         (ex-info
          (format "Failed to render template: %s" template)
          {:template template
           :exception-type (type e)
           ::site/request-context req}
          e))))))
