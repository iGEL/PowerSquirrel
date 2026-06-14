(ns hub.mqtt
  "MQTT ingestion via the Eclipse Paho v5 client. Subscribes to `posq/#` and
   forwards every message payload to the supplied handler."
  (:require [taoensso.telemere :as t])
  (:import (java.nio.charset StandardCharsets)
           (org.eclipse.paho.mqttv5.client MqttCallback MqttClient MqttConnectionOptions)
           (org.eclipse.paho.mqttv5.client.persist MemoryPersistence)))

(def ^:const client-id "posq-hub")
(def ^:const topic "posq/#")
(def ^:const qos-at-most-once 0)

(defn connect
  "Connects to the broker and subscribes to `posq/#`, routing each message to
   `(message-handler topic ^bytes payload)`. Returns the connected MqttClient.
   Reconnects automatically after a drop and re-subscribes on reconnect."
  ^MqttClient [{:keys [host port user password]} message-handler]
  (let [uri (str "tcp://" host ":" port)
        client (MqttClient. uri client-id (MemoryPersistence.))
        opts (doto (MqttConnectionOptions.)
               (.setAutomaticReconnect true)
               (.setCleanStart true)
               (.setKeepAliveInterval 5))]
    (when (and user password)
      (.setUserName opts user)
      (.setPassword opts (.getBytes ^String password StandardCharsets/UTF_8)))
    (.setCallback client
                  (reify MqttCallback
                    (messageArrived [_ topic message]
                      (message-handler topic (.getPayload message)))
                    (connectComplete [_ reconnect _server-uri]
                      (when reconnect
                        (.subscribe client topic (int qos-at-most-once))))
                    (disconnected [_ _response])
                    (deliveryComplete [_ _token])
                    (mqttErrorOccurred [_ exception]
                      (t/log! {:level :error :error exception} "mqtt error"))
                    (authPacketArrived [_ _reason-code _properties])))
    (.connect client opts)
    (.subscribe client topic (int qos-at-most-once))
    client))

(defn disconnect [^MqttClient client]
  (when (.isConnected client)
    (.disconnect client))
  (.close client))
