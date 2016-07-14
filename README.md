# clj-dbcp

Clojure wrapper for Apache DBCP2 to create JDBC connections pools.


## Usage

On Clojars: https://clojars.org/clj-dbcp

Leiningen coordinates: `[clj-dbcp "0.9.0-SNAPSHOT"]` (supports Clojure 1.5 through Clojure 1.9)

The recommended way to create a datasource is to call the
`clj-dbcp.core/make-datasource` function, for example:

```clojure
(make-datasource {:classname "com.mysql.jdbc.Driver"
                  :jdbc-url "jdbc:mysql://localhost/empdb"
                  :username "empuser"
                  :password "s3cr3t"
                  :test-query "SELECT 1;"})
```

You can also parse a database URL (Heroku style) and use to create datasource:

```clojure
(make-datasource (parse-url "postgres://foo:bar@heroku.com:5489/hellodb"))
```

or,

```clojure
(make-datasource (parse-url (System/getenv "DATABASE_URL")))
```

Sections below describe which of the keys are applicable to various databases:


### JDBC parameters

Required: `:classname` (string), `:jdbc-url` (string)
Optional: `:test-query` (string)


### Optional keys for all JDBC connections

| Keyword arg       | Meaning                                | Default/special |
|-------------------|----------------------------------------|-----------------|
| `:properties`     | Map of property names and values       |                 |
| `:user`           | Database username                      |                 |
| `:username`       | Database username, same as `:user`     |                 |
| `:password`       | Database password                      |                 |
| `:test-query`      | Validation query                       | As per `:target`|
| `:init-size`      | Initial size of connection pool (int)  |                 |
| `:min-idle`       | Minimum idle connections in pool (int) |                 |
| `:max-idle`       | Maximum idle connections in pool (int) |                 |
| `:max-active`     | Maximum active connections in pool (int) |  -ve=no limit |
| `:pool-pstmt?`    | Whether to pool prepared statements    | true            |
| `:max-open-pstmt` | Maximum open prepared statements (int) |                 |
| `:remove-abandoned?`    | Whether to remove abandoned connections  | true        |
| `:remove-abandoned-timeout-seconds` | Timeout in seconds (int)     | 300         |
| `:log-abandoned?`       | Whether to log abandoned connections     | true        |
| `:lifo-pool?`           | Whether Last-In-First-Out (LIFO) or not  | false       |
| `:test-while-idle?`     | Whether validate the idle connections    | true        |
| `:test-on-borrow?`      | Whether validate connections on borrow   | true        |
| `:test-on-return?`      | Whether validate connections on return   | true        |
| `:test-query-timeout`   | Timeout (seconds) for validation queries |             |
| `:millis-between-eviction-runs`     | Millis to sleep between evicting unused connections | `-1` |
| `:min-evictable-millis` | Millis an object may sit idle before it is evicted              | `1800000` |
| `:tests-per-eviction`   | No. of connections to test during each eviction run             | `3` |
| `:cache-state?`         | Whether to cache state                   |      true   |


### Generic JDBC connections

| `:adapter`     | Required keys            | Desired keys |
|----------------|--------------------------|--------------|
| `:jdbc`        | `:classname` `:jdbc-url` | `:test-query` |


### JNDI connections

You can open a JNDI datasource (unlike the JDBC datasource) as follows:

```clojure
(make-datasource :jndi {:context "java:comp/env/myDataSource"})
```

or,

```clojure
(jndi-datasource "java:comp/env/myDataSource")
```


### Example

A typical CRUD example using Derby database is below:

```clojure
(ns example.app
  (:require [clj-dbcp.core     :as dbcp]
            [clojure.java.jdbc :as sql]))

(def db-derby  ;; an in-memory database instance
  {:datasource
   (dbcp/make-datasource
     {"org.apache.derby.jdbc.EmbeddedDriver"
     "jdbc:derby:memory:foo;create=true;"
     "values(1)"})})

(defn crud
  []
  (let [table :emp
        orig-record {:id 1 :name "Bashir" :age 40}
        updt-record {:id 1 :name "Shabir" :age 50}
        drop-table  #(sql/do-commands "DROP TABLE emp")
        retrieve-fn #(sql/with-query-results rows
                      ["SELECT * FROM emp WHERE id=?" 1]
                      (first rows))]
    (sql/with-connection db-derby
      ;; drop table if pre-exists
      (try (drop-table)
        (catch Exception _)) ; ignore exception
      ;; create table
      (sql/do-commands
        "CREATE TABLE emp (id INTEGER, name VARCHAR(50), age INTEGER)")
      ;; insert
      (sql/insert-values table (keys orig-record) (vals orig-record))
      ;; retrieve
      (println (retrieve-fn))
      ;; update
      (sql/update-values table ["id=?" 1] updt-record)
      ;; drop table
      (drop-table))))
```


## Development Notes

You need Java 7 and Leiningen 2 to build this code. Testing JDBC-ODBC bridge
driver requires that you use a Windows machine with ODBC DSNs configured.

Starting up the swank server (if you are going to work using Emacs/Slime):

```bash
$ lein2 dev swank
```

Testing against the dev version:

```bash
$ ./run-tests
```

Testing across several versions of Clojure:

```bash
$ ./run-tests.sh all
```


## Contributors

* Shantanu Kumar (author)
* Greg V (https://github.com/myfreeweb)


## Getting in touch

On GMail: [kumar.shantanu(at)gmail.com](mailto:kumar.shantanu@gmail.com)

On Twitter: [@kumarshantanu](https://twitter.com/kumarshantanu)


## License

Copyright © 2012-2013 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
