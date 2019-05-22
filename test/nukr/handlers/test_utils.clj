(ns nukr.handlers.test-utils
  (:use [clojure.pprint])
  (:require [nukr.entities.profile :as p]
            [nukr.handlers.profile-handler :as ph]
            [nukr.storage.in-memory :as db]))

(def success-create-req {:request-method :post
                         :params {:profile {:name "John Doe"
                                            :email "john.doe@example.com"
                                            :password "NukR123@!#"
                                            :gender "other"}}})

(defn stub-request
  "Stub a request map with params attribute set
  to the arguments received."
  [params]
  {:params params})

(defn stub-profile-uuid
  "Stub a new profile using create-profile-handler."
  [storage data]
  (-> (ph/create-profile-handler storage data)
      (:body)
      (:uuid)))

(defn stub-connected-profile-uuid!
  "Stub a micro graph composed of five profiles meant
  to represent a more realistic scenario."
  [storage]
  (let [uuid-a (stub-profile-uuid storage success-create-req)
        uuid-b (stub-profile-uuid storage success-create-req)
        uuid-c (stub-profile-uuid storage success-create-req)
        uuid-d (stub-profile-uuid storage success-create-req)
        uuid-e (stub-profile-uuid storage success-create-req)]
    (->> (stub-request {:uuid-a uuid-a :uuid-b uuid-b})
         (ph/connect-profiles-handler storage))
    (->> (stub-request {:uuid-a uuid-b :uuid-b uuid-c})
         (ph/connect-profiles-handler storage))
    (->> (stub-request {:uuid-a uuid-b :uuid-b uuid-d})
         (ph/connect-profiles-handler storage))
    uuid-a))