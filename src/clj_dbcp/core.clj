(ns clj-dbcp.core
  "Create DataSource using Apache DBCP"
  (:require [clj-dbcp.adapter :as adap])
  (:use [clj-dbcp.util :only (as-str)])
  (:import (java.sql DriverManager)
           (javax.sql DataSource)
           (org.apache.commons.dbcp BasicDataSource)
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
           val-query
           min-idle
           max-idle
           max-active
           pool-pstmt?]}]
  {:pre [(string? classname) (seq classname)
         (Class/forName classname)
         (string? jdbc-url) (seq jdbc-url)]}
  (let [^BasicDataSource datasource (doto ^BasicDataSource (BasicDataSource.)
                                          (.setDriverClassName classname)
                                          (.setUrl             jdbc-url))]
    (when properties  (do (assert (map? properties))
                          (doseq [[k v] properties]
                            (doto datasource
                              (.addConnectionProperty (as-str k) (as-str v))))))
    (when user        (doto datasource (.setUsername (as-str user))))
    (when username    (doto datasource (.setUsername (as-str username))))
    (when password    (doto datasource (.setPassword (as-str password))))
    (when val-query   (do (assert (string? val-query))
                          (doto datasource
                            (.setValidationQuery ^String val-query)
                            (.setTestOnBorrow  true)
                            (.setTestOnReturn  true)
                            (.setTestWhileIdle true))))
    (when min-idle    (do (assert (integer? min-idle))
                          (assert (pos? min-idle))
                          (doto datasource (.setMinIdle min-idle))))
    (when max-idle    (do (assert (integer? max-idle))
                          (assert (pos? max-idle))
                          (doto datasource (.setMaxIdle max-idle))))
    (when max-active  (do (assert (integer? max-active))
                          (assert (pos? max-active))
                          (doto datasource (.setMaxActive max-active))))
    (when pool-pstmt? (do (assert (true? pool-pstmt?))
                          (doto datasource (.setPoolPreparedStatements true))))
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


(defn make-datasource
  "Create datasource from a given option-map. Some examples are below:
  (make-datasource :derby {:target :memory :database :emp})            ;; embedded databases
  (make-datasource :mysql {:host :localhost :database :emp
                           :username \"root\" :password \"s3cr3t\"})   ;; standard OSS databases
  (make-datasource :jdbc  {:jdbc-url   \"jdbc:mysql://localhost/emp\"
                           :class-name \"com.mysql.Driver\"})          ;; JDBC arguments
  (make-datasource :odbc  {:dsn :sales_report})                        ;; ODBC connections
  (make-datasource :jndi  {:context \"whatever\"})                     ;; JNDI connections
  (make-datasource {:adapter :pgsql :host :localhost :database :emp
                    :username :foo :password :bar})                    ;; :adapter in opts
  (make-datasource {:adapter :odbc-lite :dsn :moo})                    ;; ODBC-lite (MS-Access, MS-Excel etc.)
  (make-datasource {:class-name 'com.mysql.Driver
                    :jdbc-url   \"jdbc:mysql://localhost/emp\"})       ;; JDBC is default adapter"
  ([adapter opts] {:pre [(keyword? adapter)]}
     (if (= adapter :jndi) (do (assert (contains? opts :context))
                               (assert (string?   (get opts :context)))
                               (jndi-datasource   (:context opts)))
         (let [e-opts (merge opts (-> opts
                                      (assoc :adapter adapter)
                                      adap/defaults))]
           (if (:lite? e-opts) (lite-datasource e-opts)
               (jdbc-datasource e-opts)))))
  ([opts] {:pre [(map? opts)]}
     (let [adapter (or (:adapter opts)
                       (when (contains? opts :subprotocol) :subprotocol)
                       (when (contains? opts :dsn)         :odbc)
                       :jdbc)]
       (make-datasource adapter opts))))
