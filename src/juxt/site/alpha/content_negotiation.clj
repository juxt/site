;; Copyright © 2022, JUXT LTD.

(ns juxt.site.alpha.content-negotiation
  (:require
   [clojure.string :as str]
   [juxt.pick.alpha.ring :refer [pick]]
   [xtdb.api :as xt]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pick (create-ns 'juxt.pick.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn negotiate-representation [request representations]
  ;; Negotiate the best representation, determining the vary
  ;; header.
  #_(log/debug "current-representations" (map (fn [rep] (dissoc rep ::http/body ::http/content)) current-representations))

  (let [{selected-representation ::pick/representation
         vary ::pick/vary}
        (when (seq representations)
          ;; TODO: Pick must upgrade to ring 2 headers
          (pick (assoc request :headers (:ring.request/headers request))
                representations {::pick/vary? true}))]

    #_(when (contains? #{:get :head} (:ring.request/method request))
        (when-not selected-representation
          (throw
           (ex-info
            "Not Acceptable"
            ;; TODO: Must add list of available representations
            ;; TODO: Add to req with into
            {:ring.response/status 406
             }))))

    #_(log/debug "result of negotiate-representation" (dissoc selected-representation ::http/body ::http/content))

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc ::http/vary (str/join ", " vary)))))


(defn find-variants [{::site/keys [resource uri db] :as req}]

  (let [variants (xt/q db '{:find [(pull v [*])]
                           :where [[v ::site/variant-of uri]]
                           :in [uri]}
                      uri)]
    (when (pos? (count variants))
      (cond-> (for [[v] variants]
                (assoc v ::http/content-location (:xt/id v)))
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

(defn current-representations [{::site/keys [resource uri db] :as req}]
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
