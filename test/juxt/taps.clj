;; Copyright © 2022, JUXT LTD.

(ns juxt.taps
  (:require  [clojure.test :refer [deftest is are testing]]))

(defn list-taps []
  @(deref #'clojure.core/tapset))

(defn remove-all-taps []
  (doseq [tap (list-taps)]
    (remove-tap tap)))

(defmacro with-portal-tap [& body]
  `(let [f# (fn [value#] (portal.api/submit value#))]
     (try
       (add-tap f#)
       (tap> "testing testing")
       (do
         ~@body)
       (finally
         (remove-tap f#)))))

(remove-all-taps)
(list-taps)

(add-tap (fn [v] (portal.api/submit v)))

(tap> "foo")

(macroexpand `(with-portal-tap
                (tap> "foo")))

(with-portal-tap
              (tap> "foo"))

(remove-tap #'portal.api/submit)
(portal.api/tap)

(list-taps)

(tap> "foo")
