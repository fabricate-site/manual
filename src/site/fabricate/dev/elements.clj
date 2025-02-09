(ns site.fabricate.dev.elements
  (:require [dev.onionpancakes.chassis.core :as c]
            [cybermonday.core :as md]
            [site.fabricate.dev.build.utils :as utils]))

(defn footer
  ([top-id]
   [:footer {:class "main-footer"}
    [:div {:class "footer-home"}
     [:a {:class "decor-internal" :href "/"} "Home"]]
    [:div {:class "footer-api"}
     [:a
      {:class "decor-internal"
       :href  "/reference/namespaces/site.fabricate.api.html"} "API"]]
    [:div {:class "footer-namespaces"}
     [:a {:class "decor-internal" :href "/reference/namespaces.html"}
      "Namespaces"]]
    [:div {:class "footer-top"}
     [:a {:class "decor-internal" :href top-id} "Top"]]])
  ([] (footer "#top")))

(def logo-img
  [:img {:src "/media/fabricate-logo-v1.svg" :class "fabricate-logo"}])

(def header-big
  [:header {:class "header-big card"}
   [:img {:src "/media/fabricate-logo-v1.svg" :class "fabricate-header-logo"}]
   [:h1 {:class "header-primary"} "Fabricate"]
   [:h3 {:class "header-secondary"} "Form by art and labor"]])

(defn escape-css-string
  "Escape the string so it can be used as a valid CSS identifier"
  [str])

(defn anchor
  "Generate a unique anchor from the given symbol."
  [sym]
  (let [resolved (resolve sym)]))

(comment
  (c/escape-attribute-value (str (symbol (resolve 'anchor))))
  (symbol #'c/escape-attribute-value)
  (meta #'site.fabricate.api/plan!))



(defn source-link
  ([fn-var
    {:keys [repo-url branch]
     :or   {branch   "main"
            repo-url "https://github.com/fabricate-site/fabricate"}
     :as   opts}]
   (let [{:keys [line file]} (meta fn-var)]
     [:a
      {:href  (str repo-url "/blob/" branch "/src/" file "#L" line)
       :class "source-link"} [:code {:class "file-link"} file]]))
  ([fn-var] (source-link fn-var {})))

(defn function-data
  ([fn-var metadata]
   (let [fn-meta (merge (meta fn-var) metadata)]
     [:dl #_{:class "function-data"} [:dt "Function"]
      [:dd {:class "docstring"} (md/parse-body (:doc fn-meta))]
      [:dt "Arguments"]
      [:dd {:class "fn-args"}
       (if-let [arglists (:arglists fn-meta)]
         [:code {:class "language-clojure"}
          (->> arglists
               (apply concat)
               (map (fn [i] [:li (utils/expr->hiccup i)]))
               (into [:ol]))]
         (:args fn-meta))] [:dt "Returns"]
      [:dd {:class "fn-return"} (:returns fn-meta)] [:dt "Source"]
      [:dd {:class "var-source"} (source-link fn-var)]]))
  ([fn-var] (function-data fn-var {})))

(defn function-card
  ([fn-var fn-meta fn-example]
   (let [fn-sym (symbol fn-var)
         id-str (str fn-sym)]
     [:div {:class "fn-card" :id id-str}
      [:div {:class "fn-identifiers"}
       [:h2 {:class "fn-name"} (str (name fn-sym))]
       [:div {:class "fqn"} (str fn-sym)]]
      [:div {:class "fn-data u-grid-flex"} (function-data fn-var fn-meta)
       (when fn-example [:div {:class "fn-example"} fn-example])]]))
  ([fn-var fn-meta] (function-card fn-var fn-meta nil))
  ([fn-var] (function-card fn-var {} nil)))

(defn multimethod-data
  ([mm-var metadata]
   (let [mm-meta (merge (meta mm-var) metadata)]
     [:dl #_{:class "multimethod-data"} [:dt "Multimethod"]
      [:dd {:class "docstring"} (md/parse-body (:doc mm-meta))]
      [:dt "Arguments"]
      [:dd {:class "mm-args"}
       (if-let [arglists (:arglists mm-meta)]
         [:code {:class "language-clojure"}
          (->> arglists
               (apply concat)
               (map (fn [i] [:li (utils/expr->hiccup i)]))
               (into [:ol]))]
         (:args mm-meta))] [:dt "Returns"]
      [:dd {:class "mm-return"} (:returns mm-meta)] [:dt "Source"]
      [:dd {:class "var-source"} (source-link mm-var)]]))
  ([mm-var] (multimethod-data mm-var {})))


(defn multimethod-card
  ([mm-var mm-meta mm-example]
   (let [mm-sym (symbol mm-var)
         id-str (str mm-sym)]
     [:div {:class "multimethod-card" :id id-str}
      [:div {:class "multimethod-identifiers"}
       [:h2 {:class "multimethod-name"} (str (name mm-sym))]
       [:div {:class "fqn"} (str mm-sym)]]
      [:div {:class "multimethod-data u-grid-flex"}
       (multimethod-data mm-var mm-meta)
       (when mm-example
         [:div {:class "multimethod-example"} [:h3 "Example"] mm-example])]]))
  ([mm-var mm-meta] (multimethod-card mm-var mm-meta nil))
  ([mm-var] (multimethod-card mm-var {} nil)))

(defn function-doc
  [fn-var]
  (let [id (anchor fn-var)]
    ;; TODO: come up with canonical, or at least stable, way of converting
    ;; Clojure symbols and vars to and from properly escaped CSS
    ;; identifiers. the rest kind of flows from there, but each function
    ;; needs a UUID-like thing so that a URL can be generated from its
    ;; fully-qualified name
    [:div {:class "fn-doc" :id id :data-clojure-var (str fn-var)}]))
