(defproject nukr "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.stuartsierra/component "0.4.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [metosin/compojure-api "1.1.11"]]
  :main ^:skip-aot nukr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
