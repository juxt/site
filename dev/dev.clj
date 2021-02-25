;; Copyright © 2021, JUXT LTD.

(ns dev
  (:require
   [dev-extras :refer :all]
   [juxt.site.alpha.dev-extras :refer :all]
   [juxt.site.alpha.init :refer [init-db!]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(set! *print-length* 100)
(set! *print-level* 4)
