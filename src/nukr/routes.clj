(ns nukr.routes
  (:use [clojure.pprint])
  (:require [bidi.ring :refer [make-handler]]
            [nukr.handlers.profile-handler :refer :all]
            [ring.util.http-response :refer [not-found]])
  (:gen-class))

(defn routes
  "Returns a data structure mapping each route to its
  associated handler function"
  [storage]
  ["" [["/profiles" [[""                                (partial create-profile-handler storage)]
                     [["/" :uuid "/opt/" :private]      (partial opt-profile-handler storage)]
                     [["/" :uuid "/suggestions"]        (partial suggestions-handler storage)]
                     [["/connect/" :uuid-a "/" :uuid-b] (partial connect-profiles-handler storage)]]]
        [true       (partial not-found)]]])

(defn routes-handler
  "Defines the matching handler for each route of the app"
  [storage]
  (make-handler (routes storage)))