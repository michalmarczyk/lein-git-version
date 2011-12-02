# lein-git-version

A Leiningen plugin causing the jar and uberjar tasks to use `git
describe --tags` to determine the version string to be included in the
names of the produced artifacts. Additionally, it causes a file called
`version.txt` containing this version string to be created in the
project's resources directory.

## Usage

Add

    [lein-git-version "the-latest-version"]

to your `:dev-dependencies` or install it as a user plugin. Then add
`leiningen.hooks.git-version` to `:hooks` in your `project.clj` (or
set `:implicit-hooks` to true).

Using the `:hooks` setting in `~/.lein/init.clj` might become another
supported option in the future, but is probably not a great idea for
now.

## Fablo

This work was sponsored by Fablo (http://fablo.eu/). Fablo provides a
set of tools for building modern e-commerce storefronts. Tools include
a search engine, product and search result navigation, accelerators,
personalized recommendations, and real-time statistics and analytics.

## Licence

Copyright (C) 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
