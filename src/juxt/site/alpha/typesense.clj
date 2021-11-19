(ns juxt.site.alpha.typesense
  (:require
   [typesense.client :as client]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]))

(def ts-settings {:uri "http://192.168.1.26:8108" :key "xyz"})

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

(defn search-terms->query
  "Given a map of search terms, creates a typesense q and query_by value following
  the following algorithm: If search-terms contains a single key named 'search',
  assume :query_by will be set manually by the client. Otherwise, use the keys
  as the input to 'query-by'"
  [search-terms]
  (let [query (or (some->> search-terms vals (str/join " ")) "*")
        query-by (some->> search-terms
                          keys
                          (remove #{"search"})
                          (map name)
                          (str/join ", "))]
    {:q (if (seq query) query "*")
     :query_by query-by}))

(defn update-doc-keys
  "Renames the keys inside each hit document that comes from typesense so site can
  handle it up the chain"
  [results]
  (def results results)
  (update results :hits (fn [hits]
                          (mapv (fn [hit]
                                  (update hit :document
                                          #(rename-keys
                                            % {:id :xt/id
                                               :type :juxt.site/type})))
                                hits))))

(defn ts-search
  [site-args argument-values]
  (def site-args site-args)
  (let [search-terms (get argument-values "searchTerms")
        limit (or (get argument-values "limit") 10)
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
                         (search-terms->query search-terms)
                         config)))]
    (update-doc-keys results)))

(defn count
  [collection search-terms]
  (:found (first
           (client/search
            ts-settings collection (merge (search-terms->query search-terms)
                                          {:per_page 1 :page 1})))))
