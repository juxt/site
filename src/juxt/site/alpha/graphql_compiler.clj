(ns juxt.site.alpha.graphql-compiler
  (:require
   [juxt.grab.alpha.document :as-alias d]
   [xtdb.api :as xt]
   [juxt.grab.alpha.graphql :as-alias g]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [juxt.grab.alpha.parser :as parser]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.authorization :as authz]
   [juxt.site.alpha :as-alias site]
   [clojure.walk :refer [keywordize-keys]]))

(declare compile-fragment-spread)
(declare compile-subquery)

(def query-template  '{:where [[action ::site/type "https://meta.juxt.site/pass/action"]
                               [(contains? actions action)]
                               [permission ::site/type "https://meta.juxt.site/pass/permission"]
                               [permission ::pass/action action]
                               (allowed? permission subject action resource)
                               [permission ::pass/purpose purpose]]
                       :in [subject actions purpose resources-in-scope]
                       :joins []})

(defn name-of-is [a b]
  (= (::g/name a) b))


(defn root-action-directive [{::d/keys [schema] :as doc} selection]
  ;; Find the action for the corresponding name in the query
  (let [selection-query-field
        (first
         (filterv #(name-of-is % (::g/name selection))
                  (-> schema
                      ::schema/types-by-name
                      (get "Query")
                      ::g/field-definitions)))
        site-directive
        (first (filterv #(name-of-is % "site")
                        (::g/directives selection-query-field)))]
    (get (::g/arguments site-directive) "action")))

(defn subquery-action-directive [{::d/keys [schema] :as doc} selection]
  ;; Find the directive from the scoped action
  (let [schema-scoped-field
        (get (::schema/types-by-name schema) (::d/scoped-type-name selection))
        schema-field
        (get (::schema/fields-by-name schema-scoped-field) (::g/name selection))
        site-directive
        (first (filterv #(name-of-is % "site")
                        (::g/directives schema-field)))]
    (get (::g/arguments site-directive) "action")))

(defn compile-lone-field [selection doc acc]
  ;; Add the name to the pull
  (update acc
          :pull
          #(conj %
                 (if (::g/arguments selection)
                   ;; I may be confusing idents with params
                   ;; How to translate fields with arguments to EQL? TODO
                   [(keyword (::g/name selection)) (first (vals (::g/arguments selection)))]
                   (keyword (::g/name selection))))))

(defn compile-selection-set [selection-set {::d/keys [operations fragments] :as doc} acc]
  (reduce (fn [acc selection]
            (cond
              (::g/selection-set selection)
              ;; If it's a subquery, add it to the join and recursively compile
              (update acc :joins
                      #(conj % (compile-subquery selection doc false)))
              (= :fragment-spread (::g/selection-type selection))
              ;; If it's a fragment spread, dispatch to fragment spread handling
              (compile-fragment-spread selection doc acc)
              :else
              (compile-lone-field selection doc acc)
              ))
          acc selection-set))


(defn compile-fragment-spread [selection {::d/keys [fragments] :as doc} acc]
  (let [fragment (first (filter (fn [fragment]
                                  (name-of-is fragment (::g/name selection)))
                                fragments))]
    ;; Splice in fragments as subqueries
    (compile-selection-set (::g/selection-set fragment) doc acc)))


(defn compile-subquery [selection doc root?]
  (let [template (conj query-template {:pull [:xt/id]})]
    (compile-selection-set (::g/selection-set selection) doc (conj template {:action (if root?
                                                                                       ;; root? determines if it is a subquery or root query
                                                                                       (root-action-directive doc selection)
                                                                                       (subquery-action-directive doc selection))}))))


(defn compile-root [{::d/keys [operations] :as doc}]
  (def doc doc)
  (case (::g/operation-type (first operations))
    :query
    (compile-subquery (-> operations first ::g/selection-set first) doc true) ;actions
    (throw (Exception. "TODO"))))


(defn build-template-as-datalog [template-query schema db]
  (def template-query template-query)
  (let [rules
        ;; Takes the rules from the template and splice them into the datalog-query
        (authz/actions->rules db #{(:action template-query)})
        ;; If there are no rules throw an error
        _ (when-not (seq rules)
            (throw (ex-info "No rules found for actions" {:actions #{(:action template-query)}})))
        new-q
        {:query
         ;; Add pull resource 
         {:find [(list 'pull 'resource (:pull template-query)) 'action 'purpose 'permission]
          :keys '[resource action purpose permission]
          :where (:where template-query)
          :in (:in template-query)
          :rules rules}
         :action
         (:action template-query)
         :joins
         ;; Recursively compile subquery selections
         (mapv #(build-template-as-datalog % schema db) (:joins template-query))}]
    new-q))

(defn compile-graphql-as-xt [query-string schema db]
  (def schema schema)
  (build-template-as-datalog (-> query-string
                                 parser/parse
                                 (document/compile-document schema)
                                 compile-root)
                             schema db))


