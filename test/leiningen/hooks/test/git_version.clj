(ns leiningen.hooks.test.git-version
  (:require [leiningen.hooks.git-version :as gv]
            (leiningen [core :as core]
                       [deps :as deps]
                       [uberjar :as uber])
            [leiningen.util.file :as luf]
            (clojure.java [io :as io]
                          [shell :as sh]))
  (:use clojure.test))

(def temp-project-template-dir
  (.getCanonicalPath (io/file "test-project/template")))
(def temp-project-test-run-dir
  (.getCanonicalPath (io/file "test-project/current-run")))

(defn setup-temp-project []
  (sh/sh "cp" "-r" temp-project-template-dir temp-project-test-run-dir)
  (sh/with-sh-dir temp-project-test-run-dir
    (sh/sh "git" "init")
    (sh/sh "git" "add" ".")
    (sh/sh "git" "commit" "-am" "Initial commit")
    (sh/sh "git" "tag" "v123")))

(defn create-test-artifacts []
  (let [leiningen-original-pwd (System/getProperty "leiningen.original.pwd")]
    (try (System/setProperty "leiningen.original.pwd"
                             temp-project-test-run-dir)
         (let [temp-project (-> (io/file temp-project-test-run-dir "project.clj")
                                .getPath
                                core/read-project)]
           (uber/uberjar temp-project))
         (finally (System/setProperty "leiningen.original.pwd"
                                      leiningen-original-pwd)))))

(def torn-down (atom false))

(defn tear-down-temp-project []
  (luf/delete-file-recursively (io/file temp-project-test-run-dir) true)
  (swap! torn-down not))

(defn temp-project-fixture [f]
  (setup-temp-project)
  (create-test-artifacts)
  (doto (Runtime/getRuntime)
    (.addShutdownHook (Thread. #(if-not @torn-down (tear-down-temp-project)))))
  (try (f)
       (finally (System/gc)
                (tear-down-temp-project))))

(deftest test-artifacts
  (is (.exists (io/file temp-project-test-run-dir
                        "test-project-v123.jar")))
  (is (.exists (io/file temp-project-test-run-dir
                        "test-project-v123-standalone.jar"))))

(deftest test-version-file
  (let [version-file (io/file temp-project-test-run-dir "resources" "version.txt")]
    (is (.exists version-file))
    (is (= "v123\n" (slurp version-file)))))

(use-fixtures :once temp-project-fixture)
