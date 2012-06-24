(ns clj-dbcp.test-util
  "This file has some utility functions pulled from Clj-MiscUtil project. It
  makes sense to do so, so that Clj-DBCP doesn't need to depend on a certain
  version of Clj-MiscUtil."
  (:import
    (java.io      File)
    (java.util    List Map Properties)
    (javax.naming Binding Context InitialContext
                  NameClassPair NamingEnumeration)))


(defn sleep
  "Sleep for `millis` milliseconds"
  [millis]
  (try (Thread/sleep millis)
    (catch InterruptedException e
      (.interrupt (Thread/currentThread)))))


(defmulti relative-path-string 
  "Interpret a String or java.io.File as a relative path string. 
   Building block for clojure.contrib.java-utils/file."
  class)

(defmethod relative-path-string String [#^String s]
  (relative-path-string (File. s)))

(defmethod relative-path-string File [#^File f]
  (if (.isAbsolute f)
    (throw (IllegalArgumentException. (str f " is not a relative path")))
    (.getPath f)))

(defmulti #^File as-file 
  "Interpret a String or a java.io.File as a File. Building block
   for clojure.contrib.java-utils/file, which you should prefer
   in most cases."
  class)
(defmethod as-file String [#^String s] (File. s))
(defmethod as-file File [f] f)

(defn #^File file
  "Returns a java.io.File from string or file args."
  ([arg]                      
     (as-file arg))
  ([parent child]             
     (File. #^File (as-file parent) #^String (relative-path-string child)))
  ([parent child & more]
     (reduce file (file parent child) more)))


(defn read-properties
  "Read properties from file-able."
  [file-able]
  (with-open [f (java.io.FileInputStream. (file file-able))]
    (doto (Properties.)
      (.load f))))


(defn property-map
  "Transform a given Properties instance to a map."
  [^Properties properties]
  (let [ks (into [] (.stringPropertyNames properties))
        vs (into [] (map #(.getProperty properties %) ks))]
    (zipmap ks vs)))


(defn is-true?
  "Tell whether a given value is equivalent to true."
  [any]
  (cond
    (string? any)  (let [v (.toLowerCase ^String any)]
                     (or
                       (= "true" v)
                       (= "yes"  v)
                       (= "on"   v)))
    (keyword? any) (let [v (keyword (.toLowerCase ^String (name any)))]
                     (or
                       (= :true v)
                       (= :yes  v)
                       (= :on   v)))
    (number? any)  (> any 0)
    :else (true? any)))


(defn as-vector
  "Convert/wrap given argument as a vector."
  [anything]
  (if (vector? anything) anything
    (if (or (seq? anything) (set? anything)) (into [] anything)
      (if (map? anything) (into [] (vals anything))
        (if (nil? anything) []
          [anything])))))


(defn ^List coll-as-keys
  "Convert each element in a collection to keyword and return a vector."
  [ks]
  (as-vector (map keyword ks)))


(defn ^Map str-to-keys
  "Given a map with every key a string, convert keys to keywords.
  Input: {\"a\" 10 \"b\" \"20\"}
  Returns: {:a 10 :b \"20\"}"
  [m]
  (let [ks (keys m)
        vs (vals m)]
    (zipmap (coll-as-keys ks) vs)))


;; ===== JNDI functions =====


(def ^{:doc "Typically bound to javax.naming.Context"
       :dynamic true :tag Context}
      *root-context* nil)


(def ^{:doc "Typically bound to an integer wrapped in an atom, e.g. (atom 0)"
       :dynamic true}
      *indent* nil)


(defmacro with-root-context
  [root-context & body]
  `(do
    (assert (not (nil? ~root-context)))
    (assert (instance? Context ~root-context))
    (binding [*root-context* ~root-context]
      ~@body)))


(defn- increase-indent []
  (swap! *indent* #(+ % 4)))


(defn- decrease-indent []
  (swap! *indent* #(- % 4)))


(defn- print-entry
  [^NameClassPair next-elem]
  (let [indent-str (apply str
                     (take @*indent* (repeat " ")))]
    (if (nil? next-elem) (println indent-str "--> <nil>")
      (println indent-str "-->"
        (.getName next-elem)
        " (" (type next-elem) "->" (.getClassName next-elem) ")"))))


(declare do-print-jndi-tree)


(defn- print-ne
  [^NamingEnumeration ne ^String parent-ctx]
  (loop []
    (when (.hasMoreElements ne)
      (let [^NameClassPair next-elem (.nextElement ne)]
        (print-entry next-elem)
        (increase-indent)
        (if (or (instance? Context next-elem)
              (and (instance? NameClassPair next-elem)
                (instance? Context (.getObject ^Binding next-elem))))
          (do-print-jndi-tree
            (if (zero? (.length parent-ctx))
              (.getName next-elem)
              (str parent-ctx "/" (.getName next-elem))))
          (println "** Not drilling "
            (type (.getObject ^Binding next-elem))))
        (decrease-indent))
      (recur))))


(defn- do-print-jndi-tree
  [^String ct]
  (assert (not (nil? ct)))
  (if (instance? Context *root-context*)
    (print-ne (.list *root-context* ct) ct)
    (print-entry *root-context*)))


(defn print-jndi-tree
  "Print JNDI tree. You should have JNDI environment configured beforehand."
  ([^String ct]
    (binding [*indent* (atom 0)]
      (do-print-jndi-tree ct)))
  ([]
   (print-jndi-tree "")))


