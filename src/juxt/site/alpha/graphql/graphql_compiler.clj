(ns juxt.site.alpha.graphql.graphql-compiler
  (:require [juxt.grab.alpha.parser :as parser]
            [juxt.grab.alpha.document :as document]
            [juxt.grab.alpha.graphql :as-alias graphql]
            [juxt.grab.alpha.schema :as schema]))

(defn selection->action-id
  [{::graphql/keys [name] ::document/keys [scoped-type-name] :as _selection} schema]
  (get-in schema [::schema/types-by-name
                  scoped-type-name
                  ::schema/fields-by-name
                  name
                  ::schema/directives-by-name
                  "site"
                  ::graphql/arguments
                  "action"]))

(declare build-query-for-selection-set)

(defn build-where-clause
  [compiled-schema action-id action-rules-map subquery-data incoming-resources?]
  (let [entity-selection-clause (if incoming-resources? ['e :xt/id 'input-id] ['e :xt/id '_])
        has-subquery-data? (seq subquery-data)
        where-clause [entity-selection-clause
                      ['action :juxt.site.alpha/type "https://meta.juxt.site/pass/action"]
                      ['action :xt/id action-id]
                      ['permission :juxt.site.alpha/type "https://meta.juxt.site/pass/permission"]
                      ['permission :juxt.pass.alpha/action action-id]
                      '[permission :juxt.pass.alpha/purpose purpose]
                      '(allowed? permission subject action e)
                      '(include? action e)]]
    (if has-subquery-data?
      (-> where-clause
          ;; Add the clause to pull the key into the query
          (into (map #(vector 'e (keyword (:juxt.grab.alpha.graphql/name %)) (symbol (:juxt.grab.alpha.graphql/name %))) subquery-data))
          ;; Add a clause for the subquery
          (into (map #(vector (list 'q
                                    (build-query-for-selection-set % compiled-schema action-rules-map true)
                                    'subject 'purpose (symbol (:juxt.grab.alpha.graphql/name %)))
                              (symbol (str "inner-"(:juxt.grab.alpha.graphql/name %))))

                     subquery-data)))
      where-clause)))

(defn build-find-clause
  [pull-fields subquery-data]
  (let [has-subquery-data? (seq subquery-data)
        find-clause (if (seq pull-fields)
                      ['e (list 'pull 'e pull-fields)]
                      ['e {}])]
    (if has-subquery-data?
      (->> subquery-data
           (reduce (fn [acc n]
                     (assoc acc
                            (keyword (:juxt.grab.alpha.graphql/name n))
                            (symbol (str "inner-" (:juxt.grab.alpha.graphql/name n)))))
                   {})
           (conj find-clause))
      find-clause)))

(defn build-query-xtdb
  [compiled-schema action-id fields-to-pull action-rules-map subquery-data incoming-resources?]
  (let [pull-fields (vec fields-to-pull)
        where-clause (build-where-clause compiled-schema action-id action-rules-map subquery-data incoming-resources?)
        find-clause (build-find-clause pull-fields subquery-data)
        in-clause (if incoming-resources? '[subject purpose input-id] '[subject purpose])]
    {:find find-clause
     :where where-clause
     :rules action-rules-map
     :in in-clause}))


(defn name-scoped-name-pair->actions
  [{::graphql/keys [name] ::document/keys [scoped-type-name]} schema]
  (get-in schema [::schema/types-by-name
                  scoped-type-name
                  ::schema/fields-by-name
                  name
                  ::schema/directives-by-name
                  "site"
                  ::graphql/arguments
                  "action"]))


(defn build-query-for-selection-set
  [selection-set compiled-schema action-rules incoming-resources?]
  (let [action (name-scoped-name-pair->actions selection-set compiled-schema)
        sel-set (:juxt.grab.alpha.graphql/selection-set selection-set)
        grouped-by-inners (group-by (comp some? :juxt.grab.alpha.graphql/selection-set) sel-set)]
    (build-query-xtdb
     compiled-schema
     action
     (map (comp keyword :juxt.grab.alpha.graphql/name) (get grouped-by-inners false))
     action-rules
     (get grouped-by-inners true)
     incoming-resources?)))

(defn selection-set->name-scoped-name-pair
  [schema selection-set]
  (let [type-entries (filter :juxt.grab.alpha.graphql/selection-set selection-set)
        current-level-entries (map #(select-keys % [:juxt.grab.alpha.graphql/name
                                                    :juxt.grab.alpha.document/scoped-type-name])
                                   type-entries)
        inner-type-entries (map :juxt.grab.alpha.graphql/selection-set type-entries)]
    (if (seq inner-type-entries)
      (let [inner-results (flatten (map #(selection-set->name-scoped-name-pair
                                          schema %)
                                        inner-type-entries))]
        (clojure.set/union inner-results current-level-entries))
      current-level-entries)))


(defn query-doc->actions
  [query-document schema]
  (let [root-selection-set (-> query-document ::document/operations first :juxt.grab.alpha.graphql/selection-set)]
    (set (map #(name-scoped-name-pair->actions % schema) (selection-set->name-scoped-name-pair schema root-selection-set)))))

(defn compile-schema
  [schema-string]
  (-> schema-string
      parser/parse
      schema/compile-schema))

(defn query->query-doc
  [query-string compiled-schema]
  (-> query-string
      parser/parse
      (document/compile-document* compiled-schema)))
