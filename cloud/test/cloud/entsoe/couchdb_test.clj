(ns cloud.entsoe.couchdb-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [cloud.couchdb :as couchdb]
   [cloud.entsoe :as entsoe]
   [cloud.entsoe.couchdb :as entsoe.couchdb]
   [cloud.result :refer [->Ok branch-err]]
   [cloud.system :as system]
   [dinero.core :refer [money-of]]
   [tick.core :as t]))

(def ^:dynamic *system* nil)

(use-fixtures :once (fn [test-fn]
                      (binding [*system* (system/init)]
                        (test-fn)
                        (system/halt *system*))))

(use-fixtures :each (fn [test-fn]
                      (-> (couchdb/setup-for-tests<> (:cloud.couchdb/couchdb *system*))
                          (branch-err (fn [e]
                                        (throw e))))
                      (test-fn)))

(deftest round-trip
  (let [date (t/date "2025-08-21")
        response [{:pricing "per_kwh"
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
                    ;; Shortened
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
                    ;; Shortened
                    {:start #time/zoned-date-time "2025-08-21T20:00Z"
                     :price (money-of 0.105M :eur)}
                    {:start #time/zoned-date-time "2025-08-21T21:00Z"
                     :price (money-of 0.09578M :eur)}]}]
        !entsoe-calls (atom 0)
        entsoe (reify entsoe/EntsoeProtocol
                 (fetch-prices<> [_ _date _zone]
                   (swap! !entsoe-calls inc)
                   (->Ok response))
                 (get-position<> [_ _ _]))
        entsoe-couchdb (entsoe.couchdb/map->EntsoeCouchDb {:entsoe entsoe
                                                           :couchdb (:cloud.couchdb/couchdb *system*)})]
    (is (= 0 @!entsoe-calls))

    ;; data is not in couchdb -> fetch from entsoe
    (is (= (->Ok response)
           (entsoe.couchdb/load-or-fetch-prices<> entsoe-couchdb date :de-lu))
        "Fetching data from entsoe doesn't match expected result")
    (is (= 1 @!entsoe-calls))

    ;; data is in couchdb -> load it from couchdb
    (is (= (->Ok response)
           (entsoe.couchdb/load-or-fetch-prices<> entsoe-couchdb date :de-lu))
        "Loading data from couchdb doesn't match expected result")
    (is (= 1 @!entsoe-calls))))
