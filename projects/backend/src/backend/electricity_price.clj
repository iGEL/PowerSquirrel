(ns backend.electricity-price
  (:require
   [cheshire.core :as json]
   [clojure.java.io :refer [reader]])
  (:import
   (java.math BigDecimal)
   (java.time DayOfWeek LocalDate LocalTime ZonedDateTime)))

(defn parse-fees [path]
  (with-open [r (reader path)]
    (json/parse-stream r true)))

(defn- parse-date [s]
  (LocalDate/parse s))

(defn- parse-time [s]
  (LocalTime/parse (if (= 5 (count s)) (str s ":00") s)))

(defn- <=time? [^LocalTime a ^LocalTime b]
  (or (.equals a b) (.isBefore a b)))

(defn- day-key [^DayOfWeek d]
  ({DayOfWeek/MONDAY :monday
    DayOfWeek/TUESDAY :tuesday
    DayOfWeek/WEDNESDAY :wednesday
    DayOfWeek/THURSDAY :thursday
    DayOfWeek/FRIDAY :friday
    DayOfWeek/SATURDAY :saturday
    DayOfWeek/SUNDAY :sunday} d))

(defn- find-applicable-period [fees ^LocalDate date]
  (->> (:fees fees)
       (filter #(not (.isAfter (parse-date (:valid_from %)) date)))
       (sort-by #(parse-date (:valid_from %)))
       last))

(defn- rate-for-time [schedule ^ZonedDateTime dt]
  (let [k (day-key (.getDayOfWeek dt))
        entries (or (get schedule k) (:default schedule))
        t (.toLocalTime dt)
        parsed (map (fn [{:keys [start price]}]
                      [(parse-time start) (BigDecimal. ^String price)])
                    entries)]
    (->> parsed
         (filter (fn [[st _]] (<=time? st t)))
         (sort-by first)
         last
         second)))

(defn calculate-detailed [^BigDecimal net ^ZonedDateTime dt fees]
  (let [period (find-applicable-period fees (.toLocalDate dt))
        per-kwh (for [{:keys [name pricing currency schedule]} (:fees period)
                      :when (= pricing "per_kwh")]
                  (let [rate (rate-for-time schedule dt)]
                    {:name name :pricing-type pricing :currency currency :rate rate :amount rate}))
        subtotal (reduce #(.add ^BigDecimal %1 ^BigDecimal (:amount %2)) net per-kwh)
        perc (for [{:keys [name pricing currency schedule]} (:fees period)
                   :when (= pricing "percent")]
               (let [rate (rate-for-time schedule dt)
                     amt (.divide (.multiply subtotal rate) (BigDecimal. "100"))]
                 {:name name :pricing-type pricing :currency currency :rate rate :amount amt}))
        total (reduce #(.add ^BigDecimal %1 ^BigDecimal (:amount %2)) subtotal perc)]
    {:net net :applied-fees (concat per-kwh perc) :total total}))

(defn calculate [^BigDecimal net ^ZonedDateTime dt fees]
  (:total (calculate-detailed net dt fees)))

