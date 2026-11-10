# Filters

A property in the PRISM property language is evaluated with respect to a particular *state* of a model,
and can yield a value of various types (Boolean, numerical, etc.), depending on the type of property.
When performing model checking, PRISM often computes the value of a property for *all* states of the model,
but typically just reports the result as a single value.
Usually, this is the value for the (single) initial state of the model. For example, this:

```prism
P=? [ F "error" ]
```

will report the probability, from the initial state of the model, of reaching an "error" state.
This:

```prism
P>0.5 [ F "error" ]
```

will return `true` if and only if the probability, from the initial state, is greater than 0.5.

For models with [multiple initial states](../ThePRISMLanguage/Variables.md#multiple-initial-states), we need to adapt these definitions slightly.
In this case, the two properties above will yield, respectively:

- the range of values (over all initial states) of the probability of reaching "error"

- `true` if and only if the probability is greater than 0.5 from *all* initial states.

Using **filters**, you can ask PRISM to obtain different results.
This is done using `filter` properties, which have the form:
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

<a id="filter-operators"></a>
## Filter operators

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

<a id="examples"></a>
## Examples

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

gives the range of all possible values for the probability of reaching a state satisfying `count=10`, from all states satisfying `count=0`.
This kind of value also results from numerical properties on models with multiple initial states (see above).

<a id="defaults"></a>
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
