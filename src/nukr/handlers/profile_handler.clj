(ns nukr.handlers.profile-handler
  (:use [clojure.pprint])
  (:require [nukr.entities.profile :refer [create-profile]]
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
  [privacy profile]
  (assoc profile :private privacy))

(defn opt-profile-privacy-handler
  "Toggles a profile privacy state, that is, set a profile
  as public if it's private or vice-versa"
  [storage uuid privacy]
  (try
     (->> (db/get-by-uuid! storage uuid)
          (with-privacy-set privacy)
          (db/update-by-uuid! storage)
          (ok))
     (catch NoSuchElementException ex
       (not-found))))