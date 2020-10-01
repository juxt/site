(ns juxt.site.holidays-test
  (:require [crux.api :as crux]
            [tick.alpha.api :as t]))

(with-open
  [node (crux/start-node {})]

  (crux/submit-tx
   node
   [[:crux.tx/put
     {:crux.db/id :our-holiday
      :owner :jms
      :beginning #inst "2020-10-01"
      :end #inst "2020-10-08"
      :description "My holiday to Bournemouth"}]

    [:crux.tx/put
     {:crux.db/id :xmas-and-new-year
      :owner :jms
      :beginning #inst "2020-12-20"
      :end #inst "2020-12-31"
      :description "Winter holidays"}]

    [:crux.tx/put
     {:crux.db/id :october-break
      :owner :cla
      :beginning #inst "2020-10-15"
      :end #inst "2020-10-20"
      :description "Winter holidays"}]

    [:crux.tx/put
     {:crux.db/id :holiday
      :crux.schema/attributes
      {:beginning {:crux.schema/type :crux.schema.type/date}
       :end {:crux.schema/type :crux.schema.type/date}
       :description {:crux.schema/type :crux.schema.type/string
                     :crux.schema/label "Description"}}}]])

  (crux/sync node)

  (crux/q
   (crux/db node)
   '{:find [?e]
     :where [[?e :beginning]
             [?e :end]
             [?e :owner :jms]]})

  ;; curl -f beginning="2020-10-15" -f end="2020-10-20"

  ;; Generate an HTML form allowing a user to input a holiday
  [:form {:enctype "multipart/form-data"}
   (into
    [:field-set]
    (for [[att-k {:crux.schema/keys [type label]}]
          (ffirst
           (crux/q
            (crux/db node)
            '{:find [?atts]
              :where [[?e :crux.db/id :holiday]
                      [?e :crux.schema/attributes ?atts]]}))
          :let [n (name att-k)]]
      [:div
       (when label [:label {:for n} label])
       [:input {:name n :type "text"}]]))]


  (let [db (crux/db node)]
    (for [e
          (map
           first
           (crux/q
            db
            '{:find [?e]
              :where [[?e :beginning]
                      [?e :end]
                      [?e :owner :jms]]}))]
      (crux/entity db e)))

  ;; TODO: This isn't correct because we're still counting weekends, and public
  ;; holidays
  (apply
   t/+
   (map t/duration
        (t/intersection
         (t/unite
          (sort-by
           :tick/beginning
           [(merge
             {:crux.db/id :our-holiday,
              :owner :jms,
              :description "My holiday to Bournemouth"}
             (t/bounds
              (t/date "2020-10-01")
              (t/date "2020-10-11")))

            (merge
             {:crux.db/id :xmas-and-new-year,
              :owner :jms,
              :description "Winter holidays"}
             (t/bounds
              (t/date "2020-12-20")
              (t/date "2021-01-06")))

            (merge
             {:crux.db/id :skiing,
              :owner :jms,
              :description "Lockdown skiing in Japan!!"}
             (t/bounds
              (t/date "2020-03-01")
              (t/date "2020-03-11")))

            (merge
             {:crux.db/id :fly-back-from-japan,
              :owner :jms,
              :description "Flyback from Japan"}
             (t/bounds
              (t/date "2020-03-07")
              (t/date "2020-03-15")))]))

         [(t/bounds (t/year 2020))]))))
