(ns backend.entsoe-test
  (:require
   [backend.entsoe :as entsoe]
   [backend.result :refer [->Ok err? ok?]]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]
   [clojure.test :refer [deftest is]]
   [dinero.core :refer [money-of]]
   [tick.core :as t]))

(deftest default
  (let [result (with-redefs [entsoe/token "sec123"]
                 (with-fake-routes-in-isolation
                   {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202508202200&periodEnd=202508212200"
                    (constantly {:status 200
                                 :body (slurp "test/fixtures/Energy_Prices_202508202200-202508212200.xml")})}
                   (entsoe/fetch-prices<> (t/date "2025-08-21")
                                          :de-lu)))]
    (is (ok? result))
    (is (= 96 (-> result :val :pt15m :schedule count)))
    (is (= 24 (-> result :val :pt60m :schedule count)))
    (is (= (->Ok {:pt15m
                  {:pricing "per_kwh",
                   :schedule
                   {#time/zoned-date-time "2025-08-20T22:00Z"
                    (money-of 0.1099M :eur),
                    #time/zoned-date-time "2025-08-20T22:15Z"
                    (money-of 0.10373M :eur),
                    #time/zoned-date-time "2025-08-20T22:30Z"
                    (money-of 0.0868M :eur),
                    #time/zoned-date-time "2025-08-20T22:45Z"
                    (money-of 0.0826M :eur),
                    #time/zoned-date-time "2025-08-20T23:00Z"
                    (money-of 0.0966M :eur),
                    #time/zoned-date-time "2025-08-20T23:15Z"
                    (money-of 0.0912M :eur),
                    #time/zoned-date-time "2025-08-20T23:30Z"
                    (money-of 0.08711M :eur),
                    #time/zoned-date-time "2025-08-20T23:45Z"
                    (money-of 0.0903M :eur),
                    #time/zoned-date-time "2025-08-21T00:00Z"
                    (money-of 0.09295M :eur),
                    #time/zoned-date-time "2025-08-21T00:15Z"
                    (money-of 0.09095M :eur),
                    #time/zoned-date-time "2025-08-21T00:30Z"
                    (money-of 0.0893M :eur),
                    #time/zoned-date-time "2025-08-21T00:45Z"
                    (money-of 0.089M :eur),
                    #time/zoned-date-time "2025-08-21T01:00Z"
                    (money-of 0.0894M :eur),
                    #time/zoned-date-time "2025-08-21T01:15Z"
                    (money-of 0.0886M :eur),
                    #time/zoned-date-time "2025-08-21T01:30Z"
                    (money-of 0.09304M :eur),
                    #time/zoned-date-time "2025-08-21T01:45Z"
                    (money-of 0.0902M :eur),
                    #time/zoned-date-time "2025-08-21T02:00Z"
                    (money-of 0.0856M :eur),
                    #time/zoned-date-time "2025-08-21T02:15Z"
                    (money-of 0.0878M :eur),
                    #time/zoned-date-time "2025-08-21T02:30Z"
                    (money-of 0.102M :eur),
                    #time/zoned-date-time "2025-08-21T02:45Z"
                    (money-of 0.101M :eur),
                    #time/zoned-date-time "2025-08-21T03:00Z"
                    (money-of 0.08349M :eur),
                    #time/zoned-date-time "2025-08-21T03:15Z"
                    (money-of 0.0962M :eur),
                    #time/zoned-date-time "2025-08-21T03:30Z"
                    (money-of 0.10459M :eur),
                    #time/zoned-date-time "2025-08-21T03:45Z"
                    (money-of 0.11809M :eur),
                    #time/zoned-date-time "2025-08-21T04:00Z"
                    (money-of 0.0947M :eur),
                    #time/zoned-date-time "2025-08-21T04:15Z"
                    (money-of 0.1098M :eur),
                    #time/zoned-date-time "2025-08-21T04:30Z"
                    (money-of 0.11957M :eur),
                    #time/zoned-date-time "2025-08-21T04:45Z"
                    (money-of 0.1294M :eur),
                    #time/zoned-date-time "2025-08-21T05:00Z"
                    (money-of 0.1311M :eur),
                    #time/zoned-date-time "2025-08-21T05:15Z"
                    (money-of 0.12498M :eur),
                    #time/zoned-date-time "2025-08-21T05:30Z"
                    (money-of 0.11585M :eur),
                    #time/zoned-date-time "2025-08-21T05:45Z"
                    (money-of 0.0975M :eur),
                    #time/zoned-date-time "2025-08-21T06:00Z"
                    (money-of 0.14746M :eur),
                    #time/zoned-date-time "2025-08-21T06:15Z"
                    (money-of 0.1173M :eur),
                    #time/zoned-date-time "2025-08-21T06:30Z"
                    (money-of 0.0929M :eur),
                    #time/zoned-date-time "2025-08-21T06:45Z"
                    (money-of 0.07753M :eur),
                    #time/zoned-date-time "2025-08-21T07:00Z"
                    (money-of 0.142M :eur),
                    #time/zoned-date-time "2025-08-21T07:15Z"
                    (money-of 0.1106M :eur),
                    #time/zoned-date-time "2025-08-21T07:30Z"
                    (money-of 0.0801M :eur),
                    #time/zoned-date-time "2025-08-21T07:45Z"
                    (money-of 0.06001M :eur),
                    #time/zoned-date-time "2025-08-21T08:00Z"
                    (money-of 0.12816M :eur),
                    #time/zoned-date-time "2025-08-21T08:15Z"
                    (money-of 0.0974M :eur),
                    #time/zoned-date-time "2025-08-21T08:30Z"
                    (money-of 0.0765M :eur),
                    #time/zoned-date-time "2025-08-21T08:45Z"
                    (money-of 0.0575M :eur),
                    #time/zoned-date-time "2025-08-21T09:00Z"
                    (money-of 0.11029M :eur),
                    #time/zoned-date-time "2025-08-21T09:15Z"
                    (money-of 0.07996M :eur),
                    #time/zoned-date-time "2025-08-21T09:30Z"
                    (money-of 0.06991M :eur),
                    #time/zoned-date-time "2025-08-21T09:45Z"
                    (money-of 0.0474M :eur),
                    #time/zoned-date-time "2025-08-21T10:00Z"
                    (money-of 0.0815M :eur),
                    #time/zoned-date-time "2025-08-21T10:15Z"
                    (money-of 0.06991M :eur),
                    #time/zoned-date-time "2025-08-21T10:30Z"
                    (money-of 0.0603M :eur),
                    #time/zoned-date-time "2025-08-21T10:45Z"
                    (money-of 0.05191M :eur),
                    #time/zoned-date-time "2025-08-21T11:00Z"
                    (money-of 0.06201M :eur),
                    #time/zoned-date-time "2025-08-21T11:15Z"
                    (money-of 0.0551M :eur),
                    #time/zoned-date-time "2025-08-21T11:30Z"
                    (money-of 0.0512M :eur),
                    #time/zoned-date-time "2025-08-21T11:45Z"
                    (money-of 0.0476M :eur),
                    #time/zoned-date-time "2025-08-21T12:00Z"
                    (money-of 0.05108M :eur),
                    #time/zoned-date-time "2025-08-21T12:15Z"
                    (money-of 0.0519M :eur),
                    #time/zoned-date-time "2025-08-21T12:30Z"
                    (money-of 0.0526M :eur),
                    #time/zoned-date-time "2025-08-21T12:45Z"
                    (money-of 0.0565M :eur),
                    #time/zoned-date-time "2025-08-21T13:00Z"
                    (money-of 0.0506M :eur),
                    #time/zoned-date-time "2025-08-21T13:15Z"
                    (money-of 0.0586M :eur),
                    #time/zoned-date-time "2025-08-21T13:30Z"
                    (money-of 0.06992M :eur),
                    #time/zoned-date-time "2025-08-21T13:45Z"
                    (money-of 0.0752M :eur),
                    #time/zoned-date-time "2025-08-21T14:00Z"
                    (money-of 0.05009M :eur),
                    #time/zoned-date-time "2025-08-21T14:15Z"
                    (money-of 0.0684M :eur),
                    #time/zoned-date-time "2025-08-21T14:30Z"
                    (money-of 0.07992M :eur),
                    #time/zoned-date-time "2025-08-21T14:45Z"
                    (money-of 0.1008M :eur),
                    #time/zoned-date-time "2025-08-21T15:00Z"
                    (money-of 0.02756M :eur),
                    #time/zoned-date-time "2025-08-21T15:15Z"
                    (money-of 0.0787M :eur),
                    #time/zoned-date-time "2025-08-21T15:30Z"
                    (money-of 0.10367M :eur),
                    #time/zoned-date-time "2025-08-21T15:45Z"
                    (money-of 0.12991M :eur),
                    #time/zoned-date-time "2025-08-21T16:00Z"
                    (money-of 0.05145M :eur),
                    #time/zoned-date-time "2025-08-21T16:15Z"
                    (money-of 0.0894M :eur),
                    #time/zoned-date-time "2025-08-21T16:30Z"
                    (money-of 0.1164M :eur),
                    #time/zoned-date-time "2025-08-21T16:45Z"
                    (money-of 0.1313M :eur),
                    #time/zoned-date-time "2025-08-21T17:00Z"
                    (money-of 0.09005M :eur),
                    #time/zoned-date-time "2025-08-21T17:15Z"
                    (money-of 0.0988M :eur),
                    #time/zoned-date-time "2025-08-21T17:30Z"
                    (money-of 0.11M :eur),
                    #time/zoned-date-time "2025-08-21T17:45Z"
                    (money-of 0.12366M :eur),
                    #time/zoned-date-time "2025-08-21T18:00Z"
                    (money-of 0.11506M :eur),
                    #time/zoned-date-time "2025-08-21T18:15Z"
                    (money-of 0.11506M :eur),
                    #time/zoned-date-time "2025-08-21T18:30Z"
                    (money-of 0.1094M :eur),
                    #time/zoned-date-time "2025-08-21T18:45Z"
                    (money-of 0.10688M :eur),
                    #time/zoned-date-time "2025-08-21T19:00Z"
                    (money-of 0.1157M :eur),
                    #time/zoned-date-time "2025-08-21T19:15Z"
                    (money-of 0.1118M :eur),
                    #time/zoned-date-time "2025-08-21T19:30Z"
                    (money-of 0.1005M :eur),
                    #time/zoned-date-time "2025-08-21T19:45Z"
                    (money-of 0.0896M :eur),
                    #time/zoned-date-time "2025-08-21T20:00Z"
                    (money-of 0.1148M :eur),
                    #time/zoned-date-time "2025-08-21T20:15Z"
                    (money-of 0.1019M :eur),
                    #time/zoned-date-time "2025-08-21T20:30Z"
                    (money-of 0.0926M :eur),
                    #time/zoned-date-time "2025-08-21T20:45Z"
                    (money-of 0.081M :eur),
                    #time/zoned-date-time "2025-08-21T21:00Z"
                    (money-of 0.09656M :eur),
                    #time/zoned-date-time "2025-08-21T21:15Z"
                    (money-of 0.0934M :eur),
                    #time/zoned-date-time "2025-08-21T21:30Z"
                    (money-of 0.0844M :eur),
                    #time/zoned-date-time "2025-08-21T21:45Z"
                    (money-of 0.0744M :eur)}}
                  :pt60m
                  {:pricing "per_kwh",
                   :schedule
                   {#time/zoned-date-time "2025-08-20T22:00Z"
                    (money-of 0.09611M :eur),
                    #time/zoned-date-time "2025-08-20T23:00Z"
                    (money-of 0.09073M :eur),
                    #time/zoned-date-time "2025-08-21T00:00Z"
                    (money-of 0.08752M :eur),
                    #time/zoned-date-time "2025-08-21T01:00Z"
                    (money-of 0.09088M :eur),
                    #time/zoned-date-time "2025-08-21T02:00Z"
                    (money-of 0.09237M :eur),
                    #time/zoned-date-time "2025-08-21T03:00Z"
                    (money-of 0.10859M :eur),
                    #time/zoned-date-time "2025-08-21T04:00Z"
                    (money-of 0.1156M :eur),
                    #time/zoned-date-time "2025-08-21T05:00Z"
                    (money-of 0.11689M :eur),
                    #time/zoned-date-time "2025-08-21T06:00Z"
                    (money-of 0.10779M :eur),
                    #time/zoned-date-time "2025-08-21T07:00Z"
                    (money-of 0.09732M :eur),
                    #time/zoned-date-time "2025-08-21T08:00Z"
                    (money-of 0.08585M :eur),
                    #time/zoned-date-time "2025-08-21T09:00Z"
                    (money-of 0.07764M :eur),
                    #time/zoned-date-time "2025-08-21T10:00Z"
                    (money-of 0.06949M :eur),
                    #time/zoned-date-time "2025-08-21T11:00Z"
                    (money-of 0.06609M :eur),
                    #time/zoned-date-time "2025-08-21T12:00Z"
                    (money-of 0.05626M :eur),
                    #time/zoned-date-time "2025-08-21T13:00Z"
                    (money-of 0.0682M :eur),
                    #time/zoned-date-time "2025-08-21T14:00Z"
                    (money-of 0.07239M :eur),
                    #time/zoned-date-time "2025-08-21T15:00Z"
                    (money-of 0.09288M :eur),
                    #time/zoned-date-time "2025-08-21T16:00Z"
                    (money-of 0.1032M :eur),
                    #time/zoned-date-time "2025-08-21T17:00Z"
                    (money-of 0.10639M :eur),
                    #time/zoned-date-time "2025-08-21T18:00Z"
                    (money-of 0.11073M :eur),
                    #time/zoned-date-time "2025-08-21T19:00Z"
                    (money-of 0.11073M :eur),
                    #time/zoned-date-time "2025-08-21T20:00Z"
                    (money-of 0.105M :eur),
                    #time/zoned-date-time "2025-08-21T21:00Z"
                    (money-of 0.09578M :eur)}}})
           result))))

(deftest dst-begin
  (let [result (with-redefs [entsoe/token "sec123"]
                 (with-fake-routes-in-isolation
                   {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202503292300&periodEnd=202503302200"
                    (constantly {:status 200
                                 :body (slurp "test/fixtures/Energy_Prices_begin_dst.xml")})}
                   (entsoe/fetch-prices<> (t/date "2025-03-30")
                                          :de-lu)))]
    (is (ok? result))
    (is (= 92 (-> result :val :pt15m :schedule count)))
    (is (= 23 (-> result :val :pt60m :schedule count)))
    (is (= {:pricing "per_kwh",
            :schedule
            {#time/zoned-date-time "2025-03-29T23:00Z"
             (money-of 0.04631M :eur),
             #time/zoned-date-time "2025-03-30T00:00Z"
             (money-of 0.01589M :eur),
             #time/zoned-date-time "2025-03-30T01:00Z"
             (money-of 0.0051M :eur),
             #time/zoned-date-time "2025-03-30T02:00Z"
             (money-of 0.0012M :eur),
             #time/zoned-date-time "2025-03-30T03:00Z"
             (money-of 0.00009M :eur),
             #time/zoned-date-time "2025-03-30T04:00Z"
             (money-of 0.00003M :eur),
             #time/zoned-date-time "2025-03-30T05:00Z"
             (money-of 0M :eur),
             #time/zoned-date-time "2025-03-30T06:00Z"
             (money-of -0.00001M :eur),
             #time/zoned-date-time "2025-03-30T07:00Z"
             (money-of -0.00004M :eur),
             #time/zoned-date-time "2025-03-30T08:00Z"
             (money-of -0.00347M :eur),
             #time/zoned-date-time "2025-03-30T09:00Z"
             (money-of -0.01134M :eur),
             #time/zoned-date-time "2025-03-30T10:00Z"
             (money-of -0.0185M :eur),
             #time/zoned-date-time "2025-03-30T11:00Z"
             (money-of -0.02576M :eur),
             #time/zoned-date-time "2025-03-30T12:00Z"
             (money-of -0.02607M :eur),
             #time/zoned-date-time "2025-03-30T13:00Z"
             (money-of -0.01296M :eur),
             #time/zoned-date-time "2025-03-30T14:00Z"
             (money-of -0.00401M :eur),
             #time/zoned-date-time "2025-03-30T15:00Z"
             (money-of -0.00001M :eur),
             #time/zoned-date-time "2025-03-30T16:00Z"
             (money-of 0.01441M :eur),
             #time/zoned-date-time "2025-03-30T17:00Z"
             (money-of 0.06083M :eur),
             #time/zoned-date-time "2025-03-30T18:00Z"
             (money-of 0.05886M :eur),
             #time/zoned-date-time "2025-03-30T19:00Z"
             (money-of 0.05001M :eur),
             #time/zoned-date-time "2025-03-30T20:00Z"
             (money-of 0.06171M :eur),
             #time/zoned-date-time "2025-03-30T21:00Z"
             (money-of 0.05644M :eur)}}
           (-> result :val :pt60m)))))

(deftest dst-end
  (let [result (with-redefs [entsoe/token "sec123"]
                 (with-fake-routes-in-isolation
                   {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202410262200&periodEnd=202410272300"
                    (constantly {:status 200
                                 :body (slurp "test/fixtures/Energy_Prices_end_dst.xml")})}
                   (entsoe/fetch-prices<> (t/date "2024-10-27")
                                          :de-lu)))]
    (is (ok? result))
    (is (= 100 (-> result :val :pt15m :schedule count)))
    (is (= 25 (-> result :val :pt60m :schedule count)))
    (is (= {:pricing "per_kwh",
            :schedule
            {#time/zoned-date-time "2024-10-26T22:00Z"
             (money-of 0.09222M :eur),
             #time/zoned-date-time "2024-10-26T23:00Z"
             (money-of 0.084M :eur),
             #time/zoned-date-time "2024-10-27T00:00Z"
             (money-of 0.08223M :eur),
             #time/zoned-date-time "2024-10-27T01:00Z"
             (money-of 0.08043M :eur),
             #time/zoned-date-time "2024-10-27T02:00Z"
             (money-of 0.07941M :eur),
             #time/zoned-date-time "2024-10-27T03:00Z"
             (money-of 0.07879M :eur),
             #time/zoned-date-time "2024-10-27T04:00Z"
             (money-of 0.08514M :eur),
             #time/zoned-date-time "2024-10-27T05:00Z"
             (money-of 0.08921M :eur),
             #time/zoned-date-time "2024-10-27T06:00Z"
             (money-of 0.08805M :eur),
             #time/zoned-date-time "2024-10-27T07:00Z"
             (money-of 0.08434M :eur),
             #time/zoned-date-time "2024-10-27T08:00Z"
             (money-of 0.06648M :eur),
             #time/zoned-date-time "2024-10-27T09:00Z"
             (money-of 0.05472M :eur),
             #time/zoned-date-time "2024-10-27T10:00Z"
             (money-of 0.0425M :eur),
             #time/zoned-date-time "2024-10-27T11:00Z"
             (money-of 0.03999M :eur),
             #time/zoned-date-time "2024-10-27T12:00Z"
             (money-of 0.04M :eur),
             #time/zoned-date-time "2024-10-27T13:00Z"
             (money-of 0.06433M :eur),
             #time/zoned-date-time "2024-10-27T14:00Z"
             (money-of 0.11153M :eur),
             #time/zoned-date-time "2024-10-27T15:00Z"
             (money-of 0.12367M :eur),
             #time/zoned-date-time "2024-10-27T16:00Z"
             (money-of 0.1483M :eur),
             #time/zoned-date-time "2024-10-27T17:00Z"
             (money-of 0.14571M :eur),
             #time/zoned-date-time "2024-10-27T18:00Z"
             (money-of 0.13047M :eur),
             #time/zoned-date-time "2024-10-27T19:00Z"
             (money-of 0.11815M :eur),
             #time/zoned-date-time "2024-10-27T20:00Z"
             (money-of 0.11201M :eur),
             #time/zoned-date-time "2024-10-27T21:00Z"
             (money-of 0.11368M :eur)
             #time/zoned-date-time "2024-10-27T22:00Z"
             (money-of 0.10299M :eur)}}
           (-> result :val :pt60m)))))

(deftest failure
  (let [result (with-redefs [entsoe/token "sec123"]
                 (with-fake-routes-in-isolation
                   {"https://web-api.tp.entsoe.eu/api?securityToken=sec123&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202508202200&periodEnd=202508212200"
                    (constantly {:status 404
                                 :body "Not found"})}
                   (entsoe/fetch-prices<> (t/date "2025-08-21")
                                          :de-lu)))]
    (is (err? result))))
