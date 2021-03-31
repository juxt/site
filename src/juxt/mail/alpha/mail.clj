;; Copyright © 2021, JUXT LTD.

(ns juxt.mail.alpha.mail
  (:require
   [amazonica.aws.simpleemail :as ses]
   ;;   [amazonica.aws.pinpoint :as pp]
   [juxt.site.alpha.effects :as fx]
   [crux.api :as x]
   [clojure.tools.logging :as log]
   [clojure.string :as str]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'mail (create-ns 'juxt.mail.alpha))

(defn get-valid-source-emails []
  (set (filter #(re-seq #"@" %)(:identities (ses/list-identities)))))

(get-valid-source-emails)

(defn send-mail! [from to subject html-body text-body]

  (log/tracef "Sending email to %s with subject %s, body %s" to subject html-body)

  (when-not (contains? (get-valid-source-emails) from)
    (throw (ex-info "Failed to send email" {:from from :to to})))

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

(defmethod fx/run-effect! ::mail/send-emails [{::site/keys [db crux-node]}
                                              {:keys [effect results]}]
  (let [{::mail/keys [html-template text-template from subject]} effect
        html-template (some-> (x/entity db html-template) ::http/content)
        text-template (some-> (x/entity db text-template) ::http/content)]
    (doseq [{::mail/keys [to] :as data} results]
      (send-mail!
       from to
       (mail-merge subject data)
       (mail-merge html-template data)
       (mail-merge text-template data)))))

;; Make this a test
#_(let [template "Alert on {{ :juxt.site.alpha/resource :title }} -- {{ :juxt.site.alpha/uri }}"
        data {::site/resource {:title "Heart Monitor"}
              ::site/uri "https://example.com/alerts/123"}]
    (mail-merge template data))

#_(let [db (x/db *crux-node*)]
    (doseq [[recipient alert]
            (x/q db '{:find [(eql/project user [:crux.db/id ::email]) alert]
                      :where [[alert ::site/type "Alert"]
                              [user ::site/type "User"]
                              [user ::email? true]
                              [mapping ::role "https://example.org/roles/service-manager"]
                              [mapping ::user user]
                              (not-join [user alert]
                                        [mr ::site/type "MailRecord"]
                                        [mr ::recipient user]
                                        [mr ::about alert])]})]

      ;; Send email to recipient (::email recipient)
      (log/tracef "Send email to %s (id=%s)" (::email recipient) (:crux.db/id recipient))

      (->>
       (x/submit-tx
        *crux-node*
        [[:crux.tx/put {:crux.db/id "https://example.org/maillogs/123"
                        ::site/type "MailRecord"
                        ::recipient (:crux.db/id recipient)
                        ::about alert}]])
       (x/await-tx *crux-node*))))
