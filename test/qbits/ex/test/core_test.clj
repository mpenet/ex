(ns qbits.ex.test.core-test
  (:use clojure.test)
  (:require [qbits.ex :as ex]))


(defmacro try-val
  [& body]
  `(try
    ~@body
    (catch Exception e#
      e#)))


(deftest test-foo
  (let [d {:type :foo}]
    ;; match
    (is (= d
           (ex/try+
            (throw (ex-info "asdf" d))
            (catch :foo x
                           x))))

    ;; no match but still ex-info
    (is (= {:type :asdf}
           (ex-data (try-val
                     (ex/try+
                      (throw (ex-info "asdf" {:type :asdf}))
                      (catch :foo x
                                     x))))))

    (is (instance? Exception
                   (try-val
                    (ex/try+
                     (throw (Exception. "boom"))
                     (catch :foo x
                                    x)))))))

(deftest test-inheritance
  (ex/derive :bar :foo)
  (let [d {:type :bar}]
    (is (-> (ex/try+
             (throw (ex-info "" d))
             (catch :foo ex
                            (= ex d))))))
  (ex/derive :baz :bar)
  (let [e {:type :baz}]
    (is (-> (ex/try+
             (throw (ex-info "" e))
             (catch :foo ex
                            (= ex e))))))

  (let [e {:type :bak}]
    (is (-> (try-val (ex/try+
              (throw (ex-info "" e))
              (catch :foo ex
                             (= e ex))))))))

(deftest test-bindings
  (is (ex/try+
       (throw (ex-info "" {:type :foo
                           :bar 1}))
       (catch :foo {:keys [bar]}
                      (= bar 1)))))

;; (prn (ex-info "" {:type :ex-with-meta}))

(deftest complex-meta
  (let [x (ex-info "" {:type :ex-with-meta})]
    (is (ex/try+
         (throw x)
         (catch :ex-with-meta
                        x'
                        (-> x' meta ::ex/exception (= x)))))))

;; (run-tests)
