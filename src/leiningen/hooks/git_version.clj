(ns leiningen.hooks.git-version
  (:use [robert.hooke :only [add-hook]]
        [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen jar uberjar]))

(try
  (use '[leiningen.core :only [abort]])
  (catch Exception e
    (if (or (instance? java.io.FileNotFoundException e)
            (instance? java.io.FileNotFoundException (.getCause ^Exception e)))
      (use '[leiningen.core.main :only [abort]])
      (throw e))))

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
           (vary-meta (assoc project
                        :jar-name (str artifact-base-name ".jar")
                        :uberjar-name (str artifact-base-name "-standalone.jar"))
                      update-in [:without-profiles]
                      assoc
                      :jar-name (str artifact-base-name ".jar")
                      :uberjar-name (str artifact-base-name "-standalone.jar"))
           args)))

(defn replace-version-hook [task project & args]
  (let [version (project ::git-version)]
    (apply task
           (vary-meta (assoc project
                        :version version)
                      update-in [:without-profiles]
                      assoc :version version)
           args)))

(defn write-version-file-hook [task project & args]
  (let [git-version (project ::git-version)
        ;; TODO: a separate defproject key?
        version-file (->> [(or (project :resources-path)
                               (last (project :resource-paths)))
                           "version.txt"]
                          (str/join java.io.File/separatorChar)
                          io/file)]
    (.mkdirs (.getParentFile version-file))
    (with-open [w (io/writer version-file)]
      (binding [*out* w]
        (println git-version)))
    (println "Created" (.getCanonicalPath version-file))
    (apply task project args)))

(doseq [task (map resolve '[leiningen.jar/jar
                            leiningen.uberjar/uberjar
                            leiningen.pom/pom])
        hook [add-git-version-hook
              replace-artifact-names-hook
              replace-version-hook]]
  (add-hook task hook))

(add-hook #'leiningen.jar/jar write-version-file-hook)
