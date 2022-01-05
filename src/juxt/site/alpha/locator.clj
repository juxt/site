;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locator
  (:require
   [clojure.tools.logging :as log]
   [juxt.apex.alpha.openapi :as openapi]
   [juxt.site.alpha.debug :as debug]
   [juxt.site.alpha.static :as static]
   [juxt.site.alpha.cache :as cache]
   [xtdb.api :as x]))

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
          {::locators (map :xt/id locators)
           ::site/request-context (assoc req :ring.response/status 500)})))

      (let [[e grps] (first locators)
            {typ ::site/type :as locator} (x/entity db e)]
        (case typ
          "ResourceLocator"
          (let [{::site/keys [locator-fn description]} locator]
            (when-not locator-fn
              (throw
               (ex-info
                "Resource locator must have a :juxt.site.alpha/locator attribute"
                {::locator locator
                 ::site/request-context (assoc req :ring.response/status 500)})))

            (when-not (symbol? locator-fn)
              (throw
               (ex-info
                "Resource locator must be a symbol"
                {::locator locator
                 ::locator-fn locator-fn
                 ::site/request-context (assoc req :ring.response/status 500)})))


            (log/debug "Requiring resolve of" locator-fn)
            (let [f (requiring-resolve locator-fn)]
              (log/debugf "Calling locator-fn %s: %s" locator-fn description)
              (f {:db db :request req :locator locator :grps grps})))

          "AppRoutes" locator)))))

(comment
  (put!
   {:xt/id "http://localhost:2021/ui/app.html"
    ::site/type "AppRoutes"
    ::site/pattern (re-pattern "http://localhost:2021/ui/.*")
    ::pass/classification "PUBLIC"
    ::http/methods #{:get :head :options}
    ::http/content-type "text/html;charset=utf-8"
    ::http/content "<h1>App</h1>"}))

(def SITE_REQUEST_ID_PATTERN #"(.*/_site/requests/\p{Alnum}+)(\.[a-z]+)?")

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [{::site/keys [db uri base-uri] :as req}]
  (or
   ;; We call OpenAPI location here, because a resource can be defined in
   ;; OpenAPI, and exist in Xtdb, simultaneously.
   (openapi/locate-resource db uri req)

   ;; Is it in XTDB?
   (when-let [r (x/entity db uri)]
     (cond-> (assoc r ::site/resource-provider ::db)
       (= (get r ::site/type) "StaticRepresentation")
       (assoc ::site/put-fn static/put-static-resource
              ::site/patch-fn static/patch-static-resource)))

   ;; Is it found by any resource locators registered in the database?
   (locate-with-locators db req)

   ;; Is it a redirect?
   (when-let [[r loc] (first
                       (x/q db '{:find [r loc]
                                 :where [[r ::site/resource uri]
                                         [r ::site/location loc]
                                         [r ::site/type "Redirect"]]
                                 :in [uri]}
                            uri))]
     {::site/uri uri
      ::http/methods #{:get :head :options}
      ::site/resource-provider r
      ::http/redirect (cond-> loc (.startsWith loc base-uri)
                              (subs (count base-uri)))})

   ;; Return a back-stop resource
   {::site/resource-provider ::default-empty-resource
    ::http/methods #{:get :head :options :put :post}
    ::site/put-fn static/put-static-resource}))
