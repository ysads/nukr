(ns nukr.server
  (:require [aleph.http :as http]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [nukr.routes :refer [routes-handler]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.http-response :refer [wrap-http-response]])
  (:gen-class))

(defn app
  "The main, highest-order handler of the application, which
  handles the request matching and add middlewares to the server"
  [storage]
  (-> (routes-handler storage)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-json-response)))

(defrecord HTTPServer [server port]
  
  component/Lifecycle

  (start [this]
    (log/info (str ";; HTTPServer: starting at port " port))
    (->> (http/start-server (app (:storage this)) {:port port})
         (assoc this :server)))

  (stop [this]
    (log/info ";; HTTPServer: stopping")
    (.close (:server this))
    (assoc this :server nil)))

(defn init-server
  "Initializes a new HTTP Server component"
  [port]
  (map->HTTPServer {:port port}))