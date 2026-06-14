(ns hub.system
  "Integrant component graph wiring the database and the MQTT aggregation loop."
  (:require [hub.aggregation :as agg]
            [hub.db :as db]
            [hub.mqtt :as mqtt]
            [integrant.core :as ig]
            [taoensso.telemere :as t]))

(defn config
  "Builds the Integrant config from the aero-loaded application config."
  [cfg]
  {:hub/db (:database cfg)
   :hub/mqtt (assoc (:mqtt cfg) :db (ig/ref :hub/db))})

(defmethod ig/init-key :hub/db [_ db-config]
  (let [datasource (db/datasource db-config)]
    (db/migrate! datasource)
    (t/log! :info (str "database ready at " (:path db-config)))
    datasource))

(defmethod ig/halt-key! :hub/db [_ _datasource]
  ;; A bare jdbcUrl datasource holds no pooled connections to close.
  nil)

(defmethod ig/init-key :hub/mqtt [_ {:keys [db] :as mqtt-config}]
  (let [state (atom nil)
        handler (agg/make-message-handler state #(db/store-agg15! db %))
        client (mqtt/connect (dissoc mqtt-config :db) handler)]
    (t/log! :info (str "subscribed to " mqtt/topic " on " (:host mqtt-config)))
    client))

(defmethod ig/halt-key! :hub/mqtt [_ client]
  (mqtt/disconnect client))
