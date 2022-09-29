;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.graphql-eql-compiler
  (:require
   [juxt.grab.alpha.document :as document]
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]))

(defn- graphql->eql-ast*
  [schema field]
  (let [{::document/keys [scoped-type-name]} field
        gtype (some-> schema :juxt.grab.alpha.schema/types-by-name (get scoped-type-name))
        sel-name (:juxt.grab.alpha.graphql/name field)
        k (case sel-name
            "id" :xt/id
            "_type" ::site/type
            (keyword sel-name))
        field-def (some-> gtype :juxt.grab.alpha.graphql/field-definitions (->> (some (fn [fdef] (when (= (:juxt.grab.alpha.graphql/name fdef) sel-name) fdef)))))
        site-dir (some-> field-def :juxt.grab.alpha.graphql/directives (->> (some (fn [dir] (when (= (:juxt.grab.alpha.graphql/name dir) "site") dir)))))
        args (reduce-kv (fn [acc k v]
                          (assoc acc (keyword k) v))
                        {} (:juxt.grab.alpha.graphql/arguments field))
        action (some-> site-dir :juxt.grab.alpha.graphql/arguments (get "action"))]

    (cond
      (:juxt.grab.alpha.graphql/selection-set field)
      {:type :join
       :dispatch-key k
       :key k
       :params (cond-> args
                 action (assoc ::pass/action action))
       :children (mapv #(graphql->eql-ast* schema %)
                       (:juxt.grab.alpha.graphql/selection-set field))}

      :else
      {:type :prop
       :dispatch-key k
       :key k})))

(defn graphql->eql-ast
  [schema op]
  {:type :root
   :children (mapv
              (fn [field] (graphql->eql-ast* schema field))
              (:juxt.grab.alpha.graphql/selection-set op))})
