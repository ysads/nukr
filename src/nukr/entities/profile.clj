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

(defn with-connections
  "Returns a modified object where the connections field
  is an instance of clojure.lang.Ref, which allows them
  to be manipulated in a thread-safe way"
  [profile]
  (if (or (empty? (:connections profile))
          (nil? (:connections profile)))
    (assoc profile :connections (ref []))
    (assoc profile :connections (ref (:connections profile)))))

(defn create-profile
  "Returns an instance of Profile record based on
  data passed as argument"
  [profile-data]
  (-> profile-data
      (with-hashed-password)
      (with-connections)
      (map->Profile)))
