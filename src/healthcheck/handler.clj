(ns healthcheck.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [healthcheck.check :refer :all])
  (:import [java.util.concurrent Executors ScheduledExecutorService TimeUnit]))

(def check-executor (atom nil))

(def check-scheduler (atom nil))

(def hc (atom nil))

(defn health-checks [executor scheduler]
  [(schedule-check scheduler
                   "Temporary directory exists?"
                   (wrap-exception-check (timed-check executor (path-check "/tmp/healthcheck.txt"))))
   (schedule-check scheduler
                   "Example.com is available?"
                   (wrap-exception-check (timed-check executor (url-check "http://www.example.com"))))])

(defn init []
  (println "Starting...")
  (swap! check-executor (fn [_] (Executors/newFixedThreadPool 10)))
  (swap! check-scheduler (fn [_] (Executors/newScheduledThreadPool 10)))
  (swap! hc (fn [_] (health-checks @check-executor @check-scheduler)))
  (println "Started!"))

(defn destroy []
  (println "Stopping...")
  (.shutdown @check-scheduler)
  (.awaitTermination @check-scheduler 3 TimeUnit/SECONDS)
  (.shutdown @check-executor)
  (.awaitTermination @check-executor 3 TimeUnit/SECONDS)
  (println "Stopped!"))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/status" [] (response (check-health @hc)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-defaults api-defaults)))

