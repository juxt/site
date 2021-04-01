;; Copyright © 2021, JUXT LTD.

(ns juxt.mail.alpha.mail
  (:require
   [amazonica.aws.simpleemail :as ses]
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
  (let [{::mail/keys [html-template text-template from subject]} trigger
        html-template (some-> (x/entity db html-template) ::http/content)
        text-template (some-> (x/entity db text-template) ::http/content)]
    (assert html-template)
    (assert text-template)
    (doseq [{::mail/keys [to] :as data} action-data]
      (send-mail!
       from to
       (mail-merge subject data)
       (mail-merge html-template data)
       (mail-merge text-template data)))))
