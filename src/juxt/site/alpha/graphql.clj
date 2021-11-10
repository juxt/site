;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql
  (:require
   [ring.util.codec :refer [form-decode]]
   [juxt.grab.alpha.schema :as schema]
   [juxt.grab.alpha.document :as document]
   [jsonista.core :as json]
   [juxt.grab.alpha.execution :refer [execute-request]]
   [juxt.grab.alpha.parser :as parser]
   [clojure.string :as str]
   [crux.api :as xt]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [clojure.edn :as edn]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'grab (create-ns 'juxt.grab.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'g (create-ns 'juxt.grab.alpha.graphql))

(defn to-xt-query [query]
  (let [result
        (postwalk
         (fn [x]
           (cond-> x
             (and (map? x) (:keyword x))
             (-> (get :keyword) keyword)
             (and (map? x) (:set x))
             (-> (get :set) set)
             (and (map? x) (:edn x))
             (-> (get :edn) edn/read-string)
             ))
         query)]
    result))

(defn generate-value [{:keys [type pathPrefix] :as m}]
  (when type
    (str pathPrefix (java.util.UUID/randomUUID))))

(defn args-to-entity [args field]
  (reduce
   (fn [acc arg-def]
     (let [generator-args (get-in arg-def [::schema/directives-by-name "site" ::g/arguments "gen"])
           kw (get-in arg-def [::schema/directives-by-name "site" ::g/arguments "a"])]
       (cond
         (::g/name arg-def) ; is it a singular (not a NON_NULL or LIST)
         (let [val (or (get args (::g/name arg-def))
                       ;; TODO: default value?
                       (generate-value generator-args))
               ;; Change a symbol value into a string

               ;; We don't want symbols in XT entities, because this leaks the
               ;; form-plane into the data-plane!
               val (cond-> val (symbol? val) str)]
           (cond-> acc
             (some? val) (assoc (keyword (or kw (::g/name arg-def))) val)))
         :else (throw (ex-info "Unsupported arg-def" {:arg-def arg-def})))))
   {}
   (::g/arguments-definition field)))

(defn process-xt-results
  [field results]
  (if (-> field ::g/type-ref ::g/list-type)
    results
    ;; If this isn't a list type, take the first
    (first results)))

(defn infer-query
  [db type]
  (let [default-query '{:find [e]
                        :in [type]
                        :where [[e :juxt.site.alpha/type type]]}
        results (for [[e] (xt/q db default-query type)]
                  (xt/entity db e))]
    (or results (throw (ex-info "No resolver found for " type)))))

(defn protected-lookup [e subject db]
  (let [lookup #(xt/entity db %)
        ent (lookup e)]
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
          ent))

      ;; Return unprotected ent
      ent)))

(defn query [schema document operation-name crux-node db subject]
  (execute-request
   {:schema schema
    :document document
    :operation-name operation-name
    :field-resolver
    (fn [{:keys [object-type object-value field-name argument-values] :as field-resolver-args}]

      (let [types-by-name (::schema/types-by-name schema)
            field (get-in object-type [::schema/fields-by-name field-name])
            site-args (get-in field [::schema/directives-by-name "site" ::g/arguments])
            field-kind (-> field ::g/type-ref ::g/name types-by-name ::g/kind)
            lookup-entity (fn [id] (xt/entity db id))
            mutation? (=
                       (get-in schema [::schema/root-operation-type-names :mutation])
                       (::g/name object-type))]
        (cond
          mutation?
          (let [object-to-put (args-to-entity argument-values field)]
            (xt/await-tx
             crux-node
             (xt/submit-tx
              crux-node
              [[:crux.tx/put object-to-put]]))
            object-to-put)

          ;; Direct lookup - useful query roots
          (get site-args "e")
          (let [e (get site-args "e")]
            (protected-lookup e subject db))

          (get site-args "q")
          (let [object-id (:crux.db/id object-value)
                q (assoc
                   (to-xt-query (get site-args "q"))
                   :in (vec (cond->> (map symbol (keys argument-values))
                              object-id (concat ['object]))))

                results
                (try
                  (apply
                   xt/q db q (cond->> (vals argument-values)
                               object-id (concat [object-id])))
                  (catch Exception e
                    (throw (ex-info "Failure when running XTDB query"
                                    {:query q}
                                    e))))

                result-entities
                (for [[e] results]
                  (protected-lookup e subject db))]

            (log/tracef "GraphQL results is %s" result-entities)
            (process-xt-results field result-entities))

          (get site-args "a")
          (let [att (get site-args "a")
                val (get object-value (keyword att))]
            (if (= field-kind 'OBJECT)
              (protected-lookup val subject db)
              val))

          ;; The use of a resolver should be a privileged operation, since it
          ;; has the potential to bypass access control.
          (get site-args "resolver")
          (let [resolver (requiring-resolve (symbol (get site-args "resolver")))]
            ;; Resolvers need to do their own access control
            (resolver (assoc field-resolver-args ::pass/subject subject :db db)))

          ;; Another strategy is to see if the field indexes the
          ;; object-value. This strategy allows for delays to be used to prevent
          ;; computing field values that aren't resolved.
          (contains? object-value field-name)
          (let [f (force (get object-value field-name))]
            (if (fn? f) (f argument-values) f))

          ;; If the key is 'id', we assume it should be translated to xt/id
          (= "id" field-name)
          (get object-value :crux.db/id)

          ;; Or simply try to extract the keyword
          (contains? object-value (keyword field-name))
          (get object-value (keyword field-name))

          (-> field ::g/type-ref ::g/list-type ::g/name)
          (infer-query db (-> field ::g/type-ref ::g/list-type ::g/name))

          :else
          (throw (ex-info "TODO" field-name)))))}))

(defn post-handler [{::site/keys [uri crux-node db]
                     ::pass/keys [subject]
                     :as req}]

  (let [schema (some-> (xt/entity db uri) ::grab/schema)
        body (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        [document-str operation-name]
        (case (some-> req ::site/received-representation ::http/content-type)
          "application/json"
          (let [json (some-> body json/read-value)]
            [(get json "query") (get json "operationName")])

          "application/graphql"
          [body nil]

          (throw (ex-info (format "Unknown content type for GraphQL request: %s" (some-> req ::site/received-representation ::http/content-type)) req)))

        _ (when (nil? document-str)
            (throw (ex-info "Nil GraphQL query" (-> req
                                                    (update-in [::site/resource] dissoc ::grab/schema)
                                                    (dissoc :juxt.pass.alpha/request-context)))))
        document
        (try
          (parser/parse document-str)
          (catch Exception e
            (log/error e "Error parsing GraphQL query")
            (throw (ex-info "Failed to parse document" {:errors [{:message (.getMessage e)}]}))))

        compiled-document
        (try
          (document/compile-document document schema)
          (catch Exception e
            (log/error e "Error parsing or compiling GraphQL query")
            (let [errors (:errors (ex-data e))]
              (log/errorf "Errors %s" (pr-str errors))
              (throw
               (ex-info
                "Error parsing or compiling GraphQL query"
                (into
                 req
                 (cond-> {:ring.response/status 400}
                   (seq errors) (assoc ::errors errors)))
                e)))))

        results (query schema compiled-document operation-name crux-node db subject)]

    (-> req
        (assoc
         :ring.response/status 200
         :ring.response/body
         (json/write-value-as-string results))
        (update :ring.response/headers assoc "content-type" "application/json"))))

(defn schema-resource [resource schema-str]
  (let [schema (schema/compile-schema (parser/parse schema-str))]
    (assoc resource ::grab/schema schema ::http/body (.getBytes schema-str))))

(defn put-schema [crux-node resource schema-str]
  (xt/await-tx
   crux-node
   (xt/submit-tx
    crux-node
    [[:crux.tx/put (schema-resource resource schema-str)]])))

(defn put-handler [{::site/keys [uri db crux-node] :as req}]
  (let [schema-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (when-not schema-str
            (throw
             (ex-info
              "No schema in request"
              (into
               req
               {:ring.response/status 400}))))

        resource (xt/entity db uri)]

    (when (nil? resource)
      (throw (ex-info "GraphQL resource not configured" {:uri uri})))

    (try
      (put-schema crux-node resource schema-str)
      (assoc req :ring.response/status 204)
      (catch Exception e
        (let [errors (:errors (ex-data e))]
          (if (seq errors)
            (do
              (log/trace e "error")
              (log/tracef "schema errors: %s" (pr-str (:errors (ex-data e))))
              (throw
               (ex-info
                "Errors in schema"
                (into
                 req
                 (cond-> {:ring.response/status 400}
                   (seq errors) (assoc ::errors errors)))
                e)))
            (throw
             (ex-info
              "Failed to put schema"
              (into
               req
               {:ring.response/status 500})
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
    (::errors req)
    (->>
     (for [error (::errors req)]
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
    :errors (::errors req)}))

(defn post-error-text-body [req]
  (->>
   (for [error (::errors req)]
     (cond-> (str \tab (:error error))
       (:location error) (str " (line " (-> error :location :row inc) ")")))
   (into ["Query errors"])
   (str/join (System/lineSeparator))))

(defn post-error-json-body [req]
  (json/write-value-as-string
   {:errors
    (for [error (::errors req)
          :let [location (:location error)]]
      (cond-> error
        location (assoc :location location)))}))

(defn stored-document-put-handler [{::site/keys [uri db crux-node] :as req}]
  (let [document-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))

        _ (when-not document-str
            (throw
             (ex-info
              "No document in request"
              (into
               req
               {:ring.response/status 400}))))

        resource (xt/entity db uri)]

    (when (nil? resource)
      (throw (ex-info "GraphQL stored-document resource not configured" {:uri uri})))

    ;; Validate resource
    (when-not (::site/graphql-schema resource)
      (throw (ex-info "Resource should have a :juxt.site.alpha/graphql-schema key" {::site/resource resource})))

    (let [schema-id (::site/graphql-schema resource)
          schema (some-> db (xt/entity schema-id) :juxt.grab.alpha/schema)]

      (when-not schema
        (throw
         (ex-info
          "Cannot store a GraphQL document when the schema hasn't been added"
          {::site/graph-schema schema-id})))

      (try
        (let [document (document/compile-document
                        (parser/parse document-str)
                        schema)]
          (xt/await-tx
           crux-node
           (xt/submit-tx
            crux-node
            [[:crux.tx/put (assoc resource ::grab/document document)]])))

        (assoc req :ring.response/status 204)

        (catch clojure.lang.ExceptionInfo e
          (let [errors (:errors (ex-data e))]
            (if (seq errors)
              (do
                (log/tracef "GraphQL document errors: %s" (pr-str (:errors (ex-data e))))
                (throw
                 (ex-info
                  "Errors in GraphQL document"
                  (into
                   req
                   (cond-> {:ring.response/status 400}
                     (seq errors) (assoc ::errors errors)))
                  e)))
              (throw
               (ex-info
                "Failed to store GraphQL document due to error"
                (into
                 req
                 {:ring.response/status 500})
                e)))))))))

(defn stored-document-post-handler
  [{::site/keys [crux-node db resource received-representation]
    ::pass/keys [subject]
    :as req}]

  (assert (.startsWith (::http/content-type received-representation)
                       "application/x-www-form-urlencoded"))

  ;; Look up GraphQL document from resource
  (let [posted-body (slurp (::http/body received-representation))
        form (form-decode posted-body)
        operation-name (get form "operationName")
        schema-id (::site/graphql-schema resource)
        schema (some-> (when schema-id (xt/entity db schema-id)) ::grab/schema)
        ;; TODO: This should be pre-parsed against schema
        document-str (String. (::http/body resource) "UTF-8")
        document (document/compile-document (parser/parse document-str) schema)
        results (query schema document operation-name
                       nil ;; for crux-node, so we prevent get updates
                       db
                       subject)]
    (-> req
        (assoc-in [:ring.response/headers "content-type"] "application/json")
        (assoc :ring.response/body (json/write-value-as-bytes results)))))

(defn text-plain-representation-body [{::site/keys [db] :as req}]
  (let [lookup (fn [id] (xt/entity db id))]
    (-> req ::site/selected-representation ::site/variant-of lookup ::http/body (String. "utf-8"))))

(defn text-html-template-model [{::site/keys [resource db]}]
  (let [original-resource (if-let [variant-of (::site/variant-of resource)] (xt/entity db variant-of) resource)
        endpoint (:juxt.site.alpha/graphql-schema original-resource)
        schema-resource (xt/entity db endpoint)
        schema (some-> schema-resource ::grab/schema)
        schema-str (String. (some-> schema-resource ::http/body))
        document-str (String. (::http/body original-resource) "UTF-8")
        document (document/compile-document (parser/parse document-str) schema)
        operation-names (->> (:juxt.grab.alpha.document/operations document)
                             (filter #(= (::g/operation-type %) :query))
                             (map ::g/name))]
    {"document" (String. (::http/body original-resource) "UTF-8")
     "endpoint" endpoint
     "operationNames" operation-names
     "schemaString" schema-str
     "form" {"action" (:crux.db/id original-resource)}}))
