# Development diary

## Context

Notes that don't go anywhere else go here. Posted in reverse chronological order.

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

