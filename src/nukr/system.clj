(ns nukr.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [nukr.storage.in-memory :as storage]
            [nukr.server :as server])
  (:gen-class))

;;; The default port to which the system should be bound if
;;; none is given as parameter
(def default-port 4000)

(def ^:redef system
  "The var which holds the system component"
  nil)

(defn build-system
  "Builds the system, initializing the components and defining
  the relations between them"
  [port]
  (try
    (-> (component/system-map :server (server/init-server port)
                              :storage (storage/init-storage))
        (component/system-using {:server [:storage]}))
    (catch Exception ex
      (log/error "Failed to build system"))))

(defn init-system!
  "Associates the mounted main system component to the
  system var"
  [port]
  (let [final-port (or port default-port)
        sys (build-system final-port)]
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