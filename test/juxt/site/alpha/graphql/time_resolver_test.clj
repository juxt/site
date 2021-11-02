;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.graphql.time-resolver-test
  (:require
   [juxt.site.alpha.graphql.time-resolver :as model]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]
   [tick.alpha.calendar :as t.c]
   [clojure.test :refer [deftest is testing]]))

(deftest holiday-report-test
  (let [holidays [{:start "2020-12-20"
                   :end "2021-01-04"}
                  {:start "2021-02-01"
                   :end "2021-02-20"}
                  {:start "2021-06-12"
                   :end "2021-06-12"}
                  {:start "2021-12-12"
                   :end "2022-01-05"}]
        report (model/holiday-report holidays)]

    (testing "can identify years for report"
      (is (= #{"2020" "2021" "2022"}
             (set (map (comp str :year) report)))))

    (testing "year report is within bounds of year"
      (is (every? (fn [date-time]
                    (t/<= #time/date-time "2021-01-01T00:00"
                          date-time
                          #time/date-time "2022-01-01T00:00"))

                  (mapcat vals
                          (some (fn [{:keys [year holidays]}]
                                  (when (= #time/year "2021" year) holidays))
                                report)))))

    (testing "holidays don't fall on weekends"
      (is (every? (complement t.c/weekend?)
                  (mapcat (fn  [h] (t.i/divide-by t/date h))
                          (mapcat :holidays report)))))))
