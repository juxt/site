;; Copyright © 2022, JUXT LTD.

(ns juxt.site.return-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [juxt.site.return :refer [return]]
   [juxt.site :as-alias site]))

(deftest ok-return
  (let [ex-data
        (try
          (return {:method :get} 200 "OK" {})
          {}
          (catch clojure.lang.ExceptionInfo e
            (ex-data e)))]
    (is (= 200 (get-in ex-data [::site/request-context :ring.response/status])))))

(deftest ok-message-arg-return
  (let [msg
        (try
          (return {:method :get} 200 "OK: %s" {} "foo")
          (assert false)
          (catch clojure.lang.ExceptionInfo e
            (.getMessage e)))]
    (is (= "OK: foo" msg))))

(deftest redirect-return
  (let [ex-data
        (try
          (return {:method :get} 302 "Redirect" {::site/request-context {:ring.response/headers {"Location" "foo"}}})
          (assert false)
          (catch clojure.lang.ExceptionInfo e
            (ex-data e)))]
    (is (= 302 (get-in ex-data [::site/request-context :ring.response/status])))
    (is (= {"Location" "foo"} (get-in ex-data [::site/request-context :ring.response/headers])))))

(deftest ex-data-return
  (let [ex-data
        (try
          (return {:method :get} 500 "Error" {:foo "bar"})
          (assert false)
          (catch clojure.lang.ExceptionInfo e
            (ex-data e)))]
    (is (= "bar" (get ex-data :foo)))))


(deftest return-with-merged-request-context-entries
  (let [ex-data
        (try
          (return {:foo "foo"} 500 "Error" {::site/request-context {:bar "bar"}})
          (assert false)
          (catch clojure.lang.ExceptionInfo e
            (ex-data e)))]
    (is (= {:foo "foo" :bar "bar" :ring.response/status 500} (get-in ex-data [::site/request-context])))))

(deftest return-with-headers-merged
  (let [ex-data
        (try
          (return {:ring.response/headers {"Foo" "foo"}} 500 "Error" {:ring.response/headers {"Bar" "bar"}})
          (assert false)
          (catch clojure.lang.ExceptionInfo e
            (ex-data e)))]
    (is (= {"Foo" "foo" "Bar" "bar"} (get-in ex-data [::site/request-context :ring.response/headers])))))
