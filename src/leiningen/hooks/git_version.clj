(ns leiningen.hooks.git-version
  (:use [robert.hooke :only [add-hook]]
        [leiningen.core :only [abort]]
        [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen jar uberjar]))

(defn add-git-version-hook [task project & args]
  (try
    (let [{:keys [exit out]}
          (with-sh-dir (project :root)
            (sh "git" "describe" "--tags"))]
      (if-not (zero? exit)
        (abort "git describe --tags has failed")
        (apply task (assoc project ::git-version (str/trimr out)) args)))
    (catch java.io.IOException e
      (abort "Could not run git describe --tags"))))

(defn replace-artifact-names-hook [task project & args]
  (let [artifact-base-name (str (project :name) "-" (project ::git-version))]
    (apply task
           (assoc project
             :jar-name (str artifact-base-name ".jar")
             :uberjar-name (str artifact-base-name "-standalone.jar"))
           args)))

(defn write-version-file-hook [task project & args]
  (let [git-version (project ::git-version)
        version-file (->> [(project :resources-path) "version.txt"]
                          (str/join java.io.File/separatorChar)
                          io/file)]
    (.mkdirs (.getParentFile version-file))
    (with-open [w (io/writer version-file)]
      (binding [*out* w]
        (println git-version)))
    (apply task project args)))

(doseq [task (keep resolve '[leiningen.jar/jar
                             leiningen.uberjar/uberjar
                             leiningen.deploy/deploy])
        hook [add-git-version-hook
              replace-artifact-names-hook]]
  (add-hook task hook))

(add-hook #'leiningen.jar/jar write-version-file-hook)
