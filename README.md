# ex

warning: ex is experimental at this point

Yet another exception library with support for `ex-info`.

So we have `qbits.ex/try+`, which supports vanilla `catch`/`finally` clauses.

If you specify a `catch-data` clause with a keyword as first argument
things get interesting.

I thought leveraging a clojure hierarchy could make sense in that
context too (I like these lately), other than that it's largely
inspired by [catch-data](https://github.com/gfredericks/catch-data),
the implementation is slightly different, we dont catch Throwable, we
instead generate a catch clause on clj.exinfo and generate a cond
that tries to match ex-data with the :type key, which arguably is
closer to what you (or I?) would write by hand in that case.

We start with the assumption that the ex-data map contains a `:type`
key, as we think it's generally a good practice to do so. That key
should contain a keyword (namespaced or not) and we should be able to
match the right clause from it, or just throw the ex-info if there's
no match.

``` clj

(require '[qbits.ex :as ex])

(ex/try+

  (throw (ex-info "Argh" {:type ::bar :foo "a foo"}))

  (catch-data ::foo data
    (prn :got-ex-data data))

  (catch-data ::bar {:as data :keys [foo]}
    ;; in that case it would hit this one
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
  (catch-data ::foo d
    (prn "got a foo with data" d)
    (prn "Original exception instance is " (-> d meta ::ex/exception))))

```

You can also get the full exception instance via the metadata on the
ex-data we extract, it's under the :qbits.ex/exception key.


If you catch :foo and :foo is a potential ancestor of another previous
catch-data clause, the catch on :foo will prevail and be run. This somewhat is
similar to how java exceptions work.

So if you have


``` clj

(ex/derive ::bar ::foo)

[...]

(catch-data ::foo e [...])
(catch-data ::bar e [...])
```

That will expand to a cond with the following checks

``` clj
(= type ::foo)
(= type ::bar)
(isa? type ::foo)
(isa? type ::bar)
```

So ::bar will be run and not the isa? ::foo clause.

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
