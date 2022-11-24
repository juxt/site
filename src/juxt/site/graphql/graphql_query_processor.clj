;; Copyright © 2022, JUXT LTD.

(ns juxt.site.graphql.graphql-query-processor
  (:require
   [xtdb.api :as xt]
   [juxt.site.graphql.graphql-compiler :as gcompiler]
   [juxt.site.actions :as actions]))

(defn graphql-query->xtdb-query
  "Transforms a graphql query string into an xtdb query

  Arguments:
    A GraphQL query string
    A GraphQL schema compiled with juxt.grab
    An XTDB db

  Note:
    Currently only supports where there is a single query as the
    first element of the query string"
  [query-string compiled-schema db]
  (let [query-document (gcompiler/query->query-doc query-string compiled-schema)
        query-operation    (first (get-in query-document [:juxt.grab.alpha.document/operations]))
        root-selection-set (get query-operation :juxt.grab.alpha.graphql/selection-set)
        action-rules       (->> (gcompiler/query-doc->actions query-document compiled-schema)
                                (actions/actions->rules db))
        built-query        (gcompiler/build-query-for-selection-set
                            (first root-selection-set)
                            compiled-schema
                            action-rules
                            false)]
    built-query))


(defn site-graphql-xtdb-result->graphql-result
  "Transforms data from the result of a site graphql query to graphql structure."
  [data]
  (let [grouped-by-id (group-by first data)]
    (set
     (map
      (fn [[_ grp]]
        (reduce
         (fn [acc [_ local-fields remote-fields]]
           (let [remote-fields-merged-down
                 (update-vals remote-fields site-graphql-xtdb-result->graphql-result)
                 local-fields-merged (merge acc local-fields)]
             (merge-with into local-fields-merged remote-fields-merged-down)))
         {}
         grp))
      grouped-by-id))))

(defn run
  "Runs a graphql query string against a site xtdb db

  Arguments:
    A GraphQL query string
    A GraphQL schema compiled with juxt.grab
    An XTDB db
    A site subject
    A site purpose

  Note:
    Currently only supports where there is a single query as the
    first element of the query string"
  [query-string db schema & query-args]
  (-> query-string
      (graphql-query->xtdb-query schema db)
      vector
      (into query-args)
      ((partial apply (partial xt/q db)))
      site-graphql-xtdb-result->graphql-result))
