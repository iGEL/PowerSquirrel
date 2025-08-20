(ns backend.fees-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [power-squirrel.fees :as fees]
   [tick.core :as t]))

(deftest parse-test
  (testing "simple schedule"
    (is (= [{:valid-from (t/date "2025-01-01")
             :fees [{:name "Konzessionsabgabe"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.0239")}]}
                     :position 10}
                    {:name "Netzentgelte"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.0997")}]}
                     :position 20}]}
            {:valid-from (t/date "2025-04-01")
             :fees [{:name "Konzessionsabgabe"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.0239")}]}
                     :position 10}
                    {:name "Netzentgelte"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.1")}]}
                     :position 20}]}]
           (fees/parse "test/fixtures/simple.json")))))

(deftest merge-test
  (testing "merging two schedules"
    (is (= [{:valid-from (t/date "2025-01-01")
             :fees [{:name "Netzentgelte"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.0997")}]}
                     :position 20}
                    {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.00277")}]}
                     :position 30}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "19")}]}
                     :position 100}]}
            {:valid-from (t/date "2025-04-01")
             :fees [{:name "Netzentgelte"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.0349")}
                                          {:start (t/time "06:30")
                                           :price (bigdec "0.0997")}
                                          {:start (t/time "17:15")
                                           :price (bigdec "0.1863")}
                                          {:start (t/time "20:15")
                                           :price (bigdec "0.0997")}
                                          {:start (t/time "22:30")
                                           :price (bigdec "0.0349")}]}
                     :position 20}
                    {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                     :pricing "per_kwh"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "0.00277")}]}
                     :position 30}
                    {:name "Mehrwertsteuer"
                     :pricing "percent"
                     :currency "EUR"
                     :schedule {:default [{:start (t/time "00:00")
                                           :price (bigdec "19")}]}
                     :position 100}]}]
           (fees/merge
            [{:valid-from (t/date "2025-01-01")
              :fees [{:name "Mehrwertsteuer"
                      :pricing "percent"
                      :currency "EUR"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (bigdec "19")}]}
                      :position 100}
                     {:name "Kraft-Wärme-Kopplungsgesetz-Umlage"
                      :pricing "per_kwh"
                      :currency "EUR"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (bigdec "0.00277")}]}
                      :position 30}]}]
            [{:valid-from (t/date "2025-01-01")
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :currency "EUR"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (bigdec "0.0997")}]}
                      :position 20}]}
             {:valid-from (t/date "2025-04-01")
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :currency "EUR"
                      :schedule {:default [{:start (t/time "00:00")
                                            :price (bigdec "0.0349")}
                                           {:start (t/time "06:30")
                                            :price (bigdec "0.0997")}
                                           {:start (t/time "17:15")
                                            :price (bigdec "0.1863")}
                                           {:start (t/time "20:15")
                                            :price (bigdec "0.0997")}
                                           {:start (t/time "22:30")
                                            :price (bigdec "0.0349")}]}
                      :position 20}]}])))))
