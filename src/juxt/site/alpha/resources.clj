;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.resources
  (:require
   [clojure.edn :as edn]
   [xtdb.api :as xt]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn read-forms [r]
  (lazy-seq
   (let [res (edn/read {:eof :eof
                        :readers {'regex #(re-pattern %)}}
                       r)]
     (when-not (= res :eof)
       (cons res (read-forms r))))))

(defn post-resource
  "Post a new resource, or overwrite an existing one, in the database."
  [{::site/keys [db xtdb-node received-representation] :as req}]

  (let [resources (read-forms
                   (java.io.PushbackReader.
                    (java.io.InputStreamReader.
                     (java.io.ByteArrayInputStream.
                      (::http/body received-representation)))))
        results
        (doall
         (for [resource resources]
           (let [uri (:xt/id resource)
                 existing? (when uri (xt/entity db uri))]
             (try
               (let [tx (xt/submit-tx xtdb-node [[::xt/put resource]])]
                 (cond-> {:status (if existing? 204 201)
                          :tx tx}
                   uri (assoc :uri uri)))
               (catch Exception e
                 (log/error e "Failed to submit resource")
                 (cond-> {:status 400
                          :error (.getMessage e)}
                   uri (assoc :uri uri)))))))]

    (when-let [last-tx (reverse (filter :tx results))]
      (xt/await-tx xtdb-node last-tx))

    (let [status (case (count results)
                   0 400
                   1 (:status (first results))
                   207)]
      (cond-> req
        status (assoc :ring.response/status status)
        (= status 207) (assoc :ring.response/body
                              (pr-str (map (fn [r] (dissoc r :tx)) results)))

        (and (#{201 204} status) (:uri (first results)))
        (assoc-in [:ring.response/headers "location"] (:uri (first results)))))))
