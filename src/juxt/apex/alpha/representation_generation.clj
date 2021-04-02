;; Copyright © 2021, JUXT LTD.

(ns juxt.apex.alpha.representation-generation
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [postwalk]]
   [crux.api :as x]
   [hiccup.page :as hp]
   [json-html.core :refer [edn->html]]
   [jsonista.core :as json]
   [juxt.apex.alpha.parameters :refer [extract-params-from-request]]
   [juxt.pass.alpha.pdp :as pdp]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn ->query [input params]

  ;; Replace input with values from params
  (let [input
        (postwalk
         (fn [x]
           (if (and (map? x)
                    (contains? x "name")
                    (= (get x "in") "query"))
             (get-in params [:query (get x "name") :value]
                     (get-in params [:query (get x "name") :param "default"]))
             x))
         input)]

    ;; Perform manipulations required for each key
    (reduce
     (fn [acc [k v]]
       (assoc acc (keyword k)
              (case (keyword k)
                :find (mapv symbol v)
                :where (mapv
                        ;; We're using some inline recursion to keep things lean-ish here
                        ;; based on the assumption we can bump our stack _a little_ higher
                        ;; and that our clauses will remain fairly simple
                        (fn translate-clause [clause]
                          (cond
                            (and (vector? clause) (every? (comp not coll?) clause))
                            (mapv (fn [item txf] (txf item)) clause [symbol keyword symbol])

                            (and (vector? clause) (vector? (second clause)))
                            (cons (symbol (first clause))
                                  (map translate-clause (rest clause)))

                            (and (vector? clause) (vector? (first clause)))
                            [(seq (map symbol (first clause)))]))
                        v)

                :limit v
                :in (mapv symbol v)
                :args (mapv clojure.walk/keywordize-keys v)
                )))
     {} input)))

;; By default we output the resource state, but there may be a better rendering
;; of collections, which can be inferred from the schema being an array and use
;; the items subschema.
(defn entity-bytes-generator [{::site/keys [uri resource selected-representation db]
                               ::pass/keys [authorization subject] :as req}]

  (let [param-defs
        (get-in resource [::apex/operation "parameters"])

        in '[now subject]

        query
        (get-in resource [::apex/operation "responses" "200" "crux/query"])

        crux-query
        (when query (->query query (extract-params-from-request req param-defs)))

        authorized-query (when crux-query
                           ;; This is just temporary, in future, fail if no
                           ;; authorization. We just need to make sure there's
                           ;; an authorization for the subject.
                           (if authorization
                             (pdp/->authorized-query crux-query authorization)
                             crux-query))

        authorized-query (when authorized-query
                           (assoc authorized-query :in in))

        resource-state
        (if authorized-query
          (for [[e] (x/q db authorized-query
                         ;; time now
                         (java.util.Date.)
                         ;; subject
                         subject)]
            (x/entity db e))
          (x/entity db uri))]

    ;; TODO: Might want to filter out the http metadata at some point
    (case (::http/content-type selected-representation)
      "application/json"
      ;; TODO: Might want to filter out the http metadata at some point
      (-> resource-state
          (json/write-value-as-string (json/object-mapper {:pretty true}))
          (str "\r\n")
          (.getBytes "UTF-8"))

      "text/html;charset=utf-8"
      (let [config (get-in resource [::apex/operation "responses"
                                     "200" "content"
                                     (::http/content-type selected-representation)])]
        (->
         (hp/html5
          [:h1 (get config "title" "No title")]

          ;; Get :path-params = {"id" "owners"}

          (cond
            (= (get config "type") "edn-table")
            (list
             [:style
              (slurp (io/resource "json.human.css"))]
             (edn->html resource-state))

            (= (get config "type") "table")
            (if (seq resource-state)
              (let [fields (distinct (concat [:crux.db/id]
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
            (let [fields (distinct (concat [:crux.db/id] (keys resource-state)))]
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

          [:h2 "Debug"]
          [:h3 "Resource"]
          [:pre (with-out-str (pprint resource))]
          (when query
            (list
             [:h3 "Query"]
             [:pre (with-out-str (pprint query))]))
          (when crux-query
            (list
             [:h3 "Crux Query"]
             [:pre (with-out-str (pprint (->query query (extract-params-from-request req param-defs))))]))

          (when (seq param-defs)
            (list
             [:h3 "Parameters"]
             [:pre (with-out-str (pprint (extract-params-from-request req param-defs)))]))

          [:h3 "Resource state"]
          [:pre (with-out-str (pprint resource-state))])
         (.getBytes "UTF-8"))))))
