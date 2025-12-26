(ns site.fabricate.dev.build-test
  (:require [site.fabricate.dev.build :as build]
            [clojure.test :as t]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as match]
            [babashka.fs :as fs]))

(def test-build-options
  (merge build/options {:site.fabricate.page/publish-dir (fs/create-temp-dir)}))

(def test-site {:site.fabricate.api/options test-build-options})

(def test-setup-tasks (drop-last 2 build/setup-tasks))

(defn get-unconverted-forms
  [hiccup-data]
  (let [unconverted (atom [])]
    (walk/prewalk (fn check-value [v]
                    (when (or (kindly-like? v)
                              (and (hiccup-like? v)
                                   (kindly-str-like? (get-first-string-elem
                                                      v))))
                      (swap! unconverted conj v))
                    v)
                  hiccup-data)
    @unconverted))

(defn broken-link?
  [link-str]
  (cond
    ;; external
    (str/starts-with? link-str "http(s)?://") (= 200
                                                 (:status (curl/get link-str)))
    ;; local
    (string? link-str) (fs/exists? (str/replace link-str #"^/" "./"))
    :default false))
(t/deftest dev-tools (t/testing "Dev namespaces"))
(t/deftest build
  (t/testing "ability to build Fabricate manual without errors"
    (t/is (= :done
             (do (->> test-site
                      (#'site.fabricate.api/plan! test-setup-tasks)
                      (#'site.fabricate.api/assemble [])
                      (#'site.fabricate.api/construct! []))
                 :done))))
  (t/testing "Properties of build steps:"
    (let [post-plan      (#'site.fabricate.api/plan! test-setup-tasks test-site)
          post-assemble  (#'site.fabricate.api/assemble [] post-plan)
          post-construct (#'site.fabricate.api/construct! [] post-assemble)]
      (t/testing "plan"
        (t/is
         (match? (match/seq-of
                  {:site.fabricate.source/file (match/pred fs/absolute?)
                   :site.fabricate.source/location (match/pred fs/exists?)
                   :git/file-path (match/pred string?)})
                 (:site.fabricate.api/entries post-plan))
         "Every collected entry should have a source location, repo-relative, and absolute file path."))
      (t/testing "assemble"
        (t/is
         (match? post-plan post-assemble)
         "No entry should contain less information after assemble than before")
        (t/is
         (match? (match/seq-of {:site.fabricate.document/data
                                (match/pred #(empty? (get-unconverted-forms %)))
                                :site.fabricate.document/format :hiccup})
                 (:site.fabricate.api/entries post-assemble))
         "No Hiccup entry should contain unconverted Kindly forms after assemble"
         ;; ... or should it?
        )
        (t/is false "No assembled entry should contain dead links"))
      (t/testing "construct!"
        (t/is
         (match? post-assemble post-construct)
         "No entry should contain less information after construct! than before")))))
