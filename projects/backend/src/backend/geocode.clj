(ns backend.geocode
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn geocode-zip [zip country]
  (let [url (format "https://nominatim.openstreetmap.org/search.php?country=%s&postalcode=%s&format=jsonv2"
                    (java.net.URLEncoder/encode country "UTF-8")
                    (java.net.URLEncoder/encode zip "UTF-8"))
        resp (http/get url {:headers {"User-Agent" "PowerSquirrel (igel@igels.net)"}
                            :accept :json
                            :as :text})]
    (when (= 200 (:status resp))
      (let [data (json/parse-string (:body resp) true)
            first (first data)]
        (when first
          {:lat (Double/parseDouble (:lat first))
           :lon (Double/parseDouble (:lon first))})))))

