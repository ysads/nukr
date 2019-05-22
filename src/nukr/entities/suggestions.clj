(ns nukr.entities.suggestions
  (:require [clojure.set :refer [intersection difference]]
            [nukr.entities.profile :as p]
            [nukr.storage.in-memory :as db])
  (:import clojure.lang.PersistentQueue)
  (:gen-class))

(defn common-connections-count
  "Returns the number of connections profile-a has in common
  with profile-b"
  [profile-a profile-b]
  (-> (intersection @(:connections profile-a) @(:connections profile-b))
      (count)))

(defn not-visited
  "Returns a set of connections not included in the
  `visited` collection"
  [connections visited]
  (difference connections visited))

(defn visited?
  "Returns true if the given UUID is included in the
  visited collection"
  [visited curr-uuid]
  (.contains visited curr-uuid))

(defn profile->suggestion
  "Returns a map where a profile is associated with its
  relevance, that is, the number of connections it has
  in common with a root profile"
  [profile root]
  {:profile profile
   :relevance (common-connections-count profile root)})

(defn find-suggestions
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
        (if-not (or (visited? visited curr)
                    (p/private? curr-prof)
                    (p/connected? curr-prof root-prof))
          (recur (dec num)
                 new-queue
                 (conj visited curr)
                 (conj suggest-list (profile->suggestion curr-prof root-prof)))
          (recur num
                 new-queue
                 (conj visited curr)
                 suggest-list))))))