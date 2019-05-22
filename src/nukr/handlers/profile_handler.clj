(ns nukr.handlers.profile-handler
  (:use [clojure.pprint])
  (:require [clojure.spec.alpha :as s]
            [nukr.entities.profile :refer [create-profile connect! suggestions]]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok bad-request not-found created method-not-allowed]])
  (:import java.util.NoSuchElementException
           java.lang.NoSuchFieldError)
  (:gen-class))

;;; The following lines define some specs used to
;;; validate incoming requests and their parameters
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

(s/def ::name string?)
(s/def ::email ::email-type)
(s/def ::gender #(.contains ["male" "female" "other"] %))
(s/def ::password (s/and #(<= 6 (count %) 32)
                         #(re-find #"\d" %)
                         #(re-find #"[!@#$%&*;?<>`]" %)
                         #(re-find #"[A-Z]" %)))

(s/def ::profile (s/keys :req-un [::name ::email ::password ::gender]))
(s/def ::params (s/keys :req-un [::profile]))
(s/def ::profile-create-request (s/keys :req-un [::params]))

(defn- uuid->created
  "Returns a `201 Created` HTTP response based on an UUID"
  [uuid]
  (-> (str "/profiles/" uuid)
      (created {:uuid uuid})))

(defn- create-and-insert-profile!
  "Creates a new profile, inserts it to storage and returns the
  the profile associated UUID."
  [storage request]
  (->> (create-profile (:profile (:params request)))
       (db/insert! storage)
       (:uuid)))

(defn create-profile-handler
  "Creates a profile with the data given in the request, persisting
  it to the storage received as argument. Note this handler only
  responds to POST requests."
  [storage request]
  (cond
    (not= :post (:request-method request))
    (method-not-allowed)

    (s/valid? ::profile-create-request request)
    (try
      (let [uuid (create-and-insert-profile! storage request)]
        (uuid->created uuid))
      (catch NoSuchFieldError ex
        (bad-request)))

    :else
    (bad-request)))

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
