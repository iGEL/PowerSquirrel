(ns power-squirrel.fees
  (:refer-clojure :exclude [merge])
  (:require
   [cheshire.core :as json]
   [tick.core :as t]))

(defn- parse-fees [fees]
  (->> fees
       (map (fn [fee]
              (update fee :schedule
                      (fn [schedule]
                        (reduce-kv
                         (fn [prev key val]
                           (assoc prev key
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
