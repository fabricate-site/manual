(ns site.fabricate.dev.build-test
  (:require [site.fabricate.dev.build :as build]
            [clojure.test :as t]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as match]
            [babashka.fs :as fs]))

(def test-build-options
  (merge build/options {:site.fabricate.page/publish-dir (fs/create-temp-dir)}))

(def test-site {:site.fabricate.api/options test-build-options})

(t/deftest dev-tools (t/testing "Dev namespaces"))
(t/deftest build
  (t/testing "ability to build Fabricate manual without errors"
    (t/is (= :done
             (do (->> test-site
                      (#'site.fabricate.api/plan! build/setup-tasks)
                      (#'site.fabricate.api/assemble [])
                      (#'site.fabricate.api/construct! []))
                 :done))))
  (t/testing "Properties of build steps:"
    (let [post-plan      (#'site.fabricate.api/plan!
                          build/setup-tasks
                          test-site)
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
         "No entry should contain less information after assemble than before"))
      (t/testing "construct!"
        (t/is
         (match? post-assemble post-construct)
         "No entry should contain less information after construct! than before")))))
