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

(deftest profile-creation-handling
  (let [response (create-profile-handler storage success-create-req)]
    (is (= 201 (:status response)))
    (is (some? (:uuid (:body response))))))

(testing "profile-privacy-opting"
  (deftest opting-profile-not-found
    (let [response (opt-profile-privacy-handler storage "1234" true)]
      (is (= 404 (:status response)))))

  (deftest opt-out-profile
    (let [response (create-profile-handler storage success-create-req)
          uuid (:uuid (:body response))]
      (is (= 200 (:status (opt-profile-privacy-handler storage uuid true))))
      (is (->> (db/get-by-uuid! storage uuid)
               (private?)))))

  (deftest opt-in-profile
    (let [response (create-profile-handler storage success-create-req)
          uuid (:uuid (:body response))]
      (is (= 200 (:status (opt-profile-privacy-handler storage uuid false))))
      (is (not (->> (db/get-by-uuid! storage uuid)
                    (private?)))))))