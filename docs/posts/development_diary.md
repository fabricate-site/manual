# Development diary

## Context

Notes that don't go anywhere else go here. Posted in reverse chronological order.

## 2026-02-19

`api/display-form` is a nice convenience; I see it as analogous to `clojure.core/print-dup`, but with a `:kind` and a page format rather than an object type and a writer. What is lacking is a standardized way of transforming a nested value into output in the way that `pr` does. Looking at [`core_print.clj`](https://github.com/clojure/clojure/blob/a3fa897590f70207eea3573759739810f2b6ab6c/src/clj/clojure/core_print.clj#L117), its implementation is simpler because it's just looping through Clojure data and appending / flushing to a writer. It has an easier time preserving nested structure because it just needs to append the appropriate delimiter to the writer when reaching the end of a collection rather than traverse back up to the parent element. 

I want to output a plain Hiccup data structure from a data structure containing nested Kindly-annotated values, so this is more analogous to a `clojure.walk` operation. 

I previously thought `kindly-advice` was the way to do this, but it (perhaps unsurprisingly) doens't have the ability to recurse through arbitrarily nested data to find the Kindly-annotated values.

This means that I should also implement a convenience function in `site.fabricate.prototype.kindly` to walk and transform a document with nested forms. The nice thing about `walk` is that it's completely agnostic to the data structure you're using.

## 2026-01-06

I am observing what I consider to be inconsistencies in the behavior and return values of the functions in `kindly-advice`. Here are some examples:

```clojure

(-> {:form ^{:kindly/kind :kind/vector} [1 2 3]}
    (scicloj.kindly-advice.v1.completion/complete)
    (scicloj.kindly-advice.v1.api/advise))
```

This returns:

```clojure
{:form [1 2 3],
 :value [1 2 3],
 :meta-kind :kind/vector,
 :kindly/options {:hide-code true},
 :kind :kind/vector,
 :advice
 [[:kind/vector {:reason :metadata}]
  [:kind/vector {:reason :predicate}]
  [:kind/seq {:reason :predicate}]]}

```

If `:kindly/kind` is the expected way to specify the kind of a value using metadata, why does the namespace get removed?

If I try to use the `scicloj.kindly-advice.v1.completion` namespace to return a completed form map, I also observe:

```clojure
(scicloj.kindly-advice.v1.completion/complete
 {:form ^{:kindly/kind :kind/vector}
  [1 2 3]})
```


```clojure
{:form [1 2 3],
 :value [1 2 3],
 :meta-kind :kind/vector,
 :kindly/options {:hide-code true}}
```

This doesn't set `:kindly/kind` _or_ `:kind`. 

Should `:kind` be compatible in the other direction? For example: `:kind :kind/hiccup`. It's currently not. This could just be based on my own misunderstanding. If I fix some of my assumptions here I could also radically simplify the code - I could potentially delete the evaluation namespace and remove many of the functions I currently use to haphazardly normalize values to kindly context maps.

I still think the meta-kind vs kind vs kindly/kind thing is worth asking about, if only to better understand the rationale.


## 2025-12-26: Kindly elements in Hiccup forms

With the addition of the `kindly-forms` branch, the implementations of both the plain-clojure and template source formats yield Hiccup forms that can contain Kindly context maps at arbitrary levels of depth. There now arises a question about how to handle these when generating output - right now they are included verbatim in the output.

The `chassis` library for rendering Hiccup to HTML might have an effective way to handle this. While I don't necessarily want to tie Fabricate's default implementation to library-specific extensions to the ordinary Hiccup model, I _can_ leverage it however I'd like in the context of generating Fabricate's manual.

The multimethod that allows for 'alias' elements to be automatically converted to their vanilla counterparts is: `dev.onionpancakes.chassis.core/resolve-alias`. I could just allow the implementations to return an element that Chassis will rewrite into 'vanilla Hiccup' before rendering the Hiccup into HTML.

Alternatively, I could provide my own implementations of chassis's core protocols. Specifically, I could add a predicate that determines whether a map is a Kindly map, then call the Kindly->HTML function on that and append it to the string that chassis is constructing. The specific protocol that does this is `dev.onionpancakes.chassis.core/AttributeValueFragment`. This seems more difficult, more complicated, and therefore more error-prone, even if theoretically attractive for its interface-level simplicity. 

Explicit handling of Kindly maps is better than implicit handling, and I don't necessarily want to spend too much time on the internals of a library I plan to use as a tool. So the alias element approach here seems like the correct one for the time being.

## 2025-12-23: `kindly-advice` notes

I spent some time working on a [preliminary implementation](https://github.com/fabricate-site/fabricate/commit/95919dcaaad45c4c634f22826a9ba15e2a42dd57) of a normalizer function for kindly values. This function converts any value annotated with kindly metadata at any level of depth within a form to a "context map" representation using kindly-advice.

However, this work left me with a question: Is recursive normalization in the `build` step necessary? 
If Fabricate produces a normalized Hiccup vector of page contents, which the library
expects to use as the main intermediate format, then performing this
normalization too early would require Fabricate to walk the results a second
time to recursively transform all nested kindly maps into plain Hiccup data
structures.

Using advisors, it seems like this normalization can be done in a
"just-in-time" fashion; a form is walked, kindly-advice gets called on any
kindly values, and then the resulting advice is used to figure out how to
conver that specific form to Hiccup that Fabricate will render into HTML.

The case for _not_ normalizing in `api/build` and then again in
`api/produce!` is that different output formats may need to handle kinds
differently. Fully normalized Hiccup is a format that may erase information
needed by the rendering/output tools.

This may be what Tim was getting at in his [response](https://clojurians.zulipchat.com/#narrow/channel/454856-kindly-dev/topic/Advising.20.2B.20normalizing.20nested.20values/with/564859985) to my suggestion on Zulip.

This postwalk stuff isn't wasted effort; it helped me better understand the
problem context.

However, it may not make sense to use postwalk for everything. What if some
sections want to handle certain kinds differently? Then there needs to be a
way to pass context-specific information to a form normalizer.

