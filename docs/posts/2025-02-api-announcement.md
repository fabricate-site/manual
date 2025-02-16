# Fabricate's new API
**February 2025**

After a long hiatus, I have considerably redesigned and simplified Fabricate. 

Most importantly, it has a new [**API**](/reference/namespaces/site.fabricate.api.html); it consists of 3 functions and 3 multimethods.

1. The [`plan!`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/plan!) function executes a list of setup-tasks (functions), and then calls the implementations of the [`collect`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/collect) multimethod to get a list of source files, specified as *entries* (maps).
2. The [`assemble`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/assemble) function uses these entries to call the [`build`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/build) multimethod, which generates structured content from each source entry. It dispatches on two keys for each entry:
   1. `site.fabricate.source/format`
   2. `site.fabricate.document/format`
   
   By dispatching on two types in the build multimethod, you tell the assemble function: "generate this type of Clojure data from this type of source file." The entry ends up with a new key - `site.fabricate.document/data` - that contains the data generated from a source for an entry. After generating this data, it runs a list of of tasks (functions) on the resulting site. 
3. The [`construct!`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/construct!) function calls the [`produce!`](/reference/namespaces/site.fabricate.api.html#site.fabricate.api/produce!) multimethod to generate a page from the entry's data. Like build, it dispatches on two keys:
   1. `site.fabricate.document/format`
   2. `site.fabricate.page/format`
   
   This generates the actual output used by Fabricate. For example, Fabricate's defaults generate HTML files from Hiccup data structures. After generating this output, 

By separating the generation of structured data for each source from the generation of output files and adding an intermediate step, Fabricate makes it easier to do tasks like the following:
1. Generate a chronological index of pages
2. Add "related pages" to existing pages by matching on page content or metadata
3. Add a table of contents to each page after generating data for that page

In many other static website generators, features like these are "options" that you have to remember [configuration](https://jekyllrb.com/docs/configuration/options/) for, and frequently can't customize beyond the parameters that have already been designed into them. Fabricate takes less of a stance on how your website should look; instead, Fabricate gives you the ability to evaluate code to produce the contents of your website. This is true for both individual pages and for the website generation process.

> The basic principle of recursive design is to make the parts have the same power as the whole.
[Bob Barton](https://worrydream.com/EarlyHistoryOfSmalltalk/)

This API is stable and will not change. Other features of Fabricate are moving towards stability now that this foundation is in place.

## Clojure evaluation
Also quite important: it now has a namespace that makes it easier to generate Fabricate documents and pages from plain Clojure source files. I think one of the biggest drawbacks of Fabricate's first release was the template files, which relied on an emacs mode to work effectively in an editor. I hope this makes it easier for users to get started.

You might think of this [Clojure source API](https://fabricate.site/reference/namespaces/site.fabricate.prototype.source.clojure) as a more lighter-weight, less "batteries included" version of Clay that outputs plain HTML.

However, on a conceptual level, Fabricate doesn't necessarily _compete_ with Clay, or Clerk, or even Markdown so much as _aggregate_ them. You could, if you wanted to, generate some parts of your site with Clay or Clerk, and others using Fabricate's functions. I intentionally designed its API to be open-ended and extensible to new ways of generating pages.

## Planned work
Markdown support is planned. More design work will be necessary in order to reconcile Markdown's [limitations](https://docs.racket-lang.org/pollen/second-tutorial.html#(part._the-case-against-markdown)) with Fabricate's data model. However, this post was written in Markdown, so interested users can look at Fabricate's [build namespace](https://github.com/fabricate-site/manual/blob/publish/src/site/fabricate/dev/build.clj) to see how it currently supports generating pages from Markdown source files.

I also plan to more thoroughly integrate Fabricate's various source-file implementations with the emerging [kindly](https://scicloj.github.io/kindly-noted/kindly) protocol for displaying Clojure values.



