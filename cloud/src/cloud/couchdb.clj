(ns cloud.couchdb
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [cloud.config :as config]
   [cloud.result :refer [->Err ->Ok branch-ok try-result]]
   [integrant.core :as ig]))

(defprotocol
 CouchDBProtocol
  (request<> [_ path] [_ path options])
  (create-db<> [_ db-name])
  (setup<> [_])
  (setup-for-tests<> [_]))

(defrecord CouchDB [base-uri basic-auth]
  CouchDBProtocol
  (request<> [this path]
    (request<> this path {}))
  (request<> [_ path {:keys [method body expected-responses headers]
                      :or {method :get
                           expected-responses [200]}}]
    (-> (try-result
         (http/request (cond-> {:url (str base-uri path)
                                :method method
                                :basic-auth basic-auth
                                :headers headers
                                :unexceptional-status #((set expected-responses) %)}
                         body (assoc-in [:headers "content-type"] "application/json")
                         body (assoc :body (json/generate-string body)))))
        (branch-ok (fn [{:keys [body headers] :as response}]
                     (if (str/starts-with? (get headers "content-type")
                                           "application/json")
                       (->Ok (assoc response :body (json/parse-string body true)))
                       (->Ok response))))))
  (create-db<> [this db-name]
    (let [path (str "/" db-name)]
      (-> (request<> this path
                     {:expected-responses [200 404]})
          (branch-ok (fn [{:keys [status]}]
                       (when (= 404 status)
                         (request<> this
                                    path
                                    {:method :put
                                     :expected-responses [201]})))))))
  (setup<> [this]
    (-> (create-db<> this "locations")
        (branch-ok (fn [_]
                     (create-db<> this "pricing")))))
  (setup-for-tests<> [this]
    (if (config/test?)
      (do
        (request<> this "/locations" {:method :delete})
        (request<> this "/pricing" {:method :delete})
        (setup<> this))
      (->Err (ex-info "Not in test environment!" {})))))

(defmethod ig/init-key ::couchdb
  [_ {{{:keys [base-uri basic-auth]} :couchdb} :cloud.config/config}]
  (map->CouchDB {:base-uri base-uri
                 :basic-auth basic-auth}))
