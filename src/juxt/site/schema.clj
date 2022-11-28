;; Copyright © 2022, JUXT LTD.

(ns juxt.site.schema
  (:require
   [malli.core :as m]
   [malli.registry :as mr]))

(def schema-registry
  {:juxt.site/db :any #_[:fn (fn [db]
                               (not (nil? db))
                               ;;(instance? QueryDatasource db)
                               )]
   :juxt.site/resource [:map [:xt/id]]
   :juxt.site/subject (m/schema [:map [:xt/id [:string {:min 1}]]])
   :juxt.site/purpose [:string {:min 1}]
   :xt/id [:string {:min 5}]})

(mr/set-default-registry!
   (mr/composite-registry
    (m/default-schemas)
    schema-registry))
