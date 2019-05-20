(ns nukr.system-test
  (:require [clojure.test :refer :all]
            [nukr.system :refer :all]))

(testing "system/build-system"
  (let [sys (build-system)]
    (is (true? (contains? sys :server)))
    (is (true? (contains? sys :storage)))))

(testing "system/system"
  (is (nil? system)))

(testing "system/init-system"
  (is (nil? system))
  (init-system!)
  (is (true? (contains? system :server)))
  (is (true? (contains? system :storage))))

(testing "system/start!"
  (testing "starts the main component"
    (start!)
    (is (some? (:data (:storage system))))
    (is (some? (:server (:server system))))))

(testing "system/stop!"
  (testing "gently stops the main component"
    (stop!)
    (is (nil? (:data (:storage system))))
    (is (nil? (:server (:server system))))))