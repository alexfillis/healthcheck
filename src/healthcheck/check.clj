(ns healthcheck.check
  (:require [clj-http.client :as http]))

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

(defn- check [f]
  (try
    (f)
    (catch Exception e
      (failure (.getMessage e)))))

(defn health-check [key f]
  (fn []
    (assoc (check f) :what key)))

(defn check-health [checks]
  (reduce (fn [coll f] (conj coll (f))) [] checks))
