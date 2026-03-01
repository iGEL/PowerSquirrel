(ns backend.energy-charts-test
  (:require
   [backend.energy-charts :as energy-charts]
   [backend.result :refer [->Ok err? ok?]]
   [backend.system :as system]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]
   [clojure.test :refer [deftest is use-fixtures]]
   [dinero.core :refer [money-of]]
   [tick.core :as t]))

(def ^:dynamic *system* nil)

(use-fixtures :once (fn [test-fn]
                      (binding [*system* (system/init)]
                        (test-fn)
                        (system/halt *system*))))

(deftest default
  (let [result (with-fake-routes-in-isolation
                 {"https://api.energy-charts.info/price?bzn=DE-LU&start=1772319600&end=1772405100"
                  (constantly
                   {:status 200
                    :body (slurp "test/fixtures/energy_charts_1772319600_1772405100.json")})}
                 (energy-charts/fetch-prices<> (:backend.energy-charts/energy-charts *system*)
                                               (t/date "2026-03-01")
                                               :de-lu))]
    (is (ok? result))
    (is (= 96
           (->> result :val :schedule count)))
    (is (= (->Ok {:pricing "per_kwh"
                  :schedule [{:start #time/zoned-date-time "2026-02-28T23:00Z[UTC]"
                              :price (money-of 0.04765M :eur)}
                             {:start #time/zoned-date-time "2026-02-28T23:15Z[UTC]"
                              :price (money-of 0.04889M :eur)}
                             {:start #time/zoned-date-time "2026-02-28T23:30Z[UTC]"
                              :price (money-of 0.04603M :eur)}
                             {:start #time/zoned-date-time "2026-02-28T23:45Z[UTC]"
                              :price (money-of 0.04802M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T00:00Z[UTC]"
                              :price (money-of 0.049M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T00:15Z[UTC]"
                              :price (money-of 0.05199M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T00:30Z[UTC]"
                              :price (money-of 0.05277M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T00:45Z[UTC]"
                              :price (money-of 0.05244M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T01:00Z[UTC]"
                              :price (money-of 0.05169M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T01:15Z[UTC]"
                              :price (money-of 0.05249M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T01:30Z[UTC]"
                              :price (money-of 0.05551M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T01:45Z[UTC]"
                              :price (money-of 0.0573M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T02:00Z[UTC]"
                              :price (money-of 0.05334M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T02:15Z[UTC]"
                              :price (money-of 0.05458M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T02:30Z[UTC]"
                              :price (money-of 0.0581M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T02:45Z[UTC]"
                              :price (money-of 0.06227M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T03:00Z[UTC]"
                              :price (money-of 0.05308M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T03:15Z[UTC]"
                              :price (money-of 0.05757M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T03:30Z[UTC]"
                              :price (money-of 0.06438M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T03:45Z[UTC]"
                              :price (money-of 0.06931M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T04:00Z[UTC]"
                              :price (money-of 0.064M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T04:15Z[UTC]"
                              :price (money-of 0.0643M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T04:30Z[UTC]"
                              :price (money-of 0.06513M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T04:45Z[UTC]"
                              :price (money-of 0.06729M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T05:00Z[UTC]"
                              :price (money-of 0.06027M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T05:15Z[UTC]"
                              :price (money-of 0.06651M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T05:30Z[UTC]"
                              :price (money-of 0.06875M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T05:45Z[UTC]"
                              :price (money-of 0.0685M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T06:00Z[UTC]"
                              :price (money-of 0.06847M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T06:15Z[UTC]"
                              :price (money-of 0.06879M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T06:30Z[UTC]"
                              :price (money-of 0.06735M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T06:45Z[UTC]"
                              :price (money-of 0.05538M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T07:00Z[UTC]"
                              :price (money-of 0.06689M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T07:15Z[UTC]"
                              :price (money-of 0.05624M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T07:30Z[UTC]"
                              :price (money-of 0.04577M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T07:45Z[UTC]"
                              :price (money-of 0.04124M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T08:00Z[UTC]"
                              :price (money-of 0.04587M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T08:15Z[UTC]"
                              :price (money-of 0.02416M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T08:30Z[UTC]"
                              :price (money-of 0.00979M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T08:45Z[UTC]"
                              :price (money-of 0.0001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T09:00Z[UTC]"
                              :price (money-of 0.00087M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T09:15Z[UTC]"
                              :price (money-of 0M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T09:30Z[UTC]"
                              :price (money-of -0.00006M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T09:45Z[UTC]"
                              :price (money-of -0.00081M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T10:00Z[UTC]"
                              :price (money-of 0.00001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T10:15Z[UTC]"
                              :price (money-of 0M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T10:30Z[UTC]"
                              :price (money-of -0.00001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T10:45Z[UTC]"
                              :price (money-of -0.0001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T11:00Z[UTC]"
                              :price (money-of -0.0001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T11:15Z[UTC]"
                              :price (money-of -0.00011M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T11:30Z[UTC]"
                              :price (money-of -0.00025M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T11:45Z[UTC]"
                              :price (money-of -0.001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T12:00Z[UTC]"
                              :price (money-of -0.00101M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T12:15Z[UTC]"
                              :price (money-of -0.00103M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T12:30Z[UTC]"
                              :price (money-of -0.001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T12:45Z[UTC]"
                              :price (money-of -0.00011M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T13:00Z[UTC]"
                              :price (money-of -0.001M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T13:15Z[UTC]"
                              :price (money-of -0.00006M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T13:30Z[UTC]"
                              :price (money-of 0M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T13:45Z[UTC]"
                              :price (money-of 0.00104M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T14:00Z[UTC]"
                              :price (money-of 0M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T14:15Z[UTC]"
                              :price (money-of 0.00012M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T14:30Z[UTC]"
                              :price (money-of 0.03361M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T14:45Z[UTC]"
                              :price (money-of 0.07008M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T15:00Z[UTC]"
                              :price (money-of 0.03731M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T15:15Z[UTC]"
                              :price (money-of 0.0624M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T15:30Z[UTC]"
                              :price (money-of 0.09466M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T15:45Z[UTC]"
                              :price (money-of 0.10499M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T16:00Z[UTC]"
                              :price (money-of 0.09201M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T16:15Z[UTC]"
                              :price (money-of 0.10003M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T16:30Z[UTC]"
                              :price (money-of 0.11M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T16:45Z[UTC]"
                              :price (money-of 0.113M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T17:00Z[UTC]"
                              :price (money-of 0.10805M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T17:15Z[UTC]"
                              :price (money-of 0.10984M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T17:30Z[UTC]"
                              :price (money-of 0.11306M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T17:45Z[UTC]"
                              :price (money-of 0.10986M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T18:00Z[UTC]"
                              :price (money-of 0.10464M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T18:15Z[UTC]"
                              :price (money-of 0.10341M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T18:30Z[UTC]"
                              :price (money-of 0.10004M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T18:45Z[UTC]"
                              :price (money-of 0.09865M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T19:00Z[UTC]"
                              :price (money-of 0.09604M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T19:15Z[UTC]"
                              :price (money-of 0.09496M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T19:30Z[UTC]"
                              :price (money-of 0.08901M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T19:45Z[UTC]"
                              :price (money-of 0.08293M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T20:00Z[UTC]"
                              :price (money-of 0.08677M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T20:15Z[UTC]"
                              :price (money-of 0.08217M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T20:30Z[UTC]"
                              :price (money-of 0.08026M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T20:45Z[UTC]"
                              :price (money-of 0.07606M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T21:00Z[UTC]"
                              :price (money-of 0.08111M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T21:15Z[UTC]"
                              :price (money-of 0.07782M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T21:30Z[UTC]"
                              :price (money-of 0.07364M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T21:45Z[UTC]"
                              :price (money-of 0.06609M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T22:00Z[UTC]"
                              :price (money-of 0.07388M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T22:15Z[UTC]"
                              :price (money-of 0.07083M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T22:30Z[UTC]"
                              :price (money-of 0.06876M :eur)}
                             {:start #time/zoned-date-time "2026-03-01T22:45Z[UTC]"
                              :price (money-of 0.05912M :eur)}]})
           result))))

(deftest begin_dst
  (let [result (with-fake-routes-in-isolation
                 {"https://api.energy-charts.info/price?bzn=DE-LU&start=1743289200&end=1743371100"
                  (constantly
                   {:status 200
                    :body (slurp "test/fixtures/energy_charts_1743289200_1743371100.json")})}
                 (energy-charts/fetch-prices<> (:backend.energy-charts/energy-charts *system*)
                                               (t/date "2025-03-30")
                                               :de-lu))]
    (is (ok? result))
    (is (= 23 ;; Before October 2025, the pricing was hourly
           (->> result :val :schedule count)))
    (is (= (->Ok {:pricing "per_kwh"
                  :schedule [{:start #time/zoned-date-time "2025-03-29T23:00Z[UTC]"
                              :price (money-of 0.04631M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T00:00Z[UTC]"
                              :price (money-of 0.01589M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T01:00Z[UTC]"
                              :price (money-of 0.0051M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T02:00Z[UTC]"
                              :price (money-of 0.0012M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T03:00Z[UTC]"
                              :price (money-of 0.00009M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T04:00Z[UTC]"
                              :price (money-of 0.00003M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T05:00Z[UTC]"
                              :price (money-of 0M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T06:00Z[UTC]"
                              :price (money-of -0.00001M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T07:00Z[UTC]"
                              :price (money-of -0.00004M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T08:00Z[UTC]"
                              :price (money-of -0.00347M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T09:00Z[UTC]"
                              :price (money-of -0.01134M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T10:00Z[UTC]"
                              :price (money-of -0.0185M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T11:00Z[UTC]"
                              :price (money-of -0.02576M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T12:00Z[UTC]"
                              :price (money-of -0.02607M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T13:00Z[UTC]"
                              :price (money-of -0.01296M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T14:00Z[UTC]"
                              :price (money-of -0.00401M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T15:00Z[UTC]"
                              :price (money-of -0.00001M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T16:00Z[UTC]"
                              :price (money-of 0.01441M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T17:00Z[UTC]"
                              :price (money-of 0.06083M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T18:00Z[UTC]"
                              :price (money-of 0.05886M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T19:00Z[UTC]"
                              :price (money-of 0.05001M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T20:00Z[UTC]"
                              :price (money-of 0.06171M :eur)}
                             {:start #time/zoned-date-time "2025-03-30T21:00Z[UTC]"
                              :price (money-of 0.05644M :eur)}]})
           result))))

(deftest begin_end
  (let [result (with-fake-routes-in-isolation
                 {"https://api.energy-charts.info/price?bzn=DE-LU&start=1729980000&end=1730069100"
                  (constantly
                   {:status 200
                    :body (slurp "test/fixtures/energy_charts_1729980000_1730069100.json")})}
                 (energy-charts/fetch-prices<> (:backend.energy-charts/energy-charts *system*)
                                               (t/date "2024-10-27")
                                               :de-lu))]
    (is (ok? result))
    (is (= 25 ;; Before October 2025, the pricing was hourly
           (->> result :val :schedule count)))
    (is (= (->Ok {:pricing "per_kwh"
                  :schedule [{:start #time/zoned-date-time "2024-10-26T22:00Z[UTC]"
                              :price (money-of 0.09222M :eur)}
                             {:start #time/zoned-date-time "2024-10-26T23:00Z[UTC]"
                              :price (money-of 0.084M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T00:00Z[UTC]"
                              :price (money-of 0.08223M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T01:00Z[UTC]"
                              :price (money-of 0.08043M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T02:00Z[UTC]"
                              :price (money-of 0.07941M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T03:00Z[UTC]"
                              :price (money-of 0.07879M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T04:00Z[UTC]"
                              :price (money-of 0.08514M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T05:00Z[UTC]"
                              :price (money-of 0.08921M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T06:00Z[UTC]"
                              :price (money-of 0.08805M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T07:00Z[UTC]"
                              :price (money-of 0.08434M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T08:00Z[UTC]"
                              :price (money-of 0.06648M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T09:00Z[UTC]"
                              :price (money-of 0.05472M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T10:00Z[UTC]"
                              :price (money-of 0.0425M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T11:00Z[UTC]"
                              :price (money-of 0.03999M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T12:00Z[UTC]"
                              :price (money-of 0.04M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T13:00Z[UTC]"
                              :price (money-of 0.06433M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T14:00Z[UTC]"
                              :price (money-of 0.11153M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T15:00Z[UTC]"
                              :price (money-of 0.12367M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T16:00Z[UTC]"
                              :price (money-of 0.1483M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T17:00Z[UTC]"
                              :price (money-of 0.14571M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T18:00Z[UTC]"
                              :price (money-of 0.13047M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T19:00Z[UTC]"
                              :price (money-of 0.11815M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T20:00Z[UTC]"
                              :price (money-of 0.11201M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T21:00Z[UTC]"
                              :price (money-of 0.11368M :eur)}
                             {:start #time/zoned-date-time "2024-10-27T22:00Z[UTC]"
                              :price (money-of 0.10299M :eur)}]})
           result))))
