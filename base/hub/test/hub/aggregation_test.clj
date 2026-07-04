(ns hub.aggregation-test
  "Ported from the Rust `aggregation.rs` test module."
  (:require [clojure.test :refer [deftest is testing]]
            [hub.aggregation :as agg])
  (:import (java.time LocalDateTime ZoneOffset)))

(defn- inst
  "UTC Instant for the given calendar fields."
  [y mo d h mi s]
  (-> (LocalDateTime/of (int y) (int mo) (int d) (int h) (int mi) (int s))
      (.toInstant ZoneOffset/UTC)))

(defn- epoch [y mo d h mi s]
  (.getEpochSecond (inst y mo d h mi s)))

(defn- event [ts sn pv loads grid battery pct]
  {:ts ts :sn sn :pv-w pv :loads-w loads :grid-w grid :battery-w battery :battery-pct pct})

(defn- event-at [h mi s]
  (event (inst 2026 1 2 h mi s) "1234" 0 0 0 0 0.0))

(deftest update-inverter-data-sequence
  (testing "first event initializes state and returns a zeroed row"
    (let [e1 (event (inst 2025 12 13 14 59 25) "1234" 200 400 100 100 20.1)
          [state1 row1] (agg/update-inverter-data nil e1)]
      (is (= {:inverter-sn "1234"
              :started-at-s (epoch 2025 12 13 14 45 0)
              :pv-ws 0 :loads-ws 0 :load-produced-ws 0
              :grid-import-ws 0 :grid-export-ws 0
              :battery-charged-ws 0 :battery-discharged-ws 0
              :battery-soc-bp 2010
              :complete false
              :updated-at-s (.getEpochSecond (:ts e1))}
             row1))
      (is (= {:last-event e1 :data-missing true
              :pv-ws 0 :loads-ws 0 :load-produced-ws 0
              :grid-import-ws 0 :grid-export-ws 0
              :battery-charged-ws 0 :battery-discharged-ws 0}
             state1))

      (testing "second event in the same quarter accumulates watt-seconds"
        (let [e2 (event (inst 2025 12 13 14 59 45) "1234" 200 500 200 100 20.1)
              [state2 row2] (agg/update-inverter-data state1 e2)]
          (is (= {:inverter-sn "1234"
                  :started-at-s (epoch 2025 12 13 14 45 0)
                  :pv-ws 4000 :loads-ws 9000 :load-produced-ws 0
                  :grid-import-ws 3000 :grid-export-ws 0
                  :battery-charged-ws 0 :battery-discharged-ws 2000
                  :battery-soc-bp 2010
                  :complete false
                  :updated-at-s (.getEpochSecond (:ts e2))}
                 row2))
          (is (= {:last-event e2 :data-missing true
                  :pv-ws 4000 :loads-ws 9000 :load-produced-ws 0
                  :grid-import-ws 3000 :grid-export-ws 0
                  :battery-charged-ws 0 :battery-discharged-ws 2000}
                 state2))

          (testing "crossing into a new quarter returns the old quarter and resets"
            (let [e3 (event (inst 2025 12 13 15 0 5) "1234" 0 -600 -500 -100 20.1)
                  [state3 row3] (agg/update-inverter-data state2 e3)]
              (is (= {:inverter-sn "1234"
                      :started-at-s (epoch 2025 12 13 14 45 0)
                      :pv-ws 6000 :loads-ws 9000 :load-produced-ws 1000
                      :grid-import-ws 3000 :grid-export-ws 3000
                      :battery-charged-ws 0 :battery-discharged-ws 2000
                      :battery-soc-bp 2010
                      :complete false
                      :updated-at-s (.getEpochSecond (:ts e3))}
                     row3))
              (is (= {:last-event e3 :data-missing false
                      :pv-ws 0 :loads-ws 0 :load-produced-ws 0
                      :grid-import-ws 0 :grid-export-ws 0
                      :battery-charged-ws 0 :battery-discharged-ws 0}
                     state3))

              (testing "next event in the new quarter accumulates from zero"
                (let [e4 (event (inst 2025 12 13 15 0 25) "1234" 0 -600 -500 -100 20.1)
                      [state4 row4] (agg/update-inverter-data state3 e4)]
                  (is (= {:inverter-sn "1234"
                          :started-at-s (epoch 2025 12 13 15 0 0)
                          :pv-ws 0 :loads-ws 0 :load-produced-ws 12000
                          :grid-import-ws 0 :grid-export-ws 10000
                          :battery-charged-ws 2000 :battery-discharged-ws 0
                          :battery-soc-bp 2010
                          :complete false
                          :updated-at-s (.getEpochSecond (:ts e4))}
                         row4))
                  (is (= {:last-event e4 :data-missing false
                          :pv-ws 0 :loads-ws 0 :load-produced-ws 12000
                          :grid-import-ws 0 :grid-export-ws 10000
                          :battery-charged-ws 2000 :battery-discharged-ws 0}
                         state4)))))))))))

(defn- state-at
  "A fold state whose last event is at the given time, all counters zeroed."
  [h mi s data-missing]
  {:last-event (event-at h mi s) :data-missing data-missing
   :pv-ws 0 :loads-ws 0 :load-produced-ws 0
   :grid-import-ws 0 :grid-export-ws 0
   :battery-charged-ws 0 :battery-discharged-ws 0})

(deftest complete-flag
  (testing "no missing data and a full 15 minutes -> complete"
    (let [[_ row] (agg/update-inverter-data (state-at 18 59 50 false) (event-at 19 0 2))]
      (is (:complete row))))

  (testing "no missing data but not a full 15 minutes -> not complete"
    (let [[_ row] (agg/update-inverter-data (state-at 18 59 50 false) (event-at 18 59 52))]
      (is (not (:complete row)))))

  (testing "full 15 minutes but missing data -> not complete"
    (let [[_ row] (agg/update-inverter-data (state-at 18 59 50 true) (event-at 19 0 2))]
      (is (not (:complete row))))))

(deftest data-missing-gap
  (testing "gap of up to 30s keeps data present"
    (let [[state _] (agg/update-inverter-data (state-at 19 50 50 false) (event-at 19 51 20))]
      (is (not (:data-missing state)))))

  (testing "gap of more than 30s flags data as missing"
    (let [[state _] (agg/update-inverter-data (state-at 19 50 50 false) (event-at 19 51 21))]
      (is (:data-missing state)))))
