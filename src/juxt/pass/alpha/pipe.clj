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

(defmulti word (fn [_ [k] ctx] k))

;; ctx must contain :db
(defmethod word ::validate [m [_ schema] ctx]
  (let [schema (m/schema schema)]
    (if-not (m/validate schema m)
      ;; Not sure why Malli throws this error here: No implementation of
      ;; method: :-form of protocol: #'malli.core/Schema found for class: clojure.lang.PersistentVector
      ;;
      ;; Workaround is to pr-str and read-string
      (throw
       (ex-info
        "Failed validation check"
        (read-string (pr-str (m/explain schema m)))))
      m)))

(defmethod word ::nest [m [_ k] ctx]
  {k m})

(defmethod word ::merge [m [_ m2] ctx]
  (merge m m2))

(defmethod word ::dissoc [m [_ & ks] ctx]
  (apply dissoc m ks))

(defmethod word ::find-matching-identity-on-password
  [m [_ k {:keys [username-in-identity-key path-to-username
                  password-hash-in-identity-key path-to-password]}]
   ctx]
  (assert (:db ctx))
  (let [identity
        (first
         (map first
              (xt/q
               (:db ctx)
               {:find '[e]
                :where [
                        ['e username-in-identity-key 'username]
                        ['e password-hash-in-identity-key 'password-hash]
                        ['(crypto.password.bcrypt/check password password-hash)]]
                :in '[username password]}
               (get-in m path-to-username)
               (get-in m path-to-password))))]
    (if identity
      (assoc m k identity)
      (throw (ex-info "Login failed" {:username (get-in m path-to-username)})))))

(defmethod word ::db-single-put
  [m _ _]
  [[:xtdb.api/put m]])

(defmethod word ::abort [m _ _]
  (throw (ex-info "Abort" {:value m})))

(defn pipe [steps seed ctx]
  (reduce (fn [acc step] (word acc step ctx)) seed (filter some? steps)))
