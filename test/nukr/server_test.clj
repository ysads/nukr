(ns nukr.server-test
  (:use [clojure.pprint])
  (:require [clojure.test :refer :all]
            [nukr.server :refer :all]
            [nukr.storage.in-memory :as db])
  (:gen-class))


(def storage (.start (db/init-storage)))

(def port 4000)

(testing "server/init-server"
  (let [http-server (init-server port)]
    (is (instance? nukr.server.HTTPServer http-server))))

(testing "HTTPServer"
  (let [http-server (init-server port)
        server-with-storage (assoc http-server :storage storage)
        started-server (.start server-with-storage)]
    
    (testing ".start"
      (is (some? (:server started-server)))
      (is (some? (:storage started-server)))
      (is (= port (.port (:server started-server)))))

    (testing ".stop"
      (let [stopped-server (.stop started-server)]
        (is (nil? (:server stopped-server)))))))