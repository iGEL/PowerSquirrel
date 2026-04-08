(ns power-squirrel.main
  (:require
   ["primereact/api" :refer [PrimeReactProvider]]
   ["primereact/button" :refer [Button]]
   ["primereact/menubar" :refer [Menubar]]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defui home []
  ($ :main
     ($ Button {:label "Yes!"})
     ($ :i.pi.pi-check)
     "Hello world!"))

(defui settings []
  ($ :<>
     ($ :h1 "Hello settings")
     ($ :a {:href (rfe/href ::home)} "Go home")))

(defui app [{:keys [route]}]
  ($ PrimeReactProvider
     ($ Menubar {:start ($ :img.logo {:src "/img/sparky.svg"
                                      :alt ""
                                      :title "Sparky is here to help"})
                 :model (clj->js [{:label "Home"
                                   :icon "pi pi-home"
                                   :url (rfe/href ::home)}
                                  {:label "Settings"
                                   :icon "pi pi-cog"
                                   :url (rfe/href ::settings)}])})
     (if-let [view (get-in route [:data :view])]
       ($ view)
       ($ :div "Not found"))))

(def routes
  [["/" {:name ::home
         :view home}]
   ["/settings" {:name ::settings
                 :view settings}]])

(def router (rf/router routes))
(defonce root (uix.dom/create-root (js/document.getElementById "root")))

(defn render [{:keys [route]}]
  (uix.dom/render-root
   ($ uix/strict-mode
      ($ app {:route route}))
   root))

(defn on-navigate [route]
  (render {:route route}))

(defn ^:export init []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))
