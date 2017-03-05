(ns healthcheck.check
  (:require [clj-http.client :as http])
  (:import [java.util.concurrent ExecutorService Future ScheduledExecutorService TimeUnit]))

(defn- init []
  {:result :initialising})

(defn- success []
  {:result :ok})

(defn- failure [reason]
  {:result :failed
   :reason reason})

(defn path-check [path]
  (fn []
    (let [file (java.io.File. path)]
      (if (.exists file)
        (success)
        (failure (format "%s does not exist!" file))))))

(defn url-check [url]
  (fn []
    (let [response (http/get url)
          status (:status response)]
      (if (= status 200)
        (success)
        (failure (format "%s responded with %d" url status))))))

(defn wrap-exception-check [check]
  (fn []
    (try
      (check)
      (catch Exception e
        (failure (.getMessage e))))))

(defn- status [result key]
  (assoc result :what key))

(defn timed-check [executor check]
  (fn []
    (let [task-future (.submit executor ^Callable check)]
      (try
        (.get task-future 30 TimeUnit/SECONDS)
        (catch Exception e
          (.cancel task-future true)
          (throw e))))))

(defn schedule-check [scheduler key check]
  (let [current-status (atom (status (init) key))
        refresh (fn []
                  (printf "Refreshing %s...%n" key)
                  (flush)
                  (swap! current-status (fn [_] (status (check) key)))
                  (printf "%s Refreshed!%n" key)
                  (flush))]
    (.scheduleAtFixedRate scheduler refresh 30 30 TimeUnit/SECONDS)
    (fn []
      @current-status)))

(defn check-health [checks]
  (reduce (fn [coll f] (conj coll (f))) [] checks))
