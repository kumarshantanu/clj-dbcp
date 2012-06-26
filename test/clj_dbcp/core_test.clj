(ns clj-dbcp.core-test
  (:require
    [clj-dbcp.dbserver-util :as du]
    [clj-dbcp.test-util     :as tu]
    [clojure.pprint         :as pp]
    [clojure.java.jdbc      :as sql])
  (:use clojure.test
        clj-dbcp.core))


(defn test-crud
  [dbspec]
  (let [table :emp
        orig-record {:id 1 :name "Bashir" :age 40}
        updt-record {:id 1 :name "Shabir" :age 50}
        drop-table  #(sql/do-commands "DROP TABLE emp")
        retrieve-fn #(sql/with-query-results rows
                      ["SELECT * FROM emp WHERE id=?" 1]
                      (first rows))
        as-int-num #(reduce into {}
                            (map (fn [[k v]] (if (number? v)
                                               {k (int v)}
                                               {k v})) %))]
    (sql/with-connection dbspec
      ;; drop table if pre-exists
      (try (drop-table)
        (catch Exception _))
      ;; create table
      (sql/do-commands
        "CREATE TABLE emp (id INTEGER, name VARCHAR(50), age INTEGER)")
      ;; insert
      (sql/insert-values table (keys orig-record) (vals orig-record))
      ;; retrieve-check
      (is (= orig-record (as-int-num (retrieve-fn))))
      ;; update
      (sql/update-values table ["id=?" 1] updt-record)
      ;; retrieve-check
      (is (= updt-record (as-int-num (retrieve-fn))))
      ;; delete
      ;; drop table
      (drop-table))))


(defn test-datasource
  "Test a datasource. It creates a dbspec out of the supplied datasource and
  sends for testing."
  [datasource]
  (test-crud {:datasource datasource}))


(defn test-one
  "Create datasource from given options and run test"
  [opts]
  (testing (pr-str opts)
           (let [setup (:setup opts)
                 context (when setup (setup))]
             (when context (tu/sleep 500))  ;; sleep for for server warmup
             (try
               (test-datasource (make-datasource opts))
               (finally
                 (when-let [teardown (:teardown opts)]
                   (if context (teardown context)
                     (teardown))))))))


(deftest test-adapter-arg
  (test-datasource (make-datasource :axiondb {:target :memory
                                              :database :defaultmax})))


(deftest test-jndi
  "See also (not used): http://commons.apache.org/dbcp/guide/jndi-howto.html"
  (tu/with-root-context (javax.naming.InitialContext.)
    (tu/print-jndi-tree))
  (let [ds (jndi-datasource "java:comp/env/myDataSource")]
    (test-datasource ds)))


(def jdbc-tests
  [;; implicit :jdbc adapter
   {:classname "org.apache.derby.jdbc.EmbeddedDriver"
    :jdbc-url  "jdbc:derby:memory:defaultjdbc;create=true;"
    :val-query "values(1)"}
   ;; explicit :jdbc adapter
   {:adapter   :jdbc
    :classname "org.apache.derby.jdbc.EmbeddedDriver"
    :jdbc-url  "jdbc:derby:memory:defaultjdbcexplicit;create=true;"
    :val-query "values(1)"}
   ;; implicit :subprotocol adapter
   {:classname "org.apache.derby.jdbc.EmbeddedDriver"
    :subprotocol "derby"
    :subname "memory:defaultsubproto;create=true;"
    :val-query "values(1)"}
   ;; explicit :subprotocol adapter
   {:classname "org.apache.derby.jdbc.EmbeddedDriver"
    :subprotocol "derby"
    :subname "memory:defaultsubprotoexplicit;create=true;"
    :val-query "values(1)"}])


(deftest test-jdbc
  (doseq [each jdbc-tests]
    (test-one each)))


(def embed-tests
  [;; in-memory (embedded) databases
   {:adapter :axiondb :target :memory :database :defaultmax}
   {:adapter :derby   :target :memory :database :default}
   {:adapter :h2      :target :memory :database :default}
   {:adapter :hsqldb  :target :memory :database :default}
   {:adapter :sqlite  :target :memory}
   ;; filesystem (embedded) databases
   {:adapter :axiondb :target :filesys :database 'target/default :db-path "target"}
   {:adapter :derby   :target :filesys :database 'target/default}
   {:adapter :h2      :target :filesys :database 'target/default}
   {:adapter :hsqldb  :target :filesys :database 'target/default}
   {:adapter :mckoi                    :database "./test/mckoi.conf"
    :user "sa" :password "pw"
    :setup du/setup-mckoi-local-database}
   {:adapter :sqlite  :target :filesys :database 'target/default3}
   ;; network (embedded) databases
   {:adapter :derby   :target :network :database 'default
    :host 'localhost :port 2345 :user "sa" :password ""
    :setup (partial du/start-derby-server "localhost" 2345)
    :teardown du/stop-derby-server}
   {:adapter :hsqldb  :target :network :database 'defaulthsql
    :host 'localhost :port 2346 :user "sa" :password ""
    :setup (partial du/start-hsql-server "localhost" 2346 "defaulthsql")
    :teardown du/stop-hsql-server}
   {:adapter :h2      :target :network :database 'defaulth2
    :host 'localhost :port 2347 :user "sa" :password ""
    :setup (partial du/start-h2-server 2347)
    :teardown du/stop-h2-server}])


(deftest test-embedded
  (doseq [each embed-tests]
    (test-one each)))


(def network-tests
  ;; Uncomment only those that you want to test - DO NOT COMMIT CHANGES TO VCS
  [;; --- network (Open Source) databases ---
   ;; [:firebird       'localhost 'fbuser   's3cr3t "C:/temp/dbcp"]
   ;; [:cubrid         'localhost 'cbuser   's3cr3t 'dbcp]
   ;; [:jtds-sqlserver 'localhost 'sqluser  's3cr3t 'dbcp]
   ;; [:jtds-sybase    'localhost 'syuser   's3cr3t 'dbcp]
   ;; [:monet          'localhost 'mouser   's3cr3t 'dbcp]
   ;; [:mysql          'localhost 'root     ""      'dbcp]
   ;; [:postgresql     'localhost 'pguser   's3cr3t 'dbcp]
   ;; --- network (proprietary) databases ---
   ;; [:db2            'localhost 'db2user  's3cr3t 'dbcp]
   ;; [:oracle         'localhost 'orauser  's3cr3t 'dbcp]
   ;; [:sapdb          'localhost 'sapuser  's3cr3t 'dbcp]
   ;; [:sqlserver      'localhost 'sqluser  's3cr3t 'dbcp]
   ;; [:sybase         'localhost 'syuser   's3cr3t 'dbcp]
   ])


(deftest test-network
  (doseq [each network-tests]
    (test-one (zipmap [:adapter :host :user :password :database]
                      each))))


(def odbc-tests
  [{:dsn :mysqldbcp :user 'root}
   {:dsn :exceldb :lite? true}])


(deftest test-odbc
  (doseq [each odbc-tests]
    (test-one each)))


(defn test-ns-hook
  []
  (test-adapter-arg)
  (test-jdbc)
  (test-embedded)
  ;; (test-odbc)    ;; to be run only on Windows
  (test-network)
  (test-jndi))
