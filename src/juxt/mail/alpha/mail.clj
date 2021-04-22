;; Copyright © 2021, JUXT LTD.

(ns juxt.mail.alpha.mail
  (:require
   [amazonica.aws.simpleemail :as ses]
   [amazonica.aws.sns :as sns]
   ;;   [amazonica.aws.pinpoint :as pp]
   [juxt.site.alpha.triggers :as triggers]
   [crux.api :as x]
   [clojure.tools.logging :as log]
   [clojure.string :as str]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))

(defn send-sms! [from-sms-name to-phone-number subject text-body]
  (sns/publish :message (str subject "\n--\n" text-body)
               :phone-number to-phone-number
               :message-attributes {"AWS.SNS.SMS.SenderID" (or from-sms-name "TESTSMS")
                                    "AWS.SNS.SMS.SMSType" "Transactional"}))

(defn send-mail! [from to subject html-body text-body]

  (log/debugf "Sending email to %s with subject %s, body %s" to subject html-body)

  (ses/send-email
   :destination {:to-addresses [to]}
   :source from
   :message {:subject subject
             :body {:html html-body
                    :text text-body}}))

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
  [{::site/keys [db]} {:keys [trigger action-data]}]
  (let [{::mail/keys [html-template text-template from from-sms-name subject]} trigger
        html-template (some-> (x/entity db html-template) ::http/content)
        text-template (some-> (x/entity db text-template) ::http/content)]
    (assert html-template)
    (assert text-template)
    (doseq [{::mail/keys [to-phone-number] :as data} action-data]
      (when to-phone-number
        (send-sms!
         from-sms-name to-phone-number
         (mail-merge subject data)
         (mail-merge text-template data))))
    (doseq [{::mail/keys [to] :as data} action-data]
      (send-mail!
       from to
       (mail-merge subject data)
       (mail-merge html-template data)
       (mail-merge text-template data)))))
