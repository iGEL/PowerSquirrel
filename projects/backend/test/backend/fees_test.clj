(ns backend.fees-test
  (:require
   [backend.fees :as fees]
   [clojure.test :refer [deftest is testing]]
   [dinero.core :refer [money-of]]
   [tick.core :as t]))

(deftest parse-test
  (testing "a schedule"
    (is (= [{:valid-from (t/date "2025-01-01")
             :timezone "Europe/Berlin"
             :fees [{:name "Konzessionsabgabe"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0239M :eur)}]}
                     :position 10}
                    {:name "Netzentgelte"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0997M :eur)}]}
                     :position 20}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :schedule {:default [{:start (t/time "00:00")
                                           :percent 19M}]}
                     :position 100}]}
            {:valid-from (t/date "2025-04-01")
             :timezone "Europe/Berlin"
             :fees [{:name "Konzessionsabgabe"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0239M :eur)}]}
                     :position 10}
                    {:name "Netzentgelte"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0349M :eur)}
                                          {:start (t/time "06:30")
                                           :price (money-of 0.0997M :eur)}
                                          {:start (t/time "17:15")
                                           :price (money-of 0.1863M :eur)}
                                          {:start (t/time "20:15")
                                           :price (money-of 0.0997M :eur)}
                                          {:start (t/time "22:15")
                                           :price (money-of 0.0349M :eur)}]
                                #time/day-of-week "SATURDAY" [{:start (t/time "00:00")
                                                               :price (money-of 0.0349M :eur)}]
                                #time/day-of-week "SUNDAY" [{:start (t/time "00:00")
                                                             :price (money-of 0.0349M :eur)}]}
                     :position 20}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :schedule {:default [{:start (t/time "00:00")
                                           :percent 19M}]}
                     :position 100}]}]
           (fees/parse "test/fixtures/fees.json")))))

(deftest merge-test
  (testing "merging two schedules"
    (is (= [{:valid-from (t/date "2025-01-01")
             :timezone "Europe/Berlin"
             :fees [{:name "Netzentgelte"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0997M :eur)}]}
                     :position 20}
                    {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.00277M :eur)}]}
                     :position 30}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :schedule {:default [{:start (t/time "00:00")
                                           :percent 19M}]}
                     :position 100}]}
            {:valid-from (t/date "2025-04-01")
             :timezone "Europe/Berlin"
             :fees [{:name "Netzentgelte"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.0349M :eur)}
                                          {:start (t/time "06:30")
                                           :price (money-of 0.0997M :eur)}
                                          {:start (t/time "17:15")
                                           :price (money-of 0.1863M :eur)}
                                          {:start (t/time "20:15")
                                           :price (money-of 0.0997M :eur)}
                                          {:start (t/time "22:30")
                                           :price (money-of 0.0349M :eur)}]}
                     :position 20}
                    {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                     :pricing "per_kwh"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (money-of 0.00277M :eur)}]}
                     :position 30}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :schedule {:default [{:start (t/time "00:00")
                                           :percent 19M}]}
                     :position 100}]}]
           (fees/merge
            [{:valid-from (t/date "2025-01-01")
              :timezone "Europe/Berlin"
              :fees [{:name "Mehrwertsteuer"
                      :pricing "percent"
                      :schedule {:default [{:start (t/time "00:00")
                                            :percent 19M}]}
                      :position 100}
                     {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                      :pricing "per_kwh"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (money-of 0.00277M :eur)}]}
                      :position 30}]}]
            [{:valid-from (t/date "2025-01-01")
              :timezone "Europe/Berlin"
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (money-of 0.0997M :eur)}]}
                      :position 20}]}
             {:valid-from (t/date "2025-04-01")
              :timezone "Europe/Berlin"
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (money-of 0.0349M :eur)}
                                           {:start (t/time "06:30")
                                            :price (money-of 0.0997M :eur)}
                                           {:start (t/time "17:15")
                                            :price (money-of 0.1863M :eur)}
                                           {:start (t/time "20:15")
                                            :price (money-of 0.0997M :eur)}
                                           {:start (t/time "22:30")
                                            :price (money-of 0.0349M :eur)}]}
                      :position 20}]}])))))
