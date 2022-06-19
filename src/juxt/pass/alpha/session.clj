;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.session
  (:require
   [juxt.site.alpha :as-alias site]
   [juxt.pass.alpha :as-alias pass]
   [ring.middleware.cookies :refer [cookies-request]]
   [xtdb.api :as xt]))

(defn lookup-session-details [db session-token-id!]
  (first
   (xt/q db '{:find [(pull session-token [*])
                     (pull session [*])
                     (pull subject [*])
                     (pull user-identity [*])
                     (pull user [*])]
              :keys [juxt.pass.alpha/session-token
                     juxt.pass.alpha/session
                     juxt.pass.alpha/subject
                     juxt.pass.alpha/user-identity
                     juxt.pass.alpha/user]
              :where
              [[session-token ::site/type "https://meta.juxt.site/pass/session-token"]
               [session-token ::pass/session-token token-id]
               [session-token ::pass/session session]
               [session ::pass/subject subject]
               [subject ::pass/user-identity user-identity]
               [user-identity ::pass/user user]]
              :in [token-id]}
         session-token-id!)))

(defn wrap-associate-session [h]
  (fn [{::site/keys [db] :as req}]
    ;; TODO: Whether and how sessions should relate to their governing session
    ;; scopes (in order to determine cookie id etc.) is still TBD.
    (let [session-token-id!
          (-> (assoc req :headers (get req :ring.request/headers))
              cookies-request
              :cookies (get "id") :value)

          session-details
          (when session-token-id!
            (lookup-session-details db session-token-id!))

          req (cond-> req
                ;; The purpose of the trailing exclamation mark (!) is to
                ;; indicate sensitivity. Avoid logging sensitive data.
                session-token-id! (assoc ::pass/session-token-id! session-token-id!)
                session-details (into session-details))]

      (h req))))
