;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.graphql-compiler
  (:require
   [juxt.grab.alpha.parser :as parser]
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

(defn get-alias-or-name-from-selection-set
  [selection-set]
  (or (get selection-set :juxt.grab.alpha.graphql/alias)
      (get selection-set :juxt.grab.alpha.graphql/name)))

(defn build-where-clause
  [compiled-schema action-ids action-rules-map subquery-data incoming-resources? arguments]
  (let [entity-selection-clause (if incoming-resources? ['e :xt/id 'input-id] ['e :xt/id '_])
        actions-ids-as-single-or-set (if (vector? action-ids)
                                       (set action-ids)
                                       action-ids)
        where-clause [entity-selection-clause
                      ['action :juxt.site/type "https://meta.juxt.site/site/action"]
                      ['action :xt/id actions-ids-as-single-or-set]
                      ['permission :juxt.site/type "https://meta.juxt.site/site/permission"]
                      ['permission :juxt.site/action actions-ids-as-single-or-set]
                      '[permission :juxt.site/purpose purpose]
                      '(allowed? permission subject action e)
                      '(include? action e)]]
    (cond-> where-clause
      (seq subquery-data) (-> ;; Add the clause to pull the key into the query
                           (into (map #(vector
                                        'e
                                        (keyword (:juxt.grab.alpha.graphql/name %))
                                        (symbol (get-alias-or-name-from-selection-set %)))
                                      subquery-data))
                           ;; Add a clause for the subquery
                           (into (map #(vector (list 'q
                                                     (build-query-for-selection-set % compiled-schema action-rules-map true)
                                                     'subject 'purpose (symbol (get-alias-or-name-from-selection-set %)))
                                               (symbol (str "inner-"(get-alias-or-name-from-selection-set %))))

                                      subquery-data)))
      (seq arguments) (conj (list 'arguments-match? 'e 'action arguments)))))

(defn build-find-clause
  [pull-fields subquery-data]
  (let [find-clause (if (seq pull-fields)
                      ['e (list 'pull 'e pull-fields)]
                      ['e {}])]
    (cond-> find-clause
      (seq subquery-data) (conj
                           (reduce
                            (fn [acc n]
                              (assoc acc
                                     (keyword (get-alias-or-name-from-selection-set n))
                                     (symbol (str "inner-" (get-alias-or-name-from-selection-set n)))))
                            {}
                            subquery-data)))))

(defn build-in-clause
  [incoming-resources?]
  (let [in-clause '[subject purpose]]
    (cond-> in-clause
      incoming-resources? (conj 'input-id))))

(defn build-query-xtdb
  [compiled-schema action-ids fields-to-pull action-rules-map subquery-data incoming-resources? arguments]
  (let [pull-fields (vec fields-to-pull)
        where-clause (build-where-clause compiled-schema action-ids action-rules-map subquery-data incoming-resources? arguments)
        find-clause (build-find-clause pull-fields subquery-data)
        in-clause (build-in-clause incoming-resources?)]
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
  (let [actions (name-scoped-name-pair->actions selection-set compiled-schema)
        sel-set (:juxt.grab.alpha.graphql/selection-set selection-set)
        grouped-by-inners (group-by (comp some? :juxt.grab.alpha.graphql/selection-set) sel-set)]
    (build-query-xtdb
     compiled-schema
     actions
     (map (comp keyword :juxt.grab.alpha.graphql/name) (get grouped-by-inners false))
     action-rules
     (get grouped-by-inners true)
     incoming-resources?
     (update-keys
      (get selection-set :juxt.grab.alpha.graphql/arguments {})
      keyword))))

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
  (let [root-selection-set
        (->
         query-document
         ::document/operations
         first
         :juxt.grab.alpha.graphql/selection-set)
        name-scoped-name-pairs (selection-set->name-scoped-name-pair schema root-selection-set)]
    (reduce (fn [acc n] (let [actions (name-scoped-name-pair->actions n schema)]
                          (cond
                            (vector? actions) (into acc actions)
                            (some? actions) (conj acc actions)
                            :default (throw
                                      (ex-info
                                       "Failed to find linked actions for field. Ensure @site directive is available in the schema for this field."
                                       {:target-pair n :schema schema})))
                          (if (vector? actions)
                            (into acc actions)
                            (conj acc actions))))
            #{}
            name-scoped-name-pairs)))

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
