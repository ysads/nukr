(ns nukr.entities.profile-test
  (:use [clojure.pprint])
  (:require [clojure.set :as set]
            [clojure.test :refer :all]
            [nukr.entities.profile :refer :all]
            [nukr.storage.in-memory :as db]))


;;;; Auxiliary definitions

;;; This section contains a series of forms and functions
;;; meant to make testing easier and faster. They help
;;; building, structuring and connecting data, mocking
;;; a more real use scenario.
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

(def storage (.start (db/init-storage)))

(defn stub-coll
  "Returns a coll with n items using the given data"
  [n data]
  (->> (fn [] (stub-profile data))
       (repeatedly n)))

(defn stub-profiles-graph!
  "Stub a graph representing a more realistic profile
  social network, including connections between people"
  [storage]
  (let [public-profiles [(stub-profile (assoc public-profile :name "  0  "))
                         (stub-profile (assoc public-profile :name "  1  "))
                         (stub-profile (assoc public-profile :name "  2  "))
                         (stub-profile (assoc public-profile :name "  3  "))
                         (stub-profile (assoc public-profile :name "  4  "))
                         (stub-profile (assoc public-profile :name "  5  "))
                         (stub-profile (assoc public-profile :name "  6  "))
                         (stub-profile (assoc public-profile :name "  7  "))
                         (stub-profile (assoc public-profile :name "  8  "))
                         (stub-profile (assoc public-profile :name "  9  "))]

        private-profiles [(stub-profile (assoc private-profile :name "  10  "))
                          (stub-profile (assoc private-profile :name "  11  "))
                          (stub-profile (assoc private-profile :name "  12  "))
                          (stub-profile (assoc private-profile :name "  13  "))]]

    (connect! (nth public-profiles 0) (nth public-profiles 1))
    (connect! (nth public-profiles 0) (nth public-profiles 2))
    (connect! (nth public-profiles 0) (nth public-profiles 7))
    (connect! (nth public-profiles 2) (nth public-profiles 4))
    (connect! (nth public-profiles 2) (nth public-profiles 6))
    (connect! (nth public-profiles 4) (nth public-profiles 7))
    (connect! (nth public-profiles 4) (nth public-profiles 3))
    (connect! (nth public-profiles 5) (nth public-profiles 6))
    (connect! (nth public-profiles 5) (nth public-profiles 1))
    (connect! (nth public-profiles 5) (nth public-profiles 8))
    (connect! (nth public-profiles 8) (nth public-profiles 9))
    (connect! (nth public-profiles 9) (nth public-profiles 1))

    (connect! (nth private-profiles 0) (nth public-profiles 1))
    (connect! (nth private-profiles 0) (nth public-profiles 9))
    (connect! (nth private-profiles 0) (nth public-profiles 3))
    (connect! (nth private-profiles 1) (nth public-profiles 5))
    (connect! (nth private-profiles 1) (nth public-profiles 7))
    (connect! (nth private-profiles 1) (nth private-profiles 2))
    (connect! (nth private-profiles 3) (nth public-profiles 4))
    (connect! (nth private-profiles 3) (nth public-profiles 6))

    (->> (into public-profiles private-profiles)
         (map #(db/insert! storage %))
         (doall))))

(defn get-profile-two-steps-away!
  "Return a profile which has at least one connection in common with
  the given profile. To achieve this, two consecutive 'hops' must be
  done from the current profile"
  [storage profile]
  (->> @(:connections profile)
       (first)
       (db/get-by-uuid! storage)
       (:connections)
       (deref)
       (first)
       (db/get-by-uuid! storage)))

(testing "profile/private?"
  (testing "for private profiles"
    (is (private? private-profile)))

  (testing "for public profiles"
    (is (not (private? public-profile)))))

(testing "profile/dissoc-plain-password"
  (testing "removes :password from profile"
    (let [dissoc-data (dissoc-plain-password public-profile)]
      (is (false? (contains? dissoc-data :password))))))

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

(testing "suggestions handling"
  (let [profile-graph (stub-profiles-graph! storage)
        root (first profile-graph)]

    (testing "profile/common-connections-count"
      (testing "returns the intersection between connections list"        
        (let [a root
              b (get-profile-two-steps-away! storage root)
              intersection (set/intersection @(:connections a) @(:connections b))]
          (is (= (count intersection)
                 (common-connections-count a b))))))

    (testing "visiting"
      (let [connections (into #{} (map :uuid (take 4 profile-graph)))
            visited     (into #{} (map :uuid (take 2 profile-graph)))]
        (testing "profile/not-visited"
          (testing "returns difference between current edges and visited edges"
            (is (= (set/difference connections visited)
                   (not-visited connections visited)))))

        (testing "profile/visited?"
          (testing "when visited includes uuid"
            (is (->> (first visited)
                     (visited? visited))))

          (testing "when visited does not includes uuid"
            (is (false? (->> (last profile-graph)
                             (:uuid)
                             (visited? visited))))))))

    (testing "profile/profile->suggestion"
      (testing "returns a map with the count of connections in common"
        (let [profile (last profile-graph)
              suggestion {:profile profile
                          :relevance (common-connections-count profile root)}]
             (is (= suggestion (profile->suggestion profile root))))))

    (testing "profile/suggestions"
      (let [max-suggest-num 5
            suggest-list (suggestions storage root max-suggest-num)]

        (testing "returns a coll with at most `max-suggest-num` items"
          (is (= max-suggest-num (count suggest-list))))
        
        (testing "returns only public profiles"
          (is (->> (map :profile suggest-list)
                   (every? (complement private?)))))

        (testing "returns only profiles not already connected"
          (is (->> (map :profile suggest-list)
                   (every? #(not (connected? root %))))))

        (testing "returns profiles sorted by connections in common"
          (is (->> (map :relevance suggest-list)
                   (apply >=)
                   (true?))))))))
