# clj-dbcp

Clojure wrapper for Apache DBCP to create JDBC connections pools.

This code is rewritten from scratch to clean up the older versions residing
here: https://bitbucket.org/kumarshantanu/clj-dbcp

The supported databases are:
* Supported and tested
  * Apache Derby, Axion, HSQLDB, H2, Mckoi, SQLite
* Supported but not tested
  * Regular ODBC DSN, Lite ODBC DSN (eg. MS-Excel workbooks)
  * CUBRID, Firebird, MySQL, MonetDB, PostgreSQL
  * IBM DB2, jTDS (SQL Server, Sybase), Oracle, SapDB, SQLServer, Sybase


## Usage

_This library is not on Clojars yet._

Include dependencies as `[clj-dbcp "0.7.0-SNAPSHOT"]`.

The recommended way to create a datasource is to call the
`clj-dbcp.core/make-datasource` function, for example:

```clojure
(make-datasource {:adapter :mysql :host 'localhost :database 'empdb})
```

or,

```clojure
(make-datasource :mysql {:host 'localhost :database 'empdb})
```

Sections below describe which of the keys are applicable to various databases:

### Generic JDBC connections

| `:adapter`     | Required keys            | Desired keys |
|----------------|--------------------------|--------------|
| `:jdbc`        | `:classname` `:jdbc-url` | `:val-query` |
| `:subprotocol` | `:classname` `:subname`  | `:val-query` |


### Prebuilt (JNDI, ODBC) connections

| `:adapter`   | Required keys | Optional keys |
|--------------|---------------|---------------|
| `:jndi`      | `:context`    |               |
| `:odbc`      | `:dsn`        | `:lite?`      |
| `:odbc-lite` | `:dsn`        |               |


### Open Source embedded databases

| Database | `:adapter` | `:target`  | Required keys           | Optional keys |
|----------|------------|------------|-------------------------|---------------|
| Axion    | `:axiondb` | `:memory`  | `:database`             |               |
|          |            | `:filesys` | `:database` `:db-path`  |               |
| Derby    | `:derby`   | `:memory`  | `:database`             |               |
|          |            | `:filesys` | `:database`             |               |
|          |            |`:classpath`| `:database`             |               |
|          |            | `:jar`     | `:jar-path` `:database` |               |
|          |            | `:network` | `:host` `:database`     | `:port`       |
| H2       | `:h2`      | `:memory`  | `:database`             |               |
|          |            | `:filesys` | `:database`             |               |
|          |            | `:network` | `:host` `:database`     | `:port`       |
| HSQLDB   | `:hsqldb`  | `:memory`  | `:database`             |               |
|          |            | `:filesys` | `:database`             |               |
|          |            | `:network` | `:host` `:database`     | `:port`       |
| Mckoi    | `:mckoi`   | `:filesys` | `:database`             |               |
| SQLite   | `:sqlite`  | `:memory`  |                         |               |
|          |            | `:filesys` | `:database`             |               |


### Open Source drivers, network connections

| Database          | `:adapter`        | Required keys       | Optional keys |
|-------------------|-------------------|---------------------|---------------|
| CUBRID            | `:cubrid`         | `:host` `:database` | `:port`       |
| Firebird          | `:firebird`       | `:host` `:database` | `:port`       |
| SQL Server (jTDS) | `:jtds-sqlserver` | `:host` `:database` | `:port`       |
| Sybase (jTDS)     | `:jtds-sybase`    | `:host` `:database` | `:port`       |
| MonetDB           | `:monetdb`        | `:host` `:database` | `:port`       |
| MySQL             | `:mysql`          | `:host` `:database` | `:port`       |
| PostgreSQL        | `:postgresql`     | `:host` `:database` | `:port`       |


### Proprietary drivers, network connections

| Database   | `:adapter`   | Required keys                    | Optional keys |
|------------|--------------|----------------------------------|---------------|
| IBM DB2    | `:db2`       | `:host` `:database`              | `:port`       |
| Oracle     | `:oracle`    | `:host` `:database`/`:system-id` | `:port`       |
| SapDB      | `:sapdb`     | `:host` `:database`              | `:port`       |
| SQL Server | `:sqlserver` |                                  | `:host` `:instance` `:port` |
| Sybase     | `:sybase`    | `:host`                          | `:port` `:database` |:


### Example

A typical CRUD example using Derby database is below:

```clojure
(ns example.app
  (:require [clj-dbcp.core     :as dbcp]
            [clojure.java.jdbc :as sql]))

(def db-derby  ;; an in-memory database instance
  {:datasource
   (dbcp/make-datasource
     {:adapter :derby :target :memory :database :empdb})})

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
$ lein2 dev test
```

Testing across several versions of Clojure:

```bash
$ lein2 all test
```


## Getting in touch

On GMail: [kumar.shantanu(at)gmail.com](mailto:kumar.shantanu@gmail.com)

On Twitter: [@kumarshantanu](https://twitter.com/kumarshantanu)


## License

Copyright © 2012 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
