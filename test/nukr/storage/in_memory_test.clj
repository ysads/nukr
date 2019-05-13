(ns nukr.storage.in-memory-test
  (:use [clojure.pprint])
  (:require [clojure.test :refer :all]
            [nukr.storage.in-memory :as db]))

(def storage (.start (db/init-storage)))

(def user-data {:name "John Doe" :age 25})

(deftest initialization
  (is (instance?
       nukr.storage.in_memory.InMemoryStorage
       storage)))

(testing "uuid-assigning"
  (deftest when-data-has-uuid
    (let [uuid (.toString (java.util.UUID/randomUUID))]
      (is (->> {:uuid uuid}
               (db/with-uuid)
               (:uuid)
               (= uuid)))))

  (deftest when-data-has-no-uuid
    (is (nil? (:uuid user-data)))
    (is (-> (db/with-uuid user-data)
            (contains? :uuid)))))
    
 (deftest data-insertion
   (let [record (db/insert! storage user-data)]
     (is (true? (contains? record :uuid)))
     (is (->> (:uuid record)
              (keyword)
              (get @(:data storage))
              (= record)))))
 
 (testing "data-retrieving"
   (deftest failed-data-retrieving-uuid-not-found
     (let [uuid (.toString (java.util.UUID/randomUUID))]
       (is (thrown? java.util.NoSuchElementException
                    (db/get-by-uuid! storage uuid)))))

   (deftest successful-data-retrieving
     (let [record (db/insert! storage user-data)]
       (is (->> (:uuid record)
                (db/get-by-uuid! storage)
                (= record))))))

(testing "data-updating"
  (deftest failed-updating-uuid-not-found
    (let [uuid (.toString (java.util.UUID/randomUUID))
          rand-data {:uuid "1234" :age 50}]
       (is (thrown? java.util.NoSuchElementException
                    (db/update-by-uuid! storage rand-data)))))

  (deftest successful-data-update
    (let [record (db/insert! storage user-data)
          record-updated (merge record {:age 39})]
      (is (->> (:uuid record)
               (db/get-by-uuid! storage)
               (= record)))
      (db/update-by-uuid! storage record-updated)
      (is (->> (:uuid record)
               (db/get-by-uuid! storage)
               (= record-updated))))))

(testing "InMemoryStorage"
  (let [component (db/init-storage)]
    (testing ".start"
      (let [started-component (.start component)]
        (is (instance? clojure.lang.Atom (:data started-component)))
        (is (empty? @(:data started-component)))))

    (testing ".stop"
      (let [stopped-component (.stop (.start component))]
        (is (nil? (:data stopped-component)))))))
  

