(ns cloud.system
  (:require
   [cloud.config]
   [integrant.core :as ig]))

(defn init []
  (-> (slurp "resources/system.edn")
      (ig/read-string)
      (ig/init)))

(defn halt [system]
  (when system
    (ig/halt! system)))
