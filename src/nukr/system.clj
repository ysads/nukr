(ns nukr.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [nukr.storage.in-memory :as storage]
            [nukr.server :as server])
  (:gen-class))


(def ^:redef system
  "The var which holds the system component"
  nil)

(defn build-system
  "Builds the system, initializing the components and defining
  the relations between them"
  []
  (try
    (-> (component/system-map :server (server/init-server 4000)
                              :storage (storage/init-storage))
        (component/system-using {:server [:storage]}))
    (catch Exception ex
      (log/error "Failed to build system"))))


(defn init-system!
  "Associates the mounted main system component to the
  system var"
  []
  (let [sys (build-system)]
    (alter-var-root #'system (constantly sys))))

(defn stop!
  "Stops system the main system component"
  []
  (alter-var-root #'system component/stop-system)
  (log/info ";; System: stopped"))

(defn start!
  "Starts the main system component"
  []
  (alter-var-root #'system component/start-system)
  (log/info ";; System: started"))