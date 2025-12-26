(ns site.fabricate.dev.build-test
  (:require [site.fabricate.dev.build :as build]
            [clojure.test :as t]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as match]
            [babashka.fs :as fs]
            [babashka.curl :as curl]
            [clojure.edn :as edn]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [dev.onionpancakes.chassis.core :as c]))

(def test-build-options
  (merge build/options {:site.fabricate.page/publish-dir (fs/create-temp-dir)}))

(def test-site {:site.fabricate.api/options test-build-options})

(def test-setup-tasks (drop-last 2 build/setup-tasks))

(defn cleanup
  [f]
  (f)
  (run! fs/delete-tree
        (fs/list-dir {:site.fabricate.page/publish-dir test-build-options})))

(t/use-fixtures :once cleanup)

(comment
  (->> test-site
       (#'site.fabricate.api/plan! build/setup-tasks)
       (#'site.fabricate.api/assemble [])
       :site.fabricate.api/entries
       first
       :site.fabricate.document/data))

(t/deftest dev-tools (t/testing "Dev namespaces"))

(def kindly-map-pattern #".*[{].*:kindly[/]hide-code\s+.+[}].*")

(defn no-kindly-maps?
  [html-str]
  (not (some? (re-matches kindly-map-pattern html-str))))

(def example-entry
  {:site.fabricate.document/data [c/doctype-html5 [:head]
                                  [:body
                                   [:article [:h1 "Test article"]
                                    [:p "test text"]
                                    {:kindly/hide-code true
                                     :kindly/hide-result false
                                     :kind  :default
                                     :form  :test/form
                                     :value :test/form}
                                    [:div
                                     ;; important corner case: kindly map
                                     ;; as first element of Hiccup vector
                                     ;; can get interpreted as element
                                     ;; attributes instead of standalone
                                     ;; element
                                     {:kindly/hide-code true
                                      :kindly/hide-result false
                                      :kind  :default
                                      :form  '(1 2 3)
                                      :value '(1 2 3)}]]]]})

(t/deftest functions
  (t/is (match? {:site.fabricate.document/data (match/pred no-kindly-maps?)}
                (update example-entry
                        :site.fabricate.document/data
                        #(c/html (build/kindly-maps->chassis-elements %))))
        "Kindly maps in Hiccup forms should not be present in HTML output"))

(comment
  (-> example-entry
      :site.fabricate.document/data
      build/kindly-maps->chassis-elements
      c/html))

(defn hiccup-like? [v] (and (vector? v) (keyword? (first v))))

(defn kindly-str-like?
  [v]
  (let [parsed (try (edn/read-string v) (catch Exception e nil))]
    (build/kindly-like? parsed)))

(defn get-first-string-elem
  [v]
  (->> v
       (filter string?)
       first))

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
