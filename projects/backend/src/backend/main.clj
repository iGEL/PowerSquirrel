(ns backend.main
  (:require
   [backend.couchdb :as couchdb]
   [backend.electricity-price :as ep]
   [backend.entsoe :as entsoe]
   [backend.fees :as fees]
   [backend.location :as location]
   [backend.result :refer [branch-err ok?]]
   [backend.system :as system]
   [backend.weather :as weather]
   [dinero.format :refer [format-money]]
   [tick.core :as t]))

(def version "0.0.1")

(defn -main [& _]
  (println (str "PowerSquirrel " version " 🐿️"))
  (let [{:keys [backend.couchdb/couchdb
                backend.entsoe/entsoe
                backend.location/location]} (system/init)]
    (-> (couchdb/setup<> couchdb)
        (branch-err (fn [e]
                      (binding [*out* *err*]
                        (println (str "\u001b[31m"
                                      "Failure to setup couchdb. "
                                      (ex-message e)
                                      "\u001b[0m")))
                      (System/exit 1))))
    (let [zip "12207"
          country "Germany"
          loc (location/find-or-create-location location
                                                {:zip zip
                                                 :country country})
          weathers (when loc (weather/fetch-weather loc))]
      (when (and loc weathers)
        (doseq [{:keys [time temp clouds]} weathers]
          ;; Placeholder for sun position; can be implemented with astronomical formulas later.
          (println (format "%s Temperature: %.2f°, Clouds: %d%%" time temp (int clouds))))))

    (let [stromnetz-berlin (fees/parse "../backend/resources/stromnetz-berlin.json")
          fees (fees/merge stromnetz-berlin (fees/parse "../backend/resources/fees.json"))
          prices<> (entsoe/fetch-prices<> entsoe (t/today) :de-lu)
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
          (println "  Gross price:" (format-money total)))))))
