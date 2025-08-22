(ns backend.main
  (:require [backend.electricity-price :as ep]
            [backend.entsoe :as entsoe]
            [backend.fees :as fees]
            [backend.geocode :as geocode]
            [backend.result :refer [ok?]]
            [backend.weather :as weather]
            [dinero.format :refer [format-money]]
            [tick.core :as t]))

(defn -main [& _]
  (let [zip "12207"
        country "Germany"
        loc (geocode/geocode-zip zip country)
        weathers (when loc (weather/fetch-weather loc))]
    (when (and loc weathers)
      (doseq [{:keys [time temp clouds]} weathers]
        ;; Placeholder for sun position; can be implemented with astronomical formulas later.
        (println (format "%s Temperature: %.2f°, Clouds: %d%%" time temp (int clouds))))))

  (let [stromnetz-berlin (fees/parse "../backend/resources/stromnetz-berlin.json")
        fees (fees/merge stromnetz-berlin (fees/parse "../backend/resources/fees.json"))
        prices<> (entsoe/fetch-prices<> (t/today) :de-lu)
        step (t/new-duration 15 :minutes)
        datetime-format (t/formatter "yyyy-MM-dd HH:mm")]
    (when (ok? prices<>)
      (doseq [datetime (->> (iterate #(t/>> % step) (-> (t/today)
                                                        (t/at "00:00")
                                                        (t/in (t/zone "Europe/Berlin"))))
                            (take 96))]
        (let [{:keys [net total]} (ep/calculate datetime
                                                (-> prices<> :val :pt60m :schedule)
                                                fees)]
          (println (t/format datetime-format datetime)
                   "- net:"
                   (format-money net)
                   "- gross:"
                   (format-money total))))
      (println "Current price:")
      (let [{:keys [net fees total]} (ep/calculate (t/zoned-date-time)
                                                   (-> prices<> :val :pt60m :schedule)
                                                   fees)]
        (println "  Net price:" (format-money net))
        (doseq [{:keys [name price]} fees]
          (println (str "  " name ": " (format-money price))))
        (println "  Gross price:" (format-money total))))))
