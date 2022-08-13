;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.content-negotiation
  (:require
   [clojure.string :as str]
   [juxt.pick.alpha.ring :refer [pick]]
   [xtdb.api :as xt]
   [juxt.http.alpha :as-alias http]
   [juxt.pick.alpha :as-alias pick]
   [juxt.site.alpha :as-alias site]))

(defn pick-representation [req representations]
  (when representations
    (::pick/representation
     (pick
      ;; TODO: Pick must upgrade to ring 2 headers
      (assoc req :headers (:ring.request/headers req))
      representations
      {::pick/vary? false}))))

(defn pick-with-vary [req representations]
  (when representations
    (pick
     ;; TODO: Pick must upgrade to ring 2 headers
     (assoc req :headers (:ring.request/headers req))
     representations
     {::pick/vary? true})))

(defn negotiate-representation [req representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  #_(log/debug "current-representations" (map (fn [rep] (dissoc rep ::http/body ::http/content)) current-representations))

  (let [{selected-representation ::pick/representation
         vary ::pick/vary}
        (pick-with-vary req representations)]

    (when (contains? #{:get :head} (:ring.request/method req))
      (when-not selected-representation
        (throw
         (ex-info
          "Not Acceptable"
          ;; TODO: Must add list of available representations
          ;; TODO: Add to req with into
          {::site/request-context (assoc req :ring.response/status 406)}))))

    #_(log/debug "result of negotiate-representation" (dissoc selected-representation ::http/body ::http/content))

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc ::http/vary (str/join ", " vary)))))

(defn negotiate-error-representation [req representations]
  (let [{selected-representation ::pick/representation}
        (pick-with-vary req representations)]
    selected-representation))

(defn find-variants [{::site/keys [resource uri db]}]

  (let [variants (xt/q db '{:find [(pull v [*])]
                           :where [[v ::site/variant-of uri]]
                           :in [uri]}
                      uri)]
    (when (pos? (count variants))
      (cond-> (for [[v] variants]
                (assoc v ::http/content-location (:xt/id v)))
        ;; If the resource has a content-type (or template), then add it as an
        ;; additional representation.
        (or (::http/content-type resource) (::site/template resource))
        (conj resource)))))

(defn merge-template-maybe
  "Some representations use a template engine to generate the payload. The
  referenced template (::site/template) may provide defaults for the
  representation metadata. This function takes a representation and merges in
  its template's metadata, if necessary."
  [db representation]
  (if-let [template (some->> representation ::site/template (xt/entity db))]
    (merge
     (select-keys template [::http/content-type ::http/content-encoding ::http/content-language])
     representation)
    representation))

(defn current-representations [{::site/keys [resource db] :as req}]
  (->> (or
        ;; This is not common to find statically in the db, but this option
        ;; allows 'dynamic' resources to declare multiple representations.
        (::http/representations resource)

        ;; See if there are variants
        (find-variants req)

        ;; Most resources have a content-type, which indicates there is only one
        ;; variant.
        (when (or (::http/content-type resource) (::site/template resource))
          [resource])

        ;; No representations. On a GET, this would yield a 404.
        [])
       ;; Merge in an template defaults
       (mapv #(merge-template-maybe db %))))
