;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.perf)

(defmacro fast-get-in
  "In his ClojuTre 2019 talk, Tommi Riemann says that `->` is
  signifantly faster than `get-in`."
  ([m args]
   `(fast-get-in ~m ~args nil))
  ([m args not-found]
   `(let [res# (-> ~m ~@(for [i args] (if (keyword? i) `~i `(get ~i))))]
      (if res# res# ~not-found))))
