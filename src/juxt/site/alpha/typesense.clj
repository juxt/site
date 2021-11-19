(ns juxt.site.alpha.typesense
  (:require
   [typesense.client :as client]
   [juxt.site.alpha.util :refer [assoc-some]]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]))

(def ts-settings {:uri "http://localhost:8108" :key "xyz"})

(defn operator->string
  [operator]
  (case operator
    "_eq" ":= "
    (throw (ex-info "Unkown operator in typesense filter" {:operator operator}))))

(defn process-filters
  [filters]
  (let [filter-array
        (for [filter filters
              :let [[filter-key filter] filter
                    [operator value] filter]]
          (str (name filter-key) (operator->string operator) value))]
    (str/join " && " filter-array)))

(defn update-doc-keys
  "Renames the keys inside each hit document that comes from typesense so site can
  handle it up the chain"
  [results]
  (update results :hits (fn [hits]
                          (mapv (fn [hit]
                                  (update hit :document
                                          #(rename-keys
                                            % {:id :xt/id
                                               :type :juxt.site/type})))
                                hits))))

(defn ts-search
  [site-args argument-values]
  (let [limit (or (get argument-values "limit") 10)
        offset (or (get argument-values "offset") 0)
        config (get site-args "typesenseConfig")
        collection (:typesenseIndex config)
        filters (get site-args "filters")
        processed-filters (process-filters filters)
        results
        (first
         (client/search ts-settings collection
                        (merge
                         {:filter_by processed-filters
                          :per_page limit
                          :page (+ 1 (long (/ offset limit)))}
                         (when (get argument-values "disableFuzzy")
                           {:prefix false
                            :typo_tokens_threshold 0
                            :drop_tokens_threshold 0})
                         (assoc-some config
                                     :q (or (get argument-values "query") "*")
                                     :filter_by (get argument-values "filter_by")
                                     :sort_by (get argument-values "sort_by")
                                     :group_by (get argument-values "group_by")
                                     :group_limit (get argument-values "group_limit")
                                     :query_by (get argument-values "query_by")))))]
    (update-doc-keys results)))
