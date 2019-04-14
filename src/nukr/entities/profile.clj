(ns nukr.entities.profile
  (:require [buddy.hashers :as hashers])
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

(defn create-profile
  "Returns an instance of Profile record based on
  data passed as argument"
  [profile-data]
  (-> profile-data
      (with-hashed-password)
      (map->Profile)))
