^{:kindly/hide-code true :kindly/hide-result true}
(ns ^{:site.fabricate.document/title "Fabricate API"
      :site.fabricate.document/description
      "A complete listing of the API provided by the Fabricate Clojure library."
      :site.fabricate.document/url "https://fabricate.site/api.html"}
    site.fabricate.docs.api
  (:require [site.fabricate.dev.elements :as elements]
            [site.fabricate.api]
            [site.fabricate.source]
            [site.fabricate.document]
            [site.fabricate.page]
            [site.fabricate.prototype.source.clojure]
            [site.fabricate.prototype.source.fabricate]
            [site.fabricate.prototype.document.clojure]
            [site.fabricate.prototype.document.fabricate]
            [site.fabricate.prototype.read]
            [site.fabricate.prototype.read.grammar]
            [site.fabricate.prototype.schema]
            [clojure.datafy :as datafy]))


^{:kindly/kind :kind/hiccup}
[:header {:id "top" :style {:grid-column "1 / -1"}}
 [:h1 (:site.fabricate.document/title (meta *ns*))]
 [:p (:site.fabricate.document/description (meta *ns*))]]

^{:kindly/hide-code true :kindly/hide-result true}
(def fabricate-namespaces
  (->> (all-ns)
       (filter (fn [nmspc]
                 (let [nmspc-name (str (ns-name nmspc))]
                   (and
                    (re-find #"site\.fabricate" nmspc-name)
                    (not
                     (re-find
                      #"site\.fabricate\.(dev|adorn|docs|notes|prototype\.time)"
                      nmspc-name))))))
       (mapv datafy/datafy)))


^{:kindly/kind :kind/hiccup} elements/namespace-tree


^{:kindly/kind :kind/hiccup}
(into [:section {:class "ns-section" :id "site.fabricate.api"}]
      (elements/ns-doc (->> fabricate-namespaces
                            (filter #(= (:name %) 'site.fabricate.api))
                            (first))))


^{:kindly/kind :kind/hiccup}
(->> fabricate-namespaces
     (map (fn [n] [:section {:class "ns-section"} (elements/ns-doc n)]))
     (into [:div {:class "ns-sections"}]))
