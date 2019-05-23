(ns nukr.core-test
  (:require [clojure.test :refer :all]
            [nukr.core :refer :all]
            [nukr.system :as sys]))

(testing "core/-main"
  (testing "when integer port is passed as arg"
    (let [custom-port 12000]
      (-main custom-port)
      (is (= 12000 (:port (:server sys/system))))
      (sys/stop!)))

  (testing "when string port is passed as arg"
    (let [custom-port "12000"]
      (-main custom-port)
      (is (= 12000 (:port (:server sys/system))))
      (sys/stop!)))

  (testing "when no arg is given"
    (-main)
    (is (= sys/default-port (:port (:server sys/system))))
    (sys/stop!)))