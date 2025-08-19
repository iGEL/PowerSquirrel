(ns power-squirrel.core
  (:require [power-squirrel.geocode :as geocode]
            [power-squirrel.weather :as weather]
            [power-squirrel.electricity-price :as ep]
            [power-squirrel.fees :as fees])
  (:import (java.time ZonedDateTime ZoneId LocalDate Duration)
           (java.math BigDecimal RoundingMode)))

(defn fmt-money [^BigDecimal amount currency]
  (str (.setScale amount 2 RoundingMode/HALF_UP) " " currency))

(defn get-net-prices []
  [["2025-07-26T00:00:00+02:00" "0.10676"]
   ["2025-07-26T01:00:00+02:00" "0.1007"]
   ["2025-07-26T02:00:00+02:00" "0.09773"]
   ["2025-07-26T03:00:00+02:00" "0.09665"]
   ["2025-07-26T04:00:00+02:00" "0.09645"]
   ["2025-07-26T05:00:00+02:00" "0.09489"]
   ["2025-07-26T06:00:00+02:00" "0.09713"]
   ["2025-07-26T07:00:00+02:00" "0.0961"]
   ["2025-07-26T08:00:00+02:00" "0.08786"]
   ["2025-07-26T09:00:00+02:00" "0.08585"]
   ["2025-07-26T10:00:00+02:00" "0.07959"]
   ["2025-07-26T11:00:00+02:00" "0.0752"]
   ["2025-07-26T12:00:00+02:00" "0.06642"]
   ["2025-07-26T13:00:00+02:00" "0.04987"]
   ["2025-07-26T14:00:00+02:00" "0.0509"]
   ["2025-07-26T15:00:00+02:00" "0.06807"]
   ["2025-07-26T16:00:00+02:00" "0.07481"]
   ["2025-07-26T17:00:00+02:00" "0.0842"]
   ["2025-07-26T18:00:00+02:00" "0.0982"]
   ["2025-07-26T19:00:00+02:00" "0.1066"]
   ["2025-07-26T20:00:00+02:00" "0.12195"]
   ["2025-07-26T21:00:00+02:00" "0.12707"]
   ["2025-07-26T22:00:00+02:00" "0.11672"]
   ["2025-07-26T23:00:00+02:00" "0.1088"]])

(defn -main [& _]
  #_(let [zip "12207"
          country "Germany"
          loc (geocode/geocode-zip zip country)
          weathers (when loc (weather/fetch-weather loc))]
      (when (and loc weathers)
        (doseq [{:keys [time temp clouds]} weathers]
        ;; Placeholder for sun position; can be implemented with astronomical formulas later.
          (println (format "%s Temperature: %.2f°, Clouds: %d%%" time temp (int clouds))))))

  (let [stromnetz-berlin (fees/parse "../backend/resources/stromnetz-berlin.json")
        fees (fees/parse "../backend/resources/fees.json")]
    (clojure.pprint/pprint (fees/merge  fees stromnetz-berlin)))
  #_(let [fees (ep/parse-fees "../backend/resources/fees.json")
          now (ZonedDateTime/now (ZoneId/systemDefault))
          net-prices (map (fn [[ts p]] [(ZonedDateTime/parse ts) (BigDecimal. ^String p)]) (get-net-prices))
          start (ZonedDateTime/parse "2025-07-26T00:00:00+02:00")
          end   (ZonedDateTime/parse "2025-07-26T23:45:00+02:00")
          step  (Duration/ofMinutes 15)]
      (loop [t start]
        (when (.isBefore t (.plusMinutes end 1))
          (let [[_ price] (or (last (filter (fn [[st _]] (not (.isAfter st t))) net-prices)) (first net-prices))
                gross (ep/calculate price t fees)]
            (println (format "%s: %s net - %s gross"
                             (.format t (java.time.format.DateTimeFormatter/ofPattern "HH:mm"))
                             (fmt-money price "EUR")
                             (fmt-money gross "EUR"))))
          (recur (.plus t step))))

    ;; Detailed breakdown at current time
      (let [fees (ep/parse-fees "../backend/resources/fees.json")
            now (ZonedDateTime/now (ZoneId/systemDefault))
            [_ price] (last (map (fn [[ts p]] [(ZonedDateTime/parse ts) (BigDecimal. ^String p)]) (get-net-prices)))
            {:keys [applied-fees total]} (ep/calculate-detailed price now fees)]
        (println (format "Net price for %s: %s" (.format now (java.time.format.DateTimeFormatter/ofPattern "HH:mm")) (fmt-money price "EUR")))
        (doseq [{:keys [name pricing-type rate amount]} applied-fees]
          (println (format "%s: %s %s = %s" name rate pricing-type (fmt-money amount "EUR"))))
        (println (str "Gross price: " (fmt-money total "EUR"))))))

