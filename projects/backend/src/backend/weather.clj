(ns backend.weather
  (:require [cheshire.core :as json]
            [clj-http.client :as http]))

(defn fetch-weather [{:keys [lat lon]}]
  (when-let [api (System/getenv "OPENWEATHERMAP_APIKEY")]
    (let [url (format (str "https://api.openweathermap.org/data/3.0/onecall?lat=%s&lon=%s"
                           "&exclude=minutely,daily,alerts&units=metric&appid=%s") lat lon api)
          resp (http/get url {:headers {"User-Agent" "PowerSquirrel (igel@igels.net)"}
                              :accept :json
                              :as :text})]
      (when (= 200 (:status resp))
        (let [data (json/parse-string (:body resp) true)]
          (map (fn [{:keys [dt temp clouds]}]
                 {:time (java.time.Instant/ofEpochSecond (long dt))
                  :temp (double temp)
                  :clouds (int clouds)})
               (:hourly data)))))))

