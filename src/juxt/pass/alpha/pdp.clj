;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.pdp
  (:require
   [clojure.set :as set]
   [clojure.walk :refer [postwalk-replace]]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [juxt.pass.alpha :as pass]
   [juxt.spin.alpha :as spin]
   [juxt.spin.alpha.auth :as spin.auth]))

;; PDP (Policy Decision Point)

;; See 3.2 of
;; http://docs.oasis-open.org/xacml/3.0/errata01/os/xacml-3.0-core-spec-errata01-os-complete.html#_Toc489959503

(defn authorization
  "Returns authorization information. Context is all the attributes that pertain to the subject, resource,
  action, environment and maybe more."
  [db {:keys [request resource]}]

  ;; Map across each rule in the system (we can memoize later for
  ;; performance).

  (let [ ;; TODO: But we should go through all policies, bring in their
        ;; attributes to merge into the target, then for each rule in the
        ;; policy... the apply rule-combining algo...

        rules (crux/q db '{:find [rule]
                           :where [[rule :type "Rule"]]})

        _  (log/debugf "Rules to match are %s" (pr-str rules))

        request-id (java.util.UUID/randomUUID)
        _ (log/debugf "Submitting request to db: %s" request)
        resource-id (java.util.UUID/randomUUID)
        ;;_ (log/debugf "Submitting resource to db: %s" resource)

        db (crux/with-tx db
             [[:crux.tx/put (assoc request :crux.db/id request-id)]
              [:crux.tx/put (assoc resource :crux.db/id resource-id)]])

        evaluated-rules
        (keep
         (fn [[rule]]
           (let [rule-ent (crux/entity db rule)]
             (when-let [target (::pass/target rule-ent)]
               (let [q {:find ['success]
                        :where (into '[[(identity true) success]]
                                     target)
                        :in ['request 'resource]}
                     match-results (crux/q db q request-id resource-id)]
                 (assoc rule-ent ::pass/matched? (pos? (count match-results)))))))
         rules)

        _ (log/debugf "Result of rule matching: %s" (pr-str evaluated-rules))

        matched-rules (filter ::pass/matched? evaluated-rules)

        allowed? (and
                  (pos? (count matched-rules))
                  ;; Rule combination algorithm is every?
                  (every? #(= (::pass/effect %) ::pass/allow) matched-rules))]

    (log/debugf "Allowed?: %s" allowed?)
    (if-not allowed?
      #::pass
      {:access ::pass/denied
       :matched-rules matched-rules}

      #::pass
      {:access ::pass/approved
       :matched-rules matched-rules
       :limiting-clauses
       ;; Get all the query limits added by these rules
       (->> matched-rules
            (filter #(= (::pass/effect %) ::pass/allow))
            (map ::pass/limiting-clauses)
            (apply concat))
       :allow-methods
       (->> matched-rules
            (filter #(= (::pass/effect %) ::pass/allow))
            (map ::pass/allow-methods)
            (apply set/union))})))

(defn ->authorized-query [query authorization]
  ;; Ensure they apply to ALL entities queried by a given Datalog
  ;; query. We duplicate each set of limiting clauses. For each copy,
  ;; we replace 'e (which, by convention, is what we use in a limiting
  ;; clause), with the actual symbols used in the given Datalog query.

  (when (= (::pass/access authorization) ::pass/approved)
    (let [combined-limiting-clauses
          (apply concat (for [sym (distinct (filter symbol? (map first (:where query))))]
                          (postwalk-replace {'e sym} (::pass/limiting-clauses authorization))))]
      (cond-> query
        ;;(assoc :in '[context])
        combined-limiting-clauses (update :where (comp vec concat) combined-limiting-clauses)))))

(defn authorize
  "Return the resource, as it appears to the request after authorization rules
  have been applied."
  [request resource db]

  (let [username (get request ::pass/username)]
    (when resource
      (cond

        ;; Give the crux admin god-like power
        (= username "crux/admin")
        {:resource resource}

        (and
         (.startsWith (:uri request) "/_crux/")
         (not= username "crux/admin"))
        (throw
         (if username
           (ex-info
            "Forbidden"
            {::spin/response
             {:status 403
              :body "Forbidden\r\n"}})

           (ex-info
            "Unauthorized"
            {::spin/response
             {:status 401
              :headers
              {"www-authenticate"
               (spin.auth/www-authenticate
                [{::spin/authentication-scheme "Basic"
                  ::spin/realm "Crux Administration"}])}
              :body "Unauthorized\r\n"}})))

        :else
        (let [request-context {:request (dissoc request :body)
                               :resource resource}
              authorization (authorization db request-context)]

          (if-not (= (::pass/access authorization) ::pass/approved)
            (throw
             (if username
               (ex-info
                "Forbidden"
                {::spin/response
                 {:status 403
                  :body "Forbidden\r\n"}})

               (ex-info
                "Unauthorized"
                {::spin/response
                 {:status 401
                  :headers
                  {"www-authenticate"
                   (spin.auth/www-authenticate
                    [{::spin/authentication-scheme "Basic"
                      ::spin/realm "Users"}])}
                  :body "Unauthorized\r\n"}})))

            {:resource resource :authorization authorization}))))))
