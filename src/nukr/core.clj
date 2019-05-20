(ns nukr.core
  (:require [nukr.system :as sys])
  (:gen-class))

(defn -main [& args]
  (sys/init-system!)
  (sys/start!))