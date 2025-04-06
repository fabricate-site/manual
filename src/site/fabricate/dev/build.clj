(ns site.fabricate.dev.build
  "Build namespace for generating Fabricate's own documentation."
  (:require [site.fabricate.api :as api]
            [site.fabricate.dev.styles :as styles]
            [site.fabricate.dev.elements :as elements]
            [site.fabricate.prototype.time :as time]
            [site.fabricate.prototype.read :as read]
            [site.fabricate.prototype.read.grammar :as grammar]
            [site.fabricate.prototype.hiccup :as hiccup]
            [site.fabricate.prototype.html :as html]
            [site.fabricate.prototype.document.clojure :as clj]
            [site.fabricate.prototype.document.fabricate :as fabricate]
            [site.fabricate.dev.source.markdown :as markdown]
            [garden.core :as garden]
            [garden.stylesheet :refer [at-import]]
            [rewrite-clj.zip :as z]
            [site.fabricate.adorn :as adorn]
            [babashka.fs :as fs]
            [dev.onionpancakes.chassis.core :as c]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn simple-expr
  "Takes a Clojure form and yields a string with the Fabricate template expression for that form."
  {:malli/schema [:=> [:cat :any :map] :string]}
  [form {:keys [ctrl-char format-fn] :or {format-fn str ctrl-char ""}}]
  (let [[start end] grammar/delimiters]
    (str start ctrl-char (format-fn form) end)))

(defn create-dir-recursive
  [target-dir]
  (let [absolute-path? (fs/absolute? target-dir)
        target-dir     (if (fs/relative? target-dir)
                         (fs/relativize (fs/cwd) (fs/path (fs/cwd) target-dir))
                         target-dir)]
    (->> target-dir
         fs/components
         (reduce (fn [paths path]
                   (conj paths
                         (let [next-path (fs/path (peek paths) path)]
                           (if absolute-path?
                             (fs/absolutize (str fs/file-separator next-path))
                             next-path))))
                 [])
         (filter #(not (fs/exists? %)))
         (run! fs/create-dir))))

(defn create-dir? [d] (when-not (fs/exists? d) (create-dir-recursive d)))

(defn create-publish-dirs!
  [{:keys [site.fabricate.api/options] :as site}]
  (let [{:keys [site.fabricate.page/publish-dir]} options
        css-dir   (fs/path publish-dir "css")
        fonts-dir (fs/path publish-dir "fonts")]
    (run! create-dir? [publish-dir css-dir fonts-dir])
    site))

(defn get-css!
  [{:keys [site.fabricate.api/options] :as site}]
  (let
    [{:keys [site.fabricate.page/publish-dir]} options
     remedy
     {:file (fs/file (fs/path publish-dir "css" "remedy.css"))
      :url
      "https://raw.githubusercontent.com/jensimmons/cssremedy/6590d9630bdd324469620636d85b7ea3753e9a7b/css/remedy.css"}
     normalize
     {:file (fs/file (fs/path publish-dir "css" "normalize.css"))
      :url  "https://unpkg.com/@csstools/normalize.css@12.1.1/normalize.css"}
     patterns {:file (fs/file (fs/path publish-dir "css" "patterns.css"))
               :url  "https://iros.github.io/patternfills/patterns.css"}]
    (doseq [{:keys [file url]} [normalize remedy patterns]]
      (when-not (fs/exists? file) (spit file (slurp url))))
    site))

(defn copy-fonts!
  [{:keys [site.fabricate.api/options] :as site}]
  (let [{:keys [site.fabricate.page/publish-dir]} options
        font-dir (System/getProperty "user.font-dir")
        fonts    (reduce
                  (fn [fonts path] (conj fonts {:src "" :file ""}))
                  [{:src
                    (fs/file
                     font-dir
                     "CommitMono-stdV142-design/CommitMono VariableFont.woff2")
                    :file "html/fonts/CommitMono VariableFont.woff2"}]
                  (fs/glob (fs/path font-dir "Lapidar0.3") "*.woff2"))]
    (doseq [{:keys [src file]} fonts]
      (when-not (fs/exists? file) (fs/copy src file)))
    site))

(def options
  "Options for building Fabricate's own documentation."
  (let [d "html"] {:site.fabricate.page/publish-dir d}))


(defmethod api/collect "*/**.fab"
  [src options]
  (mapv (fn path->entry [p]
          {:site.fabricate.source/format   :site.fabricate.read/v0
           :site.fabricate.document/format :hiccup
           :site.fabricate.source/location (fs/file p)
           :site.fabricate.api/source      src
           :site.fabricate.source/created  (time/file-created p)
           :site.fabricate.source/modified (time/file-modified p)
           ;; multi-outputs are superfluous; this should be replaced with a
           ;; call to mapcat
           :site.fabricate.page/outputs    [{:site.fabricate.page/format :html
                                             :site.fabricate.page/location
                                             (fs/file
                                              (:site.fabricate.page/publish-dir
                                               options))}]})
        (fs/glob (System/getProperty "user.dir") src)))

(defmethod api/collect "docs/**.clj"
  [src {:keys [site.fabricate.page/publish-dir] :as opts}]
  (mapv (fn path->entry [p]
          {:site.fabricate.source/format   :clojure/v0
           :site.fabricate.document/format :hiccup
           :site.fabricate.source/location (fs/file (fs/cwd) p)
           :site.fabricate.page/outputs    [{:site.fabricate.page/format :html
                                             :site.fabricate.page/location
                                             (fs/file publish-dir)}]
           :site.fabricate.api/source      src
           :site.fabricate.source/created  (time/file-created p)
           :site.fabricate.source/modified (time/file-modified p)})
        (fs/glob "." src)))

(comment
  (api/collect "docs/**.clj" {:site.fabricate.page/publish-dir "html"}))


;; example of single-file handling; conflict resolution can be handled
;; separately if there's overlap.

#_(defmethod api/collect "README.md.fab"
    [src options]
    [{:site.fabricate.source/location (fs/file src)
      :site.fabricate.api/source      src
      :site.fabricate.source/created  (time/file-created src)
      :site.fabricate.source/modified (time/file-modified src)
      :site.fabricate.page/title      "Fabricate: README"
      :site.fabricate.source/format   :site.fabricate.markdown/v0
      :site.fabricate.document/format :markdown
      :site.fabricate.page/outputs    [{:site.fabricate.page/format :markdown
                                        :site.fabricate.page/location
                                        (fs/file
                                         (str (:site.fabricate.page/publish-dir
                                               options)
                                              "/README.md"))}]}])

(comment
  (hiccup/doc-header {}))

(defn fabricate-v0->hiccup
  "Generate a Hiccup representation of the page by evaluating the parsed Fabricate template of the page contents."
  [entry]
  (let [parsed-page    (read/parse (slurp (:site.fabricate.source/location
                                           entry)))
        evaluated-page (read/eval-all parsed-page)
        page-metadata  (hiccup/lift-metadata evaluated-page
                                             (let [m (:metadata
                                                      (meta evaluated-page))]
                                               ;; TODO: better handling of
                                               ;; unbound metadata vars
                                               (if (map? m) m {})))
        hiccup-page    [:html
                        (into (conj elements/html-head-defaults
                                    [:title (:title page-metadata)])
                              (-> page-metadata
                                  (select-keys [:title :description :image])
                                  elements/opengraph-metadata))
                        [:body
                         [:main
                          (apply conj
                                 [:article
                                  {:lang  "en-us"
                                   :class "u-grid-flex fabricate-article"}]
                                 (hiccup/parse-paragraphs evaluated-page))]
                         (elements/footer)
                         #_[:footer [:div [:a {:href "/"} "Home"]]]]]]
    (assoc entry
           :site.fabricate.document/data hiccup-page
           :site.fabricate.page/title    (:title page-metadata))))

(defmethod api/build [:site.fabricate.read/v0 :hiccup]
  ([{loc :site.fabricate.source/location :as entry} _opts]
   (try (fabricate-v0->hiccup entry)
        (catch Exception ex
          (throw (ex-info (ex-message ex)
                          (assoc (Throwable->map ex
                                                 :site.fabricate.source/location
                                                 loc))))))))

(defmethod api/build [:site.fabricate.markdown/v0 :markdown]
  ([entry _opts]
   (assoc entry
          :site.fabricate.document/data
          (slurp (:site.fabricate.source/location entry)))))

(defn clj-entry->hiccup
  [entry]
  (let [[a_ attrs & contents :as article] (-> (:site.fabricate.source/location
                                               entry)
                                              (clj/read-forms)
                                              (clj/eval-forms)
                                              (clj/forms->hiccup))
        ns-meta       (-> article
                          (get-in [1 :data-clojure-namespace])
                          (find-ns)
                          meta)
        updated-entry (merge entry ns-meta)]
    (assoc entry
           :site.fabricate.document/data
           [:html
            (into (conj elements/html-head-defaults
                        [:title (:site.fabricate.document/title updated-entry)])
                  (-> updated-entry
                      (select-keys [:site.fabricate.document/title
                                    :site.fabricate.document/description])
                      (clojure.set/rename-keys
                       {:site.fabricate.document/title       :title
                        :site.fabricate.document/description :description})
                      elements/opengraph-metadata))
            [:body [:main attrs [:article {:class "u-grid-flex"} contents]]
             (elements/footer)]])))

(defmethod api/build [:clojure/v0 :hiccup]
  [{source-location :site.fabricate.source/location :as entry} opts]
  (println "generating hiccup from" (str source-location))
  (clj-entry->hiccup entry))

(defn ns-sym->hiccup
  "Automatically generate a Hiccup page documenting the vars in the given namespace."
  [ns-sym]
  (let [nmspc   (find-ns ns-sym)
        ns-vars (ns-publics nmspc)
        ns-meta (meta ns-sym)]
    [:html
     [:head
      (conj elements/html-head-defaults
            [:title (str "Fabricate: " ns-sym " namespace")])]
     [:body
      [:main
       [:article (elements/ns-header nmspc) [:h2 "Functions"]
        (elements/function-dl ns-vars) [:h2 "Constants"]
        (elements/constants-dl ns-vars)]] (elements/footer)]]))

(def doc-namespaces
  '[site.fabricate.prototype.document.clojure
    site.fabricate.prototype.document.fabricate
    site.fabricate.prototype.hiccup])


(defmethod api/collect #'doc-namespaces
  [ns-syms {:keys [site.fabricate.page/publish-dir] :as opts}]
  (mapv (fn [ns-sym]
          {:site.fabricate.source/format :clojure.namespace/v0
           :site.fabricate.document/format :hiccup
           :site.fabricate.source/location
           (fs/file (fs/cwd) (str "docs/reference/namespaces/" ns-sym ".clj"))
           :site.fabricate.page/format :html
           :site.fabricate.page/location "html"
           :site.fabricate.page/outputs [{:site.fabricate.page/format :html
                                          :site.fabricate.page/location
                                          (fs/file publish-dir)}]
           :clojure/namespace ns-sym})
        doc-namespaces))

(defmethod api/build [:clojure.namespace/v0 :hiccup]
  [{entry-ns :clojure/namespace :as entry} opts]
  (assoc entry :site.fabricate.document/data (ns-sym->hiccup entry-ns)))

(defmethod api/collect "docs/posts/*.md"
  [glob {:keys [site.fabricate.page/publish-dir :as opts]}]
  (mapv (fn [src-loc]
          {:site.fabricate.source/format   :markdown/v0
           :site.fabricate.document/format :hiccup
           :site.fabricate.source/location (fs/file src-loc)
           :site.fabricate.page/format     :html
           :site.fabricate.page/location   "html"
           :site.fabricate.page/outputs    [{:site.fabricate.page/format :html
                                             :site.fabricate.page/location
                                             (fs/file publish-dir)}]})
        (fs/glob (System/getProperty "user.dir") glob)))

(defmethod api/build [:markdown/v0 :hiccup]
  [{source-location :site.fabricate.source/location :as entry} opts]
  (let [{:keys [hiccup md/front-matter] :as entry-data} (-> source-location
                                                            slurp
                                                            markdown/md->hiccup)
        [_div _attrs h1? & contents] hiccup
        h1          (if (and (vector? h1?) (= :h1 (first h1?)))
                      h1?
                      [:h1 {} "Fabricate: " (:title front-matter)])
        title       (or (:title front-matter) (last h1))
        page-hiccup [:html
                     [:head (conj elements/html-head-defaults [:title title])
                      (elements/opengraph-metadata
                       (select-keys front-matter
                                    [:title :description :url :image]))]
                     [:body
                      [:main
                       (into [:article {:class "u-grid-flex md-page"}
                              (assoc-in h1 [1 :id] "top")]
                             contents)] (elements/footer)]]]
    (merge entry
           (-> front-matter
               (select-keys [:title :description])
               (clojure.set/rename-keys {:title :site.fabricate.document/title
                                         :description
                                         :site.fabricate.document/description}))
           {:site.fabricate.document/data page-hiccup})))

(comment
  (-> "docs/posts/2025-02-api-announcement.md"
      slurp
      markdown/md->hiccup)
  (-> "docs/posts/2025-02-api-announcement.md"
      slurp
      cybermonday.core/parse-md))


(comment
  (->> (api/plan! [] {})
       :site.fabricate.api/entries
       (filterv #(= :clojure.namespace/v0 (:site.fabricate.source/format %))))
  (api/construct! []
                  {:site.fabricate.api/entries
                   [(api/build {:site.fabricate.source/location
                                (fs/file (fs/cwd) "docs/design/utopia.clj")
                                :site.fabricate.source/format :clojure/v0
                                :site.fabricate.page/location "html"
                                :site.fabricate.document/format :hiccup
                                :site.fabricate.page/format :html}
                               {})]})
  (let [evaluated (-> "docs/design/utopia.clj"
                      clj/file->forms
                      clj/eval-forms
                      clj/forms->hiccup)]
    (-> evaluated
        (find-ns)
        meta)))

;; (def assemble-index nil)

;; (defmethod assemble "index.html" [entry] (assemble-index entry))


(defn write-hiccup-html!
  "Generate HTML from Hiccup data and write it to the given file."
  [hiccup-page-data output-file]
  (let [parent-dir (fs/parent output-file)]
    (create-dir? parent-dir)
    (spit output-file (c/html [c/doctype-html5 hiccup-page-data]))))

(defn subpath
  ([dir p] (apply fs/path (drop 1 (fs/components (fs/relativize dir p)))))
  ([p] (subpath (fs/cwd) p)))

(defn output-path
  [input-file output-location]
  (cond (fs/directory? output-location) (fs/file (fs/path output-location
                                                          (subpath input-file)))
        (instance? java.io.File output-location) output-location))

(comment
  (output-path (fs/path (fs/cwd) "docs/design/utopia.clj") "html")
  (output-path (fs/path (fs/cwd)
                        "docs/reference/namespaces.site.fabricate.api.clj")
               "html")
  (output-path "docs/design/utopia.clj" "html"))

(defn hiccup->html
  [{source-location :site.fabricate.source/location :as entry} _opts]
  (let [output-file
        (fs/file (str (output-path
                       (if (= "fab" (fs/extension source-location))
                         (fs/strip-ext (fs/strip-ext source-location))
                         (fs/strip-ext (:site.fabricate.source/location entry)))
                       (:site.fabricate.page/location entry))
                      ".html"))]
    (println "writing output to" (str output-file))
    (write-hiccup-html! (:site.fabricate.document/data entry) output-file)
    (assert (fs/exists? output-file))
    (-> entry
        (assoc :site.fabricate.page/output output-file
               :site.fabricate.page/format :html))))

(defmethod api/produce! [:hiccup :html]
  [entry opts]
  (try (hiccup->html entry opts)
       (catch Exception e
         (throw (ex-info (str "Error building page "
                              (:site.fabricate.source/location entry))
                         (merge (Throwable->map e)
                                (select-keys
                                 entry
                                 [:site.fabricate.source/location
                                  :site.fabricate.source/format
                                  :site.fabricate.api/source
                                  :site.fabricate.page/location
                                  :site.fabricate.page/format])))))))


(defmethod api/produce! [:markdown :markdown]
  [entry _opts]
  (let [output-file (fs/file (output-path
                              (:site.fabricate.source/location entry)
                              (:site.fabricate.page/location entry)))]
    (spit output-file (:site.fabricate.document/data entry))
    (assoc entry :site.fabricate.page/output output-file)))

(def setup-tasks [create-publish-dirs! get-css! copy-fonts!])

(comment
  ;; it's hard to beat this simplicity. also, a point in favor of the
  ;; "return a modified site with modified options" implementation:
  ;; potentially storing a reference to a server or other stateful
  ;; component
  (do (->> {:site.fabricate.api/options site.fabricate.dev.build/options}
           (#'site.fabricate.api/plan! site.fabricate.dev.build/setup-tasks)
           (#'site.fabricate.api/assemble [])
           (#'site.fabricate.api/construct! []))
      :done)
  ;;fully-qualified versions of these functions allow you to
  ;; eval from a register regardless of namespace
  (run! fs/delete (fs/glob "html" "**.html"))
  (.getMethodTable api/produce!)
  (.getMethodTable api/collect)
  (names)
  (list-methods)
  (str/split (str (symbol :site.fabricate.document/data)) #"\.")
  (name :abc/xyz)
  clojure.string/split
  (run! (fn [[_ v]] (clojure.pprint/pprint [v (:doc (meta v))]))
        (ns-publics (find-ns 'site.fabricate.api)))
  (filterv #(re-find #"fabricate.prototype" (str (ns-name %))) (all-ns))
  (filterv #(re-find #"fabricate.*page" (str (ns-name %))) (all-ns)))
