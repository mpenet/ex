(ns qbits.ex
  (:refer-clojure :exclude [derive underive ancestors descendants isa? parents]))

(defonce hierarchy (atom (make-hierarchy)))

(defn derive
  "Like clojure.core/derive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/derive tag parent))

(defn underive
  "Like clojure.core/underive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/underive tag parent))

(defn ancestors
  "Like clojure.core/ancestors but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/ancestors @hierarchy tag))

(defn descendants
  "Like clojure.core/descendants but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/descendants @hierarchy tag))

(defn parents
  "Like clojure.core/parents but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/parents @hierarchy tag))

(defn isa?
  "Like clojure.core/isa? but scoped on our ex-info type hierarchy"
  [child parent]
  (clojure.core/isa? @hierarchy child parent))

(defn find-clause-fn
  [pred]
  (fn [x]
    (and (seq? x)
         (pred (first x)))))

(def catch-clause? (find-clause-fn #{'catch 'finally}))
(def catch-data-clause? (find-clause-fn #{'catch-data}))

(defmacro try+
  "Like try but with support for ex-info/ex-data.

  If you pass a `catch-ex-info` form it will try to match an
  ex-info :type key, or it's potential ancestors in the local hierarchy.

  ex-info clauses are checked first, in the order they were specified.
  catch-ex-info will take as arguments a :type key, and a binding for
  the ex-data of the ex-info instance.

  (try
    [...]
    (catch-ex-info ::something my-ex-data
      (do-something my-ex-info))
    (catch-ex-info ::something-else {:as my-ex-data :keys [foo bar]}
      (do-something foo bar))
    (catch Exception e
      (do-something e))
    (catch OtherException e
      (do-something e))
    (finally :and-done))

  You can specify normal catch clauses for regular java errors and/or
  finally these are left untouched.

  There is no
  "
  {:style/indent 2}
  [& xs]
  (let [[body mixed-clauses]
        (split-with (complement (some-fn catch-clause? catch-data-clause?))
                    xs)
        clauses (filter catch-clause? mixed-clauses)
        ex-info-clauses (filter catch-data-clause? mixed-clauses)
        type-sym (gensym "ex-type-")
        data-sym (gensym "ex-data-")]
    `(try
       ~@body
       ~@(cond-> clauses
           (seq ex-info-clauses)
           (conj `(catch clojure.lang.ExceptionInfo e#
                    (let [~data-sym (vary-meta (ex-data e#)
                                               assoc ::exception e#)
                          ~type-sym (:type ~data-sym)]
                      (cond
                        ~@(mapcat (fn [[_ type binding & body]]
                                    `[(isa? ~type-sym ~type)
                                      (let [~binding ~data-sym] ~@body)])
                                  ex-info-clauses)
                        :else
                        ;; rethrow ex-info with other clauses since we
                        ;; have no match
                        (try (throw e#)
                             ~@clauses)))))))))

#_(clojure.pprint/pprint
   ;; do

   (macroexpand-1 '(try+
                    (prn "body1")
                    (prn "body2")
                    (catch-data :1 ex1
                                (prn :fo1)
                                (prn :bar1))

                    (catch-data :2 ex2
                                (prn :fo2)
                                (prn :bar2))

                    (catch Exception e
                      :meh)
                    (finally :final))))
