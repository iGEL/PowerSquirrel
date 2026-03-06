(ns cloud.entsoe.couchdb
  (:require
   [cloud.couchdb :as couchdb]
   [cloud.entsoe :as entsoe]
   [cloud.result :refer [->Ok branch-err branch-ok]]
   [dinero.core :as dinero]
   [integrant.core :as ig]
   [tick.core :as t])
  (:import [java.time ZonedDateTime]))

(def path-date-format (t/formatter "yyyy-MM-dd"))

(defn path [date zone]
  (str "/pricing/" (name zone) "--" (t/format path-date-format date)))

(defn- prices->json [prices]
  (->> prices
       (map (fn [prices]
              (update prices :schedule
                      (fn [schedule]
                        (map (fn [{:keys [start price]}]
                               {"start" (str start)
                                "price" {"amount" (-> price dinero/get-amount str)
                                         "currency" (-> price dinero/get-currency name)}})
                             schedule)))))))

(defn- json->prices [json]
  (map (fn [series]
         (-> series
             (update :resolution keyword)
             (update :schedule (fn [schedule]
                                 (map (fn [{{:keys [amount currency]} :price
                                            :keys [start]}]
                                        {:price (dinero/money-of amount currency)
                                         :start (ZonedDateTime/parse start)})
                                      schedule)))))
       json))

(defprotocol
 EntsoeCouchDbProtocol
  (load-or-fetch-prices<> [this date zone]))

(defrecord
 EntsoeCouchDb [couchdb entsoe]
  EntsoeCouchDbProtocol
  (load-or-fetch-prices<> [_ date zone]
    (let [path* (path date zone)]
      (-> (couchdb/request<> couchdb
                             path*
                             {:headers {"accept" "application/json"}})
          (branch-ok (fn [{{:keys [prices]} :body}]
                       (->Ok (json->prices prices))))
          (branch-err (fn [_]
                        (-> (entsoe/fetch-prices<> entsoe date zone)
                            (branch-ok (fn [prices]
                                         (-> (couchdb/request<> couchdb path*
                                                                {:method :put
                                                                 :body {:prices (prices->json prices)}
                                                                 :expected-responses [201]})
                                             (branch-ok (fn [_]
                                                          (->Ok prices)))))))))))))

(defmethod ig/init-key ::entsoe-couchdb
  [_ {entsoe :cloud.entsoe/entsoe
      couchdb :cloud.couchdb/couchdb}]
  (map->EntsoeCouchDb {:couchdb couchdb
                       :entsoe entsoe}))
