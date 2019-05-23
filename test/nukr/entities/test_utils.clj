(ns nukr.entities.test-utils
  (:require [nukr.entities.profile :as p]
            [nukr.storage.in-memory :as db]))

(def public-profile {:name "John Doe"
                     :email "john.doe@example.com"
                     :password "NukR123@!#"
                     :gender "other"})

(def private-profile (assoc public-profile :private true))

(defn with-uuid
  "Returns a copy of the given object with a UUID set."
  [data]
  (assoc data :uuid (.toString (java.util.UUID/randomUUID))))

(defn stub-profile
  "Returns a valid profile instance stubbed with data
  from the map passed as argument. Note this function
  returns a profile already with UUID."
  [data]
  (p/create-profile (with-uuid data)))

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

    (p/connect! (nth public-profiles 0) (nth public-profiles 1))
    (p/connect! (nth public-profiles 0) (nth public-profiles 2))
    (p/connect! (nth public-profiles 0) (nth public-profiles 7))
    (p/connect! (nth public-profiles 2) (nth public-profiles 4))
    (p/connect! (nth public-profiles 2) (nth public-profiles 6))
    (p/connect! (nth public-profiles 4) (nth public-profiles 7))
    (p/connect! (nth public-profiles 4) (nth public-profiles 3))
    (p/connect! (nth public-profiles 5) (nth public-profiles 6))
    (p/connect! (nth public-profiles 5) (nth public-profiles 1))
    (p/connect! (nth public-profiles 5) (nth public-profiles 8))
    (p/connect! (nth public-profiles 8) (nth public-profiles 9))
    (p/connect! (nth public-profiles 9) (nth public-profiles 1))

    (p/connect! (nth private-profiles 0) (nth public-profiles 1))
    (p/connect! (nth private-profiles 0) (nth public-profiles 9))
    (p/connect! (nth private-profiles 0) (nth public-profiles 3))
    (p/connect! (nth private-profiles 1) (nth public-profiles 5))
    (p/connect! (nth private-profiles 1) (nth public-profiles 7))
    (p/connect! (nth private-profiles 1) (nth private-profiles 2))
    (p/connect! (nth private-profiles 3) (nth public-profiles 4))
    (p/connect! (nth private-profiles 3) (nth public-profiles 6))

    (->> (into public-profiles private-profiles)
         (map #(db/insert! storage %))
         (doall))))

(defn get-profile-two-levels-away!
  "Return a profile which has at least one connection in common with
  the given profile. To achieve this, two consecutive 'hops' must be
  done from the current profile."
  [storage profile]
  (->> @(:connections profile)
       (first)
       (db/get-by-uuid! storage)
       (:connections)
       (deref)
       (first)
       (db/get-by-uuid! storage)))
