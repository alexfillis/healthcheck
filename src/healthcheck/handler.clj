(ns healthcheck.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [healthcheck.app :as app]))

(defn init []
  (app/startup))

(defn destroy []
  (app/shutdown))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/status" [] (response (app/check-health)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-defaults api-defaults)))

