(ns healthcheck.app
  (:require [healthcheck.check :as check]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent Executors ExecutorService ScheduledExecutorService TimeUnit]))

(def check-executor (atom nil))

(def check-scheduler (atom nil))

(def hc (atom nil))

(defn- health-checks [executor scheduler]
  [(check/schedule-check scheduler
                   "Temporary directory exists?"
                   (check/wrap-exception-check (check/timed-check executor (check/path-check "/tmp/healthcheck.txt"))))
   (check/schedule-check scheduler
                   "Example.com is available?"
                   (check/wrap-exception-check (check/timed-check executor (check/url-check "http://www.example.com"))))])

(defn startup []
  (log/info "Starting...")
  (swap! check-executor (fn [_] (Executors/newFixedThreadPool 10)))
  (swap! check-scheduler (fn [_] (Executors/newScheduledThreadPool 10)))
  (swap! hc (fn [_] (health-checks @check-executor @check-scheduler)))
  (log/info "Started!"))

(defn shutdown []
  (log/info "Stopping...")
  (.shutdown @check-scheduler)
  (.awaitTermination @check-scheduler 3 TimeUnit/SECONDS)
  (.shutdown @check-executor)
  (.awaitTermination @check-executor 3 TimeUnit/SECONDS)
  (log/info "Stopped!"))

(defn check-health []
  (check/check-health @hc))