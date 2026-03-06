(ns cloud.electricity-price-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [cloud.electricity-price :as electricity-price]
   [dinero.core :refer [money-of]]
   [tick.core :as t]))

(deftest calculate-detailed
  (let [fees [{:valid-from (t/date "2025-01-01")
               :timezone "Europe/Berlin"
               :fees [{:name "Netzentgelte"
                       :pricing "per_kwh"
                       :schedule {:default [{:start (t/time "00:00")
                                             :price (money-of 0.0997M :eur)}]}
                       :position 20}
                      {:name "Stromsteuer"
                       :pricing "per_kwh"
                       :currency "EUR"
                       :schedule {:default [{:start (t/time "00:00")
                                             :price (money-of 0.0205M :eur)}]}
                       :position 30}
                      {:name "Mehrwertsteuer"
                       :pricing "percent"
                       :currency "EUR"
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
                                            {:start (t/time "22:15")
                                             :price (money-of 0.0349M :eur)}]
                                  #time/day-of-week "SATURDAY" [{:start (t/time "00:00")
                                                                 :price (money-of 0.0349M :eur)}]
                                  #time/day-of-week "SUNDAY" [{:start (t/time "00:00")
                                                               :price (money-of 0.0349M :eur)}]}
                       :position 20}
                      {:name "Stromsteuer"
                       :pricing "per_kwh"
                       :currency "EUR"
                       :schedule {:default [{:start (t/time "00:00")
                                             :price (money-of 0.0205M :eur)}]}
                       :position 30}
                      {:name "Mehrwertsteuer"
                       :pricing "percent"
                       :currency "EUR"
                       :schedule {:default [{:start (t/time "00:00")
                                             :percent 19M}]}
                       :position 100}]}]
        net-prices-for (fn [date]
                         (reduce
                          (fn [prev {:keys [start price]}]
                            (conj prev
                                  {:start (-> date
                                              (t/at start)
                                              (t/in (t/zone "UTC")))
                                   :price price}))
                          []
                          [{:start (t/time "00:00")
                            :price (money-of 0.08M :eur)}
                           {:start (t/time "06:00")
                            :price (money-of 0.09M :eur)}
                           {:start (t/time "12:00")
                            :price (money-of 0.04M :eur)}
                           {:start (t/time "18:00")
                            :price (money-of 0.13M :eur)}]))]
    (testing "simple cases"
      (is (= {:net (money-of 0.08M :eur)
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :price (money-of 0.0997M :eur)}
                     {:name "Stromsteuer"
                      :pricing "per_kwh"
                      :price (money-of 0.0205M :eur)}
                     {:name "Mehrwertsteuer"
                      :pricing "percent"
                      :percent 19M
                      :price (money-of 0.038038M :eur)}]
              :total (money-of 0.238238M :eur)}
             (electricity-price/calculate
              #time/zoned-date-time "2025-03-31T05:59Z"
              (net-prices-for (t/date "2025-03-31"))
              fees)))
      (is (= {:net (money-of 0.09M :eur)
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :price (money-of 0.0997M :eur)}
                     {:name "Stromsteuer"
                      :pricing "per_kwh"
                      :price (money-of 0.0205M :eur)}
                     {:name "Mehrwertsteuer"
                      :pricing "percent"
                      :percent 19M
                      :price (money-of 0.039938M :eur)}]
              :total (money-of 0.250138M :eur)}
             (electricity-price/calculate
              #time/zoned-date-time "2025-03-31T06:00Z"
              (net-prices-for (t/date "2025-03-31"))
              fees))))
    (testing "changing Netzentgelte"
      (is (= {:net (money-of 0.08M :eur)
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :price (money-of 0.0349M :eur)}
                     {:name "Stromsteuer"
                      :pricing "per_kwh"
                      :price (money-of 0.0205M :eur)}
                     {:name "Mehrwertsteuer"
                      :pricing "percent"
                      :percent 19M
                      :price (money-of 0.025726M :eur)}]
              :total (money-of 0.161126M :eur)}
             (electricity-price/calculate
              #time/zoned-date-time "2025-08-15T03:59Z"
              (net-prices-for (t/date "2025-08-15"))
              fees)))
      (is (= {:net (money-of 0.08M :eur)
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :price (money-of 0.0997M :eur)}
                     {:name "Stromsteuer"
                      :pricing "per_kwh"
                      :price (money-of 0.0205M :eur)}
                     {:name "Mehrwertsteuer"
                      :pricing "percent"
                      :percent 19M
                      :price (money-of 0.038038M :eur)}]
              :total (money-of 0.238238M :eur)}
             (electricity-price/calculate
              #time/zoned-date-time "2025-03-31T05:59Z"
              (net-prices-for (t/date "2025-03-31"))
              fees))))
    (testing "special weekdays (Saturday)"
      (is (= {:net (money-of 0.08M :eur)
              :fees [{:name "Netzentgelte"
                      :pricing "per_kwh"
                      :price (money-of 0.0349M :eur)}
                     {:name "Stromsteuer"
                      :pricing "per_kwh"
                      :price (money-of 0.0205M :eur)}
                     {:name "Mehrwertsteuer"
                      :pricing "percent"
                      :percent 19M
                      :price (money-of 0.025726M :eur)}]
              :total (money-of 0.161126M :eur)}
             (electricity-price/calculate
              #time/zoned-date-time "2025-08-16T04:00Z"
              (net-prices-for (t/date "2025-08-16"))
              fees))))))
