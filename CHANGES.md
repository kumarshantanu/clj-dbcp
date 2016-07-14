# Release History and TODO


## 0.9.0 / 2016-July-??

* Drop support for JDK 1.5 (now Java 6 or higher is required)
* Drop support for Clojure `1.2` through `1.4` (for code cleanup)
* [TODO] Drop support for DSL API for JDBC params (use Cumulus instead)
* [TODO] Drop support for `:val-query` option (use `:test-query` instead)
* [TODO] Use Apache DBCP2 instead of DBCP1.x
  * [TODO] API options for DBCP2


## 0.8.2 / 2016-July-14

* Refactor DSL for constructing basic JDBC params
  * Extract the DSL into [Cumulus](https://github.com/kumarshantanu/cumulus)
  * Use Cumulus as dependency for DSL support
  * Deprecate the DSL support (to be removed in a future release)
* Deprecate the `:val-query` option keyword
* Add support for `:test-query` (replacement for `:val-query`)
* Deprecate JDK 1.5 support (to be removed in a future release)
* Deprecate support for Clojure 1.2 through Clojure 1.4 (to be removed in a future release)


## 0.8.1 / 2013-February-16

* Don't lose query string when parsing URLs (Greg V)

## 0.8.0 / 2012-August-12

* Parse argument map from java.net.URI
* Parse argument map from String (Heroku compatibility)
* Comprehensive Oracle connection parameters


## 0.7.0 / 2012-July-30

* Move to Github
* Move to Leiningen 2 for builds
* Move to Eclipse license
* Refactor API
  * Drop properties file support
  * Drop per-database API functions
  * Adopt generic API based on keywords

