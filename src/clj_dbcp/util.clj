(ns clj-dbcp.util
  (:require [clojure.string :as str]))


(defn as-str
  "Convert `x` to string and return it"
  [x]
  (if (or (keyword? x) (symbol? x))
    (name x)
    (str x)))


(defn as-lkey
  [x]
  (-> x as-str str/lower-case keyword))


(defn as-vector
  [x]
  (if (coll? x) (into [] x)
      [x]))
