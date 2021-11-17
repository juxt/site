(ns juxt.site.alpha.typesense
  (:require
   [typesense.client :as client]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]))

(def ts-settings {:uri "http://192.168.1.26:8108" :key "xyz"})

(defn prepare-doc
  [{:keys [document highlights]} found out-of]
  (-> document
      (assoc :found found
             :outOf out-of
             :matched (flatten (map :matched_tokens highlights)))
      (rename-keys {:id :xt/id :type :juxt.site/type})))

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

(defn ts-search
  [collection site-args argument-values]
  (let [search-terms (get argument-values "searchTerms")
        limit (get argument-values "limit")
        offset (get argument-values "offset")
        query (or (some->> search-terms vals (str/join " ")) "*")
        query-by (some->> search-terms keys (map name) (str/join ", "))
        config (get site-args "typesenseConfig")
        _ (def config config)
        filters (get site-args "filters")
        processed-filters (process-filters filters)
        {:keys [hits found out_of] :as results}
        (first
         (client/search ts-settings collection
                        (merge
                         {:q (if (seq query) query "*")
                          :query_by query-by
                          :filter_by processed-filters
                          :per_page limit
                          :page (+ 1 (/ (or offset 0) limit))}
                         config)))]
    (def results results)
    (map (fn [hit] (prepare-doc hit found out_of)) hits)))

(defn count
  [collection search-terms]
  (:found (first (client/search ts-settings collection {:q "*" :per_page 1 :page 1}))))
