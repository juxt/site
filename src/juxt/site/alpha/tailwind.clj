;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.tailwind)

(defn relativize [s]
  (cond-> s
    (.startsWith s "https://home.juxt.site")
    (subs (count "https://home.juxt.site"))))

(defn link [href text]
  [:a
   {:href (relativize href)
    :class "text-yellow-800"}
   text])
