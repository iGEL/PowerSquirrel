(ns cloud.energy-charts
  (:require
   [cheshire.core :as json]
   [cheshire.parse :as json.parse]
   [clj-http.client :as http]
   [clojure.string :as str]
   [cloud.result :refer [->Err ->Ok branch-ok try-result]]
   [dinero.core :refer [money-of]]
   [dinero.math :as d.math]
   [integrant.core :as ig]
   [tick.core :as t]))

(defn- timestamp-at
  "Returns midnight in the given timezone, converted afterwards to UTC"
  [{:keys [date time-str timezone]}]
  (-> date
      (t/at time-str)
      (t/in (t/zone timezone))
      (t/instant)
      (.getEpochSecond)))

(defn- extract-info<> [{:keys [unix_seconds price unit license_info deprecated]}]
  (cond
    deprecated
    (->Err (ex-info "Pricing data is deprecated!" {}))

    (not (str/starts-with? (str license_info) "CC BY 4.0 (creativecommons.org/licenses/by/4.0)"))
    (->Err (ex-info "Licence info has changed" {:actual-license license_info
                                                :expected "CC BY 4.0 (creativecommons.org/licenses/by/4.0)"}))

    :else
    (->Ok (let [[currency unit] (str/split unit #" */ *")
                kwh-conversation (case (str/lower-case unit)
                                   "mwh" #(d.math/divide % 1000)
                                   "kwh" identity)]
            {:pricing "per_kwh"
             :schedule (mapv (fn [time price]
                               {:start (-> time
                                           (* 1000)
                                           (t/instant)
                                           (t/in t/UTC))
                                :price (-> price
                                           (money-of currency)
                                           kwh-conversation)})
                             unix_seconds
                             price)}))))

(defprotocol EnergyChartsProtocol
  (fetch-prices<> [this date zone]))

(defrecord EnergyCharts [base-uri bidding-zones]
  EnergyChartsProtocol
  (fetch-prices<> [_ date zone]
    (let [{:keys [timezone bidding-zone-id]} (bidding-zones zone)
          uri (str base-uri
                   "?bzn=" bidding-zone-id
                   "&start=" (timestamp-at {:date date
                                            :time-str "00:00"
                                            :timezone timezone})
                   "&end=" (timestamp-at {:date date
                                          :time-str "23:45"
                                          :timezone timezone}))]
      (-> (try-result (http/get uri {:unexceptional-status #(= % 200)}))
          (branch-ok (fn [{:keys [body]}]
                       (binding [json.parse/*use-bigdecimals?* true]
                         (->Ok (json/parse-string body true)))))
          (branch-ok (fn [body]
                       (extract-info<> body)))))))

(defmethod ig/init-key ::energy-charts
  [_ {{{:keys [base-uri bidding-zones]} :energy-charts} :cloud.config/config}]
  (map->EnergyCharts {:base-uri base-uri
                      :bidding-zones bidding-zones}))

(comment
  (let [ec (map->EnergyCharts {:base-uri "https://api.energy-charts.info/price"
                               :bidding-zones {:de-lu {:bidding-zone-id "DE-LU"
                                                       :timezone "Europe/Berlin"}}})]
    (fetch-prices<> ec (t/date "2025-08-21") :de-lu)))
