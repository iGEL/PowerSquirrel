(ns build
  "Builds the hub uberjar. Usage: clojure -T:build uber"
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def uber-file "target/hub.jar")

(defn- basis [] (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
  (b/compile-clj {:basis (basis) :ns-compile '[hub.main] :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis (basis)
           :main 'hub.main}))
