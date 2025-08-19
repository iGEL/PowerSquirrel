(ns power-squirrel.fees
  (:require
   [cheshire.core :as json])
  (:import
   (java.time LocalDate LocalTime)
   (java.math BigDecimal)))

(defn- parse-time [time]
  (LocalTime/parse (if (= 5 (count time)) (str time ":00") time)))

(defn- parse-fees [fees]
  (map (fn [fee]
         (update fee :schedule
                 (fn [schedule]
                   (reduce-kv
                    (fn [prev key val]
                      (assoc prev key
                             (map (fn [start+price]
                                    (-> start+price
                                        (update :start parse-time)
                                        (update :price #(BigDecimal. %))))
                                  val)))
                    {}
                    schedule))))
       fees))

(defn- parse-period [period]
  (-> period
      (update :valid-from LocalDate/parse)
      (update :fees parse-fees)))

(defn parse [path]
  (->> (json/parse-string (slurp path) true)
       (map parse-period)))

(defn merge [fee1 fee2]
  (->> (concat (map :valid-from fee1)
               (map :valid-from fee2))
       set
       vec
       sort
       (map (fn [date]
              (let [fee1-fees (->> fee1
                                   (remove #(.isAfter (:valid-from %) date))
                                   (sort-by :valid-from)
                                   last
                                   :fees)
                    fee2-fees (->> fee2
                                   (remove #(.isAfter (:valid-from %) date))
                                   (sort-by :valid-from)
                                   last
                                   :fees)]
                {:valid-from date
                 :fees (->> (concat fee1-fees fee2-fees)
                            (sort-by :position))})))))
