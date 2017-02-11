(ns healthcheck.check)

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
    (success)))

(defn- check [f]
  (try
    (f)
    (catch Exception e
      (failure e))))

(defn health-check [key f]
  (fn []
    (assoc (check f) :what key)))

(defn check-health [checks]
  (reduce (fn [coll f] (conj coll (f))) [] checks))
