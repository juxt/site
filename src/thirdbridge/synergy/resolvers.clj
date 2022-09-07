(ns thirdbridge.synergy.resolvers
  (:require [juxt.site.alpha.graphql :as graphql]
            [juxt.site.alpha.xtdb :refer [put!]]
            [thirdbridge.synergy.router :as router]
            [clojure.tools.logging :as log]
            [camel-snake-kebab.extras :as cske]
            [camel-snake-kebab.core :as csk]
            [tick.core :as t]
            [xtdb.api :as xt]))

(def allowed-status ["NORMALISED" "ROUTED" "ROUTE_UNKNOWN" "ROUTE_FAILED" "HANDLED" "HANDLE_FAILED"])

(defn create-event-process-update
  "create-event-process-update expects the following input parameters:
  - synergy-event-id: The synergy event id that the event process update relates to.
  - status-context: The status context reporting the status. This should be the name that the reporting lambda knows
   itself by, e.g. \"normaliser-domain-events\".
  - recorded-status: The status to be recorded. Should be one of the following defined in the variable allowed-status
  (recorded in the synergy-events graphql schema): NORMALISED, ROUTED, ROUTE_UNKNOWN, ROUTE_FAILED, HANDLED, HANDLE_FAILED.
  Returns the event-process-update body which should then be stored in site otherwise nil if incorrect status sent."
  [synergy-event-id status-context recorded-status]
  (if (some (partial = recorded-status) allowed-status)
    {:synergy-event-id    synergy-event-id
     :status-context     status-context
     :recorded-status    recorded-status
     :status-timestamp   (str (t/instant))}
    nil))

(defn put-event-status-docs!
  [{:keys [xt-node type-k]} id status-messages]
  (let [event-statuses
        (if status-messages
          (for [{:keys [return-value success]} status-messages
                :let [status-context (get-in return-value [:endpoint])
                      recorded-status (if success "ROUTED" "ROUTE_FAILED")]]
            (create-event-process-update id status-context recorded-status))
          (create-event-process-update id "synergy-router" "ROUTE_UNKNOWN"))
        docs (for [status event-statuses]
               (assoc status
                      type-k "EventProcessStatus"
                      :xt/id (random-uuid)))]
    (log/infof "putting event status docs: %s" (map :xt/id docs))
    (try
      (apply put! xt-node docs)
      (catch Exception e
        (log/error "Error putting event status docs" e)))))

(defn create-synergy-event-mutation
  [{:keys [xt-node] :as opts}]
  (let  [{:keys [eventAction eventVersion xt/id] :as event}
         (try
           (graphql/perform-mutation! opts true)
           (catch Exception e
             (log/error "Error creating synergy event" e)))
         router-type-k
         (keyword (get-in (xt/entity (xt/db xt-node) "/apis/graphql/synergy/router")
                          [:juxt.site.alpha/graphql-compiled-schema
                           :juxt.grab.alpha.schema/directives
                           "site"
                           :juxt.grab.alpha.graphql/arguments
                           "type"]))
         endpoints-to-route-to
         (map
          #(cske/transform-keys csk/->kebab-case-keyword (first %))
          (xt/q (xt/db xt-node)
                {:find ['(pull e [:destinationType :dispatchDestination :xt/id])]
                 :where [['e router-type-k "SynergyRoute"]
                         ['e :eventAction eventAction]
                         ['e :eventVersion eventVersion]]}))
         status-messages (router/send-synergy-id-to-handlers id endpoints-to-route-to)]
    (put-event-status-docs! opts id status-messages)
    event))
