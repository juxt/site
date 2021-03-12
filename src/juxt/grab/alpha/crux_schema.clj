;; Copyright © 2020, JUXT LTD.

(ns juxt.grab.alpha.crux-schema
  (:require
   [juxt.grab.alpha.graphql :as grab]
   [crux.api :as x]
   [clojure.set :as set]))

(defn to-graphql-type
  ([db t]
   (to-graphql-type db t true)
   )
  ([db t fields?]
   (cond
     (= t String)
     {"kind" "SCALAR"
      "name" "String"}
     (= t Integer)
     {"kind" "SCALAR"
      "name" "Int"}
     (keyword? t)
     (let [e (x/entity db t)
           {:crux.graphql/keys [name]
            :crux.schema/keys [description attributes]}
           e]
       (cond->
           {"kind" "OBJECT"
            "name" name}
           description (conj ["description" description])
           fields?
           (into
            {"fields"
             (mapv
              (fn [[_ {:crux.schema/keys [description arguments type]
                       :crux.graphql/keys [name]}]]
                (cond-> {"name" name}
                  description (conj ["description" description])
                  true                  ; args is mandatory
                  (conj ["args"
                         (mapv
                          (fn [[_ {n :crux.graphql/name
                                   desc :crux.schema/description
                                   t :crux.schema/type
                                   :as arg}]]
                            (assert n (format "InputValue must have a name: %s" (pr-str arg)))
                            (assert t (format "InputValue must have a type: %s" (pr-str arg)))
                            (cond->
                                {"name" n}
                                desc (conj ["description" desc])
                                true (conj ["type" (to-graphql-type db t)])))
                          arguments)])
                  true
                  (conj ["type" (to-graphql-type db type false)])
                  true
                  (conj ["isDeprecated" false])))
              attributes)
             "interfaces" []
             :crux.schema/entity e})))

     :else (throw (ex-info "Cannot convert to GraphQL type" {:t t})))))


;; TODO: This fn appears to only visit types of attributes, not arg types in relations
(defn visit-types [db tref visited-set]
  (cond
    (keyword? tref)
    (let [e (x/entity db tref)]
      (cons tref
            (let [visits (set/difference
                          (set (map (fn [[_ v]] (:crux.schema/type v)) (:crux.schema/attributes e)))
                          visited-set)
                  new-visited-set (set/union visited-set visits)]
              (mapcat (fn [t] (visit-types db t new-visited-set)) visits))))
    :else
    [tref]))


(defrecord DbSchema [db]
  grab/Schema

  (resolve-type [this object-type field-name]
    (cond
      (= field-name "__schema")
      {"kind" "OBJECT"
       "name" "__Schema"
       "description" "A GraphQL Schema defines the capabilities of a GraphQL server. It exposes all available types and directives on the server, as well as the entry points for query, mutation, and subscription operations."
       "fields" [{"name" "types"
                  "description" "A list of all types supported by this server."
                  "args" []
                  "type" {"kind" "NON_NULL"
                          "ofType" {"kind" "LIST"
                                    "ofType" {"kind" "NON_NULL"
                                              "ofType" {"kind" "OBJECT"
                                                        "name" "__Type"}}}}
                  "isDeprecated" false}

                 {"name" "queryType"
                  "description" "The type that query operations will be rooted at."
                  "args" []
                  "type" {"kind" "NON_NULL"
                          "name" nil
                          "ofType" {"kind" "OBJECT"
                                    "name" "__Type"}}
                  "isDeprecated" false}

                 {"name" "mutationType"
                  "description" "If this server supports mutation, the type that mutation operations will be rooted at."
                  "args" []
                  "type" {"kind" "OBJECT"
                          "name" "__Type"}
                  "isDeprecated" false}

                 {"name" "subscriptionType"
                  "description" "If this server support subscription, the type that subscription operations will be rooted at."
                  "args" []
                  "type" {"kind" "OBJECT"
                          "name" "__Type"}
                  "isDeprecated" false}

                 {"name" "directives"
                  "description" "A list of all directives supported by this server."
                  "args" []
                  "type" {"kind" "NON_NULL"
                          "ofType" {"kind" "LIST"
                                    "ofType" {"kind" "NON_NULL"
                                              "ofType" {"kind" "OBJECT"
                                                        "name" "__Directive"}}}}
                  "isDeprecated" false}]}

      (= (get object-type "name") "__Schema")
      (if-let [type (some #(when (= (get % "name") field-name) (get % "type")) (get object-type "fields"))]
        type
        (throw
         (ex-info
          "Resolve schema type"
          {:object-type object-type
           :field-name field-name})))

      (= (get object-type "name") "__Type")
      (case field-name
        "kind" {"kind" "ENUM"
                "enumValues" []}

        "name" {"kind" "SCALAR"
                "name" "String"}

        "description" {"kind" "SCALAR"
                       "name" "String"}

        "fields" {"kind" "LIST"
                  "ofType" {"kind" "NON_NULL"
                            "ofType" {"kind" "OBJECT"
                                      "name" "__Field"}}}
        "interfaces" {"kind" "LIST"
                      "ofType" {"kind" "NON_NULL"
                                "ofType" {"kind" "OBJECT"
                                          "name" "__Type"}}}
        "possibleTypes" {"kind" "LIST"
                         "ofType" {"kind" "NON_NULL"
                                   "ofType" {"kind" "OBJECT"
                                             "name" "__Type"}}}

        "enumValues" {"kind" "LIST"
                      "ofType" {"kind" "NON_NULL"
                                "ofType" {"kind" "OBJECT"
                                          "name" "__EnumValue"}}}

        "inputFields" {"kind" "LIST"
                       "ofType" {"kind" "NON_NULL"
                                 "ofType" {"kind" "OBJECT"
                                           "name" "__InputValue"}}}

        "ofType" {"kind" "OBJECT"
                  "name" "__Type"}

        (throw (ex-info "No case" {:field-name field-name})))

      (= (get object-type "name") "__Field")
      (case field-name
        "name" {"kind" "NON_NULL"
                "ofType" {"kind" "SCALAR"
                          "name" "String"}}

        "description" {"kind" "SCALAR"
                       "name" "String"}

        "args" {"kind" "NON_NULL"
                "ofType" {"kind" "LIST"
                          "ofType" {"kind" "NON_NULL"
                                    "ofType" {"kind" "OBJECT"
                                              "name" "__InputValue"}}}}
        "type" {"kind" "NON_NULL"
                "ofType" {"kind" "OBJECT"
                          "name" "__Type"}}

        "isDeprecated" {"kind" "NON_NULL"
                        "ofType" {"kind" "SCALAR"
                                  "name" "Boolean"}}

        "deprecationReason" {"kind" "SCALAR"
                             "name" "String"})


      (= (get object-type "name") "__InputValue")
      (case field-name
        "name" {"kind" "NON_NULL"
                "ofType" {"kind" "SCALAR"
                          "name" "String"}}

        "description" {"kind" "SCALAR"
                       "name" "String"}

        "type" {"kind" "NON_NULL"
                "ofType" {"kind" "OBJECT"
                          "name" "__Type"}}

        "defaultValue" {"kind" "SCALAR"
                        "name" "String"})

      (= (get object-type "name") "__EnumValue")
      (throw (ex-info "TODO: __EnumValue" {}))

      (= (get object-type "name") "__Directive")
      (throw (ex-info "TODO: __Directive" {}))

      ;; Crux backed
      (:crux.schema/entity object-type)
      (let [attribute
            (some
             #(when (= (get % :crux.graphql/name) field-name) %)
             (vals (get-in object-type [:crux.schema/entity :crux.schema/attributes])))

            {:crux.schema/keys [cardinality required? description]
             type-ref :crux.schema/type}
            attribute

            graphql-object-type (to-graphql-type db type-ref)]
        (cond
          (= cardinality :crux.schema.cardinality/many)
          {"kind" "LIST"
           "ofType" graphql-object-type}

          required?
          {"kind" "NON_NULL"
           "ofType" graphql-object-type}

          :else graphql-object-type))

      :else
      (throw
       (ex-info
        (format "TODO: resolve unknown type: %s" field-name)
        {:object-type object-type
         :field-name field-name})))))

(defmethod print-method DbSchema [c w]
  (print-method (into {} (assoc c :schema "(schema)")) w))
