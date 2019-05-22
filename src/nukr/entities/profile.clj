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
