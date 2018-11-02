# ex

[![cljdoc badge](https://cljdoc.xyz/badge/cc.qbits/ex)](https://cljdoc.xyz/d/cc.qbits/ex/CURRENT)

An exception library, drop in replacement for `try`/`catch`/`finally`,
that adds support for `ex-info`/`ex-data` with a custom (clojure)
hierarchy that allows to express exceptions relations.

So we have `qbits.ex/try+`, which supports vanilla `catch`/`finally`
clauses.
If you specify a `catch-data` clause with a keyword as first argument
things get interesting. We assume you always put a `:type` key in the
ex-info you want to use with this, and will match its value to the
value of the key in the `catch-data` clause.

Essentially `catch-data` takes this form:

``` clj
(catch-data :something m
   ;; where m is a binding to the ex-data (you can destructure at that level as well)
   )
```

So you can do things like that.

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


But there's a twist.

I thought leveraging a clojure hierarchy could make sense in that
context too, so you can essentially create exceptions hierachies
without having to mess with Java classes directly and in a
clojuresque" way.

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
ex-data we extract, it's under the `:qbits.ex/exception` key.

Some real life examples of usage for this:

* make some exceptions end-user exposable in http responses via an
  error middleware in a declarative way .

* skip sentry logging for some kind of exceptions (or the inverse)

* make an exception hierachy for our query language type of errors for
  specialized reporting per "type"

Other than that it's largely inspired by
[catch-data](https://github.com/gfredericks/catch-data), the
implementation is slightly different, we dont catch Throwable, we
instead generate a catch clause on clj `ex-info` and generate a cond
that tries to match ex-data with the :type key using `isa?` with our
hierarchy, which arguably is closer to I would write by hand in that
case.

## Installation

ex is [available on Clojars](https://clojars.org/cc.qbits/ex).

Add this to your dependencies:


[![Clojars Project](https://img.shields.io/clojars/v/cc.qbits/ex.svg)](https://clojars.org/cc.qbits/ex)


or you can just grab it via `deps.edn` directly

<!-- Please check the -->
<!-- [Changelog](https://github.com/mpenet/ex/blob/master/CHANGELOG.md) -->
<!-- if you are upgrading. -->

## License

Copyright © 2018 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
