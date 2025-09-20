(ns backend.entsoe
  (:require
   [backend.result :refer [->Ok branch-ok try-result]]
   [clj-http.client :as http]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [dinero.core :refer [money-of]]
   [dinero.math :as d.math]
   [integrant.core :as ig]
   [tick.core :as t])
  (:import [java.time Duration ZonedDateTime]))

(def uri-datetime-format (t/formatter "yyyyMMddHHmm"))
(def utc (t/zone "UTC"))

(defn- midnight-utc
  "Returns midnight in the given timezone, converted afterwards to UTC"
  [{:keys [time in-timezone]}]
  (t/format uri-datetime-format
            (-> time
                (t/at (t/midnight))
                (t/in (t/zone in-timezone))
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
                       parse-points
                       (fn [points]
                         (reduce
                          (fn [result point]
                            (let [price (-> point
                                            :content
                                            (content-of-tag :price.amount)
                                            first
                                            (money-of currency)
                                            kwh-conversion)
                                  start-offset (.multipliedBy step
                                                              (-> point
                                                                  :content
                                                                  (content-of-tag :position)
                                                                  first
                                                                  Integer/parseInt
                                                                  dec))]
                              (conj result {:start (t/>> start start-offset)
                                            :price price})))
                          []
                          points))]
                   (assoc prev
                          (-> resolution str/lower-case keyword)
                          {:pricing pricing
                           :schedule (->> period
                                          (filter #(= :Point (:tag %)))
                                          parse-points)})))
               {})
       ->Ok))

(defprotocol EntsoeProtocol
  (fetch-prices<> [this date zone]))

(defrecord Entsoe [base-uri bidding-zones document-type token]
  EntsoeProtocol
  (fetch-prices<> [_ date zone]
    (let [{:keys [timezone bidding-zone-id]} (bidding-zones zone)
          uri (str base-uri
                   "?securityToken=" token
                   "&documentType=" document-type
                   "&in_Domain=" bidding-zone-id
                   "&out_Domain=" bidding-zone-id
                   "&periodStart=" (midnight-utc {:time date
                                                  :in-timezone timezone})
                   "&periodEnd=" (midnight-utc {:time (t/>> date (t/new-period 1 :days))
                                                :in-timezone timezone}))]
      (-> (try-result (http/get uri {:unexceptional-status #(= % 200)}))
          (branch-ok (fn [{:keys [body]}]
                       (->Ok body)))
          (branch-ok (fn [str]
                       (try-result
                        (-> str
                            xml/parse-str))))
          (branch-ok (fn [xml]
                       (->Ok (strip-xml-ns xml))))
          (branch-ok extract-info<>)))))

(defmethod ig/init-key ::entsoe
  [_ {{{:keys [base-uri bidding-zones document-type token]} :entsoe} :backend.config/config}]
  (map->Entsoe {:base-uri base-uri
                :bidding-zones bidding-zones
                :document-type document-type
                :token token}))

(comment
  (fetch-prices<> (t/date "2025-08-21") :de-lu))
