;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql.internal-resolver
  (:require [clojure.tools.logging :as log]
            [xtdb.api :as x]))

(alias 'pass (create-ns 'juxt.pass.alpha))

(defn subject [{::pass/keys [subject] :keys [db]}]
  (let [user (x/entity db (::pass/user subject))]
    {"user" user
     "authScheme" (::pass/auth-scheme subject)}))
