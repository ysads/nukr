(ns nukr.handlers.profile-handler
  (:use [clojure.pprint])
  (:require [nukr.entities.profile :refer [create-profile connect! suggestions]]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok bad-request not-found created method-not-allowed]])
  (:import java.util.NoSuchElementException
           java.lang.NoSuchFieldError)
  (:gen-class))

(defn- uuid->created
  "Returns a `201 Created` HTTP response based on an UUID"
  [uuid]
  (-> (str "/profiles/" uuid)
      (created {:uuid uuid})))

(defn create-profile-handler
  "Creates a profile based on data received as argument,
  saving it to storage"
  [storage request]
  (if (= :post (:request-method request))
    (try
      (->> (create-profile (:profile (:params request)))
           (db/insert! storage)
           (:uuid)
           (uuid->created))
      (catch NoSuchFieldError ex
        (bad-request)))
    (method-not-allowed)))

(defn- with-privacy-set
  "Returns a profile with it's privacy set according the
  request setting"
  [private? profile]
  (assoc profile :private private?))


(defn- deref-connections
  "Returns a profile with its connections attribute dereferenced.
  This allows the object to be safely JSON-encoded."
  [profile]
  (if (instance? clojure.lang.Ref (:connections profile))
    (update-in profile [:connections] deref)
    profile))

(defn opt-profile-handler
  "Toggles a profile privacy state, that is, set a profile
  as public if it's private or vice-versa"
  [storage request]
  (let [{:keys [uuid private]} (:params request)]
    (try
       (->> (db/get-by-uuid! storage uuid)
            (with-privacy-set private)
            (db/update-by-uuid! storage)
            (deref-connections)
            (assoc {} :profile)
            (ok))
       (catch NoSuchElementException ex
         (not-found)))))

(defn connect-profiles-handler
  "Tag two profiles as connected, if they are not yet
  connected. Otherwise, does nothing"
  [storage request]
  (let [{:keys [uuid-a uuid-b]} (:params request)]
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

(defn- parse-suggestions-count
  "Returns the suggestions count sent in the request or
  the default count otherwise."
  [request]
  (let [num (:count (:params request))]
    (cond
      (string? num) (Integer/parseInt num)
      (nil? num) default-suggest-num
      :else num)))

(defn- suggestions->ok
  "Returns a `200 ok` HTTP response containing a collection
  of profile suggestions. Note this profiles have their connections
  dereferenced."
  [suggestions]
  (->> suggestions
       (map #(update-in % [:profile] deref-connections))
       (doall)
       (assoc {} :suggestions)
       (ok)))

(defn suggestions-handler
  "Returns a collection of connection suggestions for
  a given profile based upon its existing connections"
  [storage request]
  ; (println "\n\nREQUEST")
  ; (pprint request)
  (let [{:keys [uuid]} (:params request)]
    (try
      (let [profile (db/get-by-uuid! storage uuid)
            num     (parse-suggestions-count request)]
        (->> (suggestions storage profile num)
             (suggestions->ok)))
      (catch NoSuchElementException ex
        (not-found)))))
