(defproject evelyn "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]

                 [net.boostrot/pso "0.1.0"]

                 [joda-time "2.2"]
                 [quil "1.6.0"]

                 [korma "0.3.0-RC5"]
                 [lobos "1.0.0-beta1"]
                 [org.clojure/java.jdbc "0.3.0-alpha3"]
                 [com.h2database/h2 "1.3.171"]

                 [org.xerial/sqlite-jdbc "3.7.2"]
                 ]

  :profiles {:dev {:dependencies [[midje "1.5.1"]]}})
