;; Copyright © 2021, JUXT LTD.

(ns juxt.site.http-authentication
  (:require
   [juxt.reap.alpha.encoders :refer [www-authenticate]]
   [clojure.tools.logging :as log]
   [crypto.password.bcrypt :as password]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7235 :as rfc7235]
   [xtdb.api :as xt]))

(defn authenticate-with-bearer-auth [req db token68 protection-spaces]
  (log/tracef "Protection-spaces are %s" (pr-str protection-spaces))
  (or
   (when (seq protection-spaces)
     (let [{:keys [subject access-token]}
           (first
            (xt/q db '{:find [(pull sub [*]) (pull at [*])]
                       :keys [subject access-token]
                       :where [[at :juxt.site/token tok]
                               [at :juxt.site/type "https://meta.juxt.site/site/access-token"]
                               [at :juxt.site/subject sub]
                               [sub :juxt.site/type "https://meta.juxt.site/site/subject"]]
                       :in [tok]} token68))]
       (when subject (assoc req :juxt.site/subject subject :juxt.site/access-token access-token))))
   req))

;; TODO (idea): Tie bearer token to other security aspects such as remote IP so that
;; the bearer token is more difficult to use if intercepted.

(defn find-or-create-basic-auth-subject [req user-identity protection-space]
  (assert (:juxt.site/base-uri req))
  (let [xt-node (:juxt.site/xt-node req)
        subject {:xt/id (format "%s/_site/subjects/%s" (:juxt.site/base-uri req) (random-uuid))
                 :juxt.site/type "https://meta.juxt.site/site/subject"
                 :juxt.site/user-identity (:xt/id user-identity)
                 :juxt.site/protection-space (:xt/id protection-space)}
        ;; TODO: Replace this with a Flip tx-fn to ensure database consistency
        tx (xt/submit-tx xt-node [[:xtdb.api/put subject]])]
    (xt/await-tx xt-node tx)
    ;; TODO: Find an existing subject we can re-use or we create a subject for
    ;; every basic auth request. All attributes must match the above.
    (cond-> req
      subject (assoc :juxt.site/subject subject)
      ;; We need to update the db because we have injected a subject and it will
      ;; need to be in the database for authorization rules to work.
      subject (assoc :juxt.site/db (xt/db xt-node)))))

(defn authenticate-with-basic-auth [req db token68 protection-spaces]
  (when-let [{:juxt.site/keys [canonical-root-uri realm] :as protection-space} (first protection-spaces)]
    (let [[_ username password]
          (re-matches
           #"([^:]*):([^:]*)"
           (String. (.decode (java.util.Base64/getDecoder) token68)))

          query (cond-> '{:find [(pull e [*])]
                          :keys [identity]
                          :where [[e :juxt.site/type "https://meta.juxt.site/site/user-identity"]
                                  [e :juxt.site/canonical-root-uri canonical-root-uri]
                                  [e :juxt.site/username username]]
                          :in [username canonical-root-uri realm]}
                  realm (update :where conj '[e :juxt.site/realm realm]))

          candidates
          (xt/q db query username canonical-root-uri realm)]

      (when (> (count candidates) 1)
        (log/warnf "Multiple candidates in basic auth found for username %s, using first found" username))

      (when-let [user-identity (:identity (first candidates))]
        (when-let [password-hash (:juxt.site/password-hash user-identity)]
          (when (password/check password password-hash)
            (find-or-create-basic-auth-subject req user-identity protection-space)))))))

(defn www-authenticate-header
  "Create the WWW-Authenticate header value"
  [db protection-spaces]
  (log/tracef "protection-spaces: %s" protection-spaces)
  (www-authenticate
   (for [ps-id protection-spaces
         :let [ps (xt/entity db ps-id)
               realm (:juxt.site/realm ps)]]
     {:juxt.reap.rfc7235/auth-scheme (:juxt.site/auth-scheme ps)
      :juxt.reap.rfc7235/auth-params
      (cond-> []
        realm (conj
               {:juxt.reap.rfc7235/auth-param-name "realm"
                :juxt.reap.rfc7235/auth-param-value realm}))})))

(defn authenticate-with-authorization-header
  [{:juxt.site/keys [db] :as req}
   authorization-header protection-spaces]
  (let [{::rfc7235/keys [auth-scheme token68]} (reap/authorization authorization-header)]
    (case (.toLowerCase auth-scheme)
      "basic"
      (or
       (authenticate-with-basic-auth
        req db token68
        (filter #(= (:juxt.site/auth-scheme %) "Basic") protection-spaces))
       req)

      "bearer"
      (authenticate-with-bearer-auth
       req db token68
       (filter #(= (:juxt.site/auth-scheme %) "Bearer") protection-spaces))

      (throw
       (ex-info
        "Auth scheme unsupported"
        {:juxt.site/request-context
         (cond-> (assoc req :ring.response/status 401)
           protection-spaces
           (assoc
            :ring.response/headers
            {"www-authenticate"
             (www-authenticate-header db protection-spaces)}))})))))

(defn authenticate
  "Authenticate a request. Return a modified request, with information about user,
  roles and other credentials."
  [{:juxt.site/keys [db resource] :as req}]

  ;; TODO: This might be where we also add the 'on-behalf-of' info

  (let [protection-spaces (keep #(xt/entity db %) (:juxt.site/protection-spaces resource []))
        ;;req (cond-> req protection-spaces (assoc :juxt.site/protection-spaces protection-spaces))
        authorization-header (get-in req [:ring.request/headers "authorization"])]

    (cond-> req
      authorization-header (authenticate-with-authorization-header authorization-header protection-spaces))))

(defn ^:deprecated login-template-model [req]
  {:query (str (:ring.request/query req))})

(defn ^:deprecated unauthorized-template-model [req]
  {:redirect (str
              (:ring.request/path req)
              (when-let [query (:ring.request/query req)] (str "?" query)))})
