;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locator
  (:require
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [juxt.site.alpha.util :as util]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'spin (create-ns 'juxt.spin.alpha))

(defn locate-with-locators [db request]
  (log/trace "Locate with locators")
  (let [uri (util/uri request)]
    (when-let [locators
               (seq (map first
                     (crux/q
                      db
                      '{:find [(eql/project
                                r [:crux.db/id
                                   ::site/description ::site/locator-fn])]
                        :where [[r ::site/type "ResourceLocator"]
                                [r ::site/pattern p]
                                [(re-matches p uri)]]
                        :in [uri]} uri)))]

      (log/trace "locators is" locators)

      (when (> (count locators) 1)
        (throw
         (ex-info
          "Multiple resource locators returned from query that match URI"
          {::locators (map :crux.db/id locators)
           ::uri uri
           ::spin/response
           {:status 500
            :body "Internal Error\r\n"}})))

      (let [{::site/keys [locator-fn description] :as locator} (first locators)]
        (log/trace "description is" description)
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

        (log/trace "Requiring resolve of" locator-fn)

        (let [v (requiring-resolve locator-fn)]
          (log/debugf "Calling locator-fn %s: %s" locator-fn description)
          (v db request locator))))))
