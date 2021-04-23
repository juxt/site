;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.resources
  (:require
   [clojure.edn :as edn]
   [crux.api :as x]
   [clojure.java.io :as io]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn read-forms [r]
  (lazy-seq
   (let [res (edn/read {:eof :eof} r)]
     (when-not (= res :eof)
       (cons res (read-forms r))))))

(defn post-resource
  "Post a new resource, or overwrite an existing one, in the database."
  [{::site/keys [db crux-node]
    ::apex/keys [request-instance] :as req}]

  (case (::http/content-type request-instance)
    "application/edn"

    (let [resources (read-forms
                     (java.io.PushbackReader.
                      (java.io.InputStreamReader.
                       (java.io.ByteArrayInputStream.
                        (::http/body request-instance)))))]

      (->>
       (x/submit-tx crux-node (mapv (fn [resource] [:crux.tx/put resource]) resources))
       (x/await-tx crux-node))

      (assoc req :ring.response/status 200))))
