;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.resources
  (:require
   [clojure.edn :as edn]
   [crux.api :as x]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

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
                        (::http/body request-instance)))))
          results
          (doall
           (for [resource resources]
             (let [uri (:crux.db/id resource)
                   existing? (when uri (x/entity db uri))]
               (try
                 (let [tx (x/submit-tx crux-node [[:crux.tx/put resource]])]
                   (cond-> {:status (if existing? 204 201)
                            :tx tx}
                     uri (assoc :uri uri)))
                 (catch Exception e
                   (log/error e "Failed to submit resource")
                   (cond-> {:status 400
                            :error (.getMessage e)}
                     uri (assoc :uri uri)))))))]

      (when-let [last-tx (reverse (filter :tx results))]
        (x/await-tx crux-node last-tx))

      (let [status (case (count results)
                     0 400
                     1 (:status (first results))
                     207)]
        (cond->
            (assoc req
                   :ring.response/status status
                   :ring.response/body
                   ;; TODO: For default error bodies we shouldn't add the body here
                   ;; and let an error handler put in the default message.
                   (case status
                     400 "Bad Request\r\n"
                     201 "Created\r\n"
                     204 "No Content\r\n"
                     207 (pr-str (map (fn [r] (dissoc r :tx)) results))))
            (and (#{201 204} status) (:uri (first results)))
            (assoc-in [:ring.response/headers "location"] (:uri (first results))))))))