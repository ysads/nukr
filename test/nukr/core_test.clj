(ns nukr.core-test
  (:require [clojure.test :refer :all]
            [nukr.core :refer :all]
            [nukr.system :as sys]))

(testing "core/-main"
  (testing "when port is passed as arg"
    (let [custom-port 4000]
      (-main custom-port)
      (is (= custom-port (:port (:server sys/system))))
      (sys/stop!)))

  (testing "when no arg is given"
    (-main)
    (is (= sys/default-port (:port (:server sys/system))))
    (sys/stop!)))