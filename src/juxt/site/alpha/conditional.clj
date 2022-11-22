;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.conditional
  (:require
   [clojure.tools.logging :as log]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.rfc7232 :as rfc7232]
   [juxt.apex.alpha :as-alias apex]
   [juxt.http.alpha :as-alias http]
   [juxt.pick.alpha :as-alias pick]
   [juxt.pass.alpha :as-alias pass]
   [juxt.site.alpha :as-alias site]
   [juxt.reap.alpha.rfc7230 :as-alias rfc7230]
   [juxt.reap.alpha.rfc7231 :as-alias rfc7231]
   [juxt.reap.alpha.rfc7232 :as-alias rfc7232]))

(defn evaluate-if-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource. If the precondition is found to be false, an exception is thrown
  with ex-data containing the proper response."
  [{::site/keys [current-representations] :as req}]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (when-let [header-field (reap/if-match (get-in req [:ring.request/headers "if-match"]))]
    (log/debugf "evaluate-if-match! %s" header-field)
    (cond
      ;; "If the field-value is '*' …"
      (and (map? header-field)
           (::rfc7232/wildcard header-field)
           (empty? current-representations))
      ;; "… the condition is false if the origin server does not have a current
      ;; representation for the target resource."
      (throw
       (ex-info
        "If-Match precondition failed"
        {::site/request-context (assoc req :ring.response/status 412)}))

      (sequential? header-field)
      (do
        (log/debugf "evaluate-if-match! sequential? true, header-field: %s" header-field)
        (let [matches (for [rep current-representations
                            :let [rep-etag (some-> (get rep ::http/etag) reap/entity-tag)]
                            etag header-field
                            ;; "An origin server MUST use the strong comparison function
                            ;; when comparing entity-tags"
                            :let [_ (log/debugf "evaluate-if-match! - compare %s with %s" etag rep-etag)]
                            :when (rfc7232/strong-compare-match? etag rep-etag)]
                        etag)]
          (log/debugf "matches: %d: %s" (count matches) matches)
          (when-not (seq matches)
            ;; TODO: "unless it can be determined that the state-changing
            ;; request has already succeeded (see Section 3.1)"
            (throw
             (ex-info
              "No strong matches between if-match and current representations"
              {::site/request-context (assoc req :ring.response/status 412)}))))))))

;; TODO: See Section 4.1, RFC 7232:
;;
(defn evaluate-if-none-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource and, when applicable, the representation metadata of the selected
  representation. If the precondition is found to be false, an exception is
  thrown with ex-data containing the proper response."
  [{::site/keys [selected-representation] :as req}]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (let [header-field (reap/if-none-match (get-in req [:ring.request/headers "if-none-match"]))]
    (cond
      (sequential? header-field)
      (when-let [rep-etag (some-> (get selected-representation ::http/etag) reap/entity-tag)]
        ;; "If the field-value is a list of entity-tags, the condition is false
        ;; if one of the listed tags match the entity-tag of the selected
        ;; representation."
        (doseq [etag header-field]
          ;; "A recipient MUST use the weak comparison function when comparing
          ;; entity-tags …"
          (when (rfc7232/weak-compare-match? etag rep-etag)
            (throw
             (if (#{:get :head} (:ring.request/method req))
               (ex-info
                "Not modified"
                {::matching-entity-tag etag
                 ::site/request-context (assoc req :ring.response/status 304)})
               ;; "… or 412 (Precondition Failed) status code for all other
               ;; request methods."
               (ex-info
                "If-None-Match precondition failed"
                {::matching-entity-tag etag
                 ::site/request-context (assoc req :ring.response/status 412)}))))))

      ;; "If-None-Match can also be used with a value of '*' …"
      (and (map? header-field) (::rfc7232/wildcard header-field))
      ;; "… the condition is false if the origin server has a current
      ;; representation for the target resource."
      (when selected-representation
        (throw
         (ex-info
          "At least one representation already exists for this resource"
          {::site/request-context
           (assoc
            req
            :ring.response/status
            (if (#{:get :head} (:ring.request/method req))
              ;; "the origin server MUST respond with either a) the 304 (Not
              ;; Modified) status code if the request method is GET or HEAD
              ;; …"
              304
              ;; "… or 412 (Precondition Failed) status code for all other
              ;; request methods."
              412))}))))))

(defn evaluate-if-unmodified-since! [{::site/keys [selected-representation] :as req}]
  (let [if-unmodified-since-date
        (-> req
            (get-in [:ring.request/headers "if-unmodified-since"])
            reap/http-date
            ::rfc7231/date)]
    (when (.isAfter
           (.toInstant (get selected-representation ::http/last-modified (java.util.Date.)))
           (.toInstant if-unmodified-since-date))
      (throw
       (ex-info
        "Precondition failed"
        {::site/request-context (assoc req :ring.resposne/status 304)})))))

(defn evaluate-if-modified-since! [{::site/keys [selected-representation] :as req}]
  (let [if-modified-since-date
        (-> req
            (get-in [:ring.request/headers "if-modified-since"])
            reap/http-date
            ::rfc7231/date)]

    (when-not (.isAfter
               (.toInstant (get selected-representation ::http/last-modified (java.util.Date.)))
               (.toInstant if-modified-since-date))
      (throw
       (ex-info
        "Not modified"
        {::site/request-context (assoc req :ring.response/status 304)})))))

(defn evaluate-preconditions!
  "Implementation of Section 6 of RFC 7232. Arguments are the (Ring) request, a
  resource (map, typically with Spin namespaced entries, representation metadata
  of the selected representation and the response message origination date."
  [req]
  ;; "… a server MUST ignore the conditional request header fields … when
  ;; received with a request method that does not involve the selection or
  ;; modification of a selected representation, such as CONNECT, OPTIONS, or
  ;; TRACE." -- Section 5, RFC 7232
  (when (not (#{:connect :options :trace} (:ring.request/method req)))
    (if (get-in req [:ring.request/headers "if-match"])
      ;; Step 1
      (evaluate-if-match! req)
      ;; Step 2
      (when (get-in req [:ring.request/headers "if-unmodified-since"])
        (evaluate-if-unmodified-since! req)))
    ;; Step 3
    (if (get-in req [:ring.request/headers "if-none-match"])
      (evaluate-if-none-match! req)
      ;; Step 4, else branch: if-none-match is not present
      (when (#{:get :head} (:ring.request/method req))
        (when (get-in req [:ring.request/headers "if-modified-since"])
          (evaluate-if-modified-since! req))))
    ;; (Step 5 is handled elsewhere)
    ))
