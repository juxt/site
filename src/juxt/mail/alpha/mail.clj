;; Copyright © 2021, JUXT LTD.

(ns juxt.mail.alpha.mail
  (:require
   [amazonica.aws.sns :as sns]
   [integrant.core :as ig]
   [juxt.site.alpha.triggers :as triggers]
   [crux.api :as x]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [postal.core :as postal]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))

(defmethod ig/init-key ::sns-client [_ {:keys [enabled?]}]
  (if enabled?
    sns/publish
    (fn [& {:keys [message phone-number message-attributes]}]
      (log/infof "sns-client/publish-stub: phone-number %s, message %s" phone-number message))))

(defmethod ig/init-key ::smtp-client [_ {:keys [enabled?]}]
  (if enabled?
    postal/send-message
    (fn [_ {:keys [to from subject]}]
      (log/infof "smtp-client/send-email-stub: to %s, from %s, subject %s"
                 to
                 from
                 subject))))

(defn send-sms! [sns-client from-sms-name to-phone-number subject text-body]

  (log/debugf "Sending sms to %s with subject %s, body %s" to-phone-number subject text-body)

  (future
    (try
      (sns-client :message (str subject "\n--\n" text-body)
                  :phone-number to-phone-number
                  :message-attributes {"AWS.SNS.SMS.SenderID" (or from-sms-name "TESTSMS")
                                       "AWS.SNS.SMS.SMSType" "Transactional"})
      (catch Exception e
        (log/error e)))))

(defn send-mail! [smtp-client server from to subject html-body text-body]

  (log/debugf "Sending email to %s with subject %s, body %s" to subject html-body)

  (future
    (try
      (smtp-client server
                   {:from from
                    :to to
                    :subject subject
                    :body [:alternative
                           {:type "text/plain"
                            :content text-body}
                           {:type "text/html"
                            :content html-body}]})
      (catch Exception e
        (log/error e)))))

(defn mail-merge [template data]
  (str/replace
   template #"\{\{([^\}]*)\}\}"
   (fn [[_ grp]]
     (get-in
      data
      (for [mtch (re-seq #"[\p{Alnum}\.:/_-]+" grp)]
        (cond (.startsWith mtch ":") (keyword (subs mtch 1))
              ;(re-matches #"\"([\"]*)\"" (string))
              :else  mtch))
      "<blank>"))))

(defmethod triggers/run-action! ::mail/send-emails
  [{::site/keys [db smtp-client]} {:keys [trigger action-data]}]
  (let [{::mail/keys [html-template
                      text-template
                      from
                      subject
                      host
                      username
                      password
                      port]} trigger
        html-template (some-> (x/entity db html-template) ::http/content)
        text-template (some-> (x/entity db text-template) ::http/content)]
    (assert html-template)
    (assert text-template)
    (doseq [{:keys [user] :as data} action-data]
      (send-mail!
       smtp-client
       {:host host
        :user username
        :pass password
        :port port
        :tls true}
       from (:email user)
       (mail-merge subject data)
       (mail-merge html-template data)
       (mail-merge text-template data)))))

(defmethod triggers/run-action! ::mail/send-sms
  [{::site/keys [db sns-client]} {:keys [trigger action-data]}]
  (let [{::mail/keys [text-template from-sms-name subject]} trigger
        text-template (some-> (x/entity db text-template) ::http/content)]
    (assert text-template)
    (doseq [{:keys [user] :as data} action-data]
      (when-let [phone-number (:phone-number user)]
        (send-sms!
         sns-client
         from-sms-name phone-number
         (mail-merge subject data)
         (mail-merge text-template data))))))
