^{:kindly/kind :kind/hiccup} [:h1 "Utopia design system"]

(ns site.fabricate.docs.design.utopia
  {:site.fabricate.document/title "Utopia design system"
   :site.fabricate.document/description
   "Implementation notes for Fabricate based on utopia.fyi"}
  (:require [site.fabricate.dev.elements :as elements]
            [site.fabricate.dev.build.utils :as utils :refer [expr->hiccup]]
            [site.fabricate.adorn :refer [clj->hiccup]]))


^{:kindly/kind :kind/hiccup}
[:section {:class "u-grid-flex"} [:h2 "Implementation checklist"]
 [:ul #_{:style {:grid-column "1 / span 4"}} [:li "☑ Import stylesheet"]
  [:li "☑ Adjust grid to use " [:code "auto-fit"]]
  [:li "☑ Set default styles for elements"]
  #_[:li "Begin defining utility classes"]
  #_[:li "Test out different spacing and size options"]
  [:li "Align Fabricate.css with utopia.css"] [:li "Rework API documentation"]]
 [:div
  {:style {:border      "var(--space-xs) solid var(--color-brown)"
           :grid-column "span 4"
           :height      "var(--space-3xl)"}}]
 [:div
  {:style {:border      "var(--space-2xs) solid var(--color-yellow)"
           :grid-column "1 / span 2"
           :height      "var(--space-l)"}}]
 [:div
  {:style {:border      "var(--space-2xs) solid var(--color-green)"
           :grid-column "span 2"
           :height      "var(--space-l)"}}]]


^{:kindly/hide-code true}
(comment
  (require '[dev.onionpancakes.chassis.core :as c])
  (spit "test.txt"
        (c/html [c/doctype-html5
                 [:html [:head]
                  [:body
                   [:ul #_{:style {:grid-column "1 / span 4"}}
                    [:li "☑ Import stylesheet"]
                    [:li "☑ Adjust grid to use " [:code "auto-fit"]]
                    [:li "☑ Set default styles for elements"]
                    [:li "Begin defining utility classes"]
                    [:li "Test out different spacing and size options"]]]]])))


^{:kindly/kind :kind/hiccup}
(elements/function-card #'site.fabricate.api/plan!
                        {:returns
                         (list "site: a map with two keys: "
                               [:code (clj->hiccup :site.fabricate.api/entries)]
                               " with the list of entries, and "
                               [:code (clj->hiccup :site.fabricate.api/options)]
                               ", containing site-wide options.")})
