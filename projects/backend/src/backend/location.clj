(ns backend.location
  (:require
   [backend.couchdb :as couchdb]
   [backend.geocode :as geocode]
   [backend.result :refer [->Ok branch-ok]]
   [integrant.core :as ig]))

(defprotocol LocationProtocol
  (fetch-locations<> [_])
  (create-location<> [_ params])
  (find-or-create-location [_ params]))

(defrecord Location [couchdb]
  LocationProtocol
  (fetch-locations<> [_]
    (-> (couchdb/request<> couchdb "/locations/_all_docs?include_docs=true")
        (branch-ok (fn [{:keys [body]}]
                     (->> body
                          :rows
                          (map :doc)
                          ->Ok)))))
  (find-or-create-location [this {:keys [zip country]}]
    (if-let [found (->> (fetch-locations<> this)
                        :val
                        (filter #(= [zip country]
                                    [(:zip %) (:country %)]))
                        first)]
      found
      (let [{:keys [lat lon]} (geocode/geocode-zip zip country)
            doc {:name "abc"
                 :zip zip
                 :country country
                 :lat lat
                 :lon lon}]
        (println "New location!")
        (-> (create-location<> this doc)
            (branch-ok (fn [_]
                         doc))))))
  (create-location<> [_ {:keys [name zip country lat lon]}]
    (let [id (str "loc-" (random-uuid))]
      (-> (couchdb/request<> couchdb (str "/locations/" id)
                             {:method :put
                              :body {:_id id
                                     :name name
                                     :zip zip
                                     :country country
                                     :lat lat
                                     :lon lon}
                              :expected-responses [201]})
          (branch-ok (fn [_]
                       (couchdb/request<> couchdb (str "/" id)
                                          {:method :put
                                           :expected-responses [201]})))))))

(defmethod ig/init-key ::location
  [_ {couchdb :backend.couchdb/couchdb}]
  (map->Location {:couchdb couchdb}))
