(ns nukr.handlers.profile-handler
  (:use [clojure.pprint])
  (:require [nukr.entities.profile :refer [create-profile connect!]]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok not-found created]])
  (:import java.util.NoSuchElementException)
  (:gen-class))

(defn uuid->created
  "Returns a `201 Created` HTTP response based on an UUID"
  [uuid]
  (-> (str "/profile/" uuid)
      (created {:uuid uuid})))

(defn create-profile-handler
  "Creates a profile based on data received as argument,
  saving it to storage"
  [storage request-data]
  (->> (create-profile request-data)
       (db/insert! storage)
       (:uuid)
       (uuid->created)))

(defn with-privacy-set
  "Returns a profile with it's privacy set according the
  request setting"
  [private? profile]
  (assoc profile :private private?))

(defn opt-profile-privacy-handler
  "Toggles a profile privacy state, that is, set a profile
  as public if it's private or vice-versa"
  [storage uuid private?]
  (try
     (->> (db/get-by-uuid! storage uuid)
          (with-privacy-set private?)
          (db/update-by-uuid! storage)
          (ok))
     (catch NoSuchElementException ex
       (not-found))))

(defn connect-profiles-handler
  "Tag two profiles as connected, if they are not yet
  connected. Otherwise, does nothing"
  [storage uuid-a uuid-b]
  (try
    (let [profile-a (db/get-by-uuid! storage uuid-a)
          profile-b (db/get-by-uuid! storage uuid-b)]
      (connect! profile-a profile-b)
      (db/update-by-uuid! storage profile-a)
      (db/update-by-uuid! storage profile-b))
      (ok)
    (catch NoSuchElementException ex
      (not-found))))