(ns nukr.handlers.profile-handler-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]
            [nukr.handlers.profile-handler :refer :all]
            [nukr.handlers.test-utils :as tu]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok not-found created]]))

(def storage (.start (db/init-storage)))

(def success-req tu/success-create-req)

(def bad-emails    ["12345" "12345@com" "12345.com" 1234])
(def bad-genders   ["m" "f" "o" \m :m 1])
(def bad-passwords ["AbC1@" "Ab@D!E" "1A2B3C" "1@2!3#"])

(testing "::profile-create-request spec"
  (testing "fails if key is missing"
    (doseq [k [:name :email :password :gender]]
      (let [bad-req (update-in success-req [:params :profile] dissoc k)]
        (is (not (s/valid? :nukr.handlers.profile-handler/profile-create-request
                           bad-req))))))

  (testing "fails for bad email"
    (doseq [e bad-emails]
      (let [bad-req (assoc-in success-req [:params :profile :email] e)]
        (is (not (s/valid? :nukr.handlers.profile-handler/profile-create-request
                           bad-req))))))

  (testing "fails for invalid gender"
    (doseq [g bad-genders]
      (let [bad-req (assoc-in success-req [:params :profile :gender] g)]
        (is (not (s/valid? :nukr.handlers.profile-handler/profile-create-request
                           bad-req))))))

  (testing "fails for invalid password"
    (doseq [p bad-passwords]
      (let [bad-req (assoc-in success-req [:params :profile :password] p)]
        (is (not (s/valid? :nukr.handlers.profile-handler/profile-create-request
                           bad-req))))))

  (testing "success"
    (is (s/valid? :nukr.handlers.profile-handler/profile-create-request
                  success-req))))

(testing "create-profile-handler"
  (testing "when an attribute is missing"
    (doseq [k [:name :email :password :gender]]
      (let [bad-req  (update-in success-req [:params :profile] dissoc k)
            response (create-profile-handler storage bad-req)]
        (is (= 400 (:status response))))))

  (testing "when request method is not POST"
    (let [bad-req  (assoc success-req :request-method :get)
          response (create-profile-handler storage bad-req)]
      (is (= 405 (:status response)))))

  (testing "when request data is not valid"
    (doseq [e bad-emails
            g bad-genders
            p bad-passwords]
      (let [bad-req (-> success-req
                        (update-in [:params :profile] assoc :email e)
                        (update-in [:params :profile] assoc :gender g)
                        (update-in [:params :profile] assoc :password p))
            response (create-profile-handler storage bad-req)]
        (is (= 400 (:status response))))))

  (testing "when request data is valid"
    (let [response (create-profile-handler storage success-req)]
      (is (= 201 (:status response)))
      (is (some? (:uuid (:body response)))))))

(testing "opt-profile-handler"
  (testing "returns 404 if profile not found"
    (let [request  (tu/stub-request {:uuid "1234" :private true})
          response (opt-profile-handler storage request)]
      (is (= 404 (:status response)))))

  (testing "when transition is public->private"
    (let [uuid     (tu/stub-profile-uuid storage success-req)
          request  (tu/stub-request {:uuid uuid :private true})
          response (opt-profile-handler storage request)
          profile  (:profile (:body response))]
      (is (= 200 (:status response)))
      (is (private? profile))
      (is (not (instance? clojure.lang.Ref (:connections profile))))))

  (testing "when transition is private->public"
    (let [uuid     (tu/stub-profile-uuid storage success-req)
          request  (tu/stub-request {:uuid uuid :private false})
          response (opt-profile-handler storage request)
          profile  (:profile (:body response))]
      (is (= 200 (:status response)))
      (is (not (private? profile)))
      (is (not (instance? clojure.lang.Ref (:connections profile)))))))

(testing "connect-profiles-handler"
  (testing "returns 404 if either of profiles not found"
    (let [request (tu/stub-request {:uuid-a "1234" :uuid-b "4321"})]
      (is (= 404 (:status (connect-profiles-handler storage request))))))

  (testing "when profiles are not connected"
    (let [uuid-a  (tu/stub-profile-uuid storage success-req)
          uuid-b  (tu/stub-profile-uuid storage success-req)
          request (tu/stub-request {:uuid-a uuid-a :uuid-b uuid-b})]
      (is (not (connected? (db/get-by-uuid! storage uuid-a)
                           (db/get-by-uuid! storage uuid-b))))
      (is (= 200 (:status (connect-profiles-handler storage request))))
      (is (connected? (db/get-by-uuid! storage uuid-a)
                      (db/get-by-uuid! storage uuid-b))))))
