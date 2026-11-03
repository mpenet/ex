(ns s-exp.ex.test.core-test
  (:use clojure.test)
  (:require
   [clojure.spec.test.alpha]
   [s-exp.ex :as ex]
   [s-exp.ex.test :as t]))

(clojure.spec.test.alpha/instrument)

(defmacro try-val
  [& body]
  `(try
     ~@body
     (catch Exception e#
       e#)))

(deftest test-catch
  (let [d {:type ::foo}]
    (is (= d
           (ex/try+
             (throw (ex-info "asdf" d))
             (catch ::foo x
               x))))

    (is (= d
           (ex/try+
             (throw (ex-info "asdf" {:type ::foo}))
             (catch ::foo x
               x)))
        "ensure legacy format works")

    (is (true?
         (ex/try+
           true
           (catch ::foo x
             x))))

    ;; no match but still ex-info
    (is (= {:type ::asdf}
           (ex-data (try-val
                     (ex/try+
                       (throw (ex-info "asdf" {:type ::asdf}))
                       (catch ::foo x
                         x))))))

    (is (instance? Exception
                   (try-val
                    (ex/try+
                      (throw (Exception. "boom"))
                      (catch ::foo x
                        x)))))))

(deftest test-catch
  (let [d {:type ::foo}]
    (is (= d
           (ex/catch (ex-info "asdf" d) ::foo
             identity
             (fn [] ::err))))

    ;; no match
    (is (= {:type ::asdf}
           (ex-data (ex/catch (ex-info "asdf" {:type ::asdf}) ::foo
                      (constantly false)
                      identity))))
    (is (instance? Exception
                   (ex/catch (Exception. "boom") ::foo #(throw %)
                             identity)))))

(deftest test-inheritance
  (ex/derive ::bar ::foo)
  (let [d {:type ::bar}]
    (is (ex/try+
          (throw (ex-info "" d))
          (catch ::foo ex
            (= ex d)))))

  (ex/derive ::baz ::bar)
  (let [e {:type ::baz}]
    (is (ex/try+
          (throw (ex-info "" e))
          (catch ::foo ex
            (= ex e)))))

  (let [e {:type ::bak}]
    (is (try-val (ex/try+
                   (throw (ex-info "" e))
                   (catch ::foo ex
                     (= e ex)))))))

(deftest test-bindings
  (is (ex/try+
        (throw (ex-info "" {:type ::foo
                            :bar 1}))
        (catch ::foo {:keys [bar]}
          (= bar 1)))))

(deftest special-binding
  (ex/try+
    (throw (ex-info "" {:type ::foo
                        :bar 1}))
    (catch ::foo {:as e}
      (let [e &ex]
        (ex/try+
          (throw (ex-info "" {:type ::bar}))
          (catch ::bar b
            (is (not= &ex e))
            (is (instance? Exception &ex))
            (is (instance? Exception e))))))))

(deftest test-complex-meta
  (let [x (ex-info "" {:type ::ex-with-meta})]
    (is (ex/try+
          (throw x)
          (catch ::ex-with-meta
                 x'
            (-> x' meta ::ex/exception (= x)))))))

(deftest test-within-eval
  (is (= 1 (eval `(do (ex/try+
                        (throw (ex-info "boom" {:type ::bar}))
                        (catch ::bar e# 1)))))))

(deftest test-thrown-ex-data
  (is (thrown-ex-info-type? ::foo (throw (ex/ex-info "bar" ::foo)))))

(deftest test-type
  (is (ex/type? (ex/ex-info "bar" ::foo)
                ::foo))

  (is (not (ex/type? (ex/ex-info "bar" ::foo22)
                     ::bar))))
