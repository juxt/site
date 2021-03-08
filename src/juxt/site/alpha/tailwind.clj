;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.tailwind)

(defn link [href text]
  [:a
   {:href href
    :class "text-yellow-800"}
   text])
