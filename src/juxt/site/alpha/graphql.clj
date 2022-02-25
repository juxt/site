;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql
  (:require
   [ring.util.codec :refer [form-decode]]
   [selmer.parser :as selmer]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [jsonista.core :as json]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.grab.alpha.parser :as parser]
   [juxt.site.alpha.util :refer [assoc-some]]
   [clojure.string :as str]
   [clojure.set :refer [rename-keys]]
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk keywordize-keys]]
   [clojure.edn :as edn]
   [tick.core :as t]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'g (create-ns 'juxt.grab.alpha.graphql))

(defn field->type
  [field]
  (or
   (-> field
       ::g/type-ref
       ::g/non-null-type
       ::g/list-type
       ::g/name)
   (-> field
       ::g/type-ref
       ::g/non-null-type
       ::g/list-type
       ::g/non-null-type
       ::g/name)
   (-> field
       ::g/type-ref
       ::g/list-type
       ::g/name)
   (-> field
       ::g/type-ref
       ::g/name)))

(defn list-type?
  [type-ref]
  (or
   (-> type-ref ::g/non-null-type ::g/list-type)
   (::g/list-type type-ref)))

(defn default-query
  [field-or-type type-k]
  (let [type (or (field->type field-or-type)
                 field-or-type)]
    {:find ['e '_siteCreatedAt]
     :where [['e type-k type]
             ['e :_siteCreatedAt '_siteCreatedAt]]}))

(defn to-xt-query
  [{:keys [custom-xt-query field argument-values type-k site-args object-value]}]
  (let [values argument-values
        field-or-type (or
                       (get site-args "type")
                       field)
        query (or
               custom-xt-query
               (get site-args "q")
               (default-query field-or-type type-k))
        result
        (postwalk
         (fn [x]
           (try
             (cond-> x
               (and (map? x) (:keyword x))
               (-> (get :keyword) keyword)
               (and (map? x) (:set x))
               (-> (get :set) set)
               (and (map? x) (:edn x))
               (-> (get :edn)
                   (selmer/render {"type" type-k
                                   "object-id" (:xt/id object-value)
                                   "args" values})
                   edn/read-string)
               (= 'type x)
               ((constantly type-k)))
             (catch Exception e
               (throw (ex-info "Error in q site arg" {:x x} e)))))
         query)
        limit (get values "limit")
        offset (get values "offset")
        order (keyword (get values "order"))
        search-terms (get values "searchTerms")
        search? (not (every? empty? (vals search-terms)))
        search-where-clauses
        (and search?
             (apply concat
                    (for [[key val] search-terms
                          :let [key-symbol (keyword key)
                                val (str/join " "
                                              (map (fn [s] (str s "*"))
                                                   (str/split val #" ")))]]
                      [[`(~(symbol "text-search")
                          ~key-symbol
                          ~val)
                        '[[e v s]]]])))
        order-by-dir (cond
                       (or (= :asc order) (= :desc order)) order
                       limit :desc
                       :else nil)
        result (assoc-some
                result
                :find (and search? '[e v s])
                :order-by (cond
                            search?
                            '[[s :desc]]
                            order-by-dir
                            [['_siteCreatedAt order-by-dir]]
                            :else
                            nil)
                :where (and search-where-clauses
                            (vec (concat (:where result) search-where-clauses)))
                :limit limit
                :offset (when (pos-int? offset)
                          offset))]
    result))

(defn generate-value [{:keys [type pathPrefix template] :as gen-args} args]
  (when type
    (cond
      (= (name type) "UUID")
      (str pathPrefix (java.util.UUID/randomUUID))
      (= (name type) "TEMPLATE")
      (selmer/render template args)
      :else
      (throw (ex-info "Unrecognised type specified when generating attribute value"
                      {:gen-args gen-args :args args})))))

(defn scalar? [arg-name types-by-name]
  (= (get-in types-by-name [arg-name ::g/kind]) 'SCALAR))

(defn enum? [arg-name types-by-name]
  (= (get-in types-by-name [arg-name ::g/kind]) 'ENUM))

(defn prepare-mutation-entity
  [{:keys [field juxt.pass.alpha/subject type-k argument-values site-args] :as opts}
   entity transform]
  (let [type (-> field ::g/type-ref ::g/name)
        _validate-type (and (nil? type)
                            (throw (ex-info "Couldn't infer type" {:field field})))]
    (cond-> entity
      true (assoc
            :xt/id (or
                    (:xt/id entity)
                    (:id entity)
                    (generate-value
                     {:type "UUID"
                      :pathPrefix type}
                     {})))
      ;; this should use a clock component that keeps the same time for
      ;; everything in a single graphql transcation
      true (assoc :_siteCreatedAt (str (t/now)))
      subject (assoc :_siteSubject (::pass/username subject))
      (nil? (type-k entity)) (assoc type-k type)
      (:id entity) (dissoc :id)
      transform (transform argument-values)

      ;; This is special argument that adds Site specific attributes
      (get site-args "methods")
      (assoc ::http/methods (set (map (comp keyword str/lower-case) (get site-args "methods")))))))

(defn- args-to-entity
  [{:keys [argument-values schema field base-uri site-args] :as opts}]
  (log/tracef "args-to-entity, site-args is %s" (pr-str site-args))
  (let [args argument-values
        types-by-name (:juxt.grab.alpha.schema/types-by-name schema)
        transform-sym (some-> site-args (get "transform") symbol)
        transform (when transform-sym (requiring-resolve transform-sym))
        _  (when (and transform-sym (not transform))
             (throw (ex-info "Failed to resolve transform fn" {:transform transform-sym})))
        entity
        (reduce
         (fn [acc arg-def]
           (let [site-args (get-in arg-def [::schema/directives-by-name "site" ::g/arguments])
                 generator-args (get site-args "gen")
                 transform-sym (some-> site-args (get "transform") symbol)
                 transform (when transform-sym (requiring-resolve transform-sym))
                 kw (get-in arg-def [::schema/directives-by-name "site" ::g/arguments "a"])
                 arg-name (::g/name arg-def)
                 key (keyword (or kw arg-name))
                 type-ref (::g/type-ref arg-def)
                 arg-type (or (::g/name type-ref)
                              (-> type-ref ::g/non-null-type ::g/name))]

             (when (and transform-sym (not transform))
               (throw (ex-info "Failed to resolve transform fn" {:transform transform-sym})))

             (when transform
               (log/tracef "transform is %s" transform))

             (cond
               arg-type                 ; not a LIST
               (let [value (or
                            ;; If provided we use it
                            (get args arg-name)
                            ;; Else we try to generate it
                            (generate-value generator-args args)
                            )
                     value
                     (cond-> value
                       ;; We don't want symbols in XT entities, because this leaks the
                       ;; form-plane into the data-plane!
                       (symbol? value) str

                       ;; Replace base-uri in string-template, only for an ID
                       ;; since we should be careful not to tamper with other
                       ;; values.
                       (and (string? value) (= arg-type "ID"))
                       (selmer/render {"base-uri" base-uri})

                       ;; Transform value
                       transform transform)]

                 (cond
                   (or kw (scalar? arg-type types-by-name))
                   (assoc-some acc key value)

                   :else
                   (try
                     (def value value)
                     (merge acc (keywordize-keys value))
                     ;; TODO if we need to assoc, like if someone wants to nest
                     ;; a map inside an entity to prevent it being indexed, then
                     ;; that needs to happen here. probably with a directive to
                     ;; differentiate from the default which is to merge (which
                     ;; is needed to support input types)
                     (catch Exception e
                       (throw
                        (ex-info
                         "Cannot merge value into acc"
                         {:acc acc
                          :arg-type arg-type
                          :value value}
                         e))))))

               ;; Is it a list? Then put in as a vector
               (list-type? type-ref)
               (let [val (or (get args (name key))
                             ;; TODO: default value?
                             (generate-value generator-args args))
                     ;; Change a symbol value into a string

                     ;; We don't want symbols in XT entities, because this leaks the
                     ;; form-plane into the data-plane!
                     val (cond-> val (symbol? val) str)

                     list-type (field->type arg-def)]
                 (cond
                   (scalar? list-type types-by-name) (assoc-some acc key val)
                   :else
                   (throw (ex-info "Unsupported list-type" {:arg-def arg-def
                                                            :list-type list-type}))))

               :else (throw (ex-info "Unsupported arg-def" {:arg-def arg-def})))))
         {}
         (::g/arguments-definition field))]
    (prepare-mutation-entity opts entity transform)))

(defn args-to-entities
  [{:keys [argument-values field types-by-name] :as opts}]
  (let [args (and
              (= 1 (count argument-values))
              (first (vals argument-values)))]
    (when-not args
      (throw (ex-info "Mutations that insert multiple entities must take a single InputType as their only argument"
                      argument-values)))
    (for [arg args
          :let [input-type-name (-> field ::g/arguments-definition first ::g/type-ref ::g/list-type ::g/name)
                input-type (::g/input-values (get types-by-name input-type-name))
                arg-type (::g/list-type (::g/type-ref field))
                field (assoc field ::g/arguments-definition input-type ::g/type-ref arg-type)]]
      (args-to-entity (assoc opts :argument-values arg :field field)))))

(defn process-xt-results
  [field results]
  (let [type-ref (::g/type-ref field)]
    (if-let [type (::g/non-null-type type-ref)]
      (cond
        (list-type? type-ref)
        (or results [])
        :else
        (first results))
      (cond
        (list-type? type-ref)
        results
        ;; If this isn't a list type, take the first
        :else
        (first results)))))

(defn await-tx
  [xt-node txes]
  (xt/await-tx
   xt-node
   (xt/submit-tx
    xt-node
    txes)))

(defn xt-delete
  [id]
  [:xtdb.api/delete id])

(defn xt-put
  [object valid-from]
  (and (nil? (:xt/id object))
       (throw (ex-info "Trying to put object without xt id" {:object object})))
  [:xtdb.api/put object valid-from])

(defn put-objects! [xt-node objects]
  (let [processed-objects
        (for [object objects]
          (let [valid-time (some-> object
                                   :xtdb.api/valid-time
                                   (java.time.Instant/parse)
                                   (java.util.Date/from))]
            (xt-put (dissoc object :xtdb.api/valid-time) valid-time)))]
    (await-tx xt-node processed-objects)))

(defn entity-creation-time
  [{:keys [xt/id]} db]
  (with-open [history (xt/open-entity-history db id :asc)]
    (-> history
        iterator-seq
        first
        ::xt/valid-time
        t/instant
        .toString)))

(defn entity-valid-time
  [entity db]
  (some-> db
          (xt/entity-tx (:xt/id entity))
          ::xt/valid-time
          t/instant
          .toString))

(defn assoc-valid-time
  [entity db]
  (assoc entity :_siteValidTime (entity-valid-time entity db)))

(defn assoc-creation-time
  [entity db xt-node]
  (if (:_siteCreatedAt entity)
    entity
    (let [new-entity (assoc entity :_siteCreatedAt (entity-creation-time entity db))]
      (put-objects! xt-node [new-entity])
      new-entity)))

(defn protected-lookup [e subject db xt-node]
  (let [lookup #(xt/entity db %)
        ent (some-> (lookup e)
                    (assoc-valid-time db)
                    (assoc-creation-time db xt-node))]
    (def ent ent)
    (if-let [ent-ns (::pass/namespace ent)]
      (let [rules (some-> ent-ns lookup ::pass/rules)
            acls (->>
                  (xt/q
                   db
                   {:find ['(pull ?acl [*])]
                    :where '[[?acl ::pass/type "ACL"]
                             (check ?acl ?subject ?e)]
                    :rules rules
                    :in '[?subject ?e]}
                   subject e)
                  (map first))]
        (when (seq acls)
          ;; TODO: Also use the ACL to infer when/whether to select-keys
          ;;(select-keys ent (apply concat (map :keys acls)))
          ent
          ))

      ;; Return unprotected ent
      ent)))

(defn limit-results
  "Needs to be done when we can't use xt's built in limit/offset"
  [args results]
  (let [result-count (count results)
        limit (get args "limit" result-count)]
    (if (or (get args "searchTerms") (> result-count limit))
      (take limit (drop (get args "offset" 0) results))
      results)))

(defn pull-entities
  [db xt-node subject results query]
  (for [[e] results]
    (assoc-some (protected-lookup e subject db xt-node)
                :_siteQuery (and query (pr-str query)))))

(defn infer-query
  [db xt-node subject field query args]
  (let [type (field->type field)
        results (pull-entities db xt-node subject (xt/q db query type) query)]
    (or (process-xt-results field results)
        (throw (ex-info "No resolver found for " type)))))

(defn traverse [object-value atts subject db xt-node]
  (if (seq atts)
    (let [next-object-value
          (get
           (cond-> object-value
             (string? object-value)
             (protected-lookup subject db xt-node))
           (keyword (first atts)))]
      (traverse next-object-value
                (rest atts)
                subject db xt-node))
    object-value))

(defn default-for-type
  [type-ref]
  (let [type-name (::g/name type-ref)]
    (cond
      (list-type? type-ref)
      []
      (= "String" type-name)
      ""
      :else
      (do
        (log/debugf "defaulting to nil, type-ref is %s" type-ref)
        nil))))

(defn perform-mutation!
  [{:keys [argument-values site-args xt-node lookup-entity field-kind pass/subject] :as opts}]
  (let [action (or (get site-args "mutation") "put")
        validate-id! (fn [args]
                       (let [id (get args "id")]
                         (or id (throw (ex-info "Delete mutations need an 'id' key"
                                                {:arg-values argument-values})))))
        bulk-mutation (= 'LIST field-kind)]
    (case action
      "delete"
      (let [id (validate-id! argument-values)]
        (await-tx xt-node [(xt-delete id)])
        ;; TODO: Allow an argument to correspond to valid-time, via
        ;; @site(a: "xtdb.api/valid-time").
        (lookup-entity id))
      "put"
      (if bulk-mutation
        (let [txes (args-to-entities opts)]
          (put-objects! xt-node txes)
          txes)
        (let [tx (args-to-entity opts)]
          (put-objects! xt-node [tx])
          tx))
      "update"
      (let [new-entity (args-to-entity opts)
            old-entity (some-> new-entity :xt/id lookup-entity)
            new-entity (merge old-entity new-entity)]
        (put-objects! xt-node [new-entity])
        new-entity)
      "rollback"
      (let [as-of (try
                    (let [as-of (get argument-values "asOf")]
                      (if (str/ends-with? as-of "Z")
                        (-> as-of t/inst)
                        (-> as-of
                            (t/parse-date-time
                             (t/formatter :iso-local-date-time))
                            t/inst)))
                    (catch Exception e
                      (throw (ex-info "rollback mutations need a valid asOf argument"
                                      {:args argument-values}
                                      e))))
            db (xt/db xt-node as-of)
            id (validate-id! argument-values)
            new-entity (protected-lookup id subject db xt-node)]
        (put-objects! xt-node [new-entity])
        new-entity))))

;; TODO: Need something (in grab) that will coerce to a list
;;(-> object-value :stack-trace seqable?)

(defn query [schema document operation-name variable-values
             {::pass/keys [subject]
              ::site/keys [db xt-node base-uri resource]
              :as req}]
  (let [type-k
        (or
         (some-> schema :juxt.grab.alpha.schema/directives (get "site") ::g/arguments (get "type") keyword)
         ;; Deprecated, please don't rely on this, but rather add a directive to
         ;; your schema: schema @site(type: "juxt.site/type")
         :juxt.site/type)]
    (execute-request
     {:schema schema
      :document document
      :operation-name operation-name
      :variable-values variable-values
      :abstract-type-resolver
      (fn [{:keys [object-value]}]
        (get object-value type-k))
      :field-resolver
      (fn [{:keys [object-type object-value field-name argument-values]
            :as field-resolver-args}]

        #_(when (= "SiteError" (get-in object-type [::g/name]))
            (def object-value object-value)
            )

        (let [types-by-name (::schema/types-by-name schema)
              field (get-in object-type [::schema/fields-by-name field-name])
              site-args (get-in field [::schema/directives-by-name "site" ::g/arguments])
              field-kind (or
                          (-> field ::g/type-ref ::g/name types-by-name ::g/kind)
                          (when (-> field ::g/type-ref ::g/list-type) 'LIST)
                          (when (-> field ::g/type-ref ::g/non-null-type) 'NON_NULL))
              mutation? (=
                         (get-in schema [::schema/root-operation-type-names :mutation])
                         (::g/name object-type))
              db (if (and (not mutation?)
                          (get argument-values "asOf"))
                   (xt/db xt-node (-> argument-values
                                      (get "asOf")
                                      t/date-time
                                      t/inst))
                   db)
              object-id (:xt/id object-value)
              ;; TODO: Protected lookup please!
              lookup-entity (fn [id] (xt/entity db id))
              opts
              (merge field-resolver-args
                     {:site-args site-args
                      :xt-node xt-node
                      :schema schema
                      :field field
                      :field-kind field-kind
                      :types-by-name types-by-name
                      :mutation? mutation?
                      :base-uri base-uri
                      :type-k type-k
                      :lookup-entity lookup-entity
                      ::pass/subject subject
                      :db db})]

          (cond
            ;; The registration of a resolver should be a privileged operation, since it
            ;; has the potential to bypass access control.
            (get site-args "resolver")
            (let [resolver (requiring-resolve (symbol (get site-args "resolver")))]
              ;; Resolvers need to do their own access control
              (resolver opts))

            mutation? (perform-mutation! opts)

            (get site-args "history")
            (if-let [id (get argument-values "id")]
              (let [limit (get argument-values "limit" 10)
                    offset (get argument-values "offset" 0)
                    order (case (get site-args "history")
                            "desc" :desc
                            "asc" :asc
                            :desc)
                    process-history-item
                    (fn [{::xt/keys [valid-time doc]}]
                      (assoc doc :_siteValidTime (t/instant valid-time)))]
                (with-open [history (xt/open-entity-history db id order {:with-docs? true})]
                  (->> history
                       (iterator-seq)
                       (drop offset)
                       (take limit)
                       (map process-history-item))))
              (throw (ex-info "History queries must have an id argument" {})))

            ;; Direct lookup - useful for query roots
            (get site-args "e")
            (let [e (get site-args "e")]
              (or (protected-lookup e subject db xt-node)
                  (protected-lookup (get argument-values e) subject db xt-node)))

            (get site-args "q")
            (let [object-id (:xt/id object-value)
                  arg-keys (fn [m] (remove #{"limit" "offset" "orderBy"} (keys m)))
                  in (cond->> (map symbol (arg-keys argument-values))
                       object-id (concat ['object]))
                  q (assoc
                     (to-xt-query opts)
                     :in (if (second in) [in] (vec in)))

                  query-args (cond->> (vals argument-values)
                               object-id (concat [object-id]))
                  args (if (second query-args) query-args (first query-args))
                  results
                  (try
                    (xt/q db q args)
                    (catch Exception e
                      (throw (ex-info "Failure when running XTDB query"
                                      {:message (ex-message e)
                                       :query (pr-str q)
                                       :args args}
                                      e))))
                  limited-results (limit-results argument-values results)
                  result-entities (cond->>
                                      (pull-entities db xt-node subject limited-results q)
                                      (get site-args "a")
                                      (map (keyword (get site-args "a")))

                                      )]
              ;;(log/tracef "GraphQL results is %s" (seq result-entities))

              (process-xt-results field result-entities))

            (get site-args "itemForId")
            (let [item-key (keyword (get site-args "itemForId"))
                  query {:find ['e '_siteCreatedAt]
                         :where [['e type-k (field->type field)]
                                 ['e :_siteCreatedAt '_siteCreatedAt]
                                 ['e item-key (get argument-values "id")]]}

                  query (to-xt-query (assoc opts :custom-xt-query query))
                  results (xt/q db query)
                  result-entities (cond->> (pull-entities db xt-node subject results query)
                                    (get site-args "a")
                                    (map (keyword (get site-args "a"))))]
              (vec (process-xt-results field result-entities)))

            (get site-args "a")
            (let [att (get site-args "a")
                  val (if (vector? att)
                        (traverse object-value att subject db xt-node)
                        (get object-value (keyword att)))
                  transform-sym (some-> site-args (get "transform") symbol)
                  transform (when transform-sym (requiring-resolve transform-sym))
                  ]
              (if (= field-kind 'OBJECT)
                (protected-lookup val subject db xt-node)
                ;; TODO: check for lists
                (cond-> val
                  transform transform)))

            (get site-args "ref")
            (let [ref (get site-args "ref")
                  e (or
                     (and (vector? ref) (traverse object-value ref subject db xt-node))
                     (get object-value ref)
                     (get object-value (keyword ref)))
                  type (field->type field)]
              (if e
                (protected-lookup e subject db xt-node)
                (map (comp (fn [e] (protected-lookup e subject db xt-node)) first)
                     (xt/q db {:find ['e]
                               :where [['e type-k type]
                                       ['e (keyword ref) (or
                                                          (get argument-values ref)
                                                          object-id)]]}))))

            (get site-args "each")
            (let [att (get site-args "each")
                  val (if (vector? att)
                        (traverse object-value att subject db xt-node)
                        (get object-value (keyword att)))]
              (if (-> field ::g/type-ref list-type?)
                (map #(protected-lookup % subject db xt-node) val)
                (throw (ex-info "Can only used 'each' on a LIST type" {:field-kind field-kind}))))

            (get site-args "siteResolver")
            (let [resolver
                  (case (get site-args "siteResolver")
                    "allQueryParams"
                    (requiring-resolve 'juxt.site.alpha.graphql-resolver/query-parameters)
                    "queryParam"
                    (requiring-resolve 'juxt.site.alpha.graphql-resolver/query-parameter)
                    "queryString"
                    (requiring-resolve 'juxt.site.alpha.graphql-resolver/query-string)
                    "constant"
                    (requiring-resolve 'juxt.site.alpha.graphql-resolver/constant)
                    (throw (ex-info "No such built-in resolver" {:site-resolver (get site-args "siteResolver")})))]
              (resolver (assoc field-resolver-args ::site/request-context req)))

            ;; A function whose input is the result of a GraphqL 'sub' query,
            ;; propagating the same subject and under the exact same access
            ;; control policy. This allows the function to declare its necessary
            ;; inputs as a query.
            ;;
            ;; In addition, a function's results may be memoized, with each result
            ;; stored in XTDB which acts as a large persistent memoization
            ;; cache. For this reason, the function must be pure. The function
            ;; must take a single map which contains the results of the sub-query
            ;; and any argument values (which would also be used as variable
            ;; values in the GraphQL sub-query which computes the other input
            ;; argument).
            ;;
            ;; Once this feature is working, replace it with a call to a lambda or
            ;; similarly sandboxed execution environment.
            (get site-args "function")
            (throw (ex-info "Feature not yet supported" {}))

            (and (= 1 (count argument-values))
                 (= "id" (ffirst argument-values)))
            (lookup-entity (get argument-values "id"))

            (and (= 1 (count argument-values))
                 (= "ids" (ffirst argument-values)))
            (map lookup-entity (get argument-values "ids"))

            ;; Another strategy is to see if the field indexes the
            ;; object-value. This strategy allows for delays to be used to prevent
            ;; computing field values that aren't resolved.
            (and (map? object-value) (contains? object-value field-name))
            (let [f (force (get object-value field-name))]
              (if (fn? f) (f argument-values) f))

            ;; If the key is 'id', we assume it should be translated to xt/id
            (= "id" field-name)
            (get object-value :xt/id)

            ;; Or simply try to extract the keyword
            (and (map? object-value)
                 (or
                  ;; schema specifies this field is on the object
                  (get-in field [::schema/directives-by-name "onObject"])
                  (contains? object-value (keyword field-name))))
            (let [result (get object-value (keyword field-name))]
                (cond
                  (-> field ::g/type-ref list-type?)
                  (limit-results argument-values result)
                  ;; TODO validate enum (enum? (field->type field) types-by-name)
                  :else
                  result))

            (and (field->type field)
                 (not (scalar? (field->type field) types-by-name))
                 (not (enum? (field->type field) types-by-name)))
            (infer-query db
                         xt-node
                         subject
                         field
                         (to-xt-query opts)
                         argument-values)

            (get argument-values "id")
            (xt/entity db (get argument-values "id"))

            (and (get site-args "aggregate")
                 (get site-args "type"))
            (case (get site-args "aggregate")
              "count" (count
                       (xt/q
                        db (to-xt-query opts))))

            (= "_siteValidTime" field-name)
            (entity-valid-time object-value db)

            (= "_siteCreatedAt" field-name)
            (entity-creation-time object-value db)

            :else
            (default-for-type (::g/type-ref field)))))})))

(defn common-variables
  "Return the common 'built-in' variables that are bound always bound."
  [{::site/keys [uri] ::pass/keys [subject] ::apex/keys [new-uri] :as req}]
  (log/tracef "new-uri is %s, keys are %s" new-uri (pr-str (keys req)))
  (let [username (::pass/username subject)]
    (cond-> {}
      uri (assoc "siteUri" uri)
      new-uri (assoc "siteNewUri" new-uri)
      username (assoc "siteUsername" username))))

(defn post-handler [{::site/keys [uri db]
                     ::pass/keys [subject]
                     :as req}]
  (let [schema (some-> (xt/entity db uri) ::site/graphql-compiled-schema)
        _validate-schema (and (nil? schema)
                              (let [msg (str "Schema does not exist at " uri ". Are you deploying it correctly?")]
                                (throw (ex-info
                                        msg
                                        {::site/errors [msg] ;; TODO
                                         ::site/request-context (assoc req :ring.response/status 400)}))))
        body (some-> req ::site/received-representation ::http/body (String.))

        {query "query"
         operation-name "operationName"
         variables "variables"}
        (case (some-> req ::site/received-representation ::http/content-type)
          "application/json" (try (some-> body json/read-value)
                                  (catch Exception e
                                    (throw
                                     (let [msg "Error parsing JSON body"]
                                       (ex-info
                                        msg
                                        {::site/errors [msg]
                                         ::site/request-context (assoc req :ring.response/status 400)})))))
          "application/graphql" {"query" body}

          (throw
           (ex-info
            (format "Unknown content type for GraphQL request: %s" (some-> req ::site/received-representation ::http/content-type))
            {::site/request-context req})))

        _ (when (nil? query)
            (throw
             (ex-info
              "Nil GraphQL query"
              {::site/request-context req})))

        parsed-query
        (try
          (parser/parse query)
          (catch Exception e
            (log/error e "Error parsing GraphQL query")
            (throw
             (ex-info
              "Failed to parse query"
              {::site/request-context req}
              e))))

        compiled-query
        (try
          (document/compile-document parsed-query schema)
          (catch Exception e
            (log/error e "Error parsing or compiling GraphQL query")
            (let [errors (:errors (ex-data e))]
              (log/errorf "Errors %s" (pr-str errors))
              (throw
               (ex-info
                "Error parsing or compiling GraphQL query"
                (cond-> {::site/request-context (assoc req :ring.response/status 400)}
                  (seq errors) (assoc ::grab/errors errors)))))))

        variables (into
                   (common-variables req)
                   variables)

        results
        (juxt.site.alpha.graphql/query
         schema compiled-query operation-name variables req)]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string results))
        (update :ring.response/headers assoc "content-type" "application/json"))))

(defn schema-resource [resource schema-str]
  (let [schema (schema/compile-schema (parser/parse schema-str))]
    (assoc resource ::site/graphql-compiled-schema schema ::http/body (.getBytes schema-str))))

(defn put-schema [xt-node resource schema-str]
  (xt/await-tx
   xt-node
   (xt/submit-tx
    xt-node
    [[:xtdb.api/put (schema-resource resource schema-str)]])))

(defn put-handler [{::site/keys [uri db xt-node] :as req}]
  (let [schema-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (when-not schema-str
            (throw
             (ex-info
              "No schema in request"
              {::site/request-context {:ring.response/status 400}})))

        resource (xt/entity db uri)]

    (when (nil? resource)
      (throw
       (ex-info
        "GraphQL resource not configured"
        {:uri uri
         ::site/request-context (assoc req :ring.response/status 400)})))

    (try
      (put-schema xt-node resource schema-str)
      (assoc req :ring.response/status 204)
      (catch Exception e
        (let [errors (:errors (ex-data e))]
          (if (seq errors)
            (throw
             (ex-info
              "Errors in schema"
              (cond-> {::site/request-context (assoc req :ring.response/status 400)}
                (seq errors) (assoc ::grab/errors errors))))
            (throw
             (ex-info
              "Failed to put schema"
              {::site/request-context (assoc req :ring.response/status 500)}
              e))))))))

(defn plain-text-error-message [error]
  (let [line (some-> error :location :line inc)]
    (str
     (when line (format "%4d: " line))
     (:message error)
     " [" (->> (dissoc error :message)
               sort
               (map (fn [[k v]] (format "%s=%s" (name k) v)))
               (str/join ", ")) "]")))

(defn put-error-text-body [req]
  (cond
    (::site/errors req)
    (->>
     (for [error (::site/errors req)]
       (cond-> (str \tab (plain-text-error-message error))
         ;;(:location error) (str " (line " (-> error :location :line) ")")
         ))
     (into ["Schema compilation errors"])
     (str/join (System/lineSeparator)))
    (:ring.response/body req) (:ring.response/body req)
    :else "Unknown error, check stack trace"))

(defn put-error-json-body [req]
  (json/write-value-as-string
   {:message "Schema compilation errors"
    :errors (::site/errors req)}))

(defn post-error-text-body [req]
  (->>
   (for [error (::site/errors req)]
     (cond-> (str \tab (:error error))
       (:location error) (str " (line " (-> error :location :row inc) ")")))
   (into ["Query errors"])
   (str/join (System/lineSeparator))))

(defn post-error-json-body [req]
  (json/write-value-as-string
   {:errors
    (for [error (::site/errors req)
          :let [location (:location error)]]
      (cond-> error
        location (assoc :location location)))}))

(defn stored-document-resource-map [document-str schema]
  (try
    (let [document (document/compile-document
                    (parser/parse document-str)
                    schema)]

      {::site/graphql-compiled-query document
       ::http/body (.getBytes document-str)
       ::http/content-type "text/plain;charset=utf-8"})

    (catch clojure.lang.ExceptionInfo e
      (let [errors (:errors (ex-data e))]
        (if (seq errors)
          (throw
           (ex-info
            "Errors in GraphQL document"
            {::grab/errors errors}))
          (throw
           (ex-info
            "Failed to store GraphQL document due to error"
            {}
            e)))))))

(defn stored-document-put-handler [{::site/keys [uri db xt-node] :as req}]
  (let [document-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (when-not document-str
            (throw
             (ex-info
              "No document in request"
              {::site/request-context (assoc req :ring.response/status 400)})))

        resource (xt/entity db uri)]

    (when (nil? resource)
      (throw
       (ex-info
        "GraphQL stored-document resource not configured"
        {:uri uri
         ::site/request-context (assoc req :ring.response/status 400)})))

    ;; Validate resource
    (when-not (::site/graphql-schema resource)
      (throw
       (ex-info
        "Resource should have a :juxt.site.alpha/graphql-schema key"
        {::site/resource resource
         ::site/request-context (assoc req :ring.response/status 500)})))

    (let [schema-id (::site/graphql-schema resource)
          schema (some-> db (xt/entity schema-id) ::site/graphql-compiled-schema)]

      (when-not schema
        (throw
         (ex-info
          "Cannot store a GraphQL document when the schema hasn't been added"
          {::site/graph-schema schema-id
           ::site/request-context (assoc req :ring.response/status 500)})))

      (try
        (let [m (stored-document-resource-map document-str schema)]
          (xt/await-tx
           xt-node
           (xt/submit-tx
            xt-node
            [[:xtdb.api/put (into resource m)]])))

        (assoc req :ring.response/status 204)

        (catch clojure.lang.ExceptionInfo e
          (if-let [errors (::grab/errors (ex-data e))]
            (throw
             ;; Throw but ignore the cause (since we pull out the key
             ;; information from it)
             (ex-info
              "Errors in GraphQL document"
              (cond-> {::site/request-context (assoc req :ring.response/status 400)}
                (seq errors) (assoc ::grab/errors errors))))
            (throw
             (ex-info
              "Failed to store GraphQL document due to error"
              {::site/request-context (assoc req :ring.response/status 500)}
              e))))))))

(defn graphql-query [{::site/keys [db] :as req} stored-query-id operation-name variables]
  (assert stored-query-id)

  (let [resource (xt/entity db stored-query-id)
        _ (when-not resource
            (throw
             (ex-info
              "GraphQL stored query not found"
              {:stored-query-id stored-query-id
               ::site/request-context (assoc req :ring.response/status 500)})))

        schema-id (::site/graphql-schema resource)

        _ (when-not schema-id
            (throw (ex-info "No schema id on resource" {:xt/id stored-query-id})))
        schema (some-> (when schema-id (xt/entity db schema-id)) ::site/graphql-compiled-schema)

        document (some-> resource ::site/graphql-compiled-query)]

    (when-not document
      (throw (ex-info "Resource does not contain query" {:stored-query-id stored-query-id
                                                         :keys (keys resource)})))

    (query schema document operation-name variables req)))

(defn stored-document-post-handler
  [{::site/keys [xt-node db resource received-representation base-uri]
    ::pass/keys [subject]
    :as req}]

  (assert (.startsWith (::http/content-type received-representation)
                       "application/x-www-form-urlencoded"))

  ;; Look up GraphQL document from resource
  (let [posted-body (slurp (::http/body received-representation))
        form (form-decode posted-body)
        operation-name (get form "operationName")
        schema-id (::site/graphql-schema resource)
        schema (some-> (when schema-id (xt/entity db schema-id)) ::site/graphql-compiled-schema)
        ;; TODO: This should be pre-parsed against schema
        document-str (String. (::http/body resource) "UTF-8")
        document (document/compile-document (parser/parse document-str) schema)
        results (query schema document operation-name {} req)]
    (-> req
        (assoc-in [:ring.response/headers "content-type"] "application/json")
        (assoc :ring.response/body (json/write-value-as-bytes results)))))

(defn text-plain-representation-body [{::site/keys [db] :as req}]
  (let [lookup (fn [id] (xt/entity db id))]
    (-> req ::site/selected-representation ::site/variant-of lookup ::http/body (String. "UTF-8"))))

(defn text-html-template-model [{::site/keys [resource db]}]
  (let [original-resource (if-let [variant-of (::site/variant-of resource)] (xt/entity db variant-of) resource)
        endpoint (:juxt.site.alpha/graphql-schema original-resource)
        schema-resource (xt/entity db endpoint)
        schema-str (String. (some-> schema-resource ::http/body))
        document-str (some-> original-resource ::http/body (String. "UTF-8"))
        document (::site/graphql-compiled-query original-resource)
        operation-names (->> (:juxt.grab.alpha.document/operations document)
                             (filter #(= (::g/operation-type %) :query))
                             (map ::g/name))]
    {"document" document-str
     "endpoint" endpoint
     "operationNames" operation-names
     "schemaString" schema-str
     "form" {"action" (:xt/id original-resource)}}))
