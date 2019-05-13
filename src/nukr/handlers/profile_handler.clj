(ns nukr.handlers.profile-handler
  (:require [nukr.entities.profile :refer [create-profile connect! suggestions]]
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
  [storage request]
  (->> (:form-params request)
       (create-profile)
       (db/insert! storage)
       (:uuid)
       (uuid->created)))

(defn with-privacy-set
  "Returns a profile with it's privacy set according the
  request setting"
  [private? profile]
  (assoc profile :private private?))

(defn opt-profile-handler
  "Toggles a profile privacy state, that is, set a profile
  as public if it's private or vice-versa"
  [storage request]
  (let [{:keys [uuid private]} (:route-params request)]
    (try
       (->> (db/get-by-uuid! storage uuid)
            (with-privacy-set private)
            (db/update-by-uuid! storage)
            (ok))
       (catch NoSuchElementException ex
         (not-found)))))

(defn connect-profiles-handler
  "Tag two profiles as connected, if they are not yet
  connected. Otherwise, does nothing"
  [storage request]
  (let [{:keys [uuid-a uuid-b]} (:route-params request)]
    (try
      (let [profile-a (db/get-by-uuid! storage uuid-a)
            profile-b (db/get-by-uuid! storage uuid-b)]
        (connect! profile-a profile-b)
        (db/update-by-uuid! storage profile-a)
        (db/update-by-uuid! storage profile-b))
        (ok)
      (catch NoSuchElementException ex
        (not-found)))))

;;; The number of profile suggestions the handler must return
;;; when this argument is not explicitly passed
(def default-suggest-num 5)

(defn suggestions->ok
  "Returns a `200 ok` HTTP response containing a collection
  of profile suggestions"
  [suggestions]
  (ok {:suggestions suggestions}))

(defn suggestions-handler
  "Returns a collection of connection suggestions for
  a given profile based upon its existing connections"
  [storage request]
  (let [{:keys [uuid count]} (:route-params request)]
    (try
      (let [profile (db/get-by-uuid! storage uuid)
            num (or count default-suggest-num)]
        (->> (suggestions storage profile num)
             (suggestions->ok)))
      (catch NoSuchElementException ex
        (not-found)))))
