(ns nukr.server-test
  (:require [clojure.test :refer :all]
            [nukr.server :refer :all]
            [nukr.storage.in-memory :as db])
  (:gen-class))

(def port 4000)

(def storage (.start (db/init-storage)))

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
      (is (= port (-> (:server started-server)
                      (.getURI)
                      (.getPort)))))

    (testing ".stop"
      (let [stopped-server (.stop started-server)]
        (is (nil? (:server stopped-server)))))))