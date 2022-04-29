;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.v3.access-token
  (:require
   [juxt.site.alpha.util :refer [sha random-bytes as-hex-str as-b64-str uuid-bytes]]))

(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn access-token-id->xt-id [token-id]
  (format "urn:site:access-token:%s" token-id))

(defn make-access-token-doc
  "Returns an XT doc representing an access token. Can be augmented
  with :juxt.pass.alpha/scope and other entries."
  ([subject client]
   (let [token-id (as-hex-str (random-bytes 20))]
     {:xt/id (access-token-id->xt-id token-id)
      ::site/type "AccessToken"
      ::pass/subject subject
      ::pass/client client}))
  ([subject client scope]
   (-> (make-access-token-doc subject client)
       (assoc ::pass/scope scope))))
