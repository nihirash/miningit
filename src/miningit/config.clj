(ns miningit.config
  (:require [clojure.edn :as edn]))

(def config (atom {:path "/home/git"
                   :clone-path "git@localhost"
                   :trusted true}))

(defn safe-slurp [source]
  "Reads source. On fail returns nil"
  (try
    (slurp source)
    (catch Exception e
      nil)))

(defn read-config []
  (some->> (safe-slurp "config.edn")
          (edn/read-string)
          (reset! config)))

(defn save-config [new-config]
  (reset! config new-config)
  (spit "config.edn" new-config))
