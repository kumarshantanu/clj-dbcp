(ns clj-dbcp.adapter
  (:use [clj-dbcp.util :only (as-str as-vector)]))


(defn bad-arg
  [x] {:pre [(string? x)]}
  (throw (IllegalArgumentException. ^String x)))

(defn not-found
  [k opts]
  (bad-arg (format "Key '%s' not found in %s" k (pr-str opts))))

(defn not-match
  [k v valid]
  (bad-arg (format "Key '%s' value '%s' doesn't match a valid choice '%s'"
                   k v valid)))

(defn format-url
  "Return fn that when executed with options as argument, returns JDBC URL.
  Every key is a vector [k individual-format mandatory? match-regex] where the
  default values are [k \"%s\" :mandatory #\".*\"]. When only a key is passed
  the other values are deduced."
  [template & ks] {:pre [(string? template)]}
  (let [kset?     (fn [opts kset] {:pre [(map? opts) (set? kset)]}
                    (some (partial contains? opts) kset))
        get-k     (fn [opts kset] {:pre [(map? opts) (set? kset)]}
                    (first (some #(when (contains? opts %) [(get opts %)])
                                 kset)))
        as-set    #(if (set? %) (do (assert (seq %)) %) #{%})
        as-vector #(if (vector? %) (let [[k f m r d] %]
                                     [(as-set k) f m r d])
                       [(as-set %) "%s" :mandatory #".*" ""])]
    (fn [opts]
      (apply format template (->> ks
                                  (map as-vector)
                                  (map (fn [[kset fmt mandatory? regex default]]
                                         (when (and (not (kset? opts kset))
                                                    mandatory?)
                                           (not-found kset opts))
                                         (if-not (kset? opts kset)
                                           (str default)
                                           (let [v (as-str (get-k opts kset))
                                                 f (or fmt "%s")
                                                 r (or regex #".*")]
                                             (when-not (re-matches r v)
                                               (not-match kset v r))
                                             (format f v))))))))))


(def ^{:doc "shortcut to format-url"} R format-url)


(defn U
  "Same as `format-url`, except that it simply returns the :jdbc-url value if
  already provided. If not provided, calls `format-url` to compute the URL."
  [& args]
  (let [f-url (apply format-url args)]
    (fn [opts]
      (if (contains? opts :jdbc-url)
        (get opts :jdbc-url)
        (f-url opts)))))


(defn realize
  "Walk `x` recursively using `opts` as the only argument"
  [opts x]
  (cond (fn? x)   (x opts)
        (map? x) (zipmap (keys x) (map (partial realize opts) (vals x)))
        :else    x))


(defn matcher
  "Ensure that the map `opts` has key `k`, and match the value of `k` in `opts`
  as one of the keys in map `m` and return corresponding value. If return value
  is a function, invoke repeatedly with `opts` as argument until it is non-fn.
  `k` could be a vector [k default] implying `default` when `k` is unspecified."
  [k m]
  (fn [opts]
    (let [[k d] (as-vector k)]
      (when (and (not (contains? opts k))
                 (nil? d))
        (not-found k opts))
      (let [v (get opts k d)]
        (when-not (contains? m v) (not-match k v (keys m)))
        (realize opts (get m v))))))


(def matcher-adapter (partial matcher [:adapter :jdbc]))
(def matcher-target  (partial matcher [:target  :filesys]))


;; TODO Check this URL and add more:
;; http://www.redmountainsw.com/wordpress/2009/08/08/jdbc-connection-urls/
;; http://wbissi.wordpress.com/2009/06/28/jdbc-connection/


(def ^{:doc "DSL to generate matcher from a map of adapter to data elements"}
  defaults
  (matcher-adapter
   {;; JDBC & ODBC
    :jdbc           {:classname (R "%s" :classname)
                     :jdbc-url  (U "%s" :jdbc-url)}
    :subprotocol    {:classname (R "%s" :classname)
                     :jdbc-url  (U "jdbc:%s:%s" :subprotocol :subname)}
    :odbc           {:classname "sun.jdbc.odbc.JdbcOdbcDriver"
                     :jdbc-url  (U "jdbc:odbc:%s" :dsn)}
    :odbc-lite      {:classname "sun.jdbc.odbc.JdbcOdbcDriver"
                     :jdbc-url  (U "jdbc:odbc:%s" :dsn)
                     :lite?     true}
    ;; embedded
    :axiondb        {:classname "org.axiondb.jdbc.AxionDriver"
                     :jdbc-url  (matcher-target
                                  {:memory  (U "jdbc:axiondb:%s"  :database)
                                   :filesys (U "jdbc:axiondb:%s:%s" :database :db-path)})
                     :val-query "SELECT 1"
                     :username  "sa"
                     :password  ""}
    :derby          {:classname "org.apache.derby.jdbc.EmbeddedDriver"
                     :jdbc-url
                     (matcher-target
                      {:memory    (U "jdbc:derby:memory:%s;create=true;"    :database)
                       :filesys   (U "jdbc:derby:directory:%s;create=true;" :database)
                       :classpath (U "jdbc:derby:classpath:%s"              [:database "%s" :mandatory #"/.+"])
                       :jar       (U "jdbc:derby:jar:(%s)%s"                :jar-path :database)
                       :network   (U "jdbc:derby://%s%s/%s;create=true;"    :host [:port ":%s"] :database)})
                     :val-query "values(1)"
                     :username  "sa"
                     :password  "sx"}
    :h2             {:classname "org.h2.Driver"
                     :jdbc-url
                     (matcher-target
                      {:memory  (U "jdbc:h2:mem:%s"      :database)
                       :filesys (U "jdbc:h2:file:%s"     :database)
                       :network (U "jdbc:h2:tcp:%s%s/%s" :host [:port ":%s"] :database)})
                     :val-query  "SELECT 1"
                     :username   "sa"
                     :password   ""}
    :hsqldb         {:classname "org.hsqldb.jdbcDriver"
                     :jdbc-url
                     (matcher-target
                      {:memory  (U "jdbc:hsqldb:mem:%s"         :database)
                       :filesys (U "jdbc:hsqldb:file:%s"        :database)
                       :network (U "jdbc:hsqldb:hsql://%s%s/%s" :host [:port ":%s"] :database)})
                     :val-query "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS"
                     :username  "sa"
                     :password  ""}
    :mckoi          {:classname "com.mckoi.JDBCDriver"
                     :jdbc-url  (U "jdbc:mckoi:local://%s" :database)
                     :val-query "SELECT 1"}
    :sqlite         {:classname "org.sqlite.JDBC"
                     :jdbc-url
                     (matcher-target
                      {:memory  "jdbc:sqlite::memory:"
                       :filesys (U "jdbc:sqlite:%s" :database)})
                     :val-query "SELECT 1"
                     :username  "sa"
                     :password  ""}
    ;; network OSS
    :cubrid         {:classname "cubrid.jdbc.driver.CUBRIDDriver"
                     :jdbc-url  (U "jdbc:cubrid:%s%s:%s:::"       :host [:port ":%s"] :database)
                     :val-query "SELECT 1;"}
    :firebird       {:classname "org.firebirdsql.jdbc.FBDriver"
                     :jdbc-url  (U "jdbc:firebirdsql://%s%s/%s"   :host [:port ":%s"] :database)
                     :val-query "SELECT CAST(1 AS INTEGER) FROM rdb$database;"}
    :jtds-sqlserver {:classname "net.sourceforge.jtds.jdbc.Driver"
                     :jdbc-url  (U "jdbc:jtds:sqlserver://%s%s%s" :host [:port ":%s"] :database)
                     :val-query "select 1;"}
    :jtds-sybase    {:classname "net.sourceforge.jtds.jdbc.Driver"
                     :jdbc-url  (U "jdbc:jtds:sybase://%s%s%s"    :host [:port ":%s"] :database)
                     :val-query "select 1;"}
    :monetdb        {:classname "nl.cwi.monetdb.jdbc.MonetDriver"
                     :jdbc-url  (U "jdbc:monetdb://%s%s/%s"       :host [:port ":%s"] :database)
                     :val-query "SELECT 1;"}
    :mysql          {:classname "com.mysql.jdbc.Driver"
                     :jdbc-url  (U "jdbc:mysql://%s%s/%s"         :host [:port ":%s"] :database)
                     :val-query "SELECT 1;"}
    :postgresql     {:classname "org.postgresql.Driver"
                     :jdbc-url  (U "jdbc:postgresql://%s%s/%s"    :host [:port ":%s"] :database)
                     :val-query "SELECT version();"}
    ;; network proprietary
    :db2            {:classname "com.ibm.db2.jcc.DB2Driver"
                     :jdbc-url  (U "jdbc:db2://%s%s/%s"           :host [:port ":%s"] :database)
                     :val-query "select * from sysibm.SYSDUMMY1;"}
    :oracle         {:classname "oracle.jdbc.driver.OracleDriver"
                     :jdbc-url
                     (matcher [:style :system-id]
                              {:system-id    (U "jdbc:oracle:thin:@%s:%s:%s"
                                                :host [:port "%s" nil #".*" "1521"]
                                                #{:database :system-id})
                               :service-name (U "jdbc:oracle:thin:@//%s:%s/%s"
                                                :host [:port "%s" nil #".*" "1521"]
                                                #{:database :service-name})
                               :tns-name     (U "jdbc:oracle:thin:@%s"
                                                #{:database :tns-name})
                               :ldap         (U "jdbc:oracle:thin:@ldap://%s%s/%s,%s"
                                                :host [:port ":%s"]
                                                #{:database :system-id :service-name}
                                                :ldap-str)
                               :oci          (U "jdbc:oracle:oci:@%s"
                                                #{:database :tns-alias})
                               :oci8         (U "jdbc:oracle:oci8:@%s"
                                                #{:database :tns-alias})})
                     :val-query "SELECT 1 FROM DUAL"}
    :sapdb          {:classname "com.sap.dbtech.jdbc.DriverSapDB"
                     :jdbc-url  (U "jdbc:sapdb://%s%s/%d"          :host [:port ":%s"] :database)
                     :val-query "SELECT 1 FROM DUAL"}
    :sqlserver      {:classname "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                     :jdbc-url  (U "jdbc:sqlserver://%s%s%s"     [:host] [:instance "\\%s"] [:port ":%s"])
                     :val-query "SELECT 1"}
    :sybase         {:classname "com.sybase.jdbc2.jdbc.SybDriver"
                     :jdbc-url  (U "jdbc:sybase:Tds:%s%s%s"       :host [:port ":%s"] [:database "?ServiceName=%s"])
                     :val-query "SELECT 1"}
    ;; TODO Key-value databases
    }))
