(ns nukr.entities.suggestions-test
  (:require [clojure.set :as set]
            [clojure.test :refer :all]
            [nukr.entities.profile :as p]
            [nukr.entities.suggestions :refer :all]
            [nukr.entities.test-utils :as tu]
            [nukr.storage.in-memory :as db]))

(def storage (.start (db/init-storage)))

(let [profile-graph (tu/stub-profiles-graph! storage)
      root (first profile-graph)]
  
  (testing "suggestions/common-connections-count"
    (testing "returns the intersection between connections list"        
      (let [a root
            b (tu/get-profile-two-levels-away! storage root)
            intersection (set/intersection @(:connections a) @(:connections b))]
        (is (= (count intersection)
               (common-connections-count a b))))))

  (testing "visiting"
    (let [connections (into #{} (map :uuid (take 4 profile-graph)))
          visited     (into #{} (map :uuid (take 2 profile-graph)))]
      (testing "suggestion/not-visited"
        (testing "returns difference between current edges and visited edges"
          (is (= (set/difference connections visited)
                 (not-visited connections visited)))))

      (testing "suggestion/visited?"
        (testing "when visited includes UUID"
          (is (->> (first visited)
                   (visited? visited))))

        (testing "when visited does not includes UUID"
          (is (false? (->> (last profile-graph)
                           (:uuid)
                           (visited? visited))))))))

  (testing "suggestion/profile->suggestion"
    (testing "returns a map with the count of connections in common"
      (let [profile (last profile-graph)
            suggestion {:profile profile
                        :relevance (common-connections-count profile root)}]
        (is (= suggestion (profile->suggestion profile root))))))

  (testing "suggestion/find-suggestions"
    (let [max-suggest-num 5
          suggest-list (find-suggestions storage root max-suggest-num)]

      (testing "returns a coll with at most `max-suggest-num` items"
        (is (= max-suggest-num (count suggest-list))))

      (testing "doesnt include root profile"
        (is (every? #(not= (:uuid root) (:uuid %)) suggest-list)))
      
      (testing "returns only public profiles"
        (is (->> (map :profile suggest-list)
                 (every? (complement p/private?)))))

      (testing "returns only profiles not already connected"
        (is (->> (map :profile suggest-list)
                 (every? #(not (p/connected? root %))))))

      (testing "returns profiles sorted by connections in common"
        (is (->> (map :relevance suggest-list)
                 (apply >=)
                 (true?)))))))