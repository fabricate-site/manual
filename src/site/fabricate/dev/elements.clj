(ns site.fabricate.dev.elements
  (:require [dev.onionpancakes.chassis.core :as c]
            [cybermonday.core :as md]
            [site.fabricate.adorn :as adorn]
            [site.fabricate.prototype.read.grammar :as grammar]
            [site.fabricate.dev.build.utils :as utils]
            [malli.dev.pretty]
            [clojure.string :as str]))

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
     [:a {:class "decor-internal" :href top-id} "Top"]]
    [:div {:class "footer-github"}
     [:a
      {:href  "https://github.com/fabricate-site/fabricate"
       :class "decor-external"} "GitHub"]]])
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
  (malli.dev.pretty/-printer)
  (println (:malli/schema (meta #'site.fabricate.api/plan!))))



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

(defn function-dl
  [vars]
  (->> vars
       (filter (fn [[k v]] (fn? (var-get v))))
       (reduce (fn [l [k v]]
                 (conj l
                       [:dt [:code {:class "language-clojure symbol"} k]]
                       [:dd
                        [:dl [:dt "Description"] [:dd (:doc (meta v))]
                         [:dt "Arguments"]
                         [:dd
                          (apply conj
                                 [:ul {:style {:list-style-type "none"}}]
                                 (map (fn [a] [:li
                                               [:code
                                                {:class "language-clojure"}
                                                (adorn/clj->hiccup a)]])
                                      (:arglists (meta v))))]]]))
               [:dl {:class "var-list u-grid-flex"}])))

(defn constants-dl
  [vars]
  (->> vars
       (filter (fn [[k v]] (not (fn? (var-get v)))))
       (reduce (fn [l [k v]]
                 (conj
                  l
                  [:dt [:code {:class "language-clojure symbol"} k]]
                  [:dd
                   [:dl [:dt "Description"] [:dd (:doc (meta v))] [:dt "Type"]
                    [:dd [:code (adorn/clj->hiccup (type (var-get v)))]]]]))
               [:dl {:class "var-list u-grid-flex"}])))

(defn breakup-sym [sym] (interpose [:wbr] (str/split (str sym) #"(?<=[./])")))

(defn ns-header
  [nmspc]
  [:header {:id "top" :class "ns-header"}
   [:h1 (into [:span {:class "ns-name"}] (breakup-sym (ns-name nmspc))) [:br]
    [:span {:class "ns-annotation"} "Namespace"]]
   [:p {:class "ns-description"} (:doc (meta nmspc))]])

(ns-name (find-ns 'site.fabricate.api))

(def html-head-defaults
  [:head [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE-edge"}]
   [:link {:rel :stylesheet :href "/css/normalize.css"}]
   [:link {:rel :stylesheet :href "/css/remedy.css"}]
   [:link {:rel :stylesheet :href "/css/utopia.css"}]
   [:link {:rel :stylesheet :href "/css/fabricate.css"}]])


(defn fabricate-example
  ([expr-or-str ctrl-chars]
   (let [expr (if (string? expr-or-str)
                (utils/str->hiccup expr-or-str)
                (utils/expr->hiccup expr-or-str))]
     [:code {:class "fabricate-example"}
      (str (first grammar/delimiters) ctrl-chars " ")
      [:code {:class "language-clojure"} expr]
      (str " " (last grammar/delimiters))]))
  ([expr-or-str] (fabricate-example expr-or-str "")))

(def opengraph-defaults
  {:title       "Fabricate"
   :description "Build static websites with the full power of Clojure"
   :image       "https://fabricate.site/media/logotype-v1.png"})

(defn opengraph-metadata
  "Generate opengraph metadata from the given map"
  [{:keys [title description url image] :as data}]
  (mapcat (fn [[k v]]
            (let [k (cond (string? k)  k
                          (keyword? k) (str (name k))
                          :default     (str k))]
              (list [:meta {:property (str "og:" k) :content v}]
                    [:meta {:property (str "twitter:" k) :content v}])))
   (merge opengraph-defaults data)))

(def key-links
  [{:url "/" :title "Home" :description "Main page for Fabricate's manual"}
   {:url         "/reference/intro.html"
    :title       "Intro"
    :description "Introduction to Fabricate"}
   {:url         "/guides.html"
    :title       "Guides"
    :description "Guides on how to use Fabricate"}
   {:url "/api.html" :title "API" :description "Fabricate's API"}
   {:url         "https://github.com/fabricate-site/fabricate"
    :title       "GitHub"
    :description "GitHub repo"}])

(def logotype
  [:div {:class "logotype"}
   [:img {:class "logo" :src "/media/fabricate-logo-v1.svg"}]
   [:div {:class "wordmark"} "Fabricate"]
   [:q {:class "tagline"} "Form by art and labor"]])


(def site-header
  "Header for Fabricate's index page"
  [:header {:id "site-header"} (assoc-in logotype [3 0] :h1)
   [:span {:class "primary-description"}
    "Build static websites with the full power of Clojure"]
   (->> key-links
        (drop 1)
        (map (fn [{:keys [url title description]}]
               [:a {:href url :aria-description description} title]))
        (into [:nav {:class "key-links"}]))])


(def simple-demo
  [:div {:id "simple" :class "keyword-demo"} [:h3 "Simple"]
   [:p "Fabricate's API provides 3 clear steps to build a website."]
   [:pre {:class "api-demo"}
    [:code {:class "language-clojure"}
     (adorn/clj->hiccup
      "(->> {}
     api/plan!
     api/assemble
     api/construct!)")]]])

(def versatile-demo
  [:div {:id "versatile" :class "keyword-demo"} [:h3 "Versatile"]
   [:p
    "You can easily extend Fabricate's build process to new sources of data and new ways of building pages."]
   [:pre {:class "api-demo"}
    [:code {:class "language-clojure"}
     (utils/expr->hiccup '(defmethod
                           api/build
                           [:markdown :hiccup]
                           [entry options]
                           (md-to-hiccup entry options))
                         {:width 30})]]])

(def control-demo
  [:div {:id "under-your-control" :class "keyword-demo"}
   [:h3 "Under your control"]
   [:p
    "Fabricate lets you augment your writing with Clojure: you can evaluate code within a page."]
   [:pre {:class "api-demo"}
    [:code {:class "language-clojure"}
     (utils/str->hiccup "^{:kindly/kind :kind/hiccup}
[:h1 (:title metadata)]"
                        {:width 30})]]])


(def namespace-tree
  [:ul {:class "tree main-track" :id "namespace-tree"}
   [:li [:h5 "site.fabricate"]]
   [:li
    [:ul
     [:li [:h5 "api"]
      (str/replace (:doc (meta (find-ns 'site.fabricate.api)))
                   (re-pattern "\n\\s+")
                   " ")]
     [:li [:h5 "source"]
      (str/replace (:doc (meta (find-ns 'site.fabricate.source)))
                   (re-pattern "\n\\s+")
                   " ")]
     [:li [:h5 "document"]
      (str/replace (:doc (meta (find-ns 'site.fabricate.document)))
                   (re-pattern "\n\\s+")
                   " ")] [:li [:h5 "page"]]
     [:li [:h5 "prototype"]
      [:ul
       [:li [:h5 "source"]
        [:ul
         [:li [:h5 "clojure"]
          (str/replace (:doc (meta (find-ns
                                    'site.fabricate.prototype.source.clojure)))
                       (re-pattern "\n\\s+")
                       " ")]
         [:li [:h5 "fabricate"]
          (str/replace
           (:doc (meta (find-ns 'site.fabricate.prototype.source.fabricate)))
           (re-pattern "\n\\s+")
           " ")]]]
       [:li [:h5 "document"]
        [:ul
         [:li [:h5 "clojure"]
          (str/replace
           (:doc (meta (find-ns 'site.fabricate.prototype.document.clojure)))
           (re-pattern "\n\\s+")
           " ")]
         [:li [:h5 "fabricate"]
          (str/replace
           (:doc (meta (find-ns 'site.fabricate.prototype.document.fabricate)))
           (re-pattern "\n\\s+")
           " ")]]]
       [:li [:h5 "read"]
        (str/replace (:doc (meta (find-ns 'site.fabricate.prototype.read)))
                     (re-pattern "\n\\s+")
                     " ")
        [:ul
         [:li [:h5 "grammar"]
          (str/replace (:doc (meta (find-ns
                                    'site.fabricate.prototype.read.grammar)))
                       (re-pattern "\n\\s+")
                       " ")]]]
       [:li [:h5 "schema"]
        (str/replace (:doc (meta (find-ns 'site.fabricate.prototype.schema)))
                     (re-pattern "\n\\s+")
                     " ")]]]]]])



;; TODO: figure out how to present namespace symbols in an enumerated order
(defn ns-sym-table
  [ns-vars]
  (into [:ul {:class "ns-toc"}]
        (map (fn [[sym ns-var]]
               (let [fqs (symbol ns-var)]
                 [:li {:class "ns-toc-entry"}
                  [:a {:href (str "#" fqs) :class "ns-link"}
                   (breakup-sym sym)]]))
             ns-vars)))
(comment
  (type (var-get #'site.fabricate.api/plan!))
  (fn? (var-get #'site.fabricate.api/plan!))
  (type (var-get #'site.fabricate.api/glossary))
  (instance? clojure.lang.MultiFn (type (var-get #'site.fabricate.api/build)))
  (= clojure.lang.MultiFn (type (var-get #'site.fabricate.api/build))))

(defn var-type
  [ns-var]
  (let [var-value (var-get ns-var)
        var-type  (type var-value)]
    (cond (fn? var-value) :function
          (= clojure.lang.MultiFn var-type) :multimethod
          :default        :constant)))

(def type-descriptions
  {:constant "Constant" :multimethod "Multimethod" :function "Function"})


(defn document-var
  [ns-var]
  (let [var-sym     (symbol ns-var)
        var-meta    (meta ns-var)
        ns-var-type (var-type ns-var)]
    [:div {:class "var-documentation" :id (str var-sym)} [:h4 (name var-sym)]
     [:code {:class "var-fully-qualified-name"} (breakup-sym var-sym)]
     [:p {:class "var-description"} (:doc var-meta)]
     [:dl {:class "var-props"} [:dt "Type"]
      [:dd {:class "var-description"} (type-descriptions ns-var-type)]
      (when (and (= :function) (:arglists var-meta))
        ns-var-type
        (list [:dt "Arguments"]
              [:dd
               [:code {:class "language-clojure"}
                (let [args (:arglists var-meta)]
                  (if (= 1 (count args))
                    (utils/expr->hiccup (first args))
                    (utils/expr->hiccup args)))
                #_(->> (:arglists var-meta)
                       (apply concat)
                       (map (fn [i] [:li (utils/expr->hiccup i)]))
                       (into [:ol]))]]))
      ;; TODO: legibly display schemas
      #_(when (:malli/schema var-meta) (list [:dt "Schema"] [:dd]))
      [:dt "Source"] [:dd {:class "var-source"} [:code (:file var-meta)]]]]))

(comment
  #'site.fabricate.api/build
  (document-var #'site.fabricate.api/assemble))

(defn ns-doc
  "Generate documentation from the datafied representation of the namespace"
  [{nmspc-name :name vars :publics :as datafied-ns}]
  (let [nmspc   (:clojure.datafy/obj (meta datafied-ns))
        ns-meta (meta nmspc)]
    (list [:h2 {:class "ns-name" :id (str nmspc-name)} (breakup-sym nmspc-name)]
          [:p {:class "ns-description"} (:doc ns-meta)]
          (ns-sym-table vars)
          (into [:div {:class "var-descriptions"}]
                (map (fn [[_ ns-var]] (document-var ns-var)) vars)))))
