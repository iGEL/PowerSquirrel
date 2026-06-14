(ns dev
  "REPL helpers for running the hub system locally."
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [hub.system :as system]
            [integrant.core :as ig]))

(defonce ^:private running (atom nil))

(defn start! []
  (let [cfg (aero/read-config (io/resource "config.edn"))]
    (reset! running (ig/init (system/config cfg)))))

(defn stop! []
  (when-let [sys @running]
    (ig/halt! sys)
    (reset! running nil)))

(defn restart! []
  (stop!)
  (start!))
