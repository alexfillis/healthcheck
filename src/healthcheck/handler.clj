(ns healthcheck.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [healthcheck.check :refer :all])
  (:import [java.util.concurrent Executors ScheduledExecutorService TimeUnit]))

(def scheduler (atom nil))

(def hc (atom nil))

(defn health-checks [scheduler]
  [(schedule-check scheduler
                   "Temporary directory exists?"
                   (path-check "/tmp/healthcheck.txt"))
   (schedule-check scheduler
                   "Example.com is available?"
                   (url-check "http://www.example.com"))])

(defn init []
  (println "Starting...")
  (swap! scheduler (fn [_] (Executors/newScheduledThreadPool 10)))
  (swap! hc (fn [_] (health-checks @scheduler)))
  (println "Started!"))

(defn destroy []
  (println "Stopping...")
  (.shutdown @scheduler)
  (.awaitTermination @scheduler 3 TimeUnit/SECONDS)
  (println "Stopped!"))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/status" [] (response (check-health @hc)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-defaults api-defaults)))

