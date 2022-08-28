;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.schema
  (:require
   [malli.core :as m]
   [juxt.site.alpha :as-alias site])
  ;;(:import (xtdb.query QueryDatasource))
  )

(def schema-registry
  {::site/db :any #_[:fn (fn [db]
                          (not (nil? db))
                          ;;(instance? QueryDatasource db)
                          )]
   ::site/resource [:map [:xt/id]]
   :xt/id [:string {:min 5}]})
