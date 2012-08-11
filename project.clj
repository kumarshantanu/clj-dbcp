(defproject clj-dbcp "0.8.0-SNAPSHOT"
  :description "Clojure wrapper for Apache DBCP to create JDBC connection pools."
  :url "https://github.com/kumarshantanu/clj-dbcp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :mailing-list {:name "Bitumen Framework discussion group"
                 :archive "https://groups.google.com/group/bitumenframework"
                 :other-archives ["https://groups.google.com/group/clojure"]
                 :post "bitumenframework@googlegroups.com"}
  :source-paths ["src"]
  :java-source-paths ["java-src"]
  :javac-options {:destdir "target/classes/"
                  :source  "1.5"
                  :target  "1.5"}
  :test-paths ["test"]
  :dependencies [[commons-dbcp "1.4"]]
  :profiles {:dev {:dependencies [[org.clojure/java.jdbc "0.2.3"]
                                  [oss-jdbc    "0.8.0"]
                                  [simple-jndi "0.11.4.1"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-alpha3"]]}}
  :aliases {"dev"     ["with-profile" "dev,1.4"]
            "all"     ["with-profile" "dev,1.2:dev,1.3:dev,1.4:dev,1.5"]
            "reflect" ["assoc" ":warn-on-reflection" "true" "compile"]}
  :warn-on-reflection true
  :min-lein-version "2.0.0"
  :jvm-opts ["-Xmx1g"])
