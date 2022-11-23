;; Copyright © 2022, JUXT LTD.

(ns juxt.site.logging
  (:require [clojure.tools.logging :as log]))

(defmacro with-logging [& body]
  `(try
     (org.slf4j.MDC/put "logging" "on")
    ~@body
    (finally
      (org.slf4j.MDC/remove "logging"))))
