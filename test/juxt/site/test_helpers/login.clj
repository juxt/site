;; Copyright © 2022, JUXT LTD.

(ns juxt.site.test-helpers.login
  (:require
   [clojure.java.io :as io]
   [malli.core :as malli]
   [ring.util.codec :as codec]))

(defn login-with-form!
  "Return a session id (or nil) given a map of fields."
  [handler & {:as args}]
  {:pre [(malli/validate
          [:map
           [:juxt.site/uri [:re "https://.*"]]
           ["username" [:string {:min 2}]]
           ["password" [:string {:min 6}]]] args)]}
  (let [form (codec/form-encode (dissoc args :juxt.site/uri))
        body (.getBytes form)
        req {:juxt.site/uri (:juxt.site/uri args)
             :ring.request/method :post
             :ring.request/headers
             {"content-length" (str (count body))
              "content-type" "application/x-www-form-urlencoded"}
             :ring.request/body (io/input-stream body)}
        response (handler req)
        {:strs [set-cookie]} (:ring.response/headers response)
        [_ id] (when set-cookie (re-matches #"[a-z]+=(.*?);.*" set-cookie))]
    (when-not id
      (throw
       (ex-info
        (format "Login failed: %s" (String. (:ring.response/body response)))
        {:args args
         :response response})))
    {:juxt.site/session-token id}))
