;; Copyright © 2020-2021, JUXT LTD.

(ns juxt.spin.alpha.auth
  (:require
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]
   [juxt.spin.alpha :as spin]
   [juxt.reap.alpha.encoders :as enc]))

(defmulti decode-authorization (fn [{:juxt.reap.alpha.rfc7235/keys [auth-scheme]}] auth-scheme))

(defmethod decode-authorization "Basic" [{:juxt.reap.alpha.rfc7235/keys [token68]}]
  (try
    (let [[_ user password]
          (re-matches #"([^:]*):([^:]*)"
                      (String. (.decode (java.util.Base64/getDecoder) token68)))]
      {::user user
       ::password password})
    (catch Exception e nil)))

(defn decode-authorization-header [request]
  (when-let [authorization-header (get-in request [:headers "authorization"])]
    (let [{::rfc7235/keys [auth-scheme] :as m}
          (reap/authorization authorization-header)]
      (when-let [m (decode-authorization m)]
        (into
         {::spin/auth-scheme auth-scheme} m)))))

(defmulti encode-challenge (fn [{::spin/keys [authentication-scheme]}] authentication-scheme))

(defmethod encode-challenge "Basic" [{::spin/keys [realm]}]
  #::rfc7235{:auth-scheme "Basic"
             :auth-params
             [#::rfc7235{:auth-param-name "realm",
                         :auth-param-value (str \" realm \")}]})

(defn www-authenticate [schemes]
  (enc/www-authenticate
   (mapv encode-challenge schemes)))
