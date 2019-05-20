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

(def success-create-req {:request-method :post
                         :params {:profile {:name "John Doe"
                                            :email "john.doe@example.com"
                                            :password "123456"
                                            :gender "other"}}})

(defn stub-request
  "Stub a request map with route-params key set"
  [params]
  {:params params})

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
    (->> (stub-request {:uuid-a uuid-a :uuid-b uuid-b})
         (connect-profiles-handler storage))
    (->> (stub-request {:uuid-a uuid-b :uuid-b uuid-c})
         (connect-profiles-handler storage))
    (->> (stub-request {:uuid-a uuid-b :uuid-b uuid-d})
         (connect-profiles-handler storage))
    uuid-a))

(defn suggestion?
  "Returns true if the given map contains :relevance
  and :profile entries"
  [data]
  (and (contains? data :relevance)
       (contains? data :profile)))

(testing "create-profile-handler"
  (testing "when password is missing"
    (let [bad-req  (update-in success-create-req [:params :profile] dissoc :password)
          response (create-profile-handler storage bad-req)]
      (is (= 400 (:status response)))))

  (testing "when request method is not POST"
    (let [bad-req  (assoc success-create-req :request-method :get)
          response (create-profile-handler storage bad-req)]
      (is (= 405 (:status response)))))

  (testing "when request data is valid"
    (let [response (create-profile-handler storage success-create-req)]
      (is (= 201 (:status response)))
      (is (some? (:uuid (:body response)))))))

(testing "opt-profile-handler"
  (testing "returns 404 if profile not found"
    (let [request  (stub-request {:uuid "1234" :private true})
          response (opt-profile-handler storage request)]
      (is (= 404 (:status response)))))

  (testing "when transition is public->private"
    (let [uuid     (stub-profile-uuid success-create-req)
          request  (stub-request {:uuid uuid :private true})
          response (opt-profile-handler storage request)
          profile  (:profile (:body response))]
      (is (= 200 (:status response)))
      (is (private? profile))
      (is (not (instance? clojure.lang.Ref (:connections profile))))))

  (testing "when transition is private->public"
    (let [uuid     (stub-profile-uuid success-create-req)
          request  (stub-request {:uuid uuid :private false})
          response (opt-profile-handler storage request)
          profile  (:profile (:body response))]
      (is (= 200 (:status response)))
      (is (not (private? profile)))
      (is (not (instance? clojure.lang.Ref (:connections profile)))))))

(testing "connect-profiles-handler"
  (testing "returns 404 if either of profiles not found"
    (let [request (stub-request {:uuid-a "1234" :uuid-b "4321"})]
      (is (= 404 (:status (connect-profiles-handler storage request))))))

  (testing "when profiles are not connected"
    (let [uuid-a  (stub-profile-uuid success-create-req)
          uuid-b  (stub-profile-uuid success-create-req)
          request (stub-request {:uuid-a uuid-a :uuid-b uuid-b})]
      (is (not (connected? (db/get-by-uuid! storage uuid-a)
                           (db/get-by-uuid! storage uuid-b))))
      (is (= 200 (:status (connect-profiles-handler storage request))))
      (is (connected? (db/get-by-uuid! storage uuid-a)
                      (db/get-by-uuid! storage uuid-b))))))

(testing "suggestions-handler"
  (testing "a default suggestions number exists"
    (is (= 5 default-suggest-num)))

  (testing "returns 404 if profile not found"
    (let [request (stub-request {:uuid "1234"})]
      (is (= 404 (:status (suggestions-handler storage request))))))

  (testing "returns an array with at most n items when requested"
    (let [uuid         (stub-connected-profile-uuid! storage)
          request      (stub-request {:uuid uuid :count 1})
          response     (suggestions-handler storage request)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 1 (count suggest-list)))
      (is (every? suggestion? suggest-list))))

  (testing "returns an array of suggestions of available suggestions"
    (let [uuid         (stub-connected-profile-uuid! storage)
          request      (stub-request {:uuid uuid})
          response     (suggestions-handler storage request)
          suggest-list (:suggestions (:body response))]
      (is (= 200 (:status response)))
      (is (= 2 (count suggest-list)))
      (is (every? suggestion? suggest-list)))))
