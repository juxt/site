;; Copyright © 2020, JUXT LTD.

;; TODO: Promote this namespace to juxt/grab, it's a GraphQL engine,
;; without batteries.

(ns juxt.grab.alpha.graphql
  (:require
   [juxt.reap.alpha.api :as reap]
   [juxt.reap.alpha.graphql :as reap-graphql]
   [flatland.ordered.map :refer [ordered-map]]))

(defn parse-graphql
  "Return a document"
  [s]
  (reap/decode reap-graphql/Document s))

(defn operations
    "Return just the operations of a document, expanding any shorthand
    definition."
    [graphql-doc]
    (->> graphql-doc
         (map
          (fn [definition]
            (if (= (keys definition) [:selection-set])
              (into {:operation-type "query"} definition)
              definition)))
         (filter :operation-type)))

(defn validate-graphql-document
  "'If a Document contains only one operation, that operation may be unnamed or
  represented in the shorthand form, which omits both the query keyword and
  operation name. Otherwise, if a GraphQL Document contains multiple operations,
  each operation must be named.'"
  [doc]
  (when-not
      (or
       (= (count (filter #(= (:operation-type %) "query") (operations doc))) 1)
       (every? :name (operations doc)))
      (throw (ex-info "Invalid GraphQL document" {:doc doc})))
  doc)

(defn
  ^{:graphql/name "GetOperation"}
  get-operation
  ([doc]
   (when-let [op
              (let [ops (filter #(= (:operation-type %) "query") (operations doc))]
                (if (= (count ops) 1)
                  (first ops)
                  (get-operation doc nil)))]
     op))
  ([doc op-name]
   (if (some? op-name)
     (when-let [op (some #(when (= (:name %) op-name) %) doc)]
       op)
     (get-operation doc))))

(defprotocol Schema
  (resolve-type [_ object-type field-name]))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.3.2"
    :crux.graphql.spec-ref/algorithm "CollectFields"}
  collect-fields
  [{:keys [object-type selection-set variable-values visited-fragments document]
    ;; 1. If visitedFragments if not provided, initialize it to the empty
    ;; set.
    :or {visited-fragments #{}}}]

  (assert document)

  (reduce
   (fn [grouped-fields [selection-type selection]]
     (case selection-type
       ;; c. If selection is a Field:
       :field
       (let [response-key
             ;; i. Let responseKey be the response key of selection (the alias
             ;; if defined, otherwise the field name).

             ;; TODO: The response-key will be the alias, if it exists
             (:name selection)]
         (update
          grouped-fields
          ;; ii. Let groupForResponseKey be the list in groupedFields for responseKey;
          response-key

          ;; Append selection to the groupForResponseKey.
          (-> conj
              ;; ii (cont). …if no such list exists, create it as an empty
              ;; list.
              (fnil []))
          selection))

       :fragment-spread
       ;; d. If selection is a FragmentSpread:
       ;; i. Let fragmentSpreadName be the name of selection.
       (let [fragment-spread-name (:fragment-name selection)]
         ;; ii. If fragmentSpreadName is in visitedFragments, continue with
         ;; the next selection in selectionSet.
         (if (contains? visited-fragments fragment-spread-name)
           grouped-fields

           (let [ ;; iii. Add fragmentSpreadName to visitedFragments.
                 visited-fragments (conj visited-fragments fragment-spread-name)
                 ;; iv. Let fragment be the Fragment in the current Document
                 ;; whose name is fragmentSpreadName.
                 fragment (some
                           #(when (= (:fragment-name %) fragment-spread-name)
                              %) document)]
             ;; v. If no such fragment exists, continue with the next
             ;; selection in selectionSet.
             (if-not fragment
               grouped-fields

               ;; vi. Let fragmentType be the type condition on fragment.
               (let [fragment-type (:named-type fragment)]

                 ;; vii. If DoesFragmentTypeApply(objectType, fragmentType) is
                 ;; false, continue with the next selection in selectionSet. (TODO)

                 (let [
                       ;; viii. Let fragmentSelectionSet be the top‐level selection
                       ;; set of fragment.
                       fragment-selection-set (:selection-set fragment)
                       ;; ix. Let fragmentGroupedFieldSet be the result of calling
                       ;; CollectFields(objectType, fragmentSelectionSet,
                       ;; visitedFragments).
                       fragment-group-field-set
                       (collect-fields
                        {:object-type object-type
                         :selection-set fragment-selection-set
                         :variable-values variable-values
                         :visited-fragments visited-fragments
                         :document document})]

                   (reduce
                    (fn [grouped-fields [response-key fragment-group]]
                      (update grouped-fields response-key (fnil concat (list)) fragment-group))
                    grouped-fields
                    fragment-group-field-set)

                   #_(throw
                      (ex-info
                       "TODO: fragment-spread"
                       {:selection selection
                        :fragment-spread-name fragment-spread-name
                        :visited-fragments visited-fragments
                        :fragment fragment
                        :fragment-group-field-set fragment-group-field-set
                        ;;:document document
                        }))

                   )

                 )

               )
             )))

       :inline-fragment
       (throw (ex-info "TODO: inline-fragment" {:selection selection}))))

   ;; 2. Initialize groupedFields to an empty ordered map of lists.
   (ordered-map)
   ;; 3. For each selection in selectionSet:
   selection-set))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.4.1"
    :crux.graphql.spec-ref/algorithm "CoerceArgumentValues"}
  coerce-argument-values
  [{:keys [object-type field variable-values]}]

  (let [
        ;; 1. Let coercedValues be an empty unordered Map.
        coerced-values {}
        ;; 2. Let argumentValues be the argument values provided in field.
        argument-values (:arguments field)
        ;; 3. Let fieldName be the name of field.
        field-name (:name field)
        ;; 4. Let argumentDefinitions be the arguments defined by objectType
        ;; for the field named fieldName.
        argument-definitions (some #(when (= (get % "name") field-name) (get % "args")) (get object-type "fields"))]

    ;; 5. For each argumentDefinition in argumentDefinitions:
    (reduce
     (fn [acc argument-definition]

       (let [ ;; a. Let argumentName be the name of argumentDefinition.
             argument-name (get argument-definition "name")
             ;; b. Let argumentType be the expected type of argumentDefinition.
             argument-type (get argument-definition "type")
             ;; c. Let defaultValue be the default value for argumentDefinition.
             default-value (find argument-definition "defaultValue")
             ;; d. Let hasValue be true if argumentValues provides
             ;; a value for the name argumentName.
             has-value (find argument-values argument-name)
             ;; e. Let argumentValue be the value provided in argumentValues for the name argumentName.
             argument-value (second has-value)
             ;; f. If argumentValue is a Variable: (TODO)
             ;; g. Otherwise, let value be argumentValue.
             value argument-value]

         (cond
           ;; h. If hasValue is not true and defaultValue exists (including null):
           (and (not has-value) default-value)
           ;;   i. Add an entry to coercedValues named argumentName
           ;;   with the value defaultValue.
           (conj acc [argument-name (second default-value)])

           ;; i. Otherwise if argumentType is a Non‐Nullable type,
           ;; and either hasValue is not true or value is null,
           ;; throw a field error.
           (and (= (get argument-type "kind") "NON_NULL")
                (or (not has-value)
                    (nil? (second has-value))))
           (throw (ex-info "Field error, argument type is wrapped as non-null, but no argument value given" {}))

           ;; j. Otherwise if hasValue is true:
           has-value
           (cond
             ;; i. If value is null:
             (nil? (second has-value))
             ;; 1. Add an entry to coercedValues named argumentName with the value null.
             (conj acc [argument-name nil])
             ;; ii. Otherwise, if argumentValue is a Variable: (TODO)

             :else
             ;; TODO: apply coercion rules, for now just set it to the value
             (let [coerced-value value]
               (conj acc [argument-name value])))

           :else acc)))

     coerced-values
     argument-definitions)))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.4.3"
    :crux.graphql.spec-ref/algorithm "ResolveAbstractType"}
  resolve-abstract-type
  [{:keys [field-type result]}]
  (throw (ex-info "TODO: resolve-abstract-type" (meta #'resolve-abstract-type))))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.4.3"
    :crux.graphql.spec-ref/algorithm "MergeSelectionSets"}
  merge-selection-sets
  [{:keys [fields]}]
  (reduce
   (fn [selection-set field]
     (let [field-selection-set (:selection-set field)]
       (cond-> selection-set
         field-selection-set (concat field-selection-set))))
   (list)
   fields))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.4.2"
    :crux.graphql.spec-ref/algorithm "ResolveFieldValue"}
  resolve-field-value
  [{:keys [object-type object-value field-name argument-values field-resolver] :as args}]
  (assert field-resolver)

  (field-resolver
   {:object-type object-type
    :field-name field-name
    :object-value object-value
    :argument-values argument-values}))

(declare execute-selection-set-normally)

(defn ^{:crux.graphql.spec-ref/version "June2018"
        :crux.graphql.spec-ref/section "6.4.3"
        :crux.graphql.spec-ref/algorithm "CompleteValue"}
  complete-value
  [{:keys [field-type fields result variable-values field-resolver schema document]}]

  (assert field-type)
  (assert schema)
  (assert document)

;;  (println "complete-value with type " field-type)
;;  (println "complete-value with result " result)

  (cond
    ;; 1. If the fieldType is a Non‐Null type:
    (= (get field-type "kind") "NON_NULL")
    ;; a. Let innerType be the inner type of fieldType.
    (let [inner-type (get field-type "ofType")
          _ (assert inner-type (format "Field type %s is NON_NULL but doesn't have a non-nil ofType" (pr-str field-type)))
          ;; b. Let completedResult be the result of calling
          ;; CompleteValue(…).
          completed-result
          (try
            (complete-value
             {:field-type inner-type
              :fields fields
              :result result
              :variable-values variable-values
              :field-resolver field-resolver
              :schema schema
              :document document})
            (catch Throwable e
              (throw
               (ex-info
                "Error on complete-value"
                {:field-type field-type
                 :inner-type inner-type}
                e))
              ))]
      ;; c. If completedResult is null, throw a field error.
      (when (nil? completed-result)
        (throw
         (ex-info
          "Field error, NON_NULL type returned nil value for inner type"
          {:inner-type inner-type
           :result result})))
      ;; d. Return completedResult.
      completed-result)

    ;; 2. If result is null (or another internal value similar to null such
    ;; as undefined or NaN), return null.
    (nil? result) nil

    ;; 3. If fieldType is a List type:
    (= (get field-type "kind") "LIST")
    (do
      ;; a. If result is not a collection of values, throw a field error.
      (when-not (sequential? result)
        (throw (ex-info "Resolver must return a collection" {:field-type field-type}))
        )
      ;; b. Let innerType be the inner type of fieldType.
      (let [inner-type (get field-type "ofType")]
        ;; c. Return a list where each list item is the result of calling
        ;; CompleteValue(innerType, fields, resultItem, variableValues),
        ;; where resultItem is each item in result.

        (doall
         (for [result-item result]
           (complete-value
            {:field-type inner-type
             :fields fields
             :result result-item
             :variable-values variable-values
             :field-resolver field-resolver
             :schema schema
             :document document})))))

    ;; 4. If fieldType is a Scalar or Enum type:
    (#{"SCALAR" "ENUM"} (get field-type "kind"))
    ;; a. Return the result of “coercing” result, ensuring it is a legal value of fieldType, otherwise null.
    result

    ;; 5. If fieldType is an Object, Interface, or Union type:
    (#{"OBJECT" "INTERFACE" "UNION"} (get field-type "kind"))
    (let [object-type
          (if (= (get field-type "kind") "OBJECT")
            field-type
            (resolve-abstract-type
             {:field-type field-type
              :result result}))
          sub-selection-set (merge-selection-sets {:fields fields})]
;;      (println "Going again with field-type" field-type)
;;      (println "Going again with result" result)
      (execute-selection-set-normally
       {:selection-set sub-selection-set
        :object-type object-type
        :object-value result
        :variable-values variable-values
        :field-resolver field-resolver
        :schema schema
        :document document}))))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.4"
    :crux.graphql.spec-ref/algorithm "ExecuteField"}
  execute-field
  [{:keys [object-type object-value field-type fields variable-values field-resolver schema document]}]
  (assert schema)
  (assert document)

  ;; 1. Let field be the first entry in fields.
  (let [field (first fields)
        ;; 2. Let fieldName be the field name of field.
        field-name (:name field)
        ;; 3. Let argumentValues be the result of CoerceArgumentValues(…).
        argument-values
        (coerce-argument-values
         {:object-type object-type
          :field field
          :variable-values variable-values})

        ;; 4. Let resolvedValue be ResolveFieldValue(…).
        resolved-value
        (resolve-field-value
         {:object-type object-type
          :object-value object-value
          :field-name field-name
          :argument-values argument-values
          :field-resolver field-resolver})]

    ;; 5. Return the result of CompleteValue(…).
    (complete-value
     {:field-type field-type
      :fields fields
      :result resolved-value
      :variable-values variable-values
      :field-resolver field-resolver
      :schema schema
      :document document})))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.3"
    :crux.graphql.spec-ref/algorithm "ExecuteSelectionSet"}
  execute-selection-set-normally
  "Return a map with :data and :errors."
  [{:keys [selection-set object-type object-value variable-values field-resolver schema document]}]

  (assert schema)
  (assert document)

;;  (println "execute-selection-set-normally")
;;  (pprint {:object-value object-value})

  ;; 1. Let groupedFieldSet be the result of CollectFields
  (let [grouped-field-set
        (collect-fields
         {:object-type object-type
          :selection-set selection-set
          :variable-values variable-values
          :document document})

;;        _ (println "grouped-field-set")
;;        _ (pprint grouped-field-set)

        ;; 2. Initialize resultMap to an empty ordered map.
        result-map (ordered-map)]

    ;; 3. For each groupedFieldSet as responseKey and fields:
    (reduce
     (fn [result-map [response-key fields]]

       ;; a. Let fieldName be the name of the first entry in fields. Note:
       ;; This value is unaffected if an alias is used.
       (let [field-name (:name (first fields))
             ;; b. Let fieldType be the return type defined for the field fieldName of objectType.
             field-type (resolve-type schema object-type field-name)]

         ;; c. If fieldType is defined:
         (if field-type
           ;; i. Let responseValue be ExecuteField(objectType, objectValue,
           ;; fields, fieldType, variableValues).
           (let [response-value
                 (execute-field
                  {:object-type object-type
                   :object-value object-value
                   :field-type field-type
                   :fields fields
                   :variable-values variable-values
                   :field-resolver field-resolver
                   :schema schema
                   :document document})]
             ;; ii. Set responseValue as the value for responseKey in resultMap.
             (conj result-map [response-key response-value]))
           ;; Otherwise return the accumulator
           result-map)))
     result-map
     grouped-field-set)))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.2.1"
    :crux.graphql.spec-ref/algorithm "ExecuteQuery"}
  execute-query
  [{:keys [query schema variable-values initial-value field-resolver document]}]
  (assert schema)
  (assert document)

  ;; 1. Let queryType be the root Query type in schema.
  (let [query-type-name (get schema "queryType")
        query-type (resolve-type
                    schema
                    {:crux.schema/entity
                     {:crux.schema/attributes
                      {:root {:crux.graphql/name "Root"
                              :crux.schema/type :ex.type/graphql-query-root}}}}
                    query-type-name)]

    ;; 2. Assert: queryType is an Object type.
    (when-not (= (get query-type "kind") "OBJECT")
      (throw (ex-info
              "Query type must be an OBJECT"
              (into
               {:query-type query-type
                :query-type-name query-type-name
                :crux.graphql.spec-ref/step 2}
               (meta #'execute-query)))))

    ;; 3. Let selectionSet be the top level Selection Set in query.
    (let [selection-set (:selection-set query)]
      ;; 4. Let data be the result of running ExecuteSelectionSet
      ;; normally (allowing parallelization).
      ;; 5. Let errors be any field errors produced while executing the selection set.
      ;; 6. Return an unordered map containing data and errors.
      (execute-selection-set-normally
       {:selection-set selection-set
        :object-type query-type
        :object-value initial-value
        :variable-values variable-values
        :schema schema
        :field-resolver field-resolver
        :document document}))))

(defn
  ^{:crux.graphql.spec-ref/version "June2018"
    :crux.graphql.spec-ref/section "6.1"
    :crux.graphql.spec-ref/algorithm "ExecuteRequest"}
  execute-request [{:keys [schema document operation-name variable-values initial-value field-resolver]}]

  ;; 1. Let operation be the result of GetOperation(document, operationName).
  (let [operation (get-operation document operation-name)
        ;; 2. Let coercedVariableValues be the result of
        ;; CoerceVariableValues(schema, operation, variableValues). (TODO)
        coerced-variable-values variable-values]

    (case (:operation-type operation)
      ;; 3. If operation is a
      "query" ;; operation:
      ;;   a. Return ExecuteQuery(operation, schema, coercedVariableValues,
      ;;   initialValue).
      (execute-query
       {:query operation
        :schema schema
        :variable-values coerced-variable-values
        :initial-value initial-value
        :field-resolver field-resolver
        :document document})

      ;; 4. Otherwise if operation is a mutation operation:
      ;;   a. Return ExecuteMutation(operation, schema, coercedVariableValues, initialValue).

      ;; TODO

      ;; 5. Otherwise if operation is a subscription operation:
      ;;   a. Return Subscribe(operation, schema, coercedVariableValues, initialValue).

      ;; TODO

      (throw (ex-info "No operation type on operation" {:operation operation})))))
