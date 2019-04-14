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

(def saved-profile (assoc
                    public-profile
                    :uuid
                    (.toString (java.util.UUID/randomUUID))))

(def connected-profile (assoc
                        another-profile
                        :connections
                        [(:uuid saved-profile)]))

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
  
(testing "connections-initializing"
  (deftest init-empty-connections
    (let [profile (with-connections public-profile)]
      (is (instance? clojure.lang.Ref (:connections profile)))
      (is (empty? @(:connections profile)))))

  (deftest init-existing-collections
    (let [profile (with-connections connected-profile)]
      (is (instance? clojure.lang.Ref (:connections profile)))
      (is (some? @(:connections profile))))))

(deftest profiles-creation
  (let [profile (create-profile public-profile)]
    (is (instance? nukr.entities.profile.Profile profile))
    (is (instance? clojure.lang.Ref (:connections profile)))
    (is (false? (contains? profile :password)))))
