;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.schema
  (:require
   [malli.core :as m]
   [malli.registry :as mr]
   [juxt.site.alpha :as-alias site]
   juxt.pass.alpha.schema)
  ;;(:import (xtdb.query QueryDatasource))
  )

(def schema-registry
  {::site/db :any #_[:fn (fn [db]
                          (not (nil? db))
                          ;;(instance? QueryDatasource db)
                          )]
   ::site/resource [:map [:xt/id]]
   :xt/id [:string {:min 5}]})

(mr/set-default-registry!
   (mr/composite-registry
    (m/default-schemas)
    juxt.pass.alpha.schema/schema-registry
    schema-registry))
