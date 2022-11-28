;; Copyright © 2022, JUXT LTD.

(ns juxt.site.session-scope
  (:require
   [juxt.site :as-alias site]
   [ring.middleware.cookies :refer [cookies-request]]
   [xtdb.api :as xt]
   [clojure.tools.logging :as log]))

(defn lookup-session-details [db session-token-id!]
  (let [session-details
        (first
         (xt/q db '{:find [(pull session-token [*])
                           (pull session [*])]
                    :keys [juxt.site/session-token
                           juxt.site/session]
                    :where
                    [[session-token ::site/type "https://meta.juxt.site/site/session-token"]
                     [session-token ::site/session-token token-id]
                     [session-token ::site/session session]]
                    :in [token-id]}
               session-token-id!))
        subject (some-> session-details ::site/session ::site/subject)]
    (cond-> session-details
      ;; Since subject is common and special, we promote it to the top-level
      ;; context. However, it is possible to have a session without having
      ;; established a subject (for example, while authenticating).
      subject (assoc ::site/subject (xt/entity db subject)))))

(defn wrap-session-scope [h]
  (fn [{::site/keys [db uri resource] :as req}]

    (let [scope-id (::site/session-scope resource)

          _ (log/debugf "session-scope for %s is %s" uri scope-id)
          _ (log/debugf "resources is %s" (pr-str resource))

          scope (when scope-id (xt/entity db scope-id))

          cookie-name (when scope (:juxt.site/cookie-name scope))

          session-token-id!
          (when cookie-name
            (-> (assoc req :headers (get req :ring.request/headers))
                cookies-request
                :cookies (get cookie-name) :value))

          _ (log/debugf "session-token-id is %s" session-token-id!)

          session-details
          (when session-token-id!
            (lookup-session-details db session-token-id!))

          _ (log/debugf "Session details for %s is %s" uri (pr-str session-details))
          ]

      (h (cond-> req
           scope (assoc ::site/session-scope scope)
           session-details (into session-details))))))
