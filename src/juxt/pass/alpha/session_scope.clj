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
