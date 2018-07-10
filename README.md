# ex

warning: ex is experimental at this point

Yet another exception library with support for `ex-info`.

`slingshot` is a bit too heavyweight to my taste, `catch-data` is
quite nice but catches `Throwable` to do the work, and I thought
leveraging a clojure hierarchy could make sense in that context too (I
like these lately).

So we have `ex/try+`, which supports vanilla catch/finally clauses.

If you specify a `catch` clause with a keyword as first argument
things get intresting.

We start with the assumption that the ex-data map contains a `:type`
key, as we think it's generally a good practise to do so. That key
should contain a keyword (namespaced or not) and we should be able to
match the right clause from it, or just throw the ex-info if there's
no match.

``` clj

(require '[qbits.ex :as ex])

(ex/try+

  (something that will throw)

  (catch ::foo data
    (prn :got-ex-data data))

  (catch ::bar {:as data :keys [foo]}
    (prn :got-ex-data-again foo))

  (catch ExceptionInfo e
   ;; this would match an ex-info that didn't get a hit with catch-ex-info)

  (catch Exception e (prn :boring))

  (finally (prn :boring-too)))

```


Then we have an internal hierarchy, so you can do things like that:

``` clj
;; so bar is a foo

(ex/derive ::bar ::foo)

(ex/try+
  (throw (ex-info "I am a bar" {:type ::bar})
  (catch ::foo d
    (prn "got a foo with data" d))))

```

You can also get the full exception instance via the metadata on the
ex-data we extract, it's under the :qbits.ex/exception key.


<!-- ## Installation -->

<!-- ex is [available on Clojars](https://clojars.org/cc.qbits/ex). -->

<!-- Add this to your dependencies: -->


<!-- [![Clojars Project](https://img.shields.io/clojars/v/cc.qbits/ex.svg)](https://clojars.org/cc.qbits/ex) -->


<!-- Please check the -->
<!-- [Changelog](https://github.com/mpenet/ex/blob/master/CHANGELOG.md) -->
<!-- if you are upgrading. -->

## License

Copyright © 2018 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
