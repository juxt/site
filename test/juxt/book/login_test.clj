;; Copyright © 2022, JUXT LTD.

(ns juxt.book.login-test
  (:require
   [juxt.flip.alpha.core :as f]
   [juxt.book.login :refer [login-quotation]]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]
   [ring.util.codec :as codec]))

#_(with-fixtures
  (with-resources
    #{}
    (f/eval-quotation
     []
     (::f/quotation login-quotation)
     {::site/db (xt/db *xt-node*)
      ::site/received-representation
      {::http/body (codec/form-encode {"username" "alice" "password" "garden"})
       (.getBytes "type Query { myName String }")
       }}
     )))
