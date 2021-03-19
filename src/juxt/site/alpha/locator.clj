;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locator
  (:require
   [clojure.tools.logging :as log]
   [crux.api :as x]))

(alias 'site (create-ns 'juxt.site.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(defn locate-with-locators [db req]
  (let [uri (::site/uri req)]
    (when-let [locators
               (seq (x/q
                     db
                     '{:find [r
                              grps]

                       :where [(or [r ::site/type "ResourceLocator"]
                                   [r ::site/type "AppRoutes"] )
                               [r ::site/pattern p]
                               [(first grps) grp0]
                               [(some? grp0)]
                               [(re-matches p uri) grps]]

                       :in [uri]} uri))]

      (when (> (count locators) 1)
        (throw
         (ex-info
          "Multiple resource locators returned from query that match URI"
          (into req
                {::locators (map :crux.db/id locators)}))))

      (let [[e grps] (first locators)
            {typ ::site/type :as locator} (x/entity db e)]
        (case typ
          "ResourceLocator"
          (let [{::site/keys [locator-fn description]} locator]
            (when-not locator-fn
              (throw
               (ex-info
                "Resource locator must have a :juxt.site.alpha/locator attribute"
                (into req {::locator locator}))))

            (when-not (symbol? locator-fn)
              (throw
               (ex-info
                "Resource locator must be a symbol"
                (into req {::locator locator
                           ::locator-fn locator-fn}))))


            (log/debug "Requiring resolve of" locator-fn)
            (let [f (requiring-resolve locator-fn)]
              (log/debugf "Calling locator-fn %s: %s" locator-fn description)
              (f {:db db :request req :locator locator :grps grps})))

          "AppRoutes" locator)))))

(comment
  (put!
   {:crux.db/id "http://localhost:2021/ui/app.html"
    ::site/type "AppRoutes"
    ::site/pattern (re-pattern "http://localhost:2021/ui/.*")
    ::pass/classification "PUBLIC"
    ::http/methods #{:get :head :options}
    ::http/content-type "text/html;charset=utf-8"
    ::http/content "<h1>App</h1>"}))
