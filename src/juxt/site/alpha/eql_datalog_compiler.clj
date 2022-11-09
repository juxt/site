;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.eql-datalog-compiler
  (:require
   [clojure.walk :refer [postwalk]]
   [juxt.pass.alpha.actions :as actions]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [xtdb.api :as xt]))

(defn- compile-ast*
  [db ctx ast]
  (assert (map? ctx))
  (assert (number? (:depth ctx)))
  (let [depth (:depth ctx)
        action-id (-> ast :params ::pass/action)
        _ (assert action-id "Action must be specified on metadata")
        action (when action-id (xt/entity db action-id))
        _ (when action-id (assert action (format "Action not found: %s" action-id)))
        rules (when action (actions/actions->rules db #{action-id}))
        _ (when action (assert (seq rules) (format "No rules found for action %s" action-id)))

        parent-action (::pass/action ctx)

        additional-where-clauses
        (concat
         ;; Context
         (when-let [parent-action-id (:xt/id parent-action)]
           (get-in action [::pass/action-contexts parent-action-id ::pass/additional-where-clauses]))
         ;; Parameters
         (mapcat
          (fn [[k v]]
            (when-let [clauses
                       (get-in action [::pass/params k ::pass/additional-where-clauses])]
              (postwalk
               (fn [x] (if (= x '$) v x))
               clauses)))
          (:params ast)))]

    (reduce
     (fn [acc node]
       (case (:type node)
         :prop
         (update-in acc [:find 0] #(list 'pull 'e (conj (last %) (:key node))))
         :join
         (let [{:keys [dispatch-key]} node]
           (-> acc
               (update-in [:find 1] (fnil assoc {}) dispatch-key (symbol (name dispatch-key)))
               (assoc :keys '[root joins])
               (update :where conj [`(~'q ; sub-query
                                      ~(compile-ast*
                                        db
                                        (-> ctx
                                            (assoc ::pass/action action)
                                            (update :depth inc))
                                        node)
                                      ~'e ; e becomes the parent
                                      ~'subject
                                      ~'purpose)
                                    (symbol (name dispatch-key))])))
         :else acc))

     `{:find [(~'pull ~'e [])]
       :keys [~'root]
       :where
       ~(cond-> `[[~'action :xt/id ~action-id]
                  ~'[permission ::site/type "https://meta.juxt.site/pass/permission"]
                  ~'[permission ::pass/action action]
                  ~'[permission ::pass/purpose purpose]
                  ;; We must rename 'allowed?' here because we
                  ;; cannot allow rules from parent queries to
                  ;; affect rules from sub-queries. In other
                  ;; words, sub-queries must be completely
                  ;; isolated.
                  ~(list (symbol (str "depth" depth) "allowed?") 'subject 'e 'permission)]
          additional-where-clauses (-> (concat additional-where-clauses) vec))
       :rules ~(mapv (fn [rule]
                       (update rule 0 #(apply list (cons (symbol (str "depth" depth) "allowed?") (rest %))))
                       ) rules)
       :in ~(if (pos? (:depth ctx)) '[parent subject purpose] '[subject purpose])}

     (:children ast))))

(defn compile-ast
  "This function compiles an annotated EQL query to an XTDB/Core1 query"
  [db ast]
  (map
   (fn [child]
     (compile-ast* db {:depth 0} child))
   (:children ast)))

(defn prune-result [result]
  (postwalk
   (fn [x] (if (:root x) (merge (:root x) (:joins x)) x))
   result))
