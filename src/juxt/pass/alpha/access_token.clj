;; Copyright © 2022, JUXT LTD.

(ns juxt.pass.alpha.access-token
  (:require
   [juxt.site.alpha.util :refer [random-bytes as-hex-str]]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]))

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
