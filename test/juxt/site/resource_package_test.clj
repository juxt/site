;; Copyright © 2022, JUXT LTD.

(ns juxt.site.resource-package-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is are testing]]
   [juxt.site.resource-package :as pkg]
   [juxt.site.repl :as repl]))

(deftest apply-uri-map-test
  (testing "Fail when uri-map not satisfied"
    (is
     (thrown-with-msg?
      clojure.lang.ExceptionInfo
      #"uri-map is missing some required keys"
      (pkg/apply-uri-map
       (edn/read-string (slurp "resources/whoami/index.edn"))
       {"https://example.org" "https://example.test"})))))
