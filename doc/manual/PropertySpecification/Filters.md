# Filters

As discussed above, when reporting the result of model checking a property, PRISM will by default return the value for the (single) initial state of the model. However, since PRISM in fact usually has to compute values for *all* states simultaneously, you can customise PRISM properties to obtain different results. This is done using *filters*.

Filters are created using the `filter` keyword. They take the following form:

```prism
filter(op, prop, states)
```

where `op` is the filter *operator* (see below), `prop` is any PRISM property and `states` is a Boolean-valued expression identifying a set of states over which to apply the filter.

In fact, the `states` argument is optional; if omitted, the filter is applied over all states. So, the following properties are equivalent:

```prism
filter(op, prop)
filter(op, prop, true)
```

Here's a simple example of a filter:

```prism
filter(max, P=? [ F "error" ], x=0)
```

This gives the *maximum* value, starting from any state satisfying `x=0`, of the probability of reaching an "error" state.

Here's another simple example,
which checks whether, *starting from any reachable state*,
we eventually reach a "done" state with probability 1.

```prism
filter(forall, P>=1 [ F "done" ])
```

We could modify this property slightly to instead check whether, from any state that satisfies the label "ready", we eventually reach a "done" state with probability 1. This could be done with either of the following two equivalent properties:

```prism
filter(forall, "ready" => P>=1 [ F "done" ])
filter(forall, P>=1 [ F "done" ], "ready")
```

**Note:** In older versions of PRISM, the property above could be written just as `"ready" => P>=1 [ F "done" ]` since the result was checked for all states by default, not just the [initial state](SyntaxAndSemantics.md). Now, you need to explicitly include a filter, as shown above, to achieve this.

## Types of filter

Most filters of the form `filter(op, prop, states)`
apply some operator `op` to the values of property `prop`
for all the states satisfying `states`,
resulting in a single value.
The full list of filter operators `op` in this category is:

- `min`: the minimum value of `prop` over states satisfying `states`
- `max`: the maximum value of `prop` over states satisfying `states`
- `count`: counts the number of states satisfying `states` for which `prop` is true
- `sum` (or `+`): sums the value of `prop` for states satisfying `states`
- `avg`: the average value of `prop` over states satisfying `states`
- `first`: the value of `prop` for the first (lowest-indexed) state satisfying `states`
- `range`: the range (interval) of values of `prop` over states satisfying `states`
- `forall` (or `&`): returns `true` if `prop` is `true` for all states satisfying `states`
- `exists` (or `|`): returns `true` if `prop` is `true` for some states satisfying `states`
- `state`: returns the value for the single state satisfying `states` (if there is more than one, this is an error)

There are also a few filters that, rather than returning a single value, return different values for each state, like a normal PRISM property:

- `argmin`: returns `true` for the states satisfying `states` that yield the minimum value of `prop`
- `argmax`: returns `true` for the states satisfying `states` that yield the maximum value of `prop`
- `print`: does not change the result of `prop` but prints the (non-zero) values to the log
- `printall`: like `print`, but displays *all* values, even for states where the value is zero

## More examples

Here are some further illustrative examples of properties that use filters.

Filters provide a quick way to *print* the results of a model checking query for several states. In most cases, for example, a `P=?` query just returns the probability from the initial state. To see the probability for all states satisfying `x>2`, use:

```prism
filter(print, P=? [ ... ], x>2)
```

Values are printed in the log (i.e. to the "Log" tab in the GUI or to the terminal from the command-line). For small models, you could omit the final `states` argument (`x>2` here) and view the probabilities from all states. You can also use PRISM's [verbose](../ConfiguringPRISM/OtherOptions.md) mode to view values for all states, but filters provide an easier and more flexible solution.
`print` filters do not actually alter the result returned so, in the example above, PRISM will still return the probability for the initial state, in addition to printing other probabilities in the log.

You can also use `print` filters to display lists of states. For example, this property:

```prism
filter(print, filter(argmax, P=? [ F "error" ]))
```

prints the states which have the highest probability of reaching an error state.
However, you should exercise caution when using `argmax` (or `argmin`) on properties such as `P=? [ ... ]` (or `S=? [ ... ]` or `R=? [ ... ]`), whose results are only approximate due to the nature of the methods used to compute them (or because of round-off errors.)

Another common use of filters is to display the value for a particular state of the model (rather than the initial state, which is used by default). To achieve this, use e.g.:

```prism
filter(state, P=? [ F "error" ], x=2&y=3)
```

where `x=2&y=3` is assumed to specify one particular state.
A `state` filter will produce an error if the filter expression is not satisfied by exactly one state of the model.

Filters can also be built up into more complex expressions. For example, the following two properties are equivalent:

```prism
filter(avg, P=? [ F "error" ], "init")
filter(sum, P=? [ F "error" ], "init") / filter(count, "init")
```

The `range` filter, unlike most PRISM expressions which are of type Boolean, integer or double, actually returns an interval: a pair of integers or doubles. For example:

```prism
filter(range, P=? [ F count=10 ], count=0)
```

gives the range of all possible values for the probability of reach a state satisfying `count=10`, from all states satisfying `count=0`.
As will be described below, this kind of property also results from the use of old-style (`{...}`) filters and properties on models with multiple initial states.

## Old-style filters

In older versions of PRISM, filters were also available, but in a less expressive form. Previously, they were only usable on `P`, `S` or `R` properties and only a small set of filter operators were permitted. They were also specified in a different way, using braces (`{`...`}`). For compatibility with old properties files (and for compactness), these forms of filters are still allowed. These old-style forms of filters:

```prism
P=? [ pathprop {states} ]
P=? [ pathprop {states}{min} ]
P=? [ pathprop {states}{max} ]
P=? [ pathprop {states}{min}{max} ]
```

are equivalent to:

```prism
filter(state, P=? [ pathprop ], states)
filter(min, P=? [ pathprop ], states)
filter(max, P=? [ pathprop ], states)
filter(range, P=? [ pathprop ], states)
```

Notice that the first of the four properties above (i.e. an old-style filter of the form `{states}`) will result in an error if `states` is not satisfied by exactly one state of the model. Older versions of PRISM just gave you the value for the first state satisfying the filter, without warning you about this. If you want to recreate the old behaviour, just use a `first` filter:

```prism
filter(first, P=? [ pathprop ], states)
```

## Default filters

Finally, for completeness, we show what the *default* filters are in PRISM,
i.e. how the way that PRISM returns values from properties by default
could have been achieved equivalently using filters.

Queries of the form:

```prism
P>0.5 [ F "error" ]
```

are the same as:

```prism
filter(forall, P>0.5 [ F "error" ], "init")
```

and queries of the form:

```prism
P=? [ F "error" ]
```

are the same as either:

```prism
filter(state, P=? [ F "error" ], "init")
filter(range, P=? [ F "error" ], "init")
```

for the cases where there the model has a single initial state
or multiple initial states, respectively.
