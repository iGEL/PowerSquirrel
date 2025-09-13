(ns backend.config
  (:require
   [aero.core :refer [read-config]]
   [clojure.string :as str]
   [integrant.core :as ig]))

(def environments #{:dev :test})

(defn env []
  (if-let [by-env (some-> (System/getenv "POSQ_ENV")
                          str/lower-case
                          keyword
                          environments)]
    by-env
    :dev))

(defn dev? []
  (= :dev (env)))

(defn test? []
  (= :test (env)))

(defmethod ig/init-key ::config
  [_ _]
  (read-config "resources/config.edn" {:profile (env)}))
