;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.cookie-scope
  (:require
   [xtdb.api :as xt]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]))

(defn cookie-scopes [db uri]
  (seq
   (for [{:keys [cookie-scope]}
         (xt/q
          db
          '{:find [(pull cs [*])]
            :keys [cookie-scope]
            :where [[cs ::site/type "https://meta.juxt.site/pass/cookie-scope"]
                    [cs ::pass/cookie-path path]
                    [cs ::pass/cookie-domain domain]
                    [(format "%s%s" domain path) uri-prefix]
                    [(clojure.string/starts-with? uri uri-prefix)]]
            :in [uri]}
          uri)]
     cookie-scope)))
