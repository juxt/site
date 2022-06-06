;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.pipe
  (:require
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [malli.core :as m]
   [malli.error :a me]
   [xtdb.api :as xt]))

;; See Factor, https://factorcode.org/
;; See K's XY language, http://www.nsl.com/k/xy/xy.htm

(defmulti word
  (fn [stack queue env]
    (ffirst queue)))

;; nil is identity
(defmethod word nil [stack queue env]
  [stack queue env])

(defmethod word ::push [stack [[_ & els] & queue] env]
  [(reduce (fn [acc e] (cons e acc)) stack els) queue env])

(defmethod word ::new-map [[n & stack] [_ & queue] env]
  (let [[els stack] (split-at (* 2 n) stack)]
    [(cons (apply hash-map els) stack) queue env]))

(defmethod word ::nest [stack [[_ k] & queue] env]
  [stack (concat (list [::push k]
                       [::push 1]
                       [::new-map])
                 queue) env])

(defmethod word ::first [[coll & stack] [_ & queue] env]
  [(cons (first coll) stack) queue env])

;; ctx must contain :db
(defmethod word ::validate [stack [[_ schema] & queue] env]
  (let [schema (m/schema schema)]
    (if-not (m/validate schema (first stack))
      ;; Not sure why Malli throws this error here: No implementation of
      ;; method: :-form of protocol: #'malli.core/Schema found for class: clojure.lang.PersistentVector
      ;;
      ;; Workaround is to pr-str and read-string
      (throw
       (ex-info
        "Failed validation check"
        (read-string (pr-str (m/explain schema (first stack))))))
      [stack queue env])))

#_(defmethod word ::merge [m [_ m2] ctx]
  (merge m m2))

#_(defmethod word ::dissoc [m [_ & ks] ctx]
  (apply dissoc m ks))

(defmethod word ::dup [[el & stack] [_ & queue] env]
  [(cons el (cons el stack)) queue env])

(defmethod word ::of [[k m & stack] [_ & queue] env]
  (assert (map? m))
  [(cons (get m k) stack) queue env])

(defmethod word ::?of [[k m & stack] [_ & queue] env]
  (if (find m k)
    [(cons (get m k) stack) queue env]
    (throw (ex-info "Error, failed to find key" {:k k}))))

(declare pipe)

(defmethod word ::dip [[x & stack] [[_ subqueue] & queue] env]
  (let [stack (pipe stack subqueue env)]
    [(cons x stack) queue env]))

(defmethod word ::find-matching-identity-on-password-query
  [stack
   [[_ {:keys [username-in-identity-key password-hash-in-identity-key]}]
    & queue] env]
  [(cons {:find '[e]
          :where [
                  ['e username-in-identity-key 'username]
                  ['e password-hash-in-identity-key 'password-hash]
                  ['(crypto.password.bcrypt/check password password-hash)]]
          :in '[username password]} stack) queue env])

(defmethod word ::xtdb-query
  [[q & stack] [_ & queue] env]
  (assert (map? q))
  (assert (:db env))
  (let [db (:db env)
        [in stack] (split-at (count (:in q)) stack)
        results (apply xt/q db q in)]
    (if (seq results)
      [(cons results stack) queue env]
      (throw (ex-info "Error, query didn't return any results" {})))))

(defmethod word ::set-at
  [[v k m & stack] [_ & queue] env]
  [(cons (assoc m k v) stack) queue env])

(defmethod word ::random-bytes
  [[size & stack] [_ & queue] env]
  [(cons (random-bytes size) stack) queue env])

(defmethod word ::as-hex-string
  [[bytes & stack] [_ & queue] env]
  [(cons (as-hex-str bytes) stack) queue env])

(defmethod word ::str
  [[s1 s2 & stack] [_ & queue] env]
  [(cons (str s1 s2) stack) queue env])

#_(defmethod word ::find-matching-identity-on-password
    [[path-to-password path-to-username & stack]
     [[_ {:keys [username-in-identity-key path-to-username
                 password-hash-in-identity-key path-to-password]}]
      & queue]
     env]
    (assert (:db env))
    (let [identity
          (first
           (map first
                (xt/q
                 (:db env)
                 {:find '[e]
                  :where [
                          ['e username-in-identity-key 'username]
                          ['e password-hash-in-identity-key 'password-hash]
                          ['(crypto.password.bcrypt/check password password-hash)]]
                  :in '[username password]}
                 (get-in m path-to-username)
                 (get-in m path-to-password))))]
      (if identity
        [(cons identity stack) queue env]
        (throw (ex-info "Login failed" {:username (get-in m path-to-username)})))))

#_(defmethod word ::db-single-put
  [m _ _]
  [[:xtdb.api/put m]])

#_(defmethod word ::abort [m _ _]
  (throw (ex-info "Abort" {:value m})))

(defn pipe [stack queue env]
  ;; Naiive implementation. A production implementation would put an upper limit
  ;; on the number of iterations to prevent overly long running transactions.
  (loop [[stack queue env] [stack queue env]]
    (assert list? stack)
    (assert list? queue)
    (assert map? env)
    (if (seq queue)
      (recur (word stack queue env))
      stack)))
