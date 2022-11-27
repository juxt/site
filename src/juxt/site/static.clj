;; Copyright © 2021, JUXT LTD.

(ns juxt.site.static
  (:require
   [clojure.string :as str]
   [juxt.site.conditional :as conditional]
   [juxt.site.util :as util]
   [xtdb.api :as xt]
   [juxt.http :as-alias http]
   [juxt.site :as-alias site]))

(defn put-static-resource
  "PUT a new representation of the target resource. All other representations are
  replaced."
  [{::site/keys [uri db received-representation start-date xt-node request-id] :as req}]

  (let [existing (xt/entity db uri)
        classification (get-in req [:ring.request/headers "site-classification"])
        variant-of (get-in req [:ring.request/headers "site-variant-of"])
        template-dialect (get-in req [:ring.request/headers "site-template-dialect"])
        type (get-in req [:ring.request/headers "site-type"])
        pattern (get-in req [:ring.request/headers "site-pattern"])
        new-rep (merge
                 {:xt/id uri
                  ::http/methods {:get {} :head {} :options {} :put {} :patch {}}
                  ::site/type "StaticRepresentation"}
                 existing
                 (cond->
                     {::http/etag (util/etag received-representation)
                      ::http/last-modified start-date
                      ::site/request request-id}
                   pattern (assoc ::site/pattern pattern)
                   type (assoc ::site/type type)
                   variant-of (assoc ::site/variant-of variant-of)
                   classification (assoc ::site/classification classification)
                   template-dialect (assoc ::site/template-dialect (str/lower-case template-dialect)))
                 received-representation)]

    ;; Currently we cannot tell whether a submitted tx has been successful,
    ;; see https://github.com/juxt/xtdb/issues/1480. As a workaround, we do
    ;; the conditional checks here. In the future, we'll call into separate
    ;; tx fns.
    (conditional/evaluate-preconditions! req)

    (->> (xt/submit-tx
          xt-node
          [[:xtdb.api/put new-rep]])
         (xt/await-tx xt-node))

    (into req {:ring.response/status (if existing 204 201)})))

(defn patch-static-resource
  [{::site/keys [received-representation] :as req}]
  (throw
   (ex-info
    "TODO: patch"
    {::site/request-context req
     ::incoming received-representation})))
