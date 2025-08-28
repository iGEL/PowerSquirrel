(ns backend.couchdb
  (:require
   [backend.result :refer [->Ok branch-ok try-result]]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]))

(def base-uri "http://localhost:5984")
(def basic-auth ["admin" "password"])

(defn- request<>
  ([path]
   (request<> path {}))
  ([path {:keys [method body expected-responses]
          :or {method :get
               expected-responses [200]}}]
   (-> (try-result
        (http/request (cond-> {:url (str base-uri path)
                               :method method
                               :basic-auth basic-auth
                               :unexceptional-status #((set expected-responses) %)}
                        (map? body) (assoc :body (json/generate-string body)))))
       (branch-ok (fn [{:keys [body headers] :as response}]
                    (if (str/starts-with? (get headers "content-type")
                                          "application/json")
                      (->Ok (assoc response :body (json/parse-string body true)))
                      (->Ok response)))))))

(defn create-db<> [db-name]
  (let [path (str "/" db-name)]
    (-> (request<> path
                   {:expected-responses [200 404]})
        (branch-ok (fn [{:keys [status]}]
                     (when (= 404 status)
                       (request<> path
                                  {:method :put
                                   :expected-responses [201]})))))))

(defn fetch-locations<> []
  (-> (request<> "/locations/_all_docs?include_docs=true")
      (branch-ok (fn [{:keys [body]}]
                   (->> body
                        :rows
                        (map :doc)
                        ->Ok)))))

(defn create-location<> [{:keys [name zip country lat lon]}]
  (let [id (str "loc-" (random-uuid))]
    (-> (request<> (str "/locations/" id)
                   {:method :put
                    :body {:_id id
                           :name name
                           :zip zip
                           :country country
                           :lat lat
                           :lon lon}
                    :expected-responses [201]})
        (branch-ok (fn [_]
                     (request<> (str "/" id)
                                {:method :put
                                 :expected-responses [201]}))))))

(defn setup<> []
  (-> (create-db<> "locations")
      (branch-ok (fn [_]
                   (create-db<> "pricing")))))
