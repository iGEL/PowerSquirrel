(ns backend.sun
  (:import (org.shredzone.commons.suncalc SunPosition)))

(defn position [{:keys [datetime]
                 {:keys [lat lon]} :location}]
  (let [pos (-> (SunPosition/compute)
                (.on datetime)
                (.at lat lon)
                (.execute))]
    {:azimuth (.getAzimuth pos)
     :altitude (.getAltitude pos)}))
