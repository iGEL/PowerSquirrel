(ns backend.entsoe
  (:require
   [backend.result :refer [->Ok branch-ok try-result]]
   [clj-http.client :as http]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [dinero.core :refer [money-of]]
   [dinero.math :as d.math]
   [tick.core :as t])
  (:import [java.time Duration ZonedDateTime]))

(def token (System/getenv "ENTSOE_TOKEN"))
(def base-uri "https://web-api.tp.entsoe.eu/api")
(def bidding-zones
  {:de-lu "10Y1001A1001A82H"})
(def document-type "A44") ;; Pricing info
(def uri-datetime-format (t/formatter "yyyyMMddHHmm"))
(def utc (t/zone "UTC"))
(def europe-berlin (t/zone "Europe/Berlin"))

(defn- utc-midnight [time]
  (t/format uri-datetime-format
            (-> time
                (t/at (t/midnight))
                (t/in europe-berlin)
                (t/in utc))))

(defn- strip-xml-ns [xml]
  (if (map? xml)
    (-> xml
        (update :tag #(-> % name keyword))
        (update :content #(->> %
                               (remove (fn [s]
                                         (and (string? s)
                                              (str/blank? s))))
                               (mapv strip-xml-ns))))
    xml))

(defn content-of-tag [xml tag]
  (->> xml
       (filter #(= tag (:tag %)))
       first
       :content))

(defn- extract-info<> [xml]
  (->> xml
       :content
       (filter #(= :TimeSeries (:tag %)))
       (reduce (fn [prev {:keys [content]}]
                 (let [currency (-> (content-of-tag content :currency_Unit.name)
                                    first
                                    str/lower-case
                                    keyword)
                       input-unit (-> (content-of-tag content :price_Measure_Unit.name)
                                      first)
                       {:keys [pricing kwh-conversion]} (case (str/lower-case input-unit)
                                                          "mwh" {:pricing "per_kwh" :kwh-conversion #(d.math/divide % 1000)}
                                                          "kwh" {:pricing "per_kwh" :kwh-conversion identity})
                       period (content-of-tag content :Period)
                       resolution (-> (content-of-tag period :resolution)
                                      first)
                       step (Duration/parse resolution)
                       start (-> (content-of-tag period :timeInterval)
                                 (content-of-tag :start)
                                 first
                                 ZonedDateTime/parse)
                       end (-> (content-of-tag period :timeInterval)
                               (content-of-tag :end)
                               first
                               ZonedDateTime/parse)
                       parse-points (fn [points]
                                      (loop [position 1
                                             result []
                                             remaining points
                                             time start]
                                        (let [price (-> remaining
                                                        first
                                                        :content
                                                        (content-of-tag :price.amount)
                                                        first
                                                        (money-of currency)
                                                        kwh-conversion)
                                              xml-position (-> remaining
                                                               first
                                                               :content
                                                               (content-of-tag :position)
                                                               first
                                                               Integer/parseInt)
                                              new-result (if (= position xml-position)
                                                           (conj result [time price])
                                                           (conj result [time (-> result last last)]))
                                              new-remaining (if (= position xml-position)
                                                              (rest remaining)
                                                              remaining)
                                              new-time (t/>> time step)]
                                          (if (= new-time end)
                                            (into (sorted-map) new-result)
                                            (recur
                                             (inc position)
                                             new-result
                                             new-remaining
                                             new-time)))))]
                   (assoc prev
                          (-> resolution str/lower-case keyword)
                          {:pricing pricing
                           :schedule (->> period
                                          (filter #(= :Point (:tag %)))
                                          parse-points)})))
               {})
       ->Ok))

(defn fetch-prices<> [start zone]
  (let [uri (str base-uri
                 "?securityToken=" token
                 "&documentType=" document-type
                 "&in_Domain=" (bidding-zones zone)
                 "&out_Domain=" (bidding-zones zone)
                 "&periodStart=" (utc-midnight start)
                 "&periodEnd=" (utc-midnight (t/>> start (t/new-period 1 :days))))]
    (-> (try-result (http/get uri {:unexceptional-status #(= % 200)}))
        (branch-ok (fn [{:keys [body]}]
                     (->Ok body)))
        (branch-ok (fn [str]
                     (try-result
                      (-> str
                          xml/parse-str))))
        (branch-ok (fn [xml]
                     (->Ok (strip-xml-ns xml))))
        (branch-ok extract-info<>))))

(comment
  (def token "abc")
  (fetch-prices<> (t/date "2025-08-21") :de-lu))
