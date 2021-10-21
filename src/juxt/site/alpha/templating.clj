;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.templating
  (:require
   [juxt.site.alpha.selmer :as selmer]
   [clojure.walk :refer [postwalk]]
   [clojure.tools.logging :as log]
   [crux.api :as xt]))

(alias 'site (create-ns 'juxt.site.alpha))

(defn expand-queries [template-model db]
  (postwalk
   (fn [m]
     (cond
       (and (map? m) (contains? m ::site/query))
       (cond->> (xt/q db
                       ;; TODO: Add ACL checks
                       (::site/query m))

         ;; Deprecate? Anything using this?
         (= (::site/results m) 'first) first
         ;; Copied from juxt.apex.alpha.representation_generation
         (::site/extract-first-projection? m) (map first)
         (::site/extract-entry m) (map #(get % (::site/extract-entry m)))
         (::site/singular-result? m) first)
       :else m))
   template-model))

(defn render-template
  "Methods should return a modified request (first arg), typically associated
  a :ring.response/body entry."
  [{::site/keys [db resource] :as req} template]

  (let [template-models
        (map first
             (xt/q db '{:find [(pull tm [*])]
                        :where [[e ::site/template-model tm]]
                        :in [e]} (:crux.db/id resource)))

        processed-template-models
        (for [tm template-models]
          (expand-queries tm db))

        combined-template-model
        (->> processed-template-models
             (map #(dissoc % :crux.db/id))
             (apply merge-with
                    (fn [a b] (concat (if (sequential? a) a [a])
                                      (if (sequential? b) b [b])))))]

    ;; Possibly we can make this an open-for-extension thing in the future. We
    ;; could register template dialects in the database and look them up here.
    (case (::site/template-dialect template)
      "selmer" (selmer/render-template req template combined-template-model)
      nil (throw (ex-info "No template dialect found" {}))
      (throw (ex-info "Unsupported template dialect" {::site/template-dialect (::site/template-dialect template)})))))
