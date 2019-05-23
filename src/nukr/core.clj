(ns nukr.core
  (:require [nukr.system :as sys])
  (:gen-class))

(defn- parse-port
  "Returns the port to which the system will be bound,
  based on the argument sent to the application."
  [args]
  ;; If a port is given, use it. Otherwise, build the system
  ;; upon its default port
  (let [port (or (first args)
                 sys/default-port)]
    (if (string? port)
      (Integer/parseInt port)
      port)))

(defn -main
  [& args]
  (sys/init-system! (parse-port args))
  (sys/start!))