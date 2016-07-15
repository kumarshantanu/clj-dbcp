# clj-dbcp

Clojure wrapper for Apache DBCP2 to create JDBC connections pools.


## Usage

On Clojars: https://clojars.org/clj-dbcp

Leiningen coordinates: `[clj-dbcp "0.9.0"]` (supports Clojure 1.5 through Clojure 1.9, Java 7 or higher)

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

Leiningen coordinates: [asphalt/asphalt "0.4.0"] [clj-dbcp "0.9.0"]

```clojure
(ns example.app
  (:require 
    [clj-dbcp.core     :as dbcp]
    [clojure.java.jdbc :as sql]
    [asphalt.core :as a]))

(def db-sql  ;; an in-memory database instance
  {:datasource
   (dbcp/make-datasource
     {:classname "com.mysql.jdbc.Driver" 
      :jdbc-url   "jdbc:mysql://localhost:3306/new_db" 
      :user "root" 
      :password "root"})})

(defn crud
  []
  (a/update db-sql "CREATE TABLE IF NOT EXISTS EMP (ID int, Name varchar (25), Age int)" [])
  (a/update db-sql
    "INSERT INTO emp (id, name, age) VALUES (?, ?, ?)"
    [1, "Bashir",40])
  (a/update db-sql
    "INSERT INTO emp (id, name, age) VALUES (?, ?, ?)"
    [2, "Shabir",50])
  (println (a/query a/fetch-rows
             db-sql
             "SELECT id, name, age FROM emp" []))
  (a/update db-sql
    "DROP TABLE EMP" []))
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
* Nandhitha R (https://github.com/nandhithar)


## Getting in touch

On GMail: [kumar.shantanu(at)gmail.com](mailto:kumar.shantanu@gmail.com)

On Twitter: [@kumarshantanu](https://twitter.com/kumarshantanu)


## License

Copyright © 2012-2016 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
