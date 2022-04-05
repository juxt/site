;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating)

(alias 'site (create-ns 'juxt.site.alpha))

(defmulti render-template
  (fn [_ template] (::site/template-engine template)) :default :selmer)

(defn process-template-model [template-model {::site/keys [db] :as req}]
  ;; A template model can be a stored query.
  (let [f (cond
            ;; If a symbol, it is expected to be a resolvable internal function
            ;; (to support basic templates built on the request and internal
            ;; Site data).
            (symbol? template-model)
            (try
              (or
                (requiring-resolve template-model)
                (throw
                  (ex-info
                    (format "Requiring resolve of %s returned nil" template-model)
                    {:template-model template-model
                     ::site/request-context (assoc req :ring.response/status 500)})))
              (catch Exception e
                (throw
                  (ex-info
                    (format "template-model fn '%s' not resolveable" template-model)
                    {:template-model template-model
                     ::site/request-context (assoc req :ring.response/status 500)}
                    e))))

            (map? template-model)
            (fn [_] template-model)

            :else
            (throw
              (ex-info
                "Unsupported form of template-model"
                {:type (type template-model)
                 ::site/request-context (assoc req :ring.response/status 500)})))]
    (f req)))