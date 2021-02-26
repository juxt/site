;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locator
  (:require
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [juxt.site.alpha.util :as util]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'spin (create-ns 'juxt.spin.alpha))

(defn locate-with-locators [db request]
  (let [uri (util/uri request)]
    (when-let [locators
               (seq (crux/q
                     db
                     '{:find [(eql/project
                               r [:crux.db/id
                                  ::site/description ::site/locator-fn])
                              grps]

                       :where [[r ::site/type "ResourceLocator"]
                               [r ::site/pattern p]
                               [(first grps) grp0]
                               [(some? grp0)]
                               [(re-matches p uri) grps]]

                       :in [uri]} uri))]

      (when (> (count locators) 1)
        (throw
         (ex-info
          "Multiple resource locators returned from query that match URI"
          {::locators (map :crux.db/id locators)
           ::uri uri
           ::spin/response
           {:status 500
            :body "Internal Error\r\n"}})))

      (let [[{::site/keys [locator-fn description] :as locator} grps] (first locators)]
        (when-not locator-fn
          (throw
           (ex-info
            "Resource locator must have a :juxt.site.alpha/locator attribute"
            {::locator locator
             ::uri uri
             ::spin/response
             {:status 500
              :body "Internal Error\r\n"}})))

        (when-not (symbol? locator-fn)
          (throw
           (ex-info
            "Resource locator must be a symbol"
            {::locator locator
             ::locator-fn locator-fn
             ::uri uri
             ::spin/response
             {:status 500
              :body "Internal Error\r\n"}})))

        (log/debug "Requiring resolve of" locator-fn)
        (let [f (requiring-resolve locator-fn)]
          (log/debugf "Calling locator-fn %s: %s" locator-fn description)
          (f db request locator grps))))))
