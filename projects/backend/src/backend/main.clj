(ns backend.main
  (:require
   [backend.couchdb :as couchdb]
   [backend.electricity-price :as ep]
   [backend.entsoe :as entsoe]
   [backend.entsoe.couchdb :as entsoe.couchdb]
   [backend.fees :as fees]
   [backend.location :as location]
   [backend.result :refer [branch-err branch-ok]]
   [backend.sun :as sun]
   [backend.system :as system]
   [backend.weather :as weather]
   [dinero.core :as dinero]
   [tick.core :as t]))

(def version "0.0.1")

(defn- format-ct-kWh
  ([amount]
   (format-ct-kWh amount {}))
  ([amount {:keys [padding]}]
   (let [padding* (-> padding (or 0) (max 0))]
     (-> amount
         dinero/get-amount
         (* 100M)
         (->> (format (str "%" (+ 4 padding*) ".1f")))
         (str "ct/kWh")))))

(defn- print-item [item amount {:keys [padding]}]
  (let [padding* (- padding (count item))]
    (println (str item (format-ct-kWh amount {:padding padding*})))))

(defn- report-err+exit! [message exception]
  (binding [*out* *err*]
    (println (str "\u001b[31m"
                  message
                  " "
                  (ex-message exception)
                  "\u001b[0m"))))

(defn -main [& _]
  (println (str "PowerSquirrel " version " 🐿️"))
  (let [{:keys [backend.couchdb/couchdb
                backend.entsoe/entsoe
                backend.entsoe.couchdb/entsoe-couchdb
                backend.location/location]} (system/init)]
    (-> (couchdb/setup<> couchdb)
        (branch-err (partial report-err+exit! "Failure to setup couchdb.")))
    (let [zip "12207"
          country "Germany"
          location (location/find-or-create-location location
                                                     {:zip zip
                                                      :country country})
          weathers (when location (weather/fetch-weather location))]
      (when (and location weathers)
        (doseq [{:keys [time temp clouds]} weathers]
          (let [{:keys [azimuth altitude]} (sun/position {:datetime time
                                                          :location location})]
            (println (format "%s Temperature: %5.2f°, Clouds: %3d%% - Solar Altitude: %6.2f° Azimuth: %6.2f°"
                             time temp (int clouds) altitude azimuth))))))

    (let [stromnetz-berlin (fees/parse "../backend/resources/stromnetz-berlin.json")
          fees (fees/merge stromnetz-berlin (fees/parse "../backend/resources/fees.json"))
          step (t/new-duration 15 :minutes)]
      (-> (entsoe.couchdb/load-or-fetch-prices<> entsoe-couchdb (t/today) :de-lu)
          (->> (entsoe/get-position<> entsoe 1))
          (branch-ok (fn [prices]
                       (doseq [datetime (->> (iterate #(t/>> % step) (-> prices :schedule first :start))
                                             (take 96))]
                         (let [{:keys [net total]} (ep/calculate datetime
                                                                 (:schedule prices)
                                                                 fees)]
                           (println (str datetime)
                                    "- net:"
                                    (format-ct-kWh net)
                                    "- gross:"
                                    (format-ct-kWh total))))
                       (println "Current price:")
                       (let [{:keys [net total]
                              items :fees} (ep/calculate (t/zoned-date-time)
                                                         (:schedule prices)
                                                         fees)
                             padding (->> items
                                          (map #(-> % :name count))
                                          (apply max)
                                          (+ 4))]
                         (print-item "  Net price:" net {:padding padding})
                         (doseq [{:keys [name price]} items]
                           (print-item (str "  " name ": ") price {:padding padding}))
                         (print-item "  Gross price:" total {:padding padding}))))
          (branch-err (partial report-err+exit! "Failure to fetch prices from entso-e."))))))
