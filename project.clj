(defproject clj-dbcp "0.9.0-SNAPSHOT"
  :description "Clojure wrapper for Apache DBCP to create JDBC connection pools."
  :url "https://github.com/kumarshantanu/clj-dbcp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :mailing-list {:name "Bitumen Framework discussion group"
                 :archive "https://groups.google.com/group/bitumenframework"
                 :other-archives ["https://groups.google.com/group/clojure"]
                 :post "bitumenframework@googlegroups.com"}
  :java-source-paths ["java-src"]
  :javac-options {:destdir "target/classes/"
                  :source  "1.5"  ; FIXME drop support for JDK 1.5 in 0.9
                  :target  "1.5"}
  :global-vars {*warn-on-reflection* true
                *assert* false}
  :dependencies [[commons-dbcp "1.4"]
                 [cumulus "0.1.1"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :dev {:dependencies [[org.clojure/java.jdbc "0.2.3"]
                                  [oss-jdbc    "0.8.0"]
                                  [simple-jndi "0.11.4.1"]]}
             :c12 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :c13 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :c14 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :c15 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :c16 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :c17 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c18 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c19 {:dependencies [[org.clojure/clojure "1.9.0-alpha10"]]}
}
  :aliases {"dev"     ["with-profile" "dev,c14"]
            "all"     ["with-profile" "dev,c12:dev,c13:dev,c14:dev,c15:dev,c16:dev,c17:dev,c18:dev,c19"]
            "reflect" ["assoc" ":warn-on-reflection" "true" "compile"]}
  :min-lein-version "2.0.0"
  :jvm-opts ["-Xmx1g"])

