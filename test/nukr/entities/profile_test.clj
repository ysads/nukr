(ns nukr.entities.profile-test
  (:require [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]))

;; Auxiliary functions intended to help testing the
;; profile namespace
(defn with-uuid
  "Returns a copy of the given object with a UUID set"
  [data]
  (assoc data :uuid (.toString (java.util.UUID/randomUUID))))

(defn stub-profile
  "Returns a valid profile instance stubbed with data
  from the map passed as argument. Note this function
  returns a profile already with UUID"
  [data]
  (create-profile (with-uuid data)))

(def public-profile {:name "John Doe"
                     :email "john.doe@example.com"
                     :password "123456"
                     :gender "other"})

(def private-profile (assoc public-profile :private true))
(def another-profile (merge public-profile {:name "Alice Springs"
                                            :email "alice.springs@example.com"}))
(def connected-profile (assoc
                        another-profile
                        :connections
                        [(:uuid (with-uuid public-profile))]))

(testing "profile/private?"
  (testing "for private profiles"
    (is (private? private-profile)))

  (testing "for public profiles"
    (is (not (private? public-profile)))))

(testing "profile/dissoc-plain-password removes :password from profile"
  (let [dissoc-data (dissoc-plain-password public-profile)]
    (is (false? (contains? dissoc-data :password)))))

(testing "profile/with-hashed-password"
  (testing "throws exception if profile doesn't have passowrds"
    (let [broken-data {:name "Elizabeth McQueen"}]
      (is (thrown? java.lang.NoSuchFieldError
                   (with-hashed-password broken-data)))))
  
  (testing "hashes password and assoc it to :password-hash"
    (let [data-with-password (with-hashed-password public-profile)]
      (is (true? (contains? data-with-password :password-hash)))
      (is (false? (contains? data-with-password :password))))))

(testing "profile/with-connections"
  (testing "init a ref to an empty set if there is no connection"
    (let [profile (with-connections public-profile)]
      (is (instance? clojure.lang.Ref (:connections profile)))
      (is (empty? @(:connections profile)))))

  (testing "init a ref to a set with the available connections"
    (let [profile (with-connections connected-profile)]
      (is (instance? clojure.lang.Ref (:connections profile)))
      (is (false? (->> @(:connections profile)
                       (remove nil?)
                       (empty?)))))))

(testing "profile/with-privacy-settings"
  (testing "keeps :private field of profile if it exists"
    (let [pub-profile (assoc public-profile :private false)]
      (is (-> (with-privacy-settings pub-profile)
              (:private)
              (false?))
      (is (-> (with-privacy-settings private-profile)
              (:private)
              (true?))))))

  (testing "assoc false to :private field if it doesn't exist"
    (let [profile (with-privacy-settings public-profile)]
      (is (false? (:private profile))))))

(testing "profile/create-profile"
  (testing "creates instance of Profile with connections and hashed password"
    (let [profile (create-profile public-profile)]
      (is (instance? nukr.entities.profile.Profile profile))
      (is (instance? clojure.lang.Ref (:connections profile)))
      (is (false? (:private profile)))
      (is (false? (contains? profile :password))))))

(testing "profile/connected?"
  (testing "when profiles are connected"
    (let [a (stub-profile public-profile)
          b (stub-profile another-profile)]
      (connect! a b)
      (is (connected? a b))
      (is (connected? b a))))

  (testing "when profiles are not connected"
    (let [a (stub-profile public-profile)
          b (stub-profile another-profile)]
      (is (not (connected? a b)))
      (is (not (connected? b a))))))