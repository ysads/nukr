(ns nukr.system-test
  (:require [clojure.test :refer :all]
            [nukr.system :refer :all]))

(defn- redef-system!
  "Redefines a system already built"
  []
  (alter-var-root #'system (constantly nil)))

(testing "system/system"
  (is (nil? system)))

(testing "system/build-system"
  (testing "builds system into given port"
    (let [port 12000
          sys (build-system port)]
      (is (= port (:port (:server sys))))
      (is (true? (contains? sys :server)))
      (is (true? (contains? sys :storage))))))

(testing "system/init-system"
  (testing "inits upon the default port if nil"
    (redef-system!)
    (is (nil? system))
    (init-system! nil)
    (is (= default-port (:port (:server system))))
    (is (true? (contains? system :server)))
    (is (true? (contains? system :storage))))

  (testing "inits system into given port if present"
    (let [custom-port 12000]
      (redef-system!)
      (is (nil? system))
      (init-system! custom-port)
      (is (= custom-port (:port (:server system))))
      (is (true? (contains? system :server)))
      (is (true? (contains? system :storage))))))

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