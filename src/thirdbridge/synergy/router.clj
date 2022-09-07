(ns thirdbridge.synergy.router
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.client.api.async :as aws.async]
            [clojure.core.async :as a]

            [clojure.tools.logging :as log]))

(def endpoint-override (if-let [override-edn (System/getProperty "aws.endpoint.override")]
                         {:endpoint-override (read-string override-edn)}
                         {}))

(def sqs (aws/client (merge {:api :sqs :retriable? false :backoff nil} endpoint-override)))

(def sns (aws/client (merge {:api :sns} endpoint-override)))

(defn sns-send
  [body]
  (log/infof "Sending message to SNS: %s" (str body))
  (aws/invoke sns body))

(defn sqs-send
  [body]
  (log/infof "Sending2 message to SQS: %s" (str body))
  (try
    (aws/invoke sqs body)
    (catch Exception e
      (log/errorf "Error sending message to SQS: %s" (str e)))))

(defn gen-status-map
  "Generate a status map from the values provided"
  [status-code status-message return-value]
  {:success status-code :description status-message :return-value return-value})

(defmulti dispatch-to-endpoint
  "Dispatches synergy event id to endpoints depending on the type"
  (fn [endpoint-type endpoint synergy-id] (:destination-type endpoint-type)))

(defmethod dispatch-to-endpoint "TOPIC"
  [_ endpoint synergy-id]
  (log/infof "Adding synergy-event-id \"%s\" to sns topic \"%s\"" synergy-id endpoint)
  (let [sns-send-response (sns-send {:op :Publish :request {:TopicArn endpoint
                                                            :MessageGroupId "synergy-router"
                                                            :MessageDeduplicationId "synergy-router"
                                                            :Message synergy-id}})
        response-msg-id (:MessageId sns-send-response)]
    ;; Get the response and determine if there were any errors
    (log/infof "SNS response message received \"%s\"" sns-send-response)
    (if (nil? response-msg-id)
      (do
        (log/errorf "Error dispatching synergy-event-id \"%s\" to topic : \"%s\" " synergy-id endpoint)
        (gen-status-map false "error-dispatching-to-topic" {:synergy-event-id synergy-id
                                                            :endpoint endpoint
                                                            :error sns-send-response}))
      (do
        (log/infof "Successfully dispatched synergy-event-id \"%s\" to topic : \"%s\"" synergy-id endpoint)
        (gen-status-map true "dispatched-to-topic" {:synergy-event-id synergy-id
                                                    :endpoint endpoint
                                                    :messageId response-msg-id})))))

(defmethod dispatch-to-endpoint "QUEUE"
  [_ endpoint synergy-id]
  (log/infof "Adding synergy-event-id \"%s\" to sqs queue \"%s\"" synergy-id endpoint)
  (let [sqs-send-response (sqs-send {:op :SendMessage
                                     :request {:QueueUrl endpoint
                                               :MessageGroupId "synergy-router"
                                               :MessageDeduplicationId "synergy-router"
                                               :MessageBody synergy-id}})
        response-msg-id (:MessageId sqs-send-response)]
    ;; Get the response and determine if there were any errors
    (log/infof "SQS response message received \"%s\"" sqs-send-response)
    (if (nil? response-msg-id)
      (do
        (log/errorf "Error dispatching synergy-event-id \"%s\" to queue : \"%s\" " synergy-id endpoint)
        (gen-status-map false "error-dispatching-to-queue" {:synergy-event-id synergy-id
                                                            :endpoint endpoint
                                                            :error sqs-send-response}))
      (do
        (log/infof "Successfully dispatched synergy-event-id \"%s\" to queue : \"%s\"" synergy-id endpoint)
        (gen-status-map true "dispatched-to-queue" {:synergy-event-id synergy-id
                                                    :endpoint endpoint
                                                    :messageId response-msg-id})))))

(defmethod dispatch-to-endpoint :default
  [endpoint-type endpoint synergy-id]
  (log/errorf "Received unknown endpoint type: \"%s\"." endpoint-type)
  (gen-status-map false "unknown-endpoint-type" {:synergy-event-id synergy-id
                                                 :endpoint         endpoint
                                                 :error            endpoint-type}))

(defn send-synergy-id-to-handlers
  "send-synergy-id-to-handlers takes a vector of maps containing 0 or more routing instructions of where to send synergy event id"
  [synergy-event-id endpoints-to-route-to]
  (prn "sending id" synergy-event-id)
  (into [] (doall (for [{:keys [destination-type dispatch-destination]} endpoints-to-route-to]
                    (dispatch-to-endpoint {:destination-type (str destination-type)} dispatch-destination synergy-event-id)))))

(comment
  (send-synergy-id-to-handlers
   "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab"
   [{:destination-type     "TOPIC"
     :dispatch-destination "arn:aws:sns:eu-west-1:123456789012:synergy_event_declines"}
    {:destination-type     "QUEUE"
     :dispatch-destination "arn:aws:sqs:eu-west-1:987654321098:synergy_event_declines"}
    {:destination-type     "S3"
     :dispatch-destination "arn:aws:s3:eu-west-1:987654321098:synergy_event_declines"}])
  (send-synergy-id-to-handlers
   "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab"
   [{:destination-type     "TOPIC"
     :dispatch-destination "arn:aws:sns:eu-west-1:123456789012:synergy_event_declines"}
    {:destination-type     "QUEUE"
     :dispatch-destination "arn:aws:sqs:eu-west-1:987654321098:synergy_event_declines"}])
  (send-synergy-id-to-handlers "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab" [])

  (gen-status-map true "dispatched-to-queue" {:synergy-event-id "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab"
                                              :endpoint         "arn:aws:sqs:eu-west-1:987654321098:synergy_event_declines"
                                              :messageId        "response-msg-id"})

  (def success-map (gen-status-map true "dispatched-to-queue" {:synergy-event-id "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab"
                                                               :endpoint         "arn:aws:sqs:eu-west-1:987654321098:synergy_event_declines"
                                                               :messageId        "response-msg-id"}))
  (get-in success-map [:return-value :endpoint])

  (def multiple-success-map [{:status true,
                              :description "dispatched-to-queue",
                              :return-value {:synergy-event-id "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab",
                                             :endpoint "arn:aws:sqs:eu-west-1:987654321098:synergy_event_declines",
                                             :messageId "response-msg-id"}}
                             {:status true,
                              :description "dispatched-to-queue",
                              :return-value {:synergy-event-id "SynergyEventcd8175c8-0ee1-48b4-88ab-1068d33c3dab",
                                             :endpoint "arn:aws:sqs:eu-west-1:987654321098:salesforce-declines",
                                             :messageId "response-msg-id"}}])

  (into [] (doall (for [{:keys [return-value]} multiple-success-map]
                    (get-in return-value [:endpoint])))))
