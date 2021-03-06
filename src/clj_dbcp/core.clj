(ns clj-dbcp.core
  "Create DataSource using Apache DBCP"
  (:require
    [clojure.string :as str]
    [clj-dbcp.util :refer (as-str)])
  (:import (java.net URI)
    (java.sql DriverManager)
    (javax.sql DataSource)
    (org.apache.commons.dbcp2 BasicDataSource)
    (clj_dbcp ConnectionWrapper)))


(defn jndi-datasource
  "Lookup JNDI DataSource. Example Tomcat 6 configuration (/WEB-INF/web.xml):
  <resource-ref>
    <description>
      Resource reference to a factory for java.sql.Connection
      instances that may be used for talking to a particular
      database that is configured in the <Context>
      configurartion for the web application.
    </description>
    <res-ref-name>
      jdbc/EmployeeDB
    </res-ref-name>
    <res-type>
      javax.sql.DataSource
    </res-type>
    <res-auth>
      Container
    </res-auth>
  </resource-ref>
  You can fetch this datasource as follows:
    (jndi-datasource \"java:comp/env/jdbc/EmployeeDB\")"
  ([^javax.naming.Context init-ctx ^String resource-ref-name]
    {:post [(instance? DataSource %)]}
    (.lookup init-ctx resource-ref-name))
  ([resource-ref-name]
    (jndi-datasource
      (javax.naming.InitialContext.) resource-ref-name)))


(defn jdbc-datasource
  "Create DataSource from a given option map"
  [{:keys [classname
           jdbc-url
           properties
           user
           username
           password
           test-query
           init-size
           min-idle
           max-idle
           max-total
           pool-pstmt?
           max-open-pstmt
           remove-abandoned?
           remove-abandoned-timeout-seconds
           log-abandoned?
           remove-abandoned-on-borrow?
           remove-abandoned-on-maintenance?
           lifo-pool?
           test-while-idle?
           test-on-borrow?
           test-on-return?
           test-query-timeout
           millis-between-eviction-runs
           min-evictable-millis
           tests-per-eviction
           cache-state?]
    :or {pool-pstmt?                      true
         remove-abandoned?                true
         init-size                        1
         min-idle                         1
         max-total                        2
         max-idle                         1
         remove-abandoned-timeout-seconds 60
         lifo-pool                        false
         log-abandoned?                   true
         test-on-borrow?                  true
         test-on-return?                  true
         test-while-idle?                 true
         millis-between-eviction-runs     -1
         min-evictable-millis             1800000
         tests-per-eviction               3
         cache-state?                     true}
    :as opts}]
  {:pre [(string? classname) (seq classname)
         (Class/forName classname)
         (string? jdbc-url) (seq jdbc-url)]}
  (let [^BasicDataSource datasource (doto ^BasicDataSource (BasicDataSource.)
                                          (.setDriverClassName classname)
                                          (.setUrl             jdbc-url))]
    (when properties  (do (assert (map? properties))
                          (doseq [[k v] properties]
                            (.addConnectionProperty datasource
                                                    (as-str k) (as-str v)))))
    (when user        (.setUsername datasource (as-str user)))
    (when username    (.setUsername datasource (as-str username)))
    (when password    (.setPassword datasource (as-str password)))
    (when test-query   (do (assert (string? test-query))
                           (doto datasource
                             (.setValidationQuery ^String test-query)
                             (.setTestOnBorrow  true)
                             (.setTestOnReturn  true)
                             (.setTestWhileIdle true))))
    (when init-size   (do (assert (integer? init-size))
                          (assert (pos?     init-size))
                          (.setInitialSize datasource init-size)))
    (when min-idle    (do (assert (integer? min-idle))
                          (assert (pos?     min-idle))
                          (.setMinIdle datasource min-idle)))
    (when max-idle    (do (assert (integer? max-idle))
                          (assert (pos?     max-idle))
                          (.setMaxIdle datasource max-idle)))
    (when max-total  (do (assert (integer? max-total))
                         (assert (pos?     max-total))
                         (.setMaxTotal datasource max-total)))
    (when pool-pstmt? (do (assert (true?    pool-pstmt?))
                          (.setPoolPreparedStatements datasource true)))
    (when max-open-pstmt
                      (do (assert (integer? max-open-pstmt))
                          (assert (pos?     max-open-pstmt))
                          (.setMaxOpenPreparedStatements datasource
                                                         max-open-pstmt)))
    (when remove-abandoned-on-borrow?
                      (do (assert (true?    remove-abandoned?))
                          (.setRemoveAbandonedOnBorrow datasource true)))
    (when remove-abandoned-on-maintenance?
                      (do (assert (true?    remove-abandoned?))
                          (.setRemoveAbandonedOnMaintenance datasource true)))
    (when remove-abandoned-timeout-seconds
                      (do (assert (integer? remove-abandoned-timeout-seconds))
                          (assert (pos?     remove-abandoned-timeout-seconds))
                          (.setRemoveAbandonedTimeout
                            datasource remove-abandoned-timeout-seconds)))
    (when log-abandoned?
                      (do (assert (true?    log-abandoned?))
                          (.setLogAbandoned datasource true)))
    (when lifo-pool? (.setLifo datasource lifo-pool?))
    (when test-on-borrow? (.setTestOnBorrow datasource test-on-borrow?))
    (when test-on-return? (.setTestOnReturn datasource test-on-return?))
    (when test-while-idle? (.setTestWhileIdle datasource test-on-return?))
    (when test-query-timeout (do (assert (integer? test-query-timeout))
                                  (.setValidationQueryTimeout datasource test-query-timeout)))
    (when millis-between-eviction-runs (do (assert (integer? millis-between-eviction-runs))
                         (.setTimeBetweenEvictionRunsMillis datasource millis-between-eviction-runs)))
    (when min-evictable-millis (do (assert (integer? min-evictable-millis))
                         (assert (pos?     min-evictable-millis))
                         (.setMinEvictableIdleTimeMillis datasource min-evictable-millis)))
    (when tests-per-eviction (do (assert (integer? tests-per-eviction))
                          (assert (pos?     tests-per-eviction))
                          (.setNumTestsPerEvictionRun datasource tests-per-eviction)))
    (when cache-state? (.setCacheState datasource cache-state?))
    datasource))


(defn ^DataSource lite-datasource
  "Create lite datasource that ignores setAutoCommit calls."
  [{:keys [classname jdbc-url]}]
  {:pre [(string? classname) (seq classname)
         (Class/forName classname)
         (string? jdbc-url) (seq jdbc-url)]}
  (let [ignore ["setAutoCommit"]]
    (proxy [DataSource] []
      (getConnection
        ([]
          (Class/forName classname)
          (ConnectionWrapper.
            (DriverManager/getConnection jdbc-url) ignore))
        ([^String username ^String password]
          (Class/forName classname)
          (ConnectionWrapper.
            (DriverManager/getConnection jdbc-url username password) ignore))))))


(def default-subproto-map {"postgres" "postgresql"})


(defn parse-url
  "Given a String or a URI instance, and an optional subproto-map for conversion
  return a map of args suitable for use with `make-datasouce`."
  ([jdbc-uri subproto-map] {:pre [(map? subproto-map)]}
    (cond
      ;; URI
      (instance? URI jdbc-uri)
      (let [host (.getHost ^URI jdbc-uri)
            port (let [p (.getPort ^URI jdbc-uri)]
                   (and (pos? p) p))
            path (.getPath ^URI jdbc-uri)
            query (.getRawQuery ^URI jdbc-uri)
            scheme  (.getScheme ^URI jdbc-uri)
            adapter (subproto-map scheme scheme)]
        (merge {:adapter  (keyword adapter)
                :classname "org.postgresql.Driver"
                :jdbc-url (str "jdbc:" adapter "://" host
                            (when port ":") (or port "") path
                            (when query "?") (or query ""))}
          (if-let [user-info (.getUserInfo ^URI jdbc-uri)]
            (let [[un pw] (str/split user-info #":")]
              {:username un
               :password pw}))))
      ;; String
      (string? jdbc-uri)
      (parse-url (if (.startsWith ^String jdbc-uri "jdbc:")
                   (URI. (subs jdbc-uri 5))
                   (URI. jdbc-uri))
        subproto-map)
     ;; default
     :otherwise
     (throw (IllegalArgumentException.
              (str "Expected `jdbc-uri` to be java.net.URI or String,"
                " but found (" (pr-str (type jdbc-uri)) ") "
                (pr-str jdbc-uri))))))
  ([jdbc-uri]
    (parse-url jdbc-uri default-subproto-map)))


(defn make-datasource
  "Create datasource from a given option-map. Some examples are below:
  (make-datasource :jdbc  {:jdbc-url   \"jdbc:mysql://localhost/emp\"
                           :class-name \"com.mysql.Driver\"})          ;; JDBC arguments
  (make-datasource :odbc  {:dsn :sales_report})                        ;; ODBC connections
  (make-datasource :jndi  {:context \"whatever\"})                     ;; JNDI connections
  (make-datasource {:adapter :odbc-lite :dsn :moo})                    ;; ODBC-lite (MS-Access, MS-Excel etc.)
  (make-datasource {:class-name 'com.mysql.Driver
                    :jdbc-url   \"jdbc:mysql://localhost/emp\"})       ;; JDBC is default adapter"
  ([adapter opts] {:pre [(keyword? adapter)]}
    (if (= adapter :jndi) (do (assert (contains? opts :context))
                            (assert (string?   (get opts :context)))
                            (jndi-datasource   (:context opts)))
      (let [e-opts (assoc opts :adapter adapter)]
        (if (:lite? e-opts) (lite-datasource e-opts)
          (jdbc-datasource e-opts)))))
  ([opts] {:pre [(map? opts)]}
    (let [adapter (or (:adapter opts)
                    (when (contains? opts :subprotocol) :subprotocol)
                    (when (contains? opts :dsn)         :odbc)
                    :jdbc)]
      (make-datasource adapter opts))))
