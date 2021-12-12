;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.response
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [juxt.site.alpha.templating :as templating]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn add-payload [{::site/keys [selected-representation db] :as req}]
  (let [{::http/keys [body content] ::site/keys [body-fn]} selected-representation
        template (some->> selected-representation ::site/template (xt/entity db))]
    (cond
      body-fn
      (if-let [f (cond-> body-fn (symbol? body-fn) requiring-resolve)]
        (do
          (log/debugf "Calling body-fn: %s %s" body-fn (type body-fn))
          (assoc req :ring.response/body (f req)))
        (throw
         (ex-info
          (format "body-fn cannot be resolved: %s" body-fn)
          {:body-fn body-fn})))

      template (templating/render-template req template)

      content (assoc req :ring.response/body content)
      body (assoc req :ring.response/body body)
      :else req)))
