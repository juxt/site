;; Copyright © 2022, JUXT LTD.

(ns juxt.pipe.alpha.core
  ;; When promoting this ns, move the defmethods that require all these
  ;; dependencies:
  (:require
   [clojure.walk :refer [postwalk]]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.pass.alpha.malli :as-alias pass.malli]
   [juxt.pass.alpha.util :refer [make-nonce]]
   [malli.core :as m]
   [malli.error :a me]
   [xtdb.api :as xt]))

;; See Factor, https://factorcode.org/
;; See K's XY language, https://www.nsl.com/k/xy/xy.htm

(defmulti word
  (fn [stack [word & queue] env]
    ;; A word can be a keyword, or a vector containing a keyword and any
    ;; arguments.
    (if (keyword? word) word (first word))))

(defmethod word :break [stack queue env]
  ;; Don't include the environment, this error may be logged
  (throw (ex-info "BREAK" {:stack stack :queue queue})))

;; no-op is identity
(defmethod word :no-op [stack queue env]
  [stack queue env])

(defmethod word :push [stack [[_ & els] & queue] env]
  [(reduce (fn [acc e] (cons e acc)) stack els) queue env])

(defmethod word :new-map [[n & stack] [_ & queue] env]
  (let [[els stack] (split-at (* 2 n) stack)]
    [(cons (apply hash-map els) stack) queue env]))

;; hashtables
(defmethod word :associate [[v k & stack] [_ & queue] env]
  [(cons {k v} stack) queue env])

(defmethod word :first [[coll & stack] [_ & queue] env]
  [(cons (first coll) stack) queue env])

;; ctx must contain :db
(defmethod word :validate [stack [[_ schema] & queue] env]
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

;; Shuffle words - see
;; https://docs.factorcode.org/content/article-shuffle-words.html

;; drop
;; 2drop
;; 3drop
;; nip
;; 2nip

(defmethod word :dup [[el & stack] [_ & queue] env]
  [(cons el (cons el stack)) queue env])

;; 2dup
;; 3dup

(defmethod word :over [[el & stack] [_ & queue] env]
  [(cons el (cons el stack)) queue env])
;; 2over
;; pick

(defmethod word :swap [[x y & stack] [_ & queue] env]
  [(cons y (cons x stack)) queue env])

(defmethod word :of [[k m & stack] [_ & queue] env]
  (assert (map? m))
  [(cons (get m k) stack) queue env])

(defmethod word :of [[k m & stack] [_ & queue] env]
  (if (find m k)
    [(cons (get m k) stack) queue env]
    (throw (ex-info "Error, failed to find key" {:k k}))))

(declare pipe)

(defmethod word :dip [[x & stack] [[_ quotation] & queue] env]
  (let [stack (pipe stack quotation env)]
    [(cons x stack) queue env]))

(defmethod word :pipe.xtdb/q
  [[q & stack] [_ & queue] env]
  (assert (map? q))
  (assert (:db env))
  (let [db (:db env)
        [in stack] (split-at (count (:in q)) stack)
        results (apply xt/q db q in)]
    [(cons results stack) queue env]))

(defmethod word :set-at
  [[v k m & stack] [_ & queue] env]
  [(cons (assoc m k v) stack) queue env])

(defmethod word :random-bytes
  [[size & stack] [_ & queue] env]
  [(cons (random-bytes size) stack) queue env])

(defmethod word :as-hex-string
  [[bytes & stack] [_ & queue] env]
  [(cons (as-hex-str bytes) stack) queue env])

(defmethod word :str
  [[s1 s2 & stack] [_ & queue] env]
  [(cons (str s1 s2) stack) queue env])

;; This could be in another ns
(defmethod word :find-matching-identity-on-password-query
  [stack
   [[_ {:keys [username-in-identity-key password-hash-in-identity-key]}]
    & queue] env]
  [(cons {:find '[e]
          :where [
                  ['e username-in-identity-key 'username]
                  ['e password-hash-in-identity-key 'password-hash]
                  ['(crypto.password.bcrypt/check password password-hash)]]
          :in '[username password]} stack) queue env])

(defmethod word :make-nonce
  [[size & stack] [_ & queue] env]
  [(cons (make-nonce size) stack) queue env])

(defmethod word :xtdb.api/put
  [[doc & stack] [_ & queue] env]
  [(cons [:xtdb.api/put doc] stack) queue env])

(defn pipe [stack queue env]
  ;; Naiive implementation. A production implementation would put an upper limit
  ;; on the number of iterations to prevent overly long running transactions.

  ;; For performance optimization, consider using a transient or a
  ;; java.util.Deque for both stack and queue. Since neither the stack nor queue
  ;; escape the pipe, and the pipe is run in a single-thread, the data
  ;; structures can be transient. However, see
  ;; https://clojure.org/reference/transients that claims that lists cannot be
  ;; made transient "as there is no benefit to be had.". So lists may be already
  ;; fast enough.

  (loop [[stack queue env] [stack queue env]]
    (assert list? stack)
    (assert list? queue)
    (assert map? env)
    (if (seq queue)
      (recur (word stack queue env))
      stack)))
