(ns site.fabricate.docs.design.utopia
  {:site.fabricate.document/title "Utopia design system"
   :site.fabricate.document/description
   "Implementation notes for Fabricate based on utopia.fyi"})


^{:kindly/kind :kind/hiccup} [:h1 "Utopia design system"]


^{:kindly/kind :kind/hiccup}
[:section {:class "u-grid-flex"} [:h2 "Implementation checklist"]
 [:ul #_{:style {:grid-column "1 / span 4"}} [:li "Import stylesheet"]
  [:li "Adjust grid to use " [:code "auto-fit"]]
  [:li "Set default styles for elements"] [:li "Begin defining utility classes"]
  [:li "Test out different spacing and size options"]]
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
