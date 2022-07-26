;; Copyright © 2022, JUXT LTD.

(ns juxt.flip.alpha.core
  ;; When promoting this ns, move the defmethods that require all these
  ;; dependencies:
  (:refer-clojure :exclude [+ first second symbol drop keep when str ex-info any? filter map])
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   jsonista.core
   juxt.pass.alpha.util
   [juxt.site.alpha :as-alias site]
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [crypto.password.bcrypt :as bcrypt]
   [malli.core :as m]
   [malli.error :a me]
   [ring.util.codec :as codec]
   [xtdb.api :as xt]
   [juxt.flip.alpha.core :as f]))

;; See Factor, https://factorcode.org/
;; See K's XY language, https://www.nsl.com/k/xy/xy.htm

(defmulti word
  (fn [stack [word & queue] env]
    ;; A word can be a symbol, or a vector containing a symbol and any
    ;; arguments.
    (if (symbol? word)
      word
      (clojure.core/first word)))
  :default ::default)

(defmethod word ::default [stack [name & queue] env]
  (if-let [quotation (get-in env [:definitions name])]
    ;;(throw (clojure.core/ex-info "lookup definition" {:quotation quotation}))
    [stack (concat quotation queue) env]
    ;; Don't apply, simply treat as a symbol. We might be in the process of
    ;; defining a word.
    (clojure.core/or

     ;; Clojure interop - but should be subject to a whitelist!!
     (clojure.core/when-let [var (requiring-resolve name)]
       (clojure.core/when-let [arglists (:arglists (clojure.core/meta var))]
         (if (= (clojure.core/count arglists) 1)
           (let [[args stack] (clojure.core/split-at (clojure.core/count (clojure.core/first arglists)) stack)]
             [(clojure.core/cons (clojure.core/apply var (clojure.core/reverse args)) stack) queue env])
           (throw (clojure.core/ex-info (format "Clojure function has multiple forms: %s" name) {:symbol name})))))

     ;; define
     (clojure.core/when
         (clojure.core/and
          (clojure.core/vector? (clojure.core/first queue))
          (= (clojure.core/second queue) 'juxt.flip.alpha.core/define))
         [(cons name stack) queue env])

     (throw (clojure.core/ex-info (format "Symbol not defined: %s" name) {:symbol name})))))

(def break 'juxt.flip.alpha.core/break)
(defmethod word 'juxt.flip.alpha.core/break [stack [_ & queue] env]
  (throw
   (clojure.core/ex-info
    "BREAK"
    {:juxt.flip.alpha/stack stack
     :juxt.flip.alpha/queue queue})))

;; no-op is identity
(def no-op 'juxt.flip.alpha.core/no-op)
(defmethod word 'juxt.flip.alpha.core/no-op
  [stack [_ & queue] env]
  [stack queue env])

;; TODO: What is the Factor equivalent name?
(def env 'juxt.flip.alpha.core/env)
(defmethod word 'juxt.flip.alpha.core/env
  [[el & stack] [_ & queue] env]
  [(cons (get env el) stack) queue env])

;; TODO: What is the Factor equivalent name?
(defmethod word 'juxt.flip.alpha.xtdb/entity
  [[id & stack] [_ & queue] {::site/keys [db] :as env}]
  (if-let [e (xt/entity db id)]
    [(cons e stack) queue env]
    ;; TODO: Arguably the developer's decision - add a word that throws if
    ;; there's a nil at the top of the stack
    (throw (clojure.core/ex-info "No such entity" {:id id}))))

;; TODO: What is the Factor equivalent name?
(def call 'juxt.flip.alpha.core/call)
(defmethod word 'juxt.flip.alpha.core/call
  [[quotation & stack] [_ & queue] env]
  (assert list? quotation)
  [stack (concat quotation queue) env])

;; TODO: What is the Factor equivalent name, if any?
(def bytes-to-string 'juxt.flip.alpha.core/bytes-to-string)
(defmethod word 'juxt.flip.alpha.core/bytes-to-string
  [[bytes & stack] [_ & queue] env]
  (assert bytes)
  [(cons (String. bytes) stack) queue env])

;; TODO: What is the Factor equivalent name, if any?
(defmethod word 'juxt.flip.alpha.edn/read-string
  [[el & stack] [_ & queue] env]
  [(cons (edn/read-string el) stack) queue env])

(defmethod word 'juxt.flip.alpha.hashtables/associate
  [[k v & stack] [_ & queue] env]
  [(cons {k v} stack) queue env])

(def first 'juxt.flip.alpha.core/first)
(defmethod word 'juxt.flip.alpha.core/first
  [[coll & stack] [_ & queue] env]
  [(cons (clojure.core/first coll) stack) queue env])

(def second 'juxt.flip.alpha.core/second)
(defmethod word 'juxt.flip.alpha.core/second
  [[coll & stack] [_ & queue] env]
  [(cons (clojure.core/second coll) stack) queue env])

(def symbol 'juxt.flip.alpha.core/symbol)
(defmethod word 'juxt.flip.alpha.core/symbol
  [[coll & stack] [_ & queue] env]
  [(cons (clojure.core/symbol coll) stack) queue env])

(def _3vector 'juxt.flip.alpha.core/_3vector)
(defmethod word 'juxt.flip.alpha.core/_3vector
  [[z y x & stack] [_ & queue] env]
  [(cons (vector x y z) stack) queue env])

(def _2vector 'juxt.flip.alpha.core/_2vector)
(defmethod word 'juxt.flip.alpha.core/_2vector
  [[y x & stack] [_ & queue] env]
  [(cons (vector x y) stack) queue env])

(def _1vector 'juxt.flip.alpha.core/_1vector)
(defmethod word 'juxt.flip.alpha.core/_1vector
  [[x & stack] [_ & queue] env]
  [(cons (vector x) stack) queue env])

(def <vector> 'juxt.flip.alpha.core/<vector>)
(defmethod word 'juxt.flip.alpha.core/<vector>
  [[_ & stack] [_ & queue] env]
  [(cons (vector) stack) queue env])

(def append 'juxt.flip.alpha.core/append)
(defmethod word 'juxt.flip.alpha.core/append
  [[seq2 seq1 & stack] [_ & queue] env]
  [(cons (cond->> (concat seq1 seq2)
           (vector? seq1) vec) stack) queue env])

(def push 'juxt.flip.alpha.core/push)
(defmethod word 'juxt.flip.alpha.core/push
  [[seq elt & stack] [_ & queue] env]
  [(cons (cond (vector? seq) (conj seq elt)
               (list? seq) (concat seq [elt])) stack)
   queue env])

(def >list 'juxt.flip.alpha.core/>list)
(defmethod word 'juxt.flip.alpha.core/>list
  [[sequence & stack] [_ & queue] env]
  [(cons (apply list sequence) stack) queue env])

(def >vector 'juxt.flip.alpha.core/>vector)
(defmethod word 'juxt.flip.alpha.core/>vector
  [[sequence & stack] [_ & queue] env]
  [(cons (apply vector sequence) stack) queue env])

(def >lower 'juxt.flip.alpha.core/>lower)
(defmethod word 'juxt.flip.alpha.core/>lower
  [[s & stack] [_ & queue] env]
  [(cons (clojure.string/lower-case s) stack) queue env])

(defmethod word 'juxt.site.alpha/validate
  [[schema & stack] [_ & queue] env]
  (let [schema (m/schema schema)]
    (if-not (m/validate schema (clojure.core/first stack))
      ;; Not sure why Malli throws this error here: No implementation of
      ;; method: :-form of protocol: #'malli.core/Schema found for class: clojure.lang.PersistentVector
      ;;
      ;; Workaround is to pr-str and read-string
      (throw
       (clojure.core/ex-info
        "Failed validation check"
        (read-string (pr-str (m/explain schema (clojure.core/first stack))))))
      [stack queue env])))

;; Shuffle words - see
;; https://docs.factorcode.org/content/article-shuffle-words.html

(def drop 'juxt.flip.alpha.core/drop)
(defmethod word 'juxt.flip.alpha.core/drop
  [[_ & stack] [_ & queue] env]
  [stack queue env])

;; 2drop
;; 3drop

(def nip 'juxt.flip.alpha.core/nip)
(defmethod word 'juxt.flip.alpha.core/nip
  [[y _ & stack] [_ & queue] env]
  [(cons y stack) queue env])

;; 2nip

(def dup 'juxt.flip.alpha.core/dup)
(defmethod word 'juxt.flip.alpha.core/dup
  [[el & stack] [_ & queue] env]
  [(cons el (cons el stack)) queue env])

;; 2dup
;; 3dup

(def over 'juxt.flip.alpha.core/over)
(defmethod word 'juxt.flip.alpha.core/over
  [[y x & stack] [_ & queue] env]
  [(cons x (cons y (cons x stack))) queue env])

;; 2over

(def pick 'juxt.flip.alpha.core/pick)
(defmethod word 'juxt.flip.alpha.core/pick
  [[z y x & stack] [_ & queue] env]
  [(cons x (cons z (cons y (cons x stack)))) queue env])

(def swap 'juxt.flip.alpha.core/swap)
(defmethod word 'juxt.flip.alpha.core/swap
  [[x y & stack] [_ & queue] env]
  [(cons y (cons x stack)) queue env])

(def of 'juxt.flip.alpha.core/of)
(defmethod word 'juxt.flip.alpha.core/of
  [[k m & stack] [_ & queue] env]
  [(cons (get m k) stack) queue env])

(def rot 'juxt.flip.alpha.core/rot)
(defmethod word 'juxt.flip.alpha.core/rot
  [[z y x & stack] [_ & queue] env]
  [(cons x (cons z (cons y stack))) queue env])

(declare eval-quotation)

(def dip 'juxt.flip.alpha.core/dip)
(defmethod word 'juxt.flip.alpha.core/dip
  [[quot x & stack] [_ & queue] env]
  (let [stack (eval-quotation stack quot env)]
    [(cons x stack) queue env]))

(def _2dip 'juxt.flip.alpha.core/_2dip)
(defmethod word 'juxt.flip.alpha.core/_2dip
  [[quot y x & stack] [_ & queue] env]
  (let [stack (eval-quotation stack quot env)]
    [(cons y (cons x stack)) queue env]))

(def keep 'juxt.flip.alpha.core/keep)
(defmethod word 'juxt.flip.alpha.core/keep
  [[quot x & stack] [_ & queue] env]
  [(cons x (eval-quotation (cons x stack) quot env)) queue env])

(def if 'juxt.flip.alpha.core/if)
(defmethod word 'juxt.flip.alpha.core/if
  [[f t ? & stack] [_ & queue] env]
  (assert (vector? t) "Expecting t to be a quotation")
  (assert (vector? f) "Expecting f to be a quotation")
  [stack (concat (if ? t f) queue) env])

;; "Alternative conditional form that preserves the cond value if it is true."
(def if* 'juxt.flip.alpha.core/if*)
(defmethod word 'juxt.flip.alpha.core/if*
  [[f t ? & stack] [_ & queue] env]
  (assert (vector? t) "Expecting t to be a quotation")
  (assert (vector? f) "Expecting f to be a quotation")
  (if ?
    [(cons ? stack) (concat t queue) env]
    [stack (concat f queue) env]))

(def when 'juxt.flip.alpha.core/when)
(defmethod word 'juxt.flip.alpha.core/when
  [[t ? & stack] [_ & queue] env]
  (assert (vector? t) "Expecting t to be a quotation")
  [stack (concat (clojure.core/when ? t) queue) env])

(def when* 'juxt.flip.alpha.core/when*)
(defmethod word 'juxt.flip.alpha.core/when*
  [[t ? & stack] [_ & queue] env]
  (assert (vector? t) "Expecting t to be a quotation")
  (if ?
    [(cons ? stack) (concat t queue) env]
    [stack queue env]))

(def unless 'juxt.flip.alpha.core/unless)
(defmethod word 'juxt.flip.alpha.core/unless
  [[f ? & stack] [_ & queue] env]
  [stack (concat (clojure.core/when-not ? f) queue) env])

(def unless* 'juxt.flip.alpha.core/unless*)
(defmethod word 'juxt.flip.alpha.core/unless*
  [[f ? & stack] [_ & queue] env]
  (assert (vector? f) "Expecting f to be a quotation")
  (if-not ?
    [(cons ? stack) (concat f queue) env]
    [(cons ? stack) queue env]))

(def + 'juxt.flip.alpha.core/+)
(defmethod word 'juxt.flip.alpha.core/+
  [[y x & stack] [_ & queue] env]
  [(cons (clojure.core/+ x y) stack) queue env])

(def <array-map> 'juxt.flip.alpha.core/<array-map>)
(defmethod word 'juxt.flip.alpha.core/<array-map>
  [stack [_ & queue] env]
  [(cons (array-map) stack) queue env])

(def <sorted-map> 'juxt.flip.alpha.core/<sorted-map>)
(defmethod word 'juxt.flip.alpha.core/<sorted-map>
  [stack [_ & queue] env]
  [(cons (sorted-map) stack) queue env])

(def set-at 'juxt.flip.alpha.core/set-at)
(defmethod word 'juxt.flip.alpha.core/set-at
  [[m k v & stack] [_ & queue] env]
  [(cons (assoc m k v) stack) queue env])

(def delete-at 'juxt.flip.alpha.core/delete-at)
(defmethod word 'juxt.flip.alpha.core/delete-at
  [[m k & stack] [_ & queue] env]
  [(cons (dissoc m k) stack) queue env])

;; Sequence combinators
;; https://docs.factorcode.org/content/article-sequences-combinators.html

;; each, reduce, map, etc.

;; https://docs.factorcode.org/content/word-map,sequences.html
(def map 'juxt.flip.alpha.core/map)
(defmethod word 'juxt.flip.alpha.core/map
  [[quot seq & stack] [_ & queue] env]
  (let [subseq (clojure.core/map
                (fn [el] (clojure.core/first (eval-quotation (cons el stack) quot env))) seq)]
    [(cons subseq stack) queue env]))

;; https://docs.factorcode.org/content/word-filter,sequences.html
(def filter 'juxt.flip.alpha.core/filter)
(defmethod word 'juxt.flip.alpha.core/filter
  [[quot seq & stack] [_ & queue] env]
  (let [subseq (clojure.core/filter
                (fn [el] (clojure.core/first (eval-quotation (cons el stack) quot env))) seq)]
    [(cons subseq stack) queue env]))

;; https://docs.factorcode.org/content/word-any__que__%2Csequences.html
(def any? 'juxt.flip.alpha.core/any?)
(defmethod word 'juxt.flip.alpha.core/any?
  [[quot seq & stack] [_ & queue] env]
  (let [result (clojure.core/some
                (fn [el] (clojure.core/first (eval-quotation (cons el stack) quot env))) seq)]
    [(cons (some? result) stack) queue env]))

;; Regex
(def <regex> 'juxt.flip.alpha.core/<regex>)
(defmethod word 'juxt.flip.alpha.core/<regex>
  [[string & stack] [_ & queue] env]
  [(cons (re-pattern string) stack) queue env])

(def matches? 'juxt.flip.alpha.core/matches?)
(defmethod word 'juxt.flip.alpha.core/matches?
  [[regexp string & stack] [_ & queue] env]
  (assert (instance? java.util.regex.Pattern regexp))
  [(cons (clojure.core/re-matches regexp string) stack) queue env])

(defmethod word 'juxt.pass.alpha/random-bytes
  [[size & stack] [_ & queue] env]
  [(cons (random-bytes size) stack) queue env])

(defmethod word 'juxt.pass.alpha/as-hex-str
  [[bytes & stack] [_ & queue] env]
  [(cons (as-hex-str bytes) stack) queue env])

(def str 'juxt.flip.alpha.core/str)
(defmethod word 'juxt.flip.alpha.core/str
  [[s1 s2 & stack] [_ & queue] env]
  [(cons (clojure.core/str s1 s2) stack) queue env])

(defmethod word 'juxt.flip.alpha.core/form-decode
  [[encoded & stack] [_ & queue] env]
  (if encoded
    [(cons (codec/form-decode encoded) stack)
     queue
     env]
    (throw (clojure.core/ex-info "String to decode is null" {}))))

(defmethod word 'juxt.flip.alpha.core/form-encode
  [[m & stack] [_ & queue] env]
  [(cons (codec/form-encode m) stack)
   queue
   env])

(defmethod word 'juxt.flip.alpha.core/split
  [[regexp string & stack] [_ & queue] env]
  [(clojure.core/cons (str/split string regexp) stack) queue env])

(def in 'juxt.flip.alpha.core/in?)
(defmethod word 'juxt.flip.alpha.core/in?
  [[set elt & stack] [_ & queue] env]
  [(cons (contains? (clojure.core/set set) elt) stack) queue env])

(defmethod word 'juxt.flip.alpha.core/assoc-filter
  [[quot assoc & stack] [_ & queue] env]
  (let [subassoc (clojure.core/into
                  {}
                  (clojure.core/filter
                   (fn [[k v]]
                     (clojure.core/first
                      (eval-quotation
                       (clojure.core/cons v (clojure.core/cons k stack)) quot env)))
                   assoc))]
    [(cons subassoc stack) queue env]))

(def push-at 'juxt.flip.alpha.core/push-at)
;; I think the documentation is wrong here: https://docs.factorcode.org/content/word-push-at%2Cassocs.html
;; I think there must be an output of the modified assoc.
(defmethod word 'juxt.flip.alpha.core/push-at
  [[assoc key value & stack] [_ & queue] env]
  [(clojure.core/cons (clojure.core/update assoc key (clojure.core/fnil conj []) value) stack)
   queue env])

#_(defmethod word 'juxt.pass.alpha/find-matching-identity-on-password-query
  [[{:keys [username-in-identity-key password-hash-in-identity-key]} & stack]
   [_ & queue] env]
  [(cons {:find '[e]
          :where [
                  ['e username-in-identity-key 'username]
                  ['e password-hash-in-identity-key 'password-hash]
                  ['(crypto.password.bcrypt/check password password-hash)]]
          :in '[username password]} stack) queue env])

(defmethod word 'juxt.pass.alpha/encrypt-password
  [[password & stack] [_ & queue] env]
  [(cons (bcrypt/encrypt password) stack) queue env])

(def make-nonce 'juxt.pass.alpha/make-nonce)
(defmethod word 'juxt.pass.alpha/make-nonce
  [[size & stack] [_ & queue] env]
  [(cons (juxt.pass.alpha.util/make-nonce size) stack) queue env])

;; Errors

(def ex-info 'juxt.flip.alpha.core/ex-info)
(defmethod word 'juxt.flip.alpha.core/ex-info
  [[ex-data msg & stack] [_ & queue] env]
  [(cons (clojure.core/ex-info
          msg
          (assoc
           ex-data
           :juxt.flip.alpha/stack stack
           :juxt.flip.alpha/queue queue))
         stack)
   queue env])

;; TODO: This is more of an 'exit'
(def throw-exception 'juxt.flip.alpha.core/throw-exception)
(defmethod word 'juxt.flip.alpha.core/throw-exception
  [[err & stack] [_ & queue] env]
  (throw err))

(defmethod word 'juxt.flip.alpha.core/throw
  [[error & stack] [_ & queue] env]
  (throw (clojure.core/ex-info "error" {::error error})))

(defmethod word 'juxt.flip.alpha.core/recover
  [[recovery try* & stack] [_ & queue] env]
  (try
    [(eval-quotation stack try* env) queue env]
    (catch clojure.lang.ExceptionInfo e
      ;; If this is thrown by flip, we can call the recover quotation
      (if-let [error (::error (ex-data e))]
        [(eval-quotation (cons error stack) recovery env) queue env]
        (throw e)))))

;; XTDB

(defmethod word 'juxt.flip.alpha.xtdb/q
  [[q & stack] [_ & queue] env]
  (assert (map? q))
  (assert (::site/db env))
  (let [db (::site/db env)
        [in stack] (split-at (count (:in q)) stack)
        results (apply xt/q db q in)]
    [(cons results stack) queue env]))

(defmethod word 'xtdb.api/put
  [[doc & stack] [_ & queue] env]
  [(cons [:xtdb.api/put doc] stack) queue env])

(defmethod word 'juxt.site.alpha/fx
  [[fx doc & stack] [_ & queue] env]
  [(cons [fx doc] stack) queue env])

;; Site

(defmethod word 'juxt.site.alpha/lookup
  [[val attr typ & stack] [_ & queue] env]
  [(cons {:find '[(pull e [*])]
          :where [['e :juxt.site.alpha/type typ]
                  ['e attr val]]
          } stack) queue env])

(defmethod word 'juxt.site.alpha/entity
  [[id & stack] [_ & queue] env]
  [(cons (xt/entity (::site/db env) id) stack) queue env])

(defmethod word 'jsonista.core/read-string
  [[s & stack] [_ & queue] env]
  [(cons (jsonista.core/read-value s) stack) queue env])

;; Some friends from Clojure

(defmethod word 'juxt.flip.clojure.core/assoc
  [[v k m & stack] [_ & queue] env]
  [(cons (clojure.core/assoc m k v) stack) queue env])

;; Convenience words

(defmethod word 'juxt.site.alpha/set-type
  [[typ m & stack] [_ & queue] env]
  [(cons (assoc m :juxt.site.alpha/type typ) stack) queue env])

(defmethod word 'juxt.site.alpha/set-methods
  [[methods m & stack] [_ & queue] env]
  [(cons (assoc m :juxt.site.alpha/methods methods) stack) queue env])

(defmethod word 'juxt.site.alpha/request-body-as-edn
  [stack [_ & queue] env]
  [stack (concat `(:juxt.site.alpha/received-representation
                   env :juxt.http.alpha/body
                   of
                   ;; TODO: Test if nil and throw an exception
                   bytes-to-string
                   juxt.flip.alpha.edn/read-string) queue) env])

(defmethod word 'juxt.site.alpha/request-body-as-json
  [stack [_ & queue] env]
  [stack (concat `(:juxt.site.alpha/received-representation
                   env :juxt.http.alpha/body
                   of
                   bytes-to-string
                   jsonista.core/read-string) queue) env])

#_(defmethod word 'juxt.site.alpha/request-query-string
  [stack [_ & queue] env]
  [stack (concat `( :ring.request/query env

                   jsonista.core/read-string) queue) env])

(defmethod word 'juxt.site.alpha/apply-to-request-context
  [stack [_ & queue] env]
  [stack
   (concat
    `[:juxt.site.alpha/apply-to-request-context
      swap _2vector] queue)
   env])

(defmethod word 'juxt.site.alpha/push-fx
  [stack [_ & queue] env]
  [stack
   (concat
    `[::site/fx juxt.flip.alpha.core/swap juxt.flip.alpha.core/push-at] queue)
   env])

(defmethod word 'juxt.site.alpha/with-fx-acc
  [[quot & stack] [_ & queue] env]
  [stack
   (concat `[[] ::site/fx <sorted-map> set-at ~quot call] queue)
   env])

;; Create an apply-to-request-context operation that sets a header
;; (header-name value -- op)
(defmethod word 'juxt.site.alpha/set-header
  [stack [_ & queue] env]
  [stack
   (concat
    `[(symbol "juxt.flip.alpha.core/dup")
      _1vector

      (symbol "juxt.flip.alpha.core/of")
      :ring.response/headers
      _2vector >list
      swap push

      (symbol "juxt.flip.alpha.core/if*")
      _1vector
      0
      <vector>
      swap push

      (symbol "juxt.flip.alpha.core/<array-map>")
      _1vector
      swap push
      >list
      swap push

      push                              ; the value on the stack
      push                              ; the header name
      (symbol "juxt.flip.alpha.core/rot")
      swap push
      (symbol "juxt.flip.alpha.core/set-at")
      swap push

      :ring.response/headers
      swap push
      (symbol "juxt.flip.alpha.core/rot")
      swap push
      (symbol "juxt.flip.alpha.core/set-at")
      swap push

      juxt.site.alpha/apply-to-request-context] queue)
   env])

(defmethod word 'juxt.site.alpha/set-status
  [stack [_ & queue] env]
  [stack
   (concat
    `[_1vector
      :ring.response/status
      swap push
      (symbol "juxt.flip.alpha.core/rot")
      swap push
      (symbol "juxt.flip.alpha.core/set-at")
      swap push
      juxt.site.alpha/apply-to-request-context]
    queue)
   env])

(defmethod word 'juxt.flip.alpha.core/define
  [[quotation n & stack] [_ & queue] env]
  [stack queue (clojure.core/assoc-in env [:definitions n] quotation)])

(defn word* [stack [w & queue] env]
  (cond
    (symbol? w)
    (word stack (clojure.core/cons w queue) env)
    (seq? w)                            ; switch from postfix to prefix notation
    (if (= (clojure.core/first w) 'quote)
      ;; Allow quotation
      [(cons (clojure.core/first (rest w)) stack) queue env]
      [stack (clojure.core/concat
              (clojure.core/rest w)
              (clojure.core/cons (clojure.core/first w) queue)) env])
    :else
    [(cons w stack) queue env]))

(defn try-word*
  [stack queue env]
  (try
    (word* stack queue env)
    (catch Throwable t
      (throw
       (clojure.core/ex-info
        (format "Failure in quotation: %s" (.getMessage t))
        (or (ex-data t) {})
        t)))))

(defn eval-quotation
  ;; Naiive implementation. A production implementation would put an upper limit
  ;; on the number of iterations to prevent overly long running transactions.

  ;; For performance optimization, consider using a transient or a
  ;; java.util.Deque for both stack and queue. Since neither the stack nor queue
  ;; escape, and is run in a single-thread, the data structures can be
  ;; transient. However, see https://clojure.org/reference/transients that
  ;; claims that lists cannot be made transient "as there is no benefit to be
  ;; had.". So lists may be already fast enough.

  ([stack queue]
   (eval-quotation stack queue {}))
  ([stack queue env]
   (loop [[stack queue env] [stack queue env]]
     (assert list? stack)
     (assert list? queue)
     (assert map? env)
     (if (seq queue)
       (recur (try-word* stack queue env))
       stack))))


(defmethod word 'juxt.flip.alpha.core/tap-stack-from-here
  [stack [_ & queue] env]
  [stack (interpose 'tap-stack queue) env])

(defmethod word 'juxt.flip.alpha.core/tap-no-more
  [stack [_ & queue] env]
  [stack (remove #(= 'tap-stack %) queue) env])

(defmethod word 'juxt.flip.alpha.core/tap-stack
  [stack [_ & queue] env]
  (tap> stack)
  [stack queue env])

(defmethod word 'juxt.flip.alpha.core/tap-stack-with-label
  [[label & stack] [_ & queue] env]
  (tap> {:label label :stack stack})
  [stack queue env])

(defmethod word 'juxt.flip.alpha.core/tap-queue [stack [_ & queue] env]
  (tap> queue)
  [stack queue env])

(defmethod word 'juxt.flip.alpha.core/tap-env [stack [_ & queue] env]
  (tap> env)
  [stack queue env])

(defmethod word 'juxt.flip.alpha.core/tap-everything [stack [_ & queue] env]
  (tap> {:stack stack
         :queue queue
         :env env})
  [stack queue env])

(defmethod word 'juxt.flip.alpha.core/tap-from-here
  [[tap-word & stack] [_ & queue] env]
  [stack (interpose tap-word queue) env])
