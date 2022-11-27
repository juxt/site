;; Copyright © 2022, JUXT LTD.

(ns juxt.site.logging
  (:import (org.slf4j MDC)))

(defmacro with-logging [& body]
  `(try
     (MDC/put "logging" "on")
    ~@body
    (finally
      (MDC/remove "logging"))))
