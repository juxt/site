;; Copyright © 2022, JUXT LTD.

(ns juxt.sci-test
  (:refer-clojure :exclude [dosynctx])
  (:require
   [clojure.test :refer [deftest is are testing]]
   [clojure.walk :refer [prewalk]]
   [sci.core :as sci]))

(declare ctx)

(def ctx
  (sci/init
   {:namespaces
    {'clojure.core
     {'dosync
      ^:sci/macro
      (fn [_&form _&env & body]
        #_(println _&form)
        #_(throw (ex-info "here" {:form _&form :env _&env
                                  :body body
                                  ;;:ctx ctx
                                  }))
        (let [form2 (prewalk
                     (fn [x] (cond-> x (symbol? x) (get _&env x)))
                     (second _&form))]
          (println form2)
          (sci.core/eval-form ctx form2)))}}}))

(sci/binding [sci/out *out*]
  (sci/eval-string*
   ctx
   (pr-str
    '(do
       (let [a 1
             b 2]
         (+ a b)
         (let [c 3]
           (dosync
            (let [z2 (dec a)] z2))
           ))
       ))

   ))
