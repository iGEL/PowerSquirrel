(ns backend.fees
  (:refer-clojure :exclude [merge])
  (:require
   [clojure.string :as str]
   [cheshire.core :as json]
   [tick.core :as t])
  (:import
   [java.time DayOfWeek]
   [java.time.format DateTimeFormatter]
   [java.util Locale]))

(defn- parse-day-of-week [str]
  (let [formatter (DateTimeFormatter/ofPattern "EEE" Locale/ENGLISH)]
    (->> str
         str/lower-case
         str/capitalize
         (.parse formatter)
         DayOfWeek/from)))

(defn- parse-fees [fees]
  (->> fees
       (map (fn [fee]
              (update fee :schedule
                      (fn [schedule]
                        (reduce-kv
                         (fn [prev key val]
                           (assoc prev
                                  (if (= :default key)
                                    key
                                    (parse-day-of-week (name key)))
                                  (map (fn [start+price]
                                         (-> start+price
                                             (update :start t/time)
                                             (update :price bigdec)))
                                       val)))
                         {}
                         schedule)))))
       (sort-by :position)))

(defn- parse-period [period]
  (-> period
      (update :valid-from t/date)
      (update :fees parse-fees)))

(defn parse [path]
  (->> (json/parse-string (slurp path) true)
       (map parse-period)
       (sort-by :valid-from)))

(defn merge
  "Merges 2 fee schedules"
  [schedule1 schedule2]
  (->> (concat (map :valid-from schedule1)
               (map :valid-from schedule2))
       set
       vec
       sort
       (map (fn [date]
              (let [fee1-fees (->> schedule1
                                   (remove #(t/> (:valid-from %) date))
                                   (sort-by :valid-from)
                                   last
                                   :fees)
                    fee2-fees (->> schedule2
                                   (remove #(t/> (:valid-from %) date))
                                   (sort-by :valid-from)
                                   last
                                   :fees)]
                {:valid-from date
                 :fees (->> (concat fee1-fees fee2-fees)
                            (sort-by :position))})))))
