(ns nukr.entities.profile
  (:require [buddy.hashers :as hashers]
            [clojure.set :refer [intersection difference]]
            [nukr.storage.in-memory :as db])
  (:import clojure.lang.PersistentQueue)
  (:gen-class))

(defrecord Profile [uuid name email password-hash gender private connections])

(defn private?
  "Returns true if the passed profile is private,
  that is, if it has opted-out of being suggested
  to other profiles"
  [profile]
  (true? (:private profile)))

(defn dissoc-plain-password
  "Returns a profile map with plain password dissociated"
  [profile]
  (dissoc profile :password))

(defn with-hashed-password
  "Returns a profile with a safe, hashed version
  of the password instead of the plain one"
  [profile]
  (when-not (:password profile)
    (throw (java.lang.NoSuchFieldError.
           "Can't hash profile without password")))
  (->> (hashers/derive (:password profile))
       (assoc profile :password-hash)
       (dissoc-plain-password)))

(defn with-connections
  "Returns a modified object where the connections field
  is an instance of clojure.lang.Ref, which allows them
  to be manipulated in a thread-safe way"
  [profile]
  (if (or (empty? (:connections profile))
          (nil? (:connections profile)))
    (assoc profile :connections (ref #{}))
    (assoc profile :connections (ref (into #{} (:connections profile))))))

(defn with-privacy-settings
  "Returns a modified object in which :private field is
  set to false if it's not present in argument received"
  [profile]
  (if (some? (:private profile))
    profile
    (assoc profile :private false)))

(defn create-profile
  "Returns an instance of Profile record based on
  data passed as argument"
  [profile-data]
  (-> profile-data
      (with-hashed-password)
      (with-connections)
      (with-privacy-settings)
      (map->Profile)))

(defn connected?
  "Returns true if profile-a is connected to profile-b.
  Note the connection relation is undirectional, that is,
  if profile-a is connected to profile-b then profile-b is
  connected to profile-a"
  [profile-a profile-b]
  (-> (partial = (:uuid profile-b))
      (some @(:connections profile-a))))

(defn connect!
  "Alters profile-a's connection list to include profile-b's
  UUID, and vice-versa, so they become connected"
  [profile-a profile-b]
  (dosync
    (alter (:connections profile-a) conj (:uuid profile-b))
    (alter (:connections profile-b) conj (:uuid profile-a))))

(defn common-connections-count
  "Returns the number of connections profile-a has in common
  with profile-b"
  [profile-a profile-b]
  (-> (intersection @(:connections profile-a) @(:connections profile-b))
      (count)))

(defn profile->suggestion
  "Returns a map where a profile is associated with its
  relevance, that is, the number of connections it has
  in common with a root profile"
  [profile root]
  {:profile profile
   :relevance (common-connections-count profile root)})

(defn not-visited
  "Returns a set of connections not included in the
  `visited` collection"
  [connections visited]
  (difference connections visited))

(defn visited?
  "Returns true if the given UUID is included in the
  visited collection"
  [visited curr-uuid]
  (contains? visited curr-uuid))

(defn suggestions
  "Returns a collection of suggested connections, with
  at most num items, for a given profile"
  [storage root-prof n]
  (loop [num          n
         queue        (conj PersistentQueue/EMPTY (:uuid root-prof))
         visited      #{(:uuid root-prof)}
         suggest-list []]
    (if (or (= num 0)
        (empty? queue))
      (sort-by :relevance > suggest-list)
      (let [curr      (peek queue)
            curr-prof (db/get-by-uuid! storage curr)
            conn      @(:connections curr-prof)
            new-queue (apply conj (pop queue) (not-visited conn visited))]
        (if-not (or (private? curr-prof)
                    (visited? visited curr)
                    (connected? curr-prof root-prof))
          (recur (dec num)
                 new-queue
                 (conj visited curr)
                 (conj suggest-list (profile->suggestion curr-prof root-prof)))
          (recur num
                 new-queue
                 (conj visited curr)
                 suggest-list))))))

