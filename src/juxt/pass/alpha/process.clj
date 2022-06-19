;; Copyright © 2022, JUXT LTD.

;; Deprecated: obsoleted by flip.clj

(ns juxt.pass.alpha.process
  (:require
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [malli.core :as m]
   [malli.error :a me]
   [xtdb.api :as xt]))

(defn resolve-with-ctx [form ctx]
  (postwalk
   (fn [x]
     (if (and (vector? x) (= (first x) ::pass/resolve))
       (ctx (second x))
       x))
   form))

(defmulti apply-processor (fn [processor acc] (first processor)))

(defmethod apply-processor :default [[kw] acc]
  (throw (ex-info (format "No processor for %s" kw) {:kw kw})))

(defmethod apply-processor :juxt.pass.alpha.process/update-in [[kw ks f-sym & update-in-args] acc]
  (assert (vector? (:args acc)))
  (let [f (case f-sym 'merge merge nil)]
    (when-not f
      (throw (ex-info "Unsupported update-in function" {:f f-sym})))
    (apply update acc :args update-in ks f (resolve-with-ctx update-in-args (:ctx acc)))))

(defmethod apply-processor :xtdb.api/put [[kw ks] acc]
  (assoc acc :ops (mapv (fn [arg] [::xt/put arg]) (:args acc))))

(defmethod apply-processor ::pass.malli/validate [_ acc]
  (let [{::pass.malli/keys [args-schema]} (:action acc)
        resolved-args-schema (resolve-with-ctx args-schema (:ctx acc))]
    (when-not (m/validate resolved-args-schema (:args acc))
      (throw
       (ex-info
        "Failed validation check"
        ;; Not sure why Malli throws this error here: No implementation of
        ;; method: :-form of protocol: #'malli.core/Schema found for class: clojure.lang.PersistentVector
        ;;
        ;; Workaround is to pr-str and read-string
        (read-string (pr-str (m/explain resolved-args-schema (:args acc))))))))
  acc)

(defmethod apply-processor :gen-hex-string [[_ k size] acc]
  (update acc :ctx assoc k (as-hex-str (random-bytes size))))

(defmethod apply-processor :add-prefix [[_ k prefix] acc]
  (update acc :ctx update k (fn [old] (str prefix old))))

(defn process-args [pass-ctx action args]
  (reduce
   (fn [acc processor]
     (apply-processor processor acc))
   {:args args
    :ctx pass-ctx
    :action action}
   (::pass/process action)))
