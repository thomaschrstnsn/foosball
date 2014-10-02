# foosball

[![Build Status](https://travis-ci.org/thomaschrstnsn/foosball.svg?branch=master)](https://travis-ci.org/thomaschrstnsn/foosball)

Web application for tracking foosball match results and statistics.

But most of all, a playground for  learning web development with 
[Clojure](http://clojure.org), [ClojureScript](https://github.com/clojure/clojurescript) and 
[Datomic](http://www.datomic.com).

Most of the concepts discovered/used/copied here have been crystalized into the 
[gyag](https://github.com/thomaschrstnsn/gyag-template) Leiningen template.

# Status

Running in [production](http://foosball.chrstnsn.dk) since april 2013. 
If you can use this as a basis for your own site, go ahead.  
This codebase has no intensions of becoming the *all singing, all dancing, generic foosball site*, 
pull requests will be considered in this light.

# Running a development environment

1. You need an instance of Datomic running. 
I can recommend the wrapper [cldwalker/datomic-free](https://github.com/cldwalker/datomic-free).

2. To run server side, see the [gyag template README](https://github.com/thomaschrstnsn/gyag-template).

3. Continuous running/live updating client-side: `lein figwheel dev` (occupies a terminal tab)

4. Continuous server side test runner: `lein test-refresh` (occupies a terminal tab)

5. Continuous client side tests in headless phantom-js (required local install): `lein auto-cljs` (occupies a terminal tab)

## License

Copyright © 2014 Thomas Christensen

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
