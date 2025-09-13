(ns backend.main
  (:require
   [backend.couchdb :as couchdb]
   [backend.electricity-price :as ep]
   [backend.entsoe :as entsoe]
   [backend.fees :as fees]
   [backend.geocode :as geocode]
   [backend.result :refer [branch-err branch-ok ok?]]
   [backend.weather :as weather]
   [dinero.format :refer [format-money]]
   [tick.core :as t]))

(def version "0.0.1")

(defn find-or-create-location [zip country]
  (if-let [found (->> (couchdb/fetch-locations<>)
                      :val
                      (filter #(= [zip country]
                                  [(:zip %) (:country %)]))
                      first)]
    found
    (let [{:keys [lat lon]} (geocode/geocode-zip zip country)
          doc {:name "abc"
               :zip zip
               :country country
               :lat lat
               :lon lon}]
      (println "New location!")
      (-> (couchdb/create-location<> doc)
          (branch-ok (fn [_]
                       doc))))))

(defn -main [& _]
  (println (str "PowerSquirrel " version " 🐿️"))
  (-> (couchdb/setup<>)
      (branch-err (fn [e]
                    (binding [*out* *err*]
                      (println (str "\u001b[31m"
                                    "Failure to setup couchdb. "
                                    (ex-message e)
                                    "\u001b[0m")))
                    (System/exit 1))))
  (let [zip "12207"
        country "Germany"
        loc (find-or-create-location zip country)
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
