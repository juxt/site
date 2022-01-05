;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.code
  (:require [clojure.tools.logging :as log]))

(alias 'site (create-ns 'juxt.site.alpha))

(defn put-handler [req]
  (let [clojure-str (some-> req :juxt.site.alpha/received-representation :juxt.http.alpha/body (String.))]
    (try
      (load-string clojure-str)
      (catch Exception e
        (throw
         (ex-info
          "Compilation error"
          {::site/request-context (assoc req :ring.response/status 400)}
          e))))

    (assoc req
           :ring.response/status 200
           :ring.response/body "Code compiled successfully")))
