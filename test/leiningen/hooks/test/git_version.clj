(ns leiningen.hooks.test.git-version
  (:require [leiningen.hooks.git-version :as gv]
            (leiningen [deps :as deps]
                       [uberjar :as uber])
            (clojure.java [io :as io]
                          [shell :as sh]))
  (:use clojure.test))

(try
  (require 'leiningen.core)
  (catch Exception e
    (if (or (instance? java.io.FileNotFoundException e)
            (instance? java.io.FileNotFoundException (.getCause ^Exception e)))
      (require 'leiningen.core.project)
      (throw e))))

(try
  (use '[leiningen.util.file :only [delete-file-recursively]])
  (catch Exception e
    (if (or (instance? java.io.FileNotFoundException e)
            (instance? java.io.FileNotFoundException (.getCause ^Exception e)))
      (use '[leiningen.clean :only [delete-file-recursively]])
      (throw e))))

(def read-project @(or (resolve 'leiningen.core/read-project)
                       (resolve 'leiningen.core.project/read)))

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
                                read-project)]
           (uber/uberjar temp-project))
         (finally (System/setProperty "leiningen.original.pwd"
                                      leiningen-original-pwd)))))

(def torn-down (atom false))

(defn tear-down-temp-project []
  (delete-file-recursively (io/file temp-project-test-run-dir) true)
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
  #_
  (is (or (.exists (io/file temp-project-test-run-dir
                            "test-project-v123.jar"))
          (.exists (io/file temp-project-test-run-dir
                            "target"
                            "test-project-v123.jar"))))
  (is (or (.exists (io/file temp-project-test-run-dir
                            "test-project-v123-standalone.jar"))
          (.exists (io/file temp-project-test-run-dir
                            "target"
                            "test-project-v123-standalone.jar")))))

(deftest test-version-file
  (let [version-file (io/file temp-project-test-run-dir "resources" "version.txt")]
    (is (.exists version-file))
    (is (= "v123\n" (slurp version-file)))))

(use-fixtures :once temp-project-fixture)
