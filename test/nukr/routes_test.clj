(ns nukr.routes-test
  (:use [clojure.pprint])
  (:require [aleph.http :as http]
            [bidi.bidi :refer [match-route]]
            [byte-streams :as bs]
            [cheshire.core :refer [generate-string parse-string]]
            [clojure.test :refer :all]
            [clojure.walk :refer [keywordize-keys]]
            [nukr.system :as sys]))

(defn setup-system
  "Configures a complete and working system so that the
  tests can perform requests"
  [f]
  (sys/init-system!)
  (sys/start!)
  (f)
  (sys/stop!))

(use-fixtures :once setup-system)

(def profile-data {:profile {:name "John Doe"
                             :email "john.doe@example.com"
                             :password "NukR123@!#"
                             :gender "other"}})

(def base-url "http://localhost:4000/profiles")

(defn with-request-options
  "Returns a map with options to be used by http-client
  when performing the request. Note this explicitly disables
  error-throwing when the response's status code is not 200,
  so that the requests can be tested."
  ([]
   (with-request-options {}))
  ([body]
   (let [options {:throw-exceptions false
                  :headers {"content-type" "application/json"}}]
     (if (empty? body)
       options
       (assoc options :body (generate-string body))))))

(defn parse-response-body
  "Converts the body, from the response object passed as
  argument, to a clojure map with keywordized keys"
  [response]
  (-> (:body response)
      (bs/to-string)
      (parse-string)
      (keywordize-keys)))

(defn stub-profile-uuid
  "Stub a valid profile UUID by creating a random profile
  using the API"
  []
  (let [url "http://localhost:4000/profiles"
        res @(http/post url (with-request-options profile-data))
        body (parse-response-body res)]
    (:uuid body)))

(defn connect-profiles!
  [uuid-a uuid-b]
  (let [url (str base-url "/connect/" uuid-a "/" uuid-b)]
    @(http/post url (with-request-options))))

(defn stub-connected-graph-around
  [uuid-a uuid-b]
  (let [uuid-c  (stub-profile-uuid)
        uuid-d  (stub-profile-uuid)
        uuid-e  (stub-profile-uuid)
        opt-url (str base-url "/" uuid-c "/opt/true")]
    (connect-profiles! uuid-a uuid-b)
    (connect-profiles! uuid-b uuid-c)
    (connect-profiles! uuid-b uuid-d)
    (connect-profiles! uuid-d uuid-e)
    @(http/post opt-url (with-request-options))))

(deftest routes-responses
  (testing "/profiles"
    (let [url base-url]
      (testing "with valid body"
        (let [res  @(http/post url (with-request-options profile-data))
              body (parse-response-body res)]
          (is (= 201 (:status res)))
          (is (some? (:uuid body)))))

      (testing "with wrong request method returns 405"
        (let [put-res @(http/put url (with-request-options profile-data))
              get-res @(http/get url (with-request-options profile-data))]
          (is (= 405 (:status put-res)))
          (is (= 405 (:status get-res)))))

      (testing "with bad body returns 400"
        ;; after spec is implemented, go back here
        )))

  ;; Stub some UUIDs for the next calls so that they have their
  ;; pre-requirements to be tested
  (let [uuid-a (stub-profile-uuid)
        uuid-b (stub-profile-uuid)]

    (testing "/profiles/:uuid/opt/:private"
      (testing "when transition is public->private"
        (let [url  (str base-url "/" uuid-a "/opt/true")
              res  @(http/post url (with-request-options))
              body (parse-response-body res)]
          (is (= 200 (:status res)))
          (is (->> (:profile body)
                   (:private)
                   (= "true")))))

      (testing "when transition is public->private"
        (let [url  (str base-url "/" uuid-a "/opt/false")
              res  @(http/post url (with-request-options))
              body (parse-response-body res)]
          (is (= 200 (:status res)))
          (is (->> (:profile body)
                   (:private)
                   (= "false")))))

      (testing "when UUID not found"
        (let [url  (str base-url "/12345/opt/true")
              res  @(http/post url (with-request-options))]
          (is (= 404 (:status res))))))

    (testing "/profiles/connect/:uuid-a/:uuid-b"
      (testing "when either UUID A or B aren't found"
        (let [url-a (str base-url "/connect/12345/" uuid-a)
              url-b (str base-url "/connect/" uuid-a "/12345")
              res-a @(http/post url-a (with-request-options))
              res-b @(http/post url-b (with-request-options))]
          (is (= 404 (:status res-a)))
          (is (= 404 (:status res-b)))))

      (testing "when profiles are not connected"
        (let [url (str base-url "/connect/" uuid-a "/" uuid-b)
              res @(http/post url (with-request-options))]
          (is (= 200 (:status res))))))

    (testing "/profiles/:uuid/suggestions"
      (testing "when UUID not found"
        (let [url (str base-url "/12345/suggestions")
              res @(http/post url (with-request-options))]
          (is (= 404 (:status res)))))

      ;; Stub some profile connections in order to make suggestions
      ;; available for querying
      (stub-connected-graph-around uuid-a uuid-b)

      (testing "when count param is not passed"
        (let [url  (str base-url "/" uuid-a "/suggestions")
              res  @(http/post url (with-request-options))
              body (parse-response-body res)]
          (is (= 200 (:status res)))
          (is (< 1 (count (:suggestions body))))))

      (testing "when count param is passed"
        (let [url  (str base-url "/" uuid-a "/suggestions?count=1")
              res  @(http/post url (with-request-options))
              body (parse-response-body res)]
          (is (= 200 (:status res)))
          (is (= 1 (count (:suggestions body))))))))

  (testing "undefined routes"
    (let [res-a @(http/post (str base-url "/connect") (with-request-options))
          res-b @(http/post (str base-url "/suggestions") (with-request-options))
          res-c @(http/post (str base-url "/abc") (with-request-options))]
      (is (= 404 (:status res-a)))
      (is (= 404 (:status res-b)))
      (is (= 404 (:status res-c))))))

