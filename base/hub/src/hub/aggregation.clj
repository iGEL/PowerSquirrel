(ns hub.aggregation
  "Folds the stream of inverter MQTT events into 15-minute watt-second
   aggregates. Ported from the original Rust `aggregation.rs`.

   The state machine is a pure function: `update-inverter-data` takes the
   previous fold state (or nil) plus the next event and returns
   `[new-state agg-row]`. The agg-row is what gets upserted into SQLite."
  (:require [cheshire.core :as json]
            [taoensso.telemere :as t])
  (:import (java.nio.charset StandardCharsets)
           (java.time Instant OffsetDateTime)))

(def ^:const minutes-per-quarter 15)
(def ^:const seconds-per-quarter (* minutes-per-quarter 60)) ; 900

(defn- epoch-s ^long [event]
  (.getEpochSecond ^Instant (:ts event)))

(defn calculate-ws
  "Watt-seconds for the given duration, trapezoidal between the two watt
   samples. Integer division truncates toward zero, matching the Rust i32 math."
  ^long [prev-w cur-w secs]
  (quot (* secs (+ prev-w cur-w)) 2))

(defn floor-to-quarter-s
  "Epoch second at the start of the 15-minute interval containing `epoch-s`,
   e.g. 16:45:00 for 16:59:03. The UTC epoch is aligned to :00/:15/:30/:45."
  ^long [^long epoch-s]
  (- epoch-s (mod epoch-s seconds-per-quarter)))

(defn- agg-row
  "Builds the row to persist from the snapshot taken before the quarter reset."
  [event snapshot complete?]
  {:inverter-sn (:sn event)
   :started-at-s (floor-to-quarter-s (epoch-s (:last-event snapshot)))
   :pv-ws (:pv-ws snapshot)
   :loads-ws (:loads-ws snapshot)
   :load-produced-ws (:load-produced-ws snapshot)
   :grid-import-ws (:grid-import-ws snapshot)
   :grid-export-ws (:grid-export-ws snapshot)
   :battery-charged-ws (:battery-charged-ws snapshot)
   :battery-discharged-ws (:battery-discharged-ws snapshot)
   :battery-soc-bp (long (Math/round (* (:battery-pct event) 100.0)))
   :complete (boolean (and complete? (not (:data-missing snapshot))))
   :updated-at-s (epoch-s event)})

(def ^:private zeroed
  {:pv-ws 0 :loads-ws 0 :load-produced-ws 0
   :grid-import-ws 0 :grid-export-ws 0
   :battery-charged-ws 0 :battery-discharged-ws 0})

(defn- accumulate
  "Adds this interval's watt-seconds onto the running fold. `data-missing` is
   set when the gap to the previous sample exceeds 30s; samples up to 30s apart
   are integrated, samples at or before the previous one are ignored."
  [state last-event event ^long secs]
  (cond
    (> secs 30) (assoc state :data-missing true)
    (> secs 0) (let [pv-ws (calculate-ws (:pv-w last-event) (:pv-w event) secs)
                     loads-ws (calculate-ws (:loads-w last-event) (:loads-w event) secs)
                     grid-ws (calculate-ws (:grid-w last-event) (:grid-w event) secs)
                     battery-ws (calculate-ws (:battery-w last-event) (:battery-w event) secs)]
                 (cond-> state
                   (> pv-ws 0) (update :pv-ws + pv-ws)
                   (> loads-ws 0) (update :loads-ws + loads-ws)
                   (<= loads-ws 0) (update :load-produced-ws + (abs loads-ws))
                   (> grid-ws 0) (update :grid-import-ws + grid-ws)
                   (<= grid-ws 0) (update :grid-export-ws + (abs grid-ws))
                   (> battery-ws 0) (update :battery-discharged-ws + battery-ws)
                   (<= battery-ws 0) (update :battery-charged-ws + (abs battery-ws))))
    :else state))

(defn update-inverter-data
  "Pure fold step. Given the previous state (nil on the first event) and the
   next event, returns `[new-state agg-row]`. Every 15-minute boundary the fold
   resets and the crossing row is flagged `complete` (unless data was missing)."
  [state event]
  (if (nil? state)
    (let [new-state (assoc zeroed :last-event event :data-missing true)]
      [new-state (agg-row event new-state false)])
    (let [last-event (:last-event state)
          secs (- (epoch-s event) (epoch-s last-event))
          accumulated (accumulate state last-event event secs)
          ;; snapshot reflects this event's contribution but the *previous*
          ;; quarter's start, since last-event hasn't advanced yet.
          snapshot accumulated
          new-quarter? (> (quot (epoch-s event) seconds-per-quarter)
                          (quot (epoch-s last-event) seconds-per-quarter))
          new-state (-> (if new-quarter?
                          (merge accumulated zeroed {:data-missing false})
                          accumulated)
                        (assoc :last-event event))]
      [new-state (agg-row event snapshot new-quarter?)])))

;; --- MQTT payload handling -------------------------------------------------

(defn parse-event
  "Parses an inverter MQTT payload (JSON bytes) into an internal event map."
  [^bytes payload]
  (let [m (json/parse-string (String. payload StandardCharsets/UTF_8) true)]
    {:ts (.toInstant (OffsetDateTime/parse (:ts m)))
     :sn (:sn m)
     :pv-w (long (:pv_w m))
     :loads-w (long (:loads_w m))
     :grid-w (long (:grid_w m))
     :battery-w (long (:battery_w m))
     :battery-pct (double (:battery_pct m))}))

(defn make-message-handler
  "Returns an MQTT message handler `(fn [topic payload-bytes])` that folds the
   event into `state-atom` and hands the resulting row to `commit-fn`. Paho
   delivers messages serially on its callback thread, so the fold needs no lock."
  [state-atom commit-fn]
  (fn [_topic ^bytes payload]
    (try
      (let [event (parse-event payload)
            [new-state agg] (update-inverter-data @state-atom event)]
        (reset! state-atom new-state)
        (try
          (commit-fn agg)
          (catch Exception e
            (t/log! {:level :error :error e} "error committing inverter aggregate"))))
      (catch Exception e
        (t/log! {:level :error :error e} "error parsing inverter event")))))
