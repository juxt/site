;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.locator
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [xtdb.api :as xt]
   [juxt.site.alpha :as-alias site]
   [juxt.http.alpha :as-alias http]))

#_(defn locate-with-locators [db req]
    (let [uri (::site/uri req)]
      (when-let [locators
                 (seq (xt/q
                       db
                       '{:find [r
                                grps]

                         :where [(or [r ::site/type "AppRoutes"]

                                     ;; Same as AppRoutes, but a more appropriate
                                     ;; name in the case of a resource that isn't
                                     ;; found in the database but does need to
                                     ;; exist for the purposes of defining PUT
                                     ;; semantics.
                                     [r ::site/type "VirtualResource"]

                                     ;; Deprecated because it relies on code
                                     ;; deployed and we want to avoid this unless
                                     ;; no alternative is possible.
                                     [r ::site/type "ResourceLocator"])

                                 [r ::site/pattern p]
                                 [(first grps) grp0]
                                 [(some? grp0)]
                                 [(re-pattern p) pat]
                                 [(re-matches pat uri) grps]]

                         :in [uri]} uri))]

        (when (> (count locators) 1)
          (throw
           (ex-info
            "Multiple resource locators returned from query that match URI"
            {::locators (map :xt/id locators)
             ::site/request-context (assoc req :ring.response/status 500)})))

        (let [[e grps] (first locators)
              {typ ::site/type :as locator} (xt/entity db e)]
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

            ("AppRoutes" "VirtualResource") locator)))))


;; TODO: Definitely a candidate for clojure.core.cache (or memoize). Always be
;; careful using memoize, but in case performance is scarcer than memory.
(memoize
 (defn to-regex [uri-template]
   (re-pattern
    (str/replace
     uri-template
     #"\{([^\}]+)\}"                      ; e.g. {id}
     (fn replacer [[_ group]]
       ;; Instead of using the regex of path parameter's schema, we use a
       ;; weak regex that includes anything except forward slashes, question
       ;; marks, or hashes (as described in the Path Templating section of
       ;; the OpenAPI Specification (version 3.0.2). We are just locating
       ;; the resource in this step, if the regex were too strong and we
       ;; reject the path then locate-resource would yield nil, and we might
       ;; consequently end up creating a static resource (or whatever other
       ;; resource locators are tried after this one). That might surprise
       ;; the user so instead (prinicple of least surprise) we are more
       ;; liberal in what we accept at this stage and leave validation
       ;; against the path parameter to later (potentially yielding a 400).
       (format "(?<%s>[^/#\\?]+)" group))))))

(defn match-uri-templated-uris [db uri]
  (when-let [{:keys [resource groups]}
             (first
              (xt/q
               db
               '{:find [(pull resource [*]) grps]
                 :keys [resource groups]
                 :where [[resource ::site/uri-template true]
                         ;; Compile the URI to a java.util.regex.Pattern
                         [(juxt.site.alpha.locator/to-regex resource) pat]
                         [(re-matches pat uri) grps]
                         [(first grps) grp0]
                         [(some? grp0)]]
                 :in [uri]}
               uri))]
    (assoc
     resource
     ::site/path-params
     (zipmap
      ;; Keys
      (map second (re-seq #"\{(\p{Alpha}+)\}" (:xt/id resource)))
      ;; Values
      (next groups)))))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [{::site/keys [db uri base-uri] :as req}]

  (assert uri)
  (assert base-uri)
  (assert db)
  (or
   ;; Deprecated. Will be replaced with simple resources that contain enough
   ;; OpenAPI metadata to render a openapi.json document. Isomorphic.
   ;; (openapi/locate-resource db uri req)

   ;; Is it in XTDB?
   (when-let [e (some-> (xt/entity db uri) (assoc ::site/resource-provider ::db))]
     (when-not (::site/uri-template e)
       e))

   ;; Is it found by any resource locators registered in the database?
   (match-uri-templated-uris db uri)

   ;; Is it a redirect?
   (when-let [[r loc] (first
                       (xt/q db '{:find [r loc]
                                  :where [[r ::site/resource uri]
                                          [r ::site/location loc]
                                          [r ::site/type "Redirect"]]
                                  :in [uri]}
                             uri))]
     {::site/uri uri
      ::site/methods {:get {} :head {} :options {}}
      ::site/resource-provider r
      ::http/redirect (cond-> loc (.startsWith loc base-uri)
                              (subs (count base-uri)))})

   ;; This can be put into the database to override the ::default-empty-resource
   ;; default.
   (xt/entity db "urn:site:resources:not-found")

   {::site/resource-provider ::default-empty-resource
    ::site/methods {:get {} :head {} :options {} :put {} :post {}}}))
