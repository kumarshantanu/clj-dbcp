(ns clj-dbcp.dbserver-util
  "Functions to start and stop embedded database servers."
  (:import java.io.PrintWriter)
  (:require
    [clj-dbcp.test-util :as tu]))


(defn msg
  [s]
  (println (format "\n*** %s ***" (str s))))


(defn start-derby-server
  "Start the Derby database server on a network port. The default host is
  localhost and the default port is 1527.
  See also:
    http://publib.boulder.ibm.com/infocenter/cscv/v10r1/index.jsp?topic=/com.ibm.cloudscape.doc/radminembeddedserverex.html
    Short link: http://j.mp/axA6t6"
  ([^String host ^Integer port]
    (let [server (org.apache.derby.drda.NetworkServerControl.
                   ^java.net.InetAddress (java.net.InetAddress/getByName host)
                   port)]
      (msg (format "Starting Derby Server at %s:%d" host port))
      (.start server
        (PrintWriter. (System/out)))
      server))
  ([port]
    (start-derby-server "localhost" port))
  ([]
    (start-derby-server "localhost" 1527)))


(defn stop-derby-server
  "Stop the Derby server"
  [^org.apache.derby.drda.NetworkServerControl server]
  (.shutdown server))


(defn start-h2-server
  "Start the H2 database server on a network port. The default host is
  localhost and the default port is 9092.
  See also:
    http://www.h2database.com/javadoc/org/h2/tools/Server.html#createTcpServer_String...
    http://www.h2database.com/html/tutorial.html"
  ([port]
    (let [server (org.h2.tools.Server/createTcpServer
                   (into-array
                     ["-tcp" "-tcpAllowOthers" "-tcpPort" (str port)]))]
      (msg (format "Starting H2 Server at port %d" port))
      (.start server)
      server))
  ([]
    (start-h2-server 9092)))


(defn stop-h2-server
  "Stop the H2 server"
  [^org.h2.tools.Server server]
  (.stop server))


(defn start-hsql-server
  "Start the HSQL database server on a network port. The default host is
  localhost and the default port is 9001.
  See also:
    http://www.hsqldb.org/doc/src/org/hsqldb/Server.html"
  ([host port dbnames]
    (let [dbnames-vec (tu/as-vector dbnames)
          dbnames-cnt (take (count dbnames-vec) (iterate #(inc %) 0))
          dbnames-map (zipmap dbnames-cnt dbnames-vec)
          server (org.hsqldb.Server.)]
      (msg (format "Starting HSQL Server at %s:%d %s" host port
             (str dbnames-vec)))
      (doall
        (map #(doto server
                (.setDatabaseName (first %) (last %))
                (.setDatabasePath (first %) (last %)))
          (seq dbnames-map)))
      (doto server
        (.setAddress host)
        (.setPort port)
        (.setSilent false)
        (.setErrWriter (PrintWriter. (System/err)))
        (.setLogWriter (PrintWriter. (System/out)))
        (.setNoSystemExit true)
        (.setRestartOnShutdown false)
        (.start))
      server))
  ([port dbnames]
    (start-hsql-server "localhost" port dbnames))
  ([dbnames]
    (start-hsql-server "localhost" 9001 dbnames)))


(defn stop-hsql-server
  "Stop the HSQL server"
  [^org.hsqldb.Server server]
  (msg "Stopping HSQL Server")
  (.shutdown server))


(defn setup-mckoi-local-database
  "Setup the Mckoi local database. By default the config is looked up in
  ./db.conf (we override as ./test/mckoi.conf) and database in ./data dir.
  See also: http://mckoi.com/database/GettingStarted.html"
  []
  (let [args (into-array ["-conf" "./test/mckoi.conf" "-create" "sa" "pw"])]
    (com.mckoi.runtime.McKoiDBMain/main args)))
