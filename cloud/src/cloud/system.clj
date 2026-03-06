(ns cloud.system
  (:require
   [cloud.config]
   [integrant.core :as ig]))

(defn init []
  (let [config (-> (slurp "resources/system.edn")
                   (ig/read-string))]
    (ig/load-namespaces config)
    (ig/init config)))

(defn halt [system]
  (when system
    (ig/halt! system)))
