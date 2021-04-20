;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.selmer
  (:require
   [selmer.parser :as selmer]
   [juxt.site.alpha.templating :as templating]
   [crux.api :as x]
   [clojure.tools.logging :as log]
   [juxt.site.alpha.util :as util])
  (:import (java.net URL)))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(selmer/cache-off!)

(defmethod templating/render-template
  :selmer
  [{::site/keys [db resource selected-representation] :as req} template]
  (let [{::site/keys [] id :crux.db/id} selected-representation
        _ (assert id "Resource must have an id to be used as a template")
        ush (proxy [java.net.URLStreamHandler] []
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
                         :else (.getBytes "(template not found)"))))))))

        template-model-> (::site/template-model selected-representation)
        _ (assert template-model-> "Resource must have a :juxt.site.alpha/template-model entry")
        template-model (x/entity db template-model->)
        _ (assert template-model (format "Template model of '%s' must exist" template-model->))

        ;; This will be checked by JSON Schema validation on entry, but doesn't
        ;; hurt to assert it here too.
        _ (when-not (= ( ::site/type template-model) "TemplateModel")
            (throw
             (ex-info
              "Template model must be of type 'TemplateModel'"
              {:template-model-id template-model->})))

        temp-id-map
        (->>
         {'subject (::pass/subject req)
          'resource resource}
         (reduce-kv
          ;; Preserve any existing crux.db/id - e.g. the resource will have one
          (fn [acc k v]
            (assoc acc k (-> v
                             util/->freezeable
                             (assoc :crux.db/id (java.util.UUID/randomUUID)))))
          {}))

        txes (vec (for [[_ v] temp-id-map] [:crux.tx/put v]))

        spec-db (x/with-tx db txes)

        query (::site/query template-model)

        model (first (apply x/q spec-db
                            (assoc query :in (vec (keys temp-id-map)))
                            (map :crux.db/id (vals temp-id-map))))

        custom-resource-path (:selmer.util/custom-resource-path template)]

    (try
      (log/tracef "Render template: %s" (:crux.db/id template))
      (selmer/render-file
       (java.net.URL. nil (:crux.db/id template) ush)
       model
       (cond-> {:url-stream-handler ush}
         custom-resource-path
         (assoc :custom-resource-path custom-resource-path)))

      (catch Exception e
        (throw (ex-info (str "Failed to render template: " template) {:template template
                                                                      :exception-type (type e)} e))))))
