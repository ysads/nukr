(ns nukr.entities.profile-test
  (:use [clojure.pprint])
  (:require [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]))


(def public-profile {:name "John Doe"
                     :email "john.doe@example.com"
                     :password "123456"
                     :gender "other"
                     :private false})

(def another-profile (assoc public-profile :name "Alice Springs"))
(def private-profile (assoc public-profile :private true))

(testing "profile-privacy"
  (deftest private-profile-privacy
    (is (private? private-profile)))

  (deftest public-profile-privacy
    (is (not (private? public-profile)))))

(testing "password-hashing"
  (deftest plain-password-dissoc
    (let [dissoc-data (dissoc-plain-password public-profile)]
      (is (false? (contains? dissoc-data :password)))))

  (deftest when-no-password-exists
    (let [broken-data {:name "Elizabeth McQueen"}]
      (is (thrown? java.lang.NoSuchFieldError
                   (with-hashed-password broken-data)))))

  (deftest when-password-exists
    (let [data-with-password (with-hashed-password public-profile)]
      (is (true? (contains? data-with-password :password-hash)))
      (is (false? (contains? data-with-password :password))))))
  
(deftest profiles-creation
  (let [profile (create-profile public-profile)]
    (is (instance? nukr.entities.profile.Profile profile))
    (is (empty? (:connections profile)))
    (is (false? (contains? profile :password)))))