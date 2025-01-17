(ns site.fabricate.dev.source.markdown-test
  (:require [site.fabricate.dev.source.markdown :as md]
            [clojure.test :as t]))


(t/deftest parsing-extensions
  (let [test-txt "[[basic wikilink]]"
        test-hiccup (md/md->hiccup test-txt)
        test-docstring
        "Return the data with ...something, after calling [[my-ns/fn]] on it"]
    (t/testing "wikilinks"
      (t/is (some? test-hiccup)
            "Basic parsing should return values without errors")
      ;; TODO: fix this property
      #_(t/is (= "[[basic wikilink]]" (get-in test-hiccup [2 2 1 :reference]))
              "Link text with brackets should be retained")
      (t/is (= "basic wikilink" (get-in test-hiccup [2 2 2]))
            "Link contents should be taken out of brackets in body text")
      (t/testing "lowering fns"
        (t/is (= test-hiccup
                 (md/md->hiccup test-txt
                                {:lower-fns {:markdown/wikilink
                                             identity}})))))))
