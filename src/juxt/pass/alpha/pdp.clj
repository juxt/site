;; Copyright © 2021, JUXT LTD.

(ns juxt.pass.alpha.pdp
  (:require
   [clojure.set :as set]
   [clojure.walk :refer [postwalk-replace]]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [integrant.core :as ig]
   [juxt.pass.alpha :as pass]
   [juxt.site.alpha.entity :as entity]
   [juxt.spin.alpha :as spin]
   [juxt.spin.alpha.auth :as spin.auth]))

;; PDP (Policy Decision Point)

;; See 3.2 of
;; http://docs.oasis-open.org/xacml/3.0/errata01/os/xacml-3.0-core-spec-errata01-os-complete.html#_Toc489959503

(defn authorization
  "Returns authorization information. Context is all the attributes that pertain
  to the subject, resource, request (action), environment and maybe more."
  [{::pass/keys [subject resource request environment]}]

  ;; Map across each rule in the system (we can memoize later for
  ;; performance).

  (let [ ;; TODO: But we should go through all policies, bring in their
        ;; attributes to merge into the target, then for each rule in the
        ;; policy... the apply rule-combining algo...

        db (:db environment)

        rules (crux/q db '{:find [rule]
                           :where [[rule :type "Rule"]]})

        _  (log/debugf "Rules to match are %s" (pr-str rules))

        subject-id (java.util.UUID/randomUUID)
        resource-id (java.util.UUID/randomUUID)
        request-id (java.util.UUID/randomUUID)

        db (crux/with-tx db
             [[:crux.tx/put (assoc subject :crux.db/id subject-id)]
              [:crux.tx/put (assoc resource :crux.db/id resource-id)]
              [:crux.tx/put (assoc request :crux.db/id request-id)]])

        evaluated-rules
        (keep
         (fn [[rule]]
           (let [rule-ent (crux/entity db rule)]
             (when-let [target (::pass/target rule-ent)]
               (let [q {:find ['success]
                        :where (into '[[(identity true) success]]
                                     target)
                        :in ['subject 'resource 'action]}
                     match-results (crux/q db q subject-id resource-id request-id)]
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

(defn authorize [request-context]

  (let [authorization (authorization request-context)]

    (when-not (= (::pass/access authorization) ::pass/approved)
      (throw
       (if (::pass/subject request-context)
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
            :body "Unauthorized\r\n"}}))))

    authorization))

(defmethod ig/init-key ::rules [_ {:keys [crux-node]}]
  (println "Adding built-in users/rules")
  (try
    (crux/submit-tx
     crux-node

     (concat
      ;; The webmaster user - in the future, the password will be provided when
      ;; the Crux instance is provisioned.
      (entity/user-entity "webmaster" "FunkyForest")

      ;; A rule that allows the webmaster to do everything.
      [[:crux.tx/put
        {:crux.db/id "/_crux/pass/rules/webmaster"
         :type "Rule"
         ::pass/target '[[subject :juxt.pass.alpha/username "webmaster"]]
         ::pass/effect ::pass/allow
         ::pass/allow-methods #{:get :head :options :put :post :delete}}]]

      ;; A rule that makes all PUBLIC classifications accessible to GET
      [[:crux.tx/put
        {:crux.db/id "/_crux/pass/rules/public"
         :type "Rule"
         ::pass/target '[[resource ::spin/classification "PUBLIC"]]
         ::pass/effect ::pass/allow
         ::pass/allow-methods #{:get :head :options}}]]

      ))
    (catch Exception e
      (prn e))))
