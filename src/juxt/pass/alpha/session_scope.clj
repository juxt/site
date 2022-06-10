;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.session-scope
  (:require
   [xtdb.api :as xt]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]))

(defn session-scopes [db uri]
  (seq
   (for [{:keys [session-scope]}
         (xt/q
          db
          '{:find [(pull cs [*])]
            :keys [session-scope]
            :where [[cs ::site/type "https://meta.juxt.site/pass/session-scope"]
                    [cs ::pass/cookie-path path]
                    [cs ::pass/cookie-domain domain]
                    [(format "%s%s" domain path) uri-prefix]
                    [(clojure.string/starts-with? uri uri-prefix)]]
            :in [uri]}
          uri)]
     session-scope)))


(defn wrap-session-scopes [h]
  (fn [{::site/keys [db uri] :as req}]
    (let [session-scopes (session-scopes db uri)]
      ;; TODO: If we do have session scopes, we should then check for a cookie
      ;; matching one of session-scopes and look up the session, binding that to
      ;; the request too.
      (h (cond-> req
           session-scopes (assoc ::pass/session-scopes session-scopes))))))
