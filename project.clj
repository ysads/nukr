(defproject nukr "0.1.0-SNAPSHOT"
  :description "A prototype of social network built using functional programming techniques and features."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[aleph "0.4.6"]
                 [bidi "2.1.6"]
                 [buddy/buddy-hashers "1.3.0"]
                 [cheshire "5.8.1"]
                 [com.stuartsierra/component "0.4.0"]
                 [metosin/ring-http-response "0.9.1"]
                 [org.clojure/clojure "1.9.0"]
                 [ring/ring-json "0.4.0"]]
  :user {:plugins [[venantius/ultra "0.6.0"]]}
  :main ^:skip-aot nukr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
