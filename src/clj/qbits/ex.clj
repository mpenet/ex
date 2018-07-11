(ns qbits.ex
  (:refer-clojure :exclude [derive ancestors]))

(defonce hierarchy (atom (make-hierarchy)))

(defn derive
  "Like clojure.core/derive but scoped on our ex-info type hierarchy"
  [tag parent]
  (swap! hierarchy
         clojure.core/derive tag parent))

(defn ancestors
  "Like clojure.core/ancestors but scoped on our ex-info type hierarchy"
  [tag]
  (clojure.core/ancestors @hierarchy tag))

(defn find-clause-fn
  [pred]
  (fn [x]
    (and (seq x)
         (pred (first x)))))

(def catch-clause? (find-clause-fn #{'catch 'finally}))
(def catch-data-clause? (find-clause-fn #{'catch-data}))

(defn gen-bindings [x y body]
  `(let [~x ~y]
     ~@body))

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
        catch-p clauses
        type-sym (gensym "type_")
        data-sym (gensym "data_")]
    `(try
       ~@body
       ~@(cond-> clauses
           (seq ex-info-clauses)
           (conj `(catch clojure.lang.ExceptionInfo e#
                    (let [~data-sym (vary-meta (ex-data e#)
                                               assoc ::exception e#)
                          ~type-sym (:type ~data-sym)]
                      (cond
                        ;; we need to gen conditions for clauses
                        ;; twice, once to catch precise types, then
                        ;; to get potential ancestors in the hierarchy
                        ~@(concat
                           ;; first pass we try to catch specific types
                           (mapcat (fn [[_ type binding & body]]
                                     [`(= ~type-sym ~type)
                                      (gen-bindings binding data-sym body)])
                                   ex-info-clauses)
                           ;; second pass we try to get potential ancestors
                           (mapcat (fn [[_ type binding & body]]
                                     [`(isa? @hierarchy ~type-sym ~type)
                                      (gen-bindings binding data-sym body)])
                                   ex-info-clauses))
                        :else
                        ;; rethrow ex-info with other clauses since we
                        ;; have no match
                        (try (throw e#)
                             ~@clauses)))))))))


;; (clojure.pprint/pprint
;;  ;; do

;;  (macroexpand-1 '(try+
;;                      (prn "body1")
;;                      (prn "body2")
;;                    (catch-data :1 ex1
;;                                   (prn :fo1)
;;                                   (prn :bar1))

;;                    (catch-data :2 ex2
;;                                   (prn :fo2)
;;                                   (prn :bar2))

;;                    (catch Exception e
;;                      :meh)
;;                    (finally :final))))
