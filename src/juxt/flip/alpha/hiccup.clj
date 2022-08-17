;; Copyright © 2022, JUXT LTD.

(ns juxt.flip.alpha.hiccup
  (:require
   [hiccup2.core :as h]
   [juxt.flip.alpha.core :as f]
   ))

(def html `html)
(defmethod f/word `html [[content & stack] [_ & queue] env]
  [(cons (h/html content) stack) queue env])

(def raw `raw)
(defmethod f/word `raw [[content & stack] [_ & queue] env]
  [(cons (h/raw content) stack) queue env])
