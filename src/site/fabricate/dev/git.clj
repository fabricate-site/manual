(ns site.fabricate.dev.git
  "Get information about files using git"
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import [java.time ZonedDateTime ZoneId]))

(defn logs
  [fp]
  (let [{:keys [out err]} (sh/sh "git"
                                 "log"        "--follow"
                                 "--format=%H %h %ad" "--date"
                                 "iso-strict" (str fp))]
    (when (not-empty out)
      (mapv (fn [row]
              (let [[sha short-sha modified-time] (str/split row #"\s")]
                {:git/modified-time (ZonedDateTime/parse modified-time)
                 :git/sha           sha
                 :git/short-sha     short-sha
                 :git/file-path     (str fp)}))
            (str/split out
                       (re-pattern (System/getProperty "line.separator")))))))


(defn info
  [file-path]
  (let [commit-history (logs file-path)
        created-time   (:git/modified-time (peek commit-history))]
    (assoc (first commit-history) :git/created-time created-time)))


(comment
  (logs "docs/index.html.fab")
  (info "docs/index.html.fab"))
