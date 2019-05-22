(ns nukr.handlers.suggestions-handler
  (:require [nukr.entities.profile :as p]
            [nukr.entities.suggestions :refer [find-suggestions]]
            [nukr.storage.in-memory :as db]
            [ring.util.http-response :refer [ok bad-request not-found created method-not-allowed]])
  (:import java.util.NoSuchElementException
           java.lang.NoSuchFieldError)
  (:gen-class))

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
       (map #(update-in % [:profile] p/deref-connections))
       (doall)
       (assoc {} :suggestions)
       (ok)))

(defn find-suggestions-handler
  "Returns a collection of connection suggestions for
  a given profile based upon its existing connections"
  [storage request]
  (let [{:keys [uuid]} (:params request)]
    (try
      (let [profile (db/get-by-uuid! storage uuid)
            num     (parse-suggestions-count request)]
        (->> (find-suggestions storage profile num)
             (suggestions->ok)))
      (catch NoSuchElementException ex
        (not-found)))))