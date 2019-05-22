(ns nukr.handlers.suggestions-handler-test
  (:require [clojure.test :refer :all]
          [nukr.entities.profile :as p]
          [nukr.handlers.suggestions-handler :refer :all]
          [nukr.handlers.test-utils :as tu]
          [nukr.storage.in-memory :as db]))

(def storage (.start (db/init-storage)))

(defn- suggestion?
  "Returns true if the given map contains :relevance
  and :profile entries"
  [data]
  (and (contains? data :relevance)
       (contains? data :profile)))

(testing "suggestion-handler/find-suggestions-handler"
  (testing "a default suggestions number exists"
    (is (= 5 default-suggest-num)))

  (testing "returns 404 if profile not found"
    (let [request (tu/stub-request {:uuid "1234"})]
      (is (= 404 (:status (find-suggestions-handler storage request))))))

  (testing "returns an array with at most n items when requested"
    (let [uuid         (tu/stub-connected-profile-uuid! storage)
          request      (tu/stub-request {:uuid uuid :count 1})
          response     (find-suggestions-handler storage request)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 1 (count suggest-list)))
      (is (every? suggestion? suggest-list))))

  (testing "returns an array of suggestions of available suggestions"
    (let [uuid         (tu/stub-connected-profile-uuid! storage)
          request      (tu/stub-request {:uuid uuid})
          response     (find-suggestions-handler storage request)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 2 (count suggest-list)))
      (is (every? suggestion? suggest-list)))))