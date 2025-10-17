(ns backend.entsoe-test
  (:require
   [backend.entsoe :as entsoe]
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
                 {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202508202200&periodEnd=202508212200"
                  (constantly {:status 200
                               :body (slurp "test/fixtures/Energy_Prices_202508202200-202508212200.xml")})}
                 (entsoe/fetch-prices<> (:backend.entsoe/entsoe *system*)
                                        (t/date "2025-08-21")
                                        :de-lu))]
    (is (ok? result))
    (is (= 95 ;; 18:15 has same price as 18:00
           (->> result :val (filter #(= (:position %) 2)) first :schedule count)))
    (is (= 23 ;; 19:00 has same price as 18:00
           (->> result :val (filter #(= (:position %) 1)) first :schedule count)))
    (is (= (->Ok [{:pricing "per_kwh"
                   :resolution :pt15m
                   :position 2
                   :schedule
                   [{:start #time/zoned-date-time "2025-08-20T22:00Z"
                     :price (money-of 0.1099M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T22:15Z"
                     :price (money-of 0.10373M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T22:30Z"
                     :price (money-of 0.0868M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T22:45Z"
                     :price (money-of 0.0826M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T23:00Z"
                     :price (money-of 0.0966M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T23:15Z"
                     :price (money-of 0.0912M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T23:30Z"
                     :price (money-of 0.08711M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T23:45Z"
                     :price (money-of 0.0903M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T00:00Z"
                     :price (money-of 0.09295M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T00:15Z"
                     :price (money-of 0.09095M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T00:30Z"
                     :price (money-of 0.0893M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T00:45Z"
                     :price (money-of 0.089M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T01:00Z"
                     :price (money-of 0.0894M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T01:15Z"
                     :price (money-of 0.0886M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T01:30Z"
                     :price (money-of 0.09304M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T01:45Z"
                     :price (money-of 0.0902M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T02:00Z"
                     :price (money-of 0.0856M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T02:15Z"
                     :price (money-of 0.0878M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T02:30Z"
                     :price (money-of 0.102M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T02:45Z"
                     :price (money-of 0.101M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T03:00Z"
                     :price (money-of 0.08349M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T03:15Z"
                     :price (money-of 0.0962M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T03:30Z"
                     :price (money-of 0.10459M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T03:45Z"
                     :price (money-of 0.11809M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T04:00Z"
                     :price (money-of 0.0947M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T04:15Z"
                     :price (money-of 0.1098M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T04:30Z"
                     :price (money-of 0.11957M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T04:45Z"
                     :price (money-of 0.1294M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T05:00Z"
                     :price (money-of 0.1311M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T05:15Z"
                     :price (money-of 0.12498M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T05:30Z"
                     :price (money-of 0.11585M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T05:45Z"
                     :price (money-of 0.0975M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T06:00Z"
                     :price (money-of 0.14746M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T06:15Z"
                     :price (money-of 0.1173M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T06:30Z"
                     :price (money-of 0.0929M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T06:45Z"
                     :price (money-of 0.07753M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T07:00Z"
                     :price (money-of 0.142M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T07:15Z"
                     :price (money-of 0.1106M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T07:30Z"
                     :price (money-of 0.0801M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T07:45Z"
                     :price (money-of 0.06001M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T08:00Z"
                     :price (money-of 0.12816M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T08:15Z"
                     :price (money-of 0.0974M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T08:30Z"
                     :price (money-of 0.0765M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T08:45Z"
                     :price (money-of 0.0575M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T09:00Z"
                     :price (money-of 0.11029M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T09:15Z"
                     :price (money-of 0.07996M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T09:30Z"
                     :price (money-of 0.06991M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T09:45Z"
                     :price (money-of 0.0474M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T10:00Z"
                     :price (money-of 0.0815M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T10:15Z"
                     :price (money-of 0.06991M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T10:30Z"
                     :price (money-of 0.0603M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T10:45Z"
                     :price (money-of 0.05191M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T11:00Z"
                     :price (money-of 0.06201M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T11:15Z"
                     :price (money-of 0.0551M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T11:30Z"
                     :price (money-of 0.0512M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T11:45Z"
                     :price (money-of 0.0476M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T12:00Z"
                     :price (money-of 0.05108M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T12:15Z"
                     :price (money-of 0.0519M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T12:30Z"
                     :price (money-of 0.0526M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T12:45Z"
                     :price (money-of 0.0565M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T13:00Z"
                     :price (money-of 0.0506M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T13:15Z"
                     :price (money-of 0.0586M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T13:30Z"
                     :price (money-of 0.06992M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T13:45Z"
                     :price (money-of 0.0752M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T14:00Z"
                     :price (money-of 0.05009M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T14:15Z"
                     :price (money-of 0.0684M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T14:30Z"
                     :price (money-of 0.07992M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T14:45Z"
                     :price (money-of 0.1008M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T15:00Z"
                     :price (money-of 0.02756M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T15:15Z"
                     :price (money-of 0.0787M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T15:30Z"
                     :price (money-of 0.10367M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T15:45Z"
                     :price (money-of 0.12991M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T16:00Z"
                     :price (money-of 0.05145M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T16:15Z"
                     :price (money-of 0.0894M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T16:30Z"
                     :price (money-of 0.1164M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T16:45Z"
                     :price (money-of 0.1313M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T17:00Z"
                     :price (money-of 0.09005M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T17:15Z"
                     :price (money-of 0.0988M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T17:30Z"
                     :price (money-of 0.11M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T17:45Z"
                     :price (money-of 0.12366M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T18:00Z"
                     :price (money-of 0.11506M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T18:30Z"
                     :price (money-of 0.1094M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T18:45Z"
                     :price (money-of 0.10688M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T19:00Z"
                     :price (money-of 0.1157M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T19:15Z"
                     :price (money-of 0.1118M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T19:30Z"
                     :price (money-of 0.1005M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T19:45Z"
                     :price (money-of 0.0896M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T20:00Z"
                     :price (money-of 0.1148M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T20:15Z"
                     :price (money-of 0.1019M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T20:30Z"
                     :price (money-of 0.0926M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T20:45Z"
                     :price (money-of 0.081M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:00Z"
                     :price (money-of 0.09656M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:15Z"
                     :price (money-of 0.0934M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:30Z"
                     :price (money-of 0.0844M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:45Z"
                     :price (money-of 0.0744M :eur)}]}
                  {:pricing "per_kwh"
                   :resolution :pt60m
                   :position 1
                   :schedule
                   [{:start #time/zoned-date-time "2025-08-20T22:00Z"
                     :price (money-of 0.09611M :eur)}
                    {:start #time/zoned-date-time "2025-08-20T23:00Z"
                     :price (money-of 0.09073M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T00:00Z"
                     :price (money-of 0.08752M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T01:00Z"
                     :price (money-of 0.09088M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T02:00Z"
                     :price (money-of 0.09237M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T03:00Z"
                     :price (money-of 0.10859M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T04:00Z"
                     :price (money-of 0.1156M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T05:00Z"
                     :price (money-of 0.11689M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T06:00Z"
                     :price (money-of 0.10779M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T07:00Z"
                     :price (money-of 0.09732M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T08:00Z"
                     :price (money-of 0.08585M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T09:00Z"
                     :price (money-of 0.07764M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T10:00Z"
                     :price (money-of 0.06949M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T11:00Z"
                     :price (money-of 0.06609M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T12:00Z"
                     :price (money-of 0.05626M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T13:00Z"
                     :price (money-of 0.0682M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T14:00Z"
                     :price (money-of 0.07239M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T15:00Z"
                     :price (money-of 0.09288M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T16:00Z"
                     :price (money-of 0.1032M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T17:00Z"
                     :price (money-of 0.10639M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T18:00Z"
                     :price (money-of 0.11073M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T20:00Z"
                     :price (money-of 0.105M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:00Z"
                     :price (money-of 0.09578M :eur)}]}])
           result))))

(deftest dst-begin
  (let [result (with-fake-routes-in-isolation
                 {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202503292300&periodEnd=202503302200"
                  (constantly {:status 200
                               :body (slurp "test/fixtures/Energy_Prices_begin_dst.xml")})}
                 (entsoe/fetch-prices<> (:backend.entsoe/entsoe *system*)
                                        (t/date "2025-03-30")
                                        :de-lu))]
    (is (ok? result))
    (is (= 91 ;; 12:15 has the same price as 12:00
           (->> result :val (filter #(= (:position %) 2)) first :schedule count)))
    (is (= 23
           (->> result :val (filter #(= (:position %) 1)) first :schedule count)))
    (is (= {:pricing "per_kwh"
            :resolution :pt60m
            :position 1
            :schedule
            [{:start #time/zoned-date-time "2025-03-29T23:00Z"
              :price (money-of 0.04631M :eur)}
             {:start #time/zoned-date-time "2025-03-30T00:00Z"
              :price (money-of 0.01589M :eur)}
             {:start #time/zoned-date-time "2025-03-30T01:00Z"
              :price (money-of 0.0051M :eur)}
             {:start #time/zoned-date-time "2025-03-30T02:00Z"
              :price (money-of 0.0012M :eur)}
             {:start #time/zoned-date-time "2025-03-30T03:00Z"
              :price (money-of 0.00009M :eur)}
             {:start #time/zoned-date-time "2025-03-30T04:00Z"
              :price (money-of 0.00003M :eur)}
             {:start #time/zoned-date-time "2025-03-30T05:00Z"
              :price (money-of 0M :eur)}
             {:start #time/zoned-date-time "2025-03-30T06:00Z"
              :price (money-of -0.00001M :eur)}
             {:start #time/zoned-date-time "2025-03-30T07:00Z"
              :price (money-of -0.00004M :eur)}
             {:start #time/zoned-date-time "2025-03-30T08:00Z"
              :price (money-of -0.00347M :eur)}
             {:start #time/zoned-date-time "2025-03-30T09:00Z"
              :price (money-of -0.01134M :eur)}
             {:start #time/zoned-date-time "2025-03-30T10:00Z"
              :price (money-of -0.0185M :eur)}
             {:start #time/zoned-date-time "2025-03-30T11:00Z"
              :price (money-of -0.02576M :eur)}
             {:start #time/zoned-date-time "2025-03-30T12:00Z"
              :price (money-of -0.02607M :eur)}
             {:start #time/zoned-date-time "2025-03-30T13:00Z"
              :price (money-of -0.01296M :eur)}
             {:start #time/zoned-date-time "2025-03-30T14:00Z"
              :price (money-of -0.00401M :eur)}
             {:start #time/zoned-date-time "2025-03-30T15:00Z"
              :price (money-of -0.00001M :eur)}
             {:start #time/zoned-date-time "2025-03-30T16:00Z"
              :price (money-of 0.01441M :eur)}
             {:start #time/zoned-date-time "2025-03-30T17:00Z"
              :price (money-of 0.06083M :eur)}
             {:start #time/zoned-date-time "2025-03-30T18:00Z"
              :price (money-of 0.05886M :eur)}
             {:start #time/zoned-date-time "2025-03-30T19:00Z"
              :price (money-of 0.05001M :eur)}
             {:start #time/zoned-date-time "2025-03-30T20:00Z"
              :price (money-of 0.06171M :eur)}
             {:start #time/zoned-date-time "2025-03-30T21:00Z"
              :price (money-of 0.05644M :eur)}]}
           (->> result :val (filter #(= (:position %) 1)) first)))))

(deftest dst-end
  (let [result (with-fake-routes-in-isolation
                 {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202410262200&periodEnd=202410272300"
                  (constantly {:status 200
                               :body (slurp "test/fixtures/Energy_Prices_end_dst.xml")})}
                 (entsoe/fetch-prices<> (:backend.entsoe/entsoe *system*)
                                        (t/date "2024-10-27")
                                        :de-lu))]
    (is (ok? result))
    (is (= 97 ;; 1:45 has the same price as 1:30, 2:45 as 2:30, 10:30 as 10:15
           (->> result :val (filter #(= (:position %) 2)) first :schedule count)))
    (is (= 25
           (->> result :val (filter #(= (:position %) 1)) first :schedule count)))
    (is (= {:pricing "per_kwh"
            :position 1
            :resolution :pt60m
            :schedule
            [{:start #time/zoned-date-time "2024-10-26T22:00Z"
              :price (money-of 0.09222M :eur)}
             {:start #time/zoned-date-time "2024-10-26T23:00Z"
              :price (money-of 0.084M :eur)}
             {:start #time/zoned-date-time "2024-10-27T00:00Z"
              :price (money-of 0.08223M :eur)}
             {:start #time/zoned-date-time "2024-10-27T01:00Z"
              :price (money-of 0.08043M :eur)}
             {:start #time/zoned-date-time "2024-10-27T02:00Z"
              :price (money-of 0.07941M :eur)}
             {:start #time/zoned-date-time "2024-10-27T03:00Z"
              :price (money-of 0.07879M :eur)}
             {:start #time/zoned-date-time "2024-10-27T04:00Z"
              :price (money-of 0.08514M :eur)}
             {:start #time/zoned-date-time "2024-10-27T05:00Z"
              :price (money-of 0.08921M :eur)}
             {:start #time/zoned-date-time "2024-10-27T06:00Z"
              :price (money-of 0.08805M :eur)}
             {:start #time/zoned-date-time "2024-10-27T07:00Z"
              :price (money-of 0.08434M :eur)}
             {:start #time/zoned-date-time "2024-10-27T08:00Z"
              :price (money-of 0.06648M :eur)}
             {:start #time/zoned-date-time "2024-10-27T09:00Z"
              :price (money-of 0.05472M :eur)}
             {:start #time/zoned-date-time "2024-10-27T10:00Z"
              :price (money-of 0.0425M :eur)}
             {:start #time/zoned-date-time "2024-10-27T11:00Z"
              :price (money-of 0.03999M :eur)}
             {:start #time/zoned-date-time "2024-10-27T12:00Z"
              :price (money-of 0.04M :eur)}
             {:start #time/zoned-date-time "2024-10-27T13:00Z"
              :price (money-of 0.06433M :eur)}
             {:start #time/zoned-date-time "2024-10-27T14:00Z"
              :price (money-of 0.11153M :eur)}
             {:start #time/zoned-date-time "2024-10-27T15:00Z"
              :price (money-of 0.12367M :eur)}
             {:start #time/zoned-date-time "2024-10-27T16:00Z"
              :price (money-of 0.1483M :eur)}
             {:start #time/zoned-date-time "2024-10-27T17:00Z"
              :price (money-of 0.14571M :eur)}
             {:start #time/zoned-date-time "2024-10-27T18:00Z"
              :price (money-of 0.13047M :eur)}
             {:start #time/zoned-date-time "2024-10-27T19:00Z"
              :price (money-of 0.11815M :eur)}
             {:start #time/zoned-date-time "2024-10-27T20:00Z"
              :price (money-of 0.11201M :eur)}
             {:start #time/zoned-date-time "2024-10-27T21:00Z"
              :price (money-of 0.11368M :eur)}
             {:start #time/zoned-date-time "2024-10-27T22:00Z"
              :price (money-of 0.10299M :eur)}]}
           (->> result :val (filter #(= (:position %) 1)) first)))))

(deftest failure
  (let [result (with-fake-routes-in-isolation
                 {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202508202200&periodEnd=202508212200"
                  (constantly {:status 404
                               :body "Not found"})}
                 (entsoe/fetch-prices<> (:backend.entsoe/entsoe *system*)
                                        (t/date "2025-08-21")
                                        :de-lu))]
    (is (err? result))))
