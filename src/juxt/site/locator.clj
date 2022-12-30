;; Copyright © 2021, JUXT LTD.

(ns juxt.site.locator
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [xtdb.api :as xt]))

;; TODO: Definitely a candidate for clojure.core.cache (or memoize). Always be
;; careful using memoize, but in case performance is scarcer than memory.
(memoize
 (defn to-regex [uri-template]
   (re-pattern
    (str/replace
     uri-template
     #"\{([^\}]+)\}"                    ; e.g. {id}
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
                 :where [[resource :juxt.site/uri-template true]
                         ;; Compile the URI to a java.util.regex.Pattern
                         [(juxt.site.locator/to-regex resource) pat]
                         [(re-matches pat uri) grps]
                         [(first grps) grp0]
                         [(some? grp0)]]
                 :in [uri]}
               uri))]
    (assoc
     resource
     :juxt.site/path-params
     (zipmap
      ;; Keys
      (map second (re-seq #"\{(\p{Alpha}+)\}" (:xt/id resource)))
      ;; Values
      (next groups)))))

(defn locate-resource
  "Call each locate-resource defmethod, in a particular order, ending
  in :default."
  [{:juxt.site/keys [db uri]}]

  (assert uri)
  (assert db)
  (or
   ;; Deprecated. Will be replaced with simple resources that contain enough
   ;; OpenAPI metadata to render a openapi.json document. Isomorphic.
   ;; (openapi/locate-resource db uri req)

   ;; Is it in XTDB?
   (when-let [e (xt/entity db uri)]
     (when-not (:juxt.site/uri-template e)
       (assoc e :juxt.site/resource-provider ::db)))

   ;; Is it found by any resource locators registered in the database?
   (some->
    (match-uri-templated-uris db uri)
    (assoc :juxt.site/resource-provider ::db))

   ;; This is put into each host at bootstrap but can be overridden if
   ;; a custom 404 page is required.
   (xt/entity db (str (.resolve (java.net.URI. uri) "/_site/not-found")))

   (throw
    (let [uri (str (.resolve (java.net.URI. uri) "/_site/not-found"))]
      (ex-info
       (format "The not-found resource has not been installed: %s" uri)
       {:uri uri})))))
