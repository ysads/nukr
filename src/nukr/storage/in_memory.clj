(ns nukr.storage.in-memory
  (:require [com.stuartsierra.component :as component])
  (:gen-class))

(defrecord InMemoryStorage [data]

  component/Lifecycle

  (start [storage]
    (println ";; Starting database")
    (assoc storage :data (atom {})))

  (stop [storage]
    (println ";; Stopping database")
    (storage)))

(defn init-storage
  "Initializes an empty in-memory storage"
  []
  (map->InMemoryStorage {}))

(defn with-uuid
  "Assigns an UUID to data"
  [data]
  (if (some? (:uuid data))
    data
    (let [uuid (.toString (java.util.UUID/randomUUID))]
      (assoc data :uuid uuid))))

(defn insert!
  "Inserts a new item into the storage"
  [storage data]
  (let [data-with-uuid (with-uuid data)]
    (swap! (:data storage)
           assoc
           (keyword (:uuid data-with-uuid))
           data-with-uuid)
    data-with-uuid))

(defn get-by-uuid!
  "Retrieves an item from storage by its UUID"
  [storage uuid]
  (if-let [data ((keyword uuid) @(:data storage))]
    data
    (throw (java.util.NoSuchElementException. (str uuid " not found")))))

(defn update-by-uuid!
  "Updates an item by it's UUID"
  [storage updated-record]
  (let [record (get-by-uuid! storage (:uuid updated-record))]
    (->> (merge record updated-record)
         (swap! (:data storage)
                assoc
                (keyword (:uuid updated-record))))
    updated-record))