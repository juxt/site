;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql.time-resolver
  (:require [clojure.set :as set]
            [clojure.walk :as w]
            [tick.alpha.calendar :as t.c]
            [tick.alpha.interval :as t.i]
            [tick.core :as t]))

(def this-year (t/year))

(defn ->interval [{:keys [start end]}]
  #:tick{:beginning (:tick/beginning (t.i/bounds (t/date start)))
         :end (:tick/end (t.i/bounds (t/date end)))})

(def holidays
  [{:start "2020-12-20"
    :end "2021-01-04"}
   {:start "2021-02-01"
    :end "2021-02-20"}
   {:start "2021-06-12"
    :end "2021-06-12"}
   {:start "2021-12-12"
    :end "2022-01-05"}])

(def bank-holidays ;; todo - bankholidays / region / JUXTholidays
  [{:start "2021-12-25"
    :end "2021-12-25"}
   {:start "2021-12-26"
    :end "2021-12-26"}
   {:start "2021-12-27"
    :end "2021-12-27"}])

(defn ->holiday-intervals
  "Multirange of users holidays"
  [holidays]
  (map ->interval holidays))

(defn ->year-interval
  "Single year in a multirange"
  [year]
  (t.i/bounds (t/year (or year this-year))))

(defn get-years-of-holiday
  "Calculates the years where there are holidays taken"
  [holiday-intervals]
  (distinct
   (mapcat (fn [{:tick/keys [beginning end]}]
             [(t/year beginning)
              (t/year end)])
           holiday-intervals)))

(defn get-this-years-holiday
  "Time off in a given period = holidays ∩ year"
  [year-interval holiday-intervals]
  (t.i/intersection [year-interval] holiday-intervals))

(defn get-bank-holidays-in-year
  "Bankholidays falling within the year = bank-holidays ∩ year"
  [year-interval bank-holidays]
  (t.i/intersection [year-interval] (map ->interval bank-holidays)))

(defn get-working-days
  "Working days:
  P = year interval
  B = Bankholidays in P
  E = Weekends in P
  W = P - (E ∪ B)"
  [year-interval bankholidays]
  (let [weekends (map t.i/bounds
                      (filter t.c/weekend?
                              (t.i/divide-by t/date
                                             year-interval)))]
    (t.i/difference [year-interval] weekends bankholidays)))

(defn get-actual-years-holidays
  "Holidays taken H = holidays for the year ∩ working days in the year"
  [holiday-for-year working-days]
  (t.i/intersection holiday-for-year working-days))

(defn holiday-report [holidays]
  (let [holiday-intervals (->holiday-intervals holidays)
        years-of-holiday (get-years-of-holiday holiday-intervals)]
    (for [year years-of-holiday
          :let [year-interval (->year-interval year)
                holiday-for-year (get-this-years-holiday year-interval holiday-intervals)
                bank-holidays-for-year (get-bank-holidays-in-year year-interval bank-holidays) ;; todo
                working-days (get-working-days year-interval bank-holidays-for-year)
                actual-years-holidays (get-actual-years-holidays holiday-for-year working-days)
                holiday-days (mapcat (fn [h] (t.i/divide-by t/date h))
                                     actual-years-holidays)]]
      {:year year
       :holidays actual-years-holidays
       :totalDays (count holiday-days)})))

(defn graphql-holiday-report
  "Format holiday report into graphQL readable data"
  [holidays]
  (w/postwalk ->graphql (holiday-report holidays)))

(holiday-report holidays)
;; ({:year #time/year "2020",
;;   :holidays
;;   (#:tick{:beginning #time/date-time "2020-12-20T00:00",
;;           :end #time/date-time "2021-01-01T00:00"}),
;;   :totalDays 9}
;;  {:year #time/year "2021",
;;   :holidays
;;   (#:tick{:beginning #time/date-time "2021-01-01T00:00",
;;           :end #time/date-time "2021-01-05T00:00"}
;;    #:tick{:beginning #time/date-time "2021-02-01T00:00",
;;           :end #time/date-time "2021-02-21T00:00"}
;;    #:tick{:beginning #time/date-time "2021-06-12T00:00",
;;           :end #time/date-time "2021-06-13T00:00"}
;;    #:tick{:beginning #time/date-time "2021-12-12T00:00",
;;           :end #time/date-time "2022-01-01T00:00"}),
;;   :totalDays 31}
;;  {:year #time/year "2022",
;;   :holidays
;;   (#:tick{:beginning #time/date-time "2022-01-01T00:00",
;;           :end #time/date-time "2022-01-06T00:00"}),
;;   :totalDays 3})

(w/postwalk ->graphql (holiday-report holidays))

;; ({"year" "2020",
;;   "holidays"
;;   ({"beginning" #inst "2020-12-20T00:00:00.000-00:00",
;;     "end" #inst "2021-01-01T00:00:00.000-00:00"}),
;;   "totalDays" 9}
;;  {"year" "2021",
;;   "holidays"
;;   ({"beginning" #inst "2021-01-01T00:00:00.000-00:00",
;;     "end" #inst "2021-01-05T00:00:00.000-00:00"}
;;    {"beginning" #inst "2021-02-01T00:00:00.000-00:00",
;;     "end" #inst "2021-02-21T00:00:00.000-00:00"}
;;    {"beginning" #inst "2021-06-11T23:00:00.000-00:00",
;;     "end" #inst "2021-06-12T23:00:00.000-00:00"}
;;    {"beginning" #inst "2021-12-12T00:00:00.000-00:00",
;;     "end" #inst "2022-01-01T00:00:00.000-00:00"}),
;;   "totalDays" 31}
;;  {"year" "2022",
;;   "holidays"
;;   ({"beginning" #inst "2022-01-01T00:00:00.000-00:00",
;;     "end" #inst "2022-01-06T00:00:00.000-00:00"}),
;;   "totalDays" 3})

