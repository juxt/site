;; Copyright © 2022, JUXT LTD.

(ns juxt.site.content-negotiation
  (:require
   [clojure.string :as str]
   [juxt.pick.ring :refer [pick]]
   [xtdb.api :as xt]))

(defn pick-representation [req representations]
  (when representations
    (:juxt.pick/representation
     (pick
      ;; TODO: Pick must upgrade to ring 2 headers
      (assoc req :headers (:ring.request/headers req))
      representations
      {:juxt.pick/vary? false}))))

(defn pick-with-vary [req representations]
  (when representations
    (pick
     ;; TODO: Pick must upgrade to ring 2 headers
     (assoc req :headers (:ring.request/headers req))
     representations
     {:juxt.pick/vary? true})))

(defn negotiate-representation [req representations]
  ;; Negotiate the best representation, determining the vary header.
  (let [{selected-representation :juxt.pick/representation
         vary :juxt.pick/vary}
        (pick-with-vary req representations)]

    (when (contains? #{:get :head} (:ring.request/method req))
      (when-not selected-representation
        (throw
         (ex-info
          "Not Acceptable"
          ;; TODO: Must add list of available representations
          ;; TODO: Add to req with into
          {:ring.response/status 406
           :juxt.site/request-context req}))))

    #_(log/debug "result of negotiate-representation" (dissoc selected-representation :juxt.http/body :juxt.http/content))

    ;; Pin the vary header onto the selected representation's
    ;; metadata
    (cond-> selected-representation
      (not-empty vary) (assoc :juxt.http/vary (str/join ", " vary)))))

(defn negotiate-error-representation [req representations]
  (let [{selected-representation :juxt.pick/representation}
        (pick-with-vary req representations)]
    selected-representation))

(defn find-variants [{:juxt.site/keys [resource uri db]}]

  (let [variants (xt/q db '{:find [(pull v [*])]
                           :where [[v :juxt.site/variant-of uri]]
                           :in [uri]}
                      uri)]
    (when (pos? (count variants))
      (cond-> (for [[v] variants]
                (assoc v :juxt.http/content-location (:xt/id v)))
        (:juxt.http/content-type resource)
        (conj resource)))))

(defn current-representations [{:juxt.site/keys [resource] :as req}]
  (or
   ;; This is not common to find statically in the db, but this option
   ;; allows 'dynamic' resources to declare multiple representations.
   (:juxt.http/representations resource)

   ;; See if there are variants
   (find-variants req)

   ;; Most resources have a content-type, which indicates there is only one
   ;; variant.
   (when (:juxt.http/content-type resource)
     [resource])

   ;; No representations. On a GET, this would yield a 404.
   []))
