;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.xtdb)

(defn inline-clj-pred [f & args]
  (apply (eval f) (vec args)))

#_(inline-clj-pred
 (fn [coll]
    (map (fn [x] (dissoc x :user)) coll))
 #{{:user "ken" :color "red"}})

#_(eval
 (list apply
       '(fn [coll]
          (map (fn [x] (dissoc x :user)) coll))

       [#{
          {:user "ken" :color "red"}}]))
