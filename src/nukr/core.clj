(ns nukr.core
  (:require [nukr.system :as sys])
  (:gen-class))

(defn -main
  [& args]
  ;; If a port is given, use it. Otherwise, build the system
  ;; upon its default port
  (let [port (or (first args)
                 sys/default-port)]
    (sys/init-system! port)
    (sys/start!)))