(ns cloud.electricity-price
  (:require
   [dinero.math :as d.math]
   [tick.core :as t]))

(defn- find-applicable-fees [raw-fees datetime]
  (let [date (.toLocalDate datetime)
        weekday (t/day-of-week datetime)
        fee-period (->> raw-fees
                        (remove #(t/> (:valid-from %) date))
                        last)
        timezone (-> fee-period :timezone (t/zone))]
    (->> fee-period
         :fees
         (mapv (fn [{:keys [name pricing schedule]}]
                 (let [{:keys [price percent]}
                       (->> (or (get schedule weekday)
                                (:default schedule))
                            (remove (fn [{:keys [start]}]
                                      (t/> (-> date
                                               (t/at start)
                                               (t/in timezone))
                                           datetime)))
                            last)]
                   (cond-> {:name name
                            :pricing pricing}
                     (= "per_kwh" pricing)
                     (assoc :price price)
                     (= "percent" pricing)
                     (assoc :percent percent))))))))

(defn calculate [datetime net-prices fees]
  (let [net-price (->> net-prices
                       (remove #(t/> (:start %) datetime))
                       last
                       :price)
        applicable-fees (find-applicable-fees fees datetime)]
    (reduce (fn [{:keys [total] :as result}
                 {:keys [pricing] :as fee}]
              (let [price (if (= pricing "per_kwh")
                            (:price fee)
                            (d.math/multiply total
                                             (/ (:percent fee)
                                                100)))
                    line-item (assoc fee :price price)]
                (-> result
                    (update :fees conj line-item)
                    (update :total d.math/add price))))
            {:net net-price
             :fees []
             :total net-price}
            applicable-fees)))
