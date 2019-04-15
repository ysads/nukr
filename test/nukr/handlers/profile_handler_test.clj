(ns nukr.handlers.profile-handler-test
  (:use [clojure.pprint])
  (:require [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]
            [nukr.handlers.profile-handler :refer :all]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok not-found created]]))

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
  (testing "returns 404 if any profile not found"
    (is (= 404 (:status (connect-profiles-handler storage "1234" "4321")))))

  (testing "when profiles are not connected"
    (let [uuid-a (stub-profile-uuid success-create-req)
          uuid-b (stub-profile-uuid success-create-req)]
      (is (not (connected? (db/get-by-uuid! storage uuid-a)
                           (db/get-by-uuid! storage uuid-b))))
      (is (= 200 (:status (connect-profiles-handler storage uuid-a uuid-b))))
      (is (connected? (db/get-by-uuid! storage uuid-a)
                      (db/get-by-uuid! storage uuid-b))))))