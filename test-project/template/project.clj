(defproject test-project "1.0.0"
  :description "Test project for lein-git-version"
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :hooks [leiningen.hooks.git-version])