;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.representation-generation
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]
   [hiccup.page :as hp]
   [json-html.core :refer [edn->html]]
   [jsonista.core :as json]
   [juxt.apex.alpha.jsonpointer :refer [json-pointer]]
   [juxt.apex.alpha.parameters :as parameters]
   [juxt.jinx.alpha :as jinx]
   [juxt.pass.alpha.pdp :as pdp]
   [juxt.site.alpha.graphql :as graphql]
   [juxt.site.alpha.response :as response]
   [juxt.site.alpha.return :refer [return]]
   [juxt.site.alpha.selmer :as selmer]
   [juxt.site.alpha.util :as util]
   [xtdb.api :as xt]
   selmer.parser))


(alias 'jinx (create-ns 'juxt.jinx.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn ->inject-params-into-query [input params req]

  ;; Replace input with values from params
  (postwalk
   (fn [x]
     (cond
       (and (map? x)
            (contains? x :name)
            (#{"query" "path"} (:in x "query")))
       (let [kw (keyword (:in x "query"))]
         (or
          (get-in params [kw (:name x) :value])
          (:default x)
          (get-in params [kw (:name x) :param "default"])))

       (and (map? x)
            (contains? x :juxt.site.alpha/ref))
       (get-in req (get x :juxt.site.alpha/ref))

       :else x))
   input))

(defn entity-body
  [{::site/keys [selected-representation resource db base-uri] :as req}
   resource-state]

  (def req req)
  (def selected-representation selected-representation)

  (case (::http/content-type selected-representation)
    "application/json"
    (-> resource-state
        (json/write-value-as-string (json/object-mapper {:pretty true}))
        (str "\r\n")
        (.getBytes "UTF-8"))

    "text/html;charset=utf-8"
    (let [template (some->
                    selected-representation
                    (get "x-juxt-site-template")
                    (selmer.parser/render {:base-uri base-uri}))]
      (cond
        template
        (selmer/render-file
         template
         resource-state
         db
         (get selected-representation "x-juxt-site-template-custom-resource-path")
         (response/xt-template-loader req db))

        :else
        (->
         (hp/html5
          [:h1 (get selected-representation "title" "No title")]

          [:pre (pr-str selected-representation)]

          ;; Get :path-params = {"id" "owners"}

          (cond
            (= (get selected-representation "type") "edn-table")
            (list
             [:style
              (slurp (io/resource "json.human.css"))]
             (edn->html resource-state))

            (= (get selected-representation "type") "table")
            (if (seq resource-state)
              (let [fields (distinct (concat [:xt/id]
                                             (keys (first resource-state))))]
                [:table {:style "border: 1px solid #888; border-collapse: collapse;"}
                 [:thead
                  [:tr
                   (for [field fields]
                     [:th {:style "border: 1px solid #888; padding: 4pt; text-align: left"}
                      (pr-str field)])]]
                 [:tbody
                  (for [row resource-state]
                    [:tr
                     (for [field fields
                           :let [val (get row field)]]
                       [:td {:style "border: 1px solid #888; padding: 4pt; text-align: left"}
                        (cond
                          (uri? val)
                          [:a {:href val} val]
                          :else
                          (pr-str (get row field)))])])]])
              [:p "No results"])

            :else
            (let [fields (distinct (concat [:xt/id] (keys resource-state)))]
              [:dl
               (for [field fields
                     :let [val (get resource-state field)]]
                 (list
                  [:dt
                   (pr-str field)]
                  [:dd
                   (cond
                     (uri? val)
                     [:a {:href val} val]
                     :else
                     (pr-str (get resource-state field)))]))]))

          [:h3 "Resource state"]
          [:pre (with-out-str (pprint resource-state))])
         (.getBytes "UTF-8"))))

    (return
     req
     500
     "Cannot produce entity body of content-type %s"
     {:representation selected-representation}
     (::http/content-type selected-representation))))


(defn deep-merge
  [a b]
  (if (map? a)
    (into a (for [[k v] b] [k (deep-merge (a k) v)]))
    b))

;; By default we output the resource state, but there may be a better rendering
;; of collections, which can be inferred from the schema being an array and use
;; the items subschema.

(defn find-path-by-operation-id [openapi operation-id]
  (let [paths (get openapi "paths")]
    (some
     (fn [[path path-object]]
       (some
        (fn [[method op]]
          (when (= (get op "operationId") operation-id)
            {:method method :path path})) path-object))
     paths)))

(defn expand-path [path resolve-param]
  (assert (string? path))
  (assert (ifn? resolve-param))
  (str/replace path #"\{([\p{Alnum}]+)\}" (fn [[_ param]] (resolve-param param))))

(comment
  (expand-path "/~{juxtcode}/employment-record/{foo}" str/reverse))

(defn deref-operation [openapi resolve-param]
  (fn [x]
    (let [server (get-in openapi ["servers" 0 "url"])]
      (or
       (when (map? x)
         (when-let [operation-ref (get x "x-juxt-site-operation-ref")]
           (when-let [{:keys [path]} (find-path-by-operation-id openapi operation-ref)]
             (when path
               (str server (expand-path path resolve-param))))))
       x))))

(defn process-merge-resource-state [merge-resource-state openapi resolve-param]
  (postwalk (deref-operation openapi resolve-param) merge-resource-state))

;; This generates representations
(defn entity-bytes-generator
  [{::site/keys [uri resource db]
    ::pass/keys [subject] :as req}]

  (let [authorization (::pass/authorization resource)
        _ (assert authorization "No authorization to generate entity payload")

        lookup (fn [id] (xt/entity db id))
        param-defs
        (get-in resource [::apex/operation "parameters"])

        openapi (delay (some-> resource ::apex/openapi lookup ::apex/openapi))

        db (xt/with-tx db [[:xtdb.api/put
                            (-> subject
                                (assoc :xt/id :subject)
                                util/->freezeable)]])

        query-params (when-let [query-param-defs
                                (not-empty (filter #(= (get % "in") "query") param-defs))]
                       (parameters/process-query-string
                        (:ring.request/query req)
                        query-param-defs))

        query
        (some->
         (get-in resource [::apex/operation "responses" "200" "juxt.site.alpha/query"])
         (edn/read-string)
         (->inject-params-into-query {:path (::apex/openapi-path-params resource)
                                      :query query-params} req)
         (pdp/->authorized-query authorization))

        {singular-result? "juxt.site.alpha/singular-result?"
         extract-first-projection? "juxt.site.alpha/extract-first-projection?"
         extract-entry "juxt.site.alpha/extract-entry"}
        (get-in resource [::apex/operation "responses" "200"])

        resource-state
        (or
         (when query
           (try
             (cond->> (xt/q db query)
               extract-first-projection? (map first)
               extract-entry (map #(get % extract-entry))
               singular-result? first)
             (catch Exception e
               (throw (ex-info "Failure during XTDB query" {:query query} e)))))

         ;; Try to get the resource's state via GraphQL
         (when-let [stored-query-resource-path (get-in resource [::apex/operation "x-juxt-site-graphql-query-resource"])]
           (let [stored-query-id (if (.startsWith stored-query-resource-path "/")
                                   (str (:juxt.site.alpha/base-uri req) stored-query-resource-path)
                                   stored-query-resource-path)
                 operation-name (or
                                 (get-in resource [::apex/operation "x-juxt-site-graphql-operation-name"])
                                 (get-in resource [::apex/operation "operationId"]))
                 variables
                 (into
                  (graphql/common-variables req)
                  (for [[k v] (::apex/openapi-path-params resource)
                        :when (::jinx/valid? v)]
                    [k (:value v)]))]

             (when operation-name
               (let [result (graphql/graphql-query
                             req
                             stored-query-id operation-name variables)]

                 ;; TODO: Need to check if the result indicates that nothing was found
                 ;; Perhaps the test should be if all values in the :data map are nil
                 (when (seq (:errors result))
                   (log/trace (first (:errors result)))
                   (throw
                    (ex-info
                     (format "Getting the state for the resource via GraphQL resulted in %d errors" (count (:errors result)))
                     {::stored-query-id stored-query-id
                      ::operation-name operation-name
                      ::variables variables
                      ::errors (:errors result)
                      ::data (:data result)})))

                 (let [check (get-in resource [::apex/operation "x-juxt-site-data-exists-if"])]
                   (when (or (not check) (json-pointer (:data result) check))
                     (if-let [resource-state (:data result)]

                       (let [resource-state
                             ;; Push the resource-state through jsonista and back. We do this
                             ;; jsonista expands out objects like StackTraceElement[], whereas
                             ;; selmer just calls a .toString. This also makes the resource state
                             ;; going into the tempate aligned with the body of the
                             ;; application/json representation.
                             (-> resource-state
                                 json/write-value-as-bytes
                                 json/read-value)

                             ;; If present, this data can be merged
                             merge-resource-state
                             (some->
                              resource
                              (get-in [::apex/operation "x-juxt-site-merge-resource-state"])
                              (process-merge-resource-state
                               @openapi
                               (fn [param]
                                 (let [path-param (get-in resource [::apex/openapi-path-params param])]
                                   (when (::jinx/valid? path-param)
                                     (:value path-param))))))]

                         (log/tracef "merge-resource-state: %s" (pr-str merge-resource-state))

                         (cond-> resource-state
                           merge-resource-state (deep-merge merge-resource-state)))

                       (throw (ex-info "Data from result is nil"
                                       {:stored-query-id stored-query-id
                                        :operation-name operation-name
                                        :variables variables})))))))))

         ;; The resource-state is the entity, if found. TODO: Might want to
         ;; filter out the http metadata?
         (do
           (log/tracef "Looking up resource-state in DB with uri %s" uri)
           (xt/entity db uri))

         ;; No resource-state, so there can be no representations, so 404.
         (throw
          (ex-info
           (str "No resource state from OpenAPI path: " (::apex/openapi-path req))
           {::apex/openapi-path (::apex/openapi-path req)
            ::site/request-context (assoc req :ring.response/status 404)})))]

    ;; Now we've found the resource state, we need to generate a representation
    ;; of it.
    (entity-body req resource-state)))
