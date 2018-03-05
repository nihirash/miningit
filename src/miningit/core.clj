(ns miningit.core
    (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh])
  (:use [miningit.config]))

(defn get-repositories-list []
  (for [name (seq (.list (io/file (:path @config))))
        :let [child (io/file (:path @config) name)]
        :when (and
               (.isDirectory child)
               (re-find #".(.git)" (.getName child)))]
    name))

(defn get-commits [repo steps]
  (:out (sh/sh "git" "log" (str "-" steps) "--pretty=%ci\t%cN\t%B(%H) %%" :dir (io/file (:path @config) repo))))

(defn get-last-commit-message [repo]
  (some-> (get-commits repo 1)
          (str/split #" %")
          (first)
          (str/split #"\t")
          (last)))

(defn read-strings [strings]
  (map #(str/split % #"\t") strings))

(defn get-commits-history [repo depth]
  (some-> (get-commits repo depth)
          (str/split #" %")
          (read-strings)))
