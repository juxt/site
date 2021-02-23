;; Copyright © 2020, JUXT LTD.

(ns juxt.apex.alpha.parameters
  (:require
   [clojure.string :as str]
   [juxt.jinx.alpha :as jinx]
   [juxt.jinx.alpha.validate :as jinx.validate]
   [ring.util.codec :refer [url-decode]]))

(alias 'apex (create-ns 'juxt.apex.alpha))

;; TODO: Promote?
(defn error? [m]
  (when (associative? m)
    (contains? m :apex/error)))

;; TODO: Promote?
(defmacro if-error [sym on-error & body]
  (assert (.endsWith (str sym) "+"))
  (let [s (symbol (subs (str sym) 0 (dec (count (str sym)))))
        error (symbol "error")]
    `(if (error? ~sym)
       (let [~error (:apex/error ~sym)]
         ~on-error)
       (let [~s ~sym]
         ~@body))))

(def default-coercions
  {String {"integer" (fn [x] (Integer/parseInt x))
           "number" (fn [x] (Double/parseDouble x))
           "array" vector
           "boolean" (fn [x] (case x "true" true "false" false nil))}
   nil {"string" (constantly "")
        ;;"boolean" (constantly false)
        }})

(defn- group-query-string [qs]
  (reduce-kv
   (fn [acc k v] (assoc acc k (mapv second v)))
   {}
   (->> (str/split qs #"&")
        (filter (comp not str/blank?))
        (map #(str/split % #"=") )
        (group-by first))))

(defn- seq->map [vs]
  (loop [[k v & vs] vs result {}]
    (if v
      (recur vs (assoc result k v))
      result)))

(defn- extract-parameter-for-object
  "For a given parameter with name n, of type object, with OpenAPI
  parameter declaration in param, extract the parameter from the
  query-state map in :apex/qsm. If a parameter can be extracted, conj
  to acc. The reason for this contrived function
  signature is to support integration with a reduction across
  parameter declarations for a given query-string."
  [{:apex/keys [qsm params errors] :as acc} [n param]]
  (let [{:strs [schema required style explode]} param
        {:keys [value raw-values]
         new-qsm :qsm}
        (reduce-kv
         (fn [acc prop-key {typ "type" :as schema}]
           (let [qsm-key (if (and explode (= style "deepObject"))
                           (str n "[" prop-key "]")
                           prop-key)]
             (if-let [[_ encoded-strings] (find qsm qsm-key)]
               (let [val
                     (cond
                       ;; If the requires a boolean value, we treat an
                       ;; 'empty' value as a true.
                       (and (= typ "boolean") (nil? (first encoded-strings)))
                       true
                       :else (first encoded-strings))]
                 (-> acc
                     (update :value assoc prop-key val)
                     (update :qsm dissoc qsm-key)
                     (update :raw-values conj [prop-key encoded-strings])))
               acc)))
         {:qsm qsm
          :value {}
          :raw-values []}
         (get schema "properties"))

        validation
        (jinx.validate/validate schema value {:coercions default-coercions})]

    (cond-> acc
      ;; set qsm to new-qsm
      true (assoc :apex/qsm new-qsm)
      ;; If valid, add the parameter
      (::jinx/valid? validation)
      (conj [n {:raw-values raw-values
                :value (::jinx/instance validation)
                :param param}])
      ;; If not valid, add an error
      ;; TODO: Adding errors should be a common function to promote
      ;; consistency across errors.
      (not (::jinx/valid? validation))
      (conj [n {::apex/error {::apex/error-message "Not valid"
                              ::apex/validation validation}}]))))

(defn extract-undeclared-params [{:apex/keys [params qsm] :as state}]
  (-> (reduce-kv
       (fn [state k v]
         (cond-> state
           (not= k "trace")
           (conj [k {:value v}])))
       state
       qsm)
      (dissoc :apex/qsm)))

(defn- extract-value-from-encoded-strings+
  "Extract a value for the given parameter from a collection
  of (possibly) encoded values. Returns value according to parameter
  schema, or an error."
  [{:strs [style explode required schema content]
    allow-empty-value "allowEmptyValue"
    :or {style "form"}
    :as param}
   encoded-strings]


  (if content
    ;; Complex Situation
    (let [[media-type {:strs [schema]}]
          ;; "The map MUST only contain one entry."
          (first content)]
      (throw (ex-info "Not supported" {}))
      ;;(m/decode muuntaja media-type (first encoded-strings))
      )

    ;; Simple Situation
    (case [(get schema "type" "string") explode]

      (["null" true]
       ["null" false])
      ;; TODO: Think about how to represent nils in query params - is there guidance here?
      ;; An empty value is an implicit nil, but how to specify nil when allowEmptyValue is false ?
      (if allow-empty-value
        (first encoded-strings)
        {:apex/error {:apex.error/message "Empty value not allowed for parameter: allowEmptyValue is false"
                      :apex.error/references [{:apex.error.reference/url ""}]}})

      (["boolean" false]
       ["boolean" true])
      (if-some [val (first encoded-strings)]
        (if (or (.equalsIgnoreCase "false" val)
                (.equalsIgnoreCase "no" val)
                (.equalsIgnoreCase "nil" val))
          false true)
        (if allow-empty-value
          true
          {:apex/error {:apex.error/message "Empty value not allowed for parameter: allowEmptyValue is false"}}))

      (["integer" false]    ; single param in collection, take it
       ["integer" true])    ; possibly multiple params in collection, take first
      (if-some [val (first encoded-strings)]
        (try
          (Integer/parseInt val)
          (catch Exception e
            {:apex/error {:apex.error/message "Failed to coerce encoded string to integer"
                          :apex.error/encoded-value val
                          :apex.error/exception e}}))
        (if allow-empty-value
          nil
          {:apex/error {:apex.error/message "Empty value not allowed for parameter: allowEmptyValue is false"}}))

      (["string" false]     ; single param in collection, take it
       ["string" true])     ; possibly multiple params in collection, take first
      (if-some [val (first encoded-strings)]
        val
        (if allow-empty-value
          nil
          {:apex/error {:apex.error/message "Empty value not allowed for parameter: allowEmptyValue is false"}}))

      ["array" false]
      (str/split
       (first encoded-strings)
       (case style "form" #"," "spaceDelimited" #" " "pipeDelimited" #"\|"))

      ["array" true]
      (if (or
           allow-empty-value
           (every? some? encoded-strings))
        (vec encoded-strings)
        {:apex/error {:apex.error/message "Empty values not allowed for parameter: allowEmptyValue is false"}})

      ["object" false]
      (seq->map
       (str/split
        (first encoded-strings)
        (case style "form" #"," "spaceDelimited" #" " "pipeDelimited" #"\|")))

      ["object" true]
      (->> encoded-strings
           (map #(str/split % (case style "form" #"," "spaceDelimited" #" " "pipeDelimited" #"\|")))
           (into {})))))


(defn process-query-string
  ([qs paramdefs]
   (process-query-string qs paramdefs {}))
  ([qs paramdefs {}]
   (let [qsm (group-query-string ((fnil url-decode "") qs))]
     (->
      (reduce
       (fn [{:apex/keys [params qsm errors] :as acc}
            {:strs [in style explode schema]
             n "name"
             allow-empty-value "allowEmptyValue"
             :as param}]
         ;; When style is form, the default value is true. For all other
         ;; styles, the default value is false.
         (case in
           "query"
           (let [explode (if (some? explode) explode (= style "form"))
                 required (get param "required" false)
                 style (get param "style" "form")
                 param (assoc
                        param
                        "explode" explode
                        "required" required
                        "style" style)]
             (if (and (.equals "object" (get schema "type"))
                      (or (.equals "form" style) (.equals "deepObject" style))
                      explode)

               (extract-parameter-for-object acc [n param])

               (if-let [[_ encoded-strings] (find qsm n)]

                 (let [acc (update acc :apex/qsm dissoc n)
                       value+        ; + postfix indicates maybe error
                       (extract-value-from-encoded-strings+
                        (assoc param "explode" explode)
                        encoded-strings
                        )]

                   ;; TODO: We seem to be coercing above in
                   ;; extract-value-from-encoded-strings+, do we also
                   ;; really need to use jinx to validate below?

                   (if-error value+
                     (conj acc [n {:apex/error error
                                   :encoded-strings encoded-strings
                                   :param param}])

                     (let [validation (jinx.validate/validate schema value {:coercions default-coercions})]

                       (cond-> acc
                         (::jinx/valid? validation)
                         (conj [n {:encoded-strings encoded-strings
                                   :value (::jinx/instance validation)
                                   :param param
                                   :validation validation}])

                         ;; TODO: Have we got any tests for jinx validation failures on params?
                         (not (::jinx/valid? validation))
                         ;; TODO: Errors should follow the same convention, using consistent keyword namespaces
                         (conj [n {:apex/error {:apex.error/message "Not valid"
                                                :validation validation}}])

                         true (update :apex/qsm dissoc n)))))

                 ;; No value exists
                 (cond-> acc
                   ;; We're going to add an entry anyway. One purpose
                   ;; is to show tables in debug mode.
                   true (conj [n (cond-> {:param param}
                                   required (assoc :apex/error {:apex.error/message "Required parameter missing"}))])))))

           ;; Default is to pass acc long to the next parameter
           acc))

       {:apex/qsm qsm}
       ;; Reduce over paramdefs
       paramdefs)

      extract-undeclared-params
      ))))

(defn process-path-parameters
  ([params paramdefs]
   (process-path-parameters params paramdefs {}))
  ([params paramdefs
    {:keys [coercions]
     :or {coercions default-coercions}}]
   (into
    {}
    (reduce                             ; reduce over paramdefs
     (fn [acc {:strs [name required schema] :as paramdef}]
       (let [[pkey pval] (find params name)]
         (if pkey
           (let [validation
                 (jinx.validate/validate schema pval {:coercions coercions})]
             (assoc
              acc
              name
              (cond-> {:param paramdef
                       :validation validation}

                (::jinx/valid? validation)
                (assoc :value (::jinx/instance validation))

                (not (::jinx/valid? validation))
                (assoc :apex/error
                       {:apex.error/message "Path parameter not valid according to schema"}))))

           (assoc acc name (cond-> {:param paramdef}
                             required (assoc :apex/error {:apex.error/message "Required parameter not found"}))))))
     {}
     paramdefs))))


(defn extract-params-from-request
  [req param-defs]
  (let [query-param-defs (not-empty (filter #(= (get % "in") "query") param-defs))
        path-param-defs (not-empty (filter #(= (get % "in") "path") param-defs))]
    (cond-> {}
      query-param-defs
      (assoc :query (process-query-string (:query-string req) query-param-defs))
      path-param-defs
      (assoc :path (process-path-parameters (:path-params req) path-param-defs)))))
