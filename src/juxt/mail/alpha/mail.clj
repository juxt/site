;; Copyright © 2021, JUXT LTD.

(ns juxt.mail.alpha.mail
  (:require
   [amazonica.aws.simpleemail :as ses]
   [amazonica.aws.sns :as sns]
   [integrant.core :as ig]
   [juxt.site.alpha.triggers :as triggers]
   [xtdb.api :as xt]
   [crux.query :as cruxq]
   [clojure.tools.logging :as log]
   [clojure.string :as str]))

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

(defmethod ig/init-key ::ses-client [_ {:keys [enabled?]}]
  (if enabled?
    ses/send-email
    (fn [& {:keys [destination source message]}]
      (log/infof "ses-client/send-email-stub: destination %s, source %s, message %s"
                 destination
                 source
                 (dissoc message :body)))))

(defn send-sms! [sns-client from-sms-name to-phone-number subject text-body]

  (log/infof "Sending sms to %s with subject %s" to-phone-number subject)
  (log/debugf "SMS body %s" text-body)

  (future
    (try
      (sns-client :message (str subject "\n--\n" text-body)
                  :phone-number to-phone-number
                  :message-attributes {"AWS.SNS.SMS.SenderID" (or from-sms-name "TESTSMS")
                                       "AWS.SNS.SMS.SMSType" "Transactional"})
      (catch Exception e
        (log/error e)))))

(defn send-mail! [ses-client from to subject html-body text-body]
  (let [to (vec (flatten [to]))]
    (log/infof "Sending email to %s with subject %s" to subject)
    (log/debugf "Email body %s" html-body)

    (future
      (try
        (ses-client
         :destination {:to-addresses to}
         :source from
         :message {:subject subject
                   :body {:html html-body
                          :text text-body}})
        (catch Exception e
          (log/error e))))))

(defn mail-merge [template data]
  (str/replace
   template #"\{\{([^\}]*)\}\}"
   (fn [[_ grp]]
     (-> (get-in
          data
          (for [mtch (re-seq #"[\p{Alnum}\.:/_-]+" grp)]
            (cond (.startsWith mtch ":") (keyword (subs mtch 1))
                  ;(re-matches #"\"([\"]*)\"" (string))
                  :else  mtch))
          "<blank>")
         str))))

(defmethod triggers/run-action! ::mail/send-emails
  [{::site/keys [db ses-client]} {:keys [trigger action-data]}]
  (let [{::mail/keys [html-template text-template from subject]} trigger
        html-template (some-> (xt/entity db html-template) ::http/content)
        text-template (some-> (xt/entity db text-template) ::http/content)]
    (assert html-template)
    (assert text-template)
    (doseq [{:keys [user] :as data} action-data]
      (send-mail!
       ses-client
       from (:email user)
       (mail-merge subject data)
       (mail-merge html-template data)
       (mail-merge text-template data)))))

(defmethod triggers/run-action! ::mail/send-sms
  [{::site/keys [db sns-client]} {:keys [trigger action-data]}]
  (let [{::mail/keys [text-template from-sms-name subject]} trigger
        text-template (some-> (xt/entity db text-template) ::http/content)]
    (assert text-template)
    (doseq [{:keys [user] :as data} action-data]
      (when-let [phone-number (:phone-number user)]
        (send-sms!
         sns-client
         from-sms-name phone-number
         (mail-merge subject data)
         (mail-merge text-template data))))))

(defmethod cruxq/aggregate 'sorted-string-list [_]
  (fn
    ([] #{})
    ([acc] (str/join "&" (sort acc)))
    ([acc x] (conj acc x))))

(defmethod cruxq/aggregate 'merge-with-flatten [_]
  (fn
    ([] nil)
    ([acc] acc)
    ([acc x] (merge-with #(-> [%1 %2] flatten distinct) acc x))))
