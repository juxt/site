;; Copyright © 2022, JUXT LTD.

(ns juxt.site.test-helpers.openid)

#_(defn put-openid-user-identity! [& {:keys [username]
                                    :juxt.site.jwt.claims/keys [iss sub nickname]}]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/put-openid-user-identity"
   :juxt.site/input
   (cond-> {:xt/id (format "https://example.org/user-identities/%s/openid" (str/lower-case username))
            :juxt.site/user ~(format "https://example.org/users/%s" (str/lower-case username))
            :juxt.site.jwt.claims/iss iss}
     sub (assoc :juxt.site.jwt.claims/sub sub)
     nickname (assoc :juxt.site.jwt.claims/nickname nickname))})


#_(defn fetch-jwks!
  [id]
  {:juxt.site/subject-id "https://example.org/subjects/system"
   :juxt.site/action-id "https://example.org/actions/openid/fetch-jwks"
   :juxt.site/input {:xt/id id}})
