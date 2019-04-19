(ns nukr.handlers.profile-handler-test
  (:use [clojure.pprint])
  (:require [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]
            [nukr.handlers.profile-handler :refer :all]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok not-found created]]))


;;;; Auxiliary definitions

;;; This section contains a series of forms and functions
;;; meant to make testing easier and faster. They help
;;; building, structuring and connecting data, mocking
;;; a more real use scenario.
(def storage (.start (db/init-storage)))

(def success-create-req {:name "John Doe"
                         :email "john.doe@example.com"
                         :password "123456"
                         :gender "other"})

(defn stub-profile-uuid
  "Stub a new profile using create-profile-handler"
  [data]
  (-> (create-profile-handler storage data)
      (:body)
      (:uuid)))

(defn stub-connected-profile-uuid!
  "Stub a graph representing a more realistic profile
  social network, including connections between people"
  [storage]
  (let [uuid-a (stub-profile-uuid success-create-req)
        uuid-b (stub-profile-uuid success-create-req)
        uuid-c (stub-profile-uuid success-create-req)
        uuid-d (stub-profile-uuid success-create-req)]
    (connect-profiles-handler storage uuid-a uuid-b)
    (connect-profiles-handler storage uuid-b uuid-c)
    (connect-profiles-handler storage uuid-b uuid-d)
    uuid-a))

(defn suggestion?
  "Returns if the given map is structurally a suggestion"
  [data]
  (and (contains? data :relevance)
       (contains? data :profile)))

(testing "create-profile-handler"
  (testing "when request data is valid"
    (let [response (create-profile-handler storage success-create-req)]
      (is (= 201 (:status response)))
      (is (some? (:uuid (:body response)))))))

(testing "privacy-changing-handler"
  (testing "returns 404 if profile not found"
    (let [response (opt-profile-privacy-handler storage "1234" true)]
      (is (= 404 (:status response)))))

  (testing "when transition is public->private"
    (let [uuid (stub-profile-uuid success-create-req)]
      (is (= 200 (:status (opt-profile-privacy-handler storage uuid true))))
      (is (->> (db/get-by-uuid! storage uuid)
               (private?)))))

  (testing "when transition is private->public"
    (let [uuid (stub-profile-uuid success-create-req)]
      (is (= 200 (:status (opt-profile-privacy-handler storage uuid false))))
      (is (not (->> (db/get-by-uuid! storage uuid)
                    (private?)))))))

(testing "connect-profiles-handler"
  (testing "returns 404 if either of profiles not found"
    (is (= 404 (:status (connect-profiles-handler storage "1234" "4321")))))

  (testing "when profiles are not connected"
    (let [uuid-a (stub-profile-uuid success-create-req)
          uuid-b (stub-profile-uuid success-create-req)]
      (is (not (connected? (db/get-by-uuid! storage uuid-a)
                           (db/get-by-uuid! storage uuid-b))))
      (is (= 200 (:status (connect-profiles-handler storage uuid-a uuid-b))))
      (is (connected? (db/get-by-uuid! storage uuid-a)
                      (db/get-by-uuid! storage uuid-b))))))

(testing "suggestions-handler"
  (testing "a default suggestions number exists"
    (is (= 5 default-suggest-num)))

  (testing "returns 404 if profile not found"
    (is (= 404 (:status (suggestions-handler storage "1234")))))

  (testing "returns an array with at most n items when requested"
    (let [uuid (stub-connected-profile-uuid! storage)
          response (suggestions-handler storage uuid 1)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 1 (count suggest-list)))
      (is (every? suggestion? suggest-list))))

  (testing "returns an array of suggestions of available suggestions"
    (let [uuid (stub-connected-profile-uuid! storage)
          response (suggestions-handler storage uuid)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 2 (count suggest-list)))
      (is (every? suggestion? suggest-list)))))
