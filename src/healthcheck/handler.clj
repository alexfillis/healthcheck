(ns healthcheck.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]))

(defn path-check [path]
  (fn []
    {:result :ok}))

(defn url-check [url]
  (fn []
    {:result :ok}))

(defn health-check [key f]
  (fn []
    {key (f)}))

(def hc [(health-check "Temporary directory exists?" 
                       (path-check "/tmp"))
         (health-check "Example.com is available?" 
                       (url-check "http://www.example.com"))])

(defn check-health [checks]
  (reduce (fn [coll f] (conj coll (f))) [] checks))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/status" [] (response (check-health hc)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-defaults api-defaults)))

