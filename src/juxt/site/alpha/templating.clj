;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating
  (:require
   [juxt.site.alpha.selmer :as selmer]
   [clojure.tools.logging :as log]))

(alias 'site (create-ns 'juxt.site.alpha))

(defn render-template
  "Methods should return a modified request (first arg), typically associated
  a :ring.response/body entry."
  [{::site/keys [selected-representation] :as req} template]
  (log/tracef "Render template! %s" (sort (keys template)))
  (cond
    (::site/template-model-provider selected-representation)
    (let [template-model-provider (requiring-resolve (::site/template-model-provider selected-representation))]
      (when-not template-model-provider
        (throw
         (ex-info
          "Failed to resolve template-model-provider"
          (select-keys selected-representation [::site/template-model-provider]))))

      (let [template-model (template-model-provider req)]
        (case (::site/template-dialect template)
          "selmer" (selmer/render-template req template template-model)
          nil (throw (ex-info "No template dialect found" {}))
          (throw (ex-info "Unsupported template dialect" {::site/template-dialect (::site/template-dialect template)})))))

    ;; Deprecated. Included to retain compatibility with previous versions of
    ;; Site.
    :else ;;(::site/template-engine selected-representation)
    (do
      (log/warn "Resource entry of :juxt.site.alpha/template-engine is deprecated and may be removed in a future release")
      (selmer/old-render-template req template))

    ;; We much prefer the requiring-resolve pattern because it doesn't require a
    ;; defmethod's namespace to have been previously required.





))
