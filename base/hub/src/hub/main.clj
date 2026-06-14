(ns hub.main
  "Entry point for the PowerSquirrel base hub."
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [hub.system :as system]
            [integrant.core :as ig]
            [taoensso.telemere :as t])
  (:gen-class))

(defn -main [& _args]
  (t/log! :info "PowerSquirrel base hub 🐿️")
  (let [cfg (aero/read-config (io/resource "config.edn"))
        system (ig/init (system/config cfg))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable #(ig/halt! system)))
    ;; The MQTT client runs on its own threads; park the main thread.
    @(promise)))
