(ns site.fabricate.dev.source.markdown
  "Markdown namespace"
  (:require [cybermonday.ir :as md-ir]
            [cybermonday.utils :as cm-utils]
            [cybermonday.parser :as md-parser]
            [cybermonday.core :as md]
            [cybermonday.lowering :as md-lower]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:import [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.util.data MutableDataSet]
           [com.vladsch.flexmark.ext.tables TablesExtension TableBlock TableHead
            TableRow TableCell TableBody TableBody TableSeparator]
           [com.vladsch.flexmark.ext.wikilink WikiLinkExtension WikiLink]
           [com.vladsch.flexmark.ext.footnotes FootnoteExtension Footnote
            FootnoteBlock]
           [com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughExtension
            Strikethrough]
           [com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension
            TaskListItem]
           [com.vladsch.flexmark.test.util AstCollectingVisitor]
           [com.vladsch.flexmark.ext.gitlab GitLabExtension GitLabInlineMath]
           [com.vladsch.flexmark.ext.toc TocExtension TocBlock]))

(def parser
  "Extensible markdown parser based on Flexmark. Can be overridden by resetting the atom to a new value."
  (atom (.build (Parser/builder
                 (.. (MutableDataSet.)
                     (set Parser/EXTENSIONS
                          [(TocExtension/create) (TablesExtension/create)
                           (FootnoteExtension/create)
                           (StrikethroughExtension/create)
                           (TaskListExtension/create) (GitLabExtension/create)
                           (WikiLinkExtension/create)])
                     (toImmutable))))))

(defn flexmark-parse
  "Parse the markdown string using the Flexmark parser"
  [^String md]
  (.parse ^Parser @parser ^String md))

(def node-tags "Extended mapping from Flexmark AST node to Hiccup tag" ())

;; TODO: figure out how to standardize and normalize this output
;; so it works with the rest of Cybermonday
(defn wikilink->map
  "Convert the Wikilink object to a Clojure map"
  [^WikiLink link]
  {:link     (str (.getLink link))
   :page-ref (str (.getPageRef link))
   ;; the text field is non-nil only when there's a bracket alias
   :text     (str (.getText link))})

(extend-protocol md-parser/HiccupRepresentable
 WikiLink
   (to-hiccup [this source]
     (cm-utils/make-hiccup-node :markdown/wikilink
                                (wikilink->map this)
                                (md-parser/map-children-to-hiccup this
                                                                  source))))

(defn ast->hiccup
  [ast source]
  (md-ir/process-inline-html (md-parser/to-hiccup ast source)))

(def ^:private front-matter-fence
  (str "---" (System/getProperty "line.separator")))

(defn get-front-matter
  "Extract and return the EDN front matter along with the remaining text, if present."
  [md-text]
  (if (str/starts-with? md-text front-matter-fence)
    (let [interval (count front-matter-fence)
          idx      (str/index-of md-text front-matter-fence interval)]
      {:md/text         (subs md-text (+ idx interval))
       :md/front-matter (edn/read-string (subs md-text interval idx))})
    {:md/text md-text :md/front-matter nil}))

(defn md->hiccup
  ([md-text opts]
   (let [{:keys [md/text] :as data} (get-front-matter md-text)]
     (assoc data
            :hiccup
            (-> (ast->hiccup (flexmark-parse text) text)
                (md-lower/to-html-hiccup opts)
                (cm-utils/cleanup)))))
  ([md-text] (md->hiccup md-text {})))

(comment
  (->> (clojure.reflect/reflect com.vladsch.flexmark.ext.wikilink.WikiNode)
       :members
       (mapv #(dissoc % :exception-types :declaring-class))
       clojure.pprint/print-table)
  (->> (clojure.reflect/reflect WikiLink)
       :members
       (mapv #(dissoc % :exception-types :declaring-class))
       clojure.pprint/print-table)
  (.getDocument (flexmark-parse "[[wikilink]]"))
  (.getReference (.getDocument (flexmark-parse "[[wikilink]]")))
  ((.getDocument (flexmark-parse
                  "text with a [[wikilink]] and more text afterwards"))))
