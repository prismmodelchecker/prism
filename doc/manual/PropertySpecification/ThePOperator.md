# The P operator

One of the most important operators in the PRISM property specification language is the `P` operator, which is used to reason about the probability of an event's occurrence. This operator was originally proposed in the logic PCTL but also features in the other logics supported by PRISM, such as CSL. The `P` operator is applicable to all types of models supported by PRISM.

Informally, the property:

```prism
P bound [ pathprop ]
```

is true in a state of a model if
"the probability that path property `pathprop` is satisfied by the paths from the state
meets the bound `bound`".
A typical example of a bound would be:

```prism
P>0.98 [ pathprop ]
```

which means: "the probability that `pathprop` is satisfied by the paths from the state is greater than 0.98".

More precisely, `bound` can be any of `>=p`, `>p`, `<=p` or `<p`,
where `p` is any constant [expression](../Reference/PRISMLanguageSyntax.md#expressions) evaluating to a double in the range [0,1], for example:

```prism
P>=1.0 [ pathprop ]
P<q [ pathprop ]
P<=(1-2*q) [ pathprop ]
```

The types of path property supported by PRISM and their semantics will be discussed shortly.

<a id="nondeterminism"></a>
## Nondeterminism

Many of the model types supported by PRISM exhibit **nondeterminism**,
notably MDPs and their various generalisations, such as PTAs and POMDPs.
These models use the notion of a **policy**
(called a **strategy** in PRISM, and also sometimes referred to as a *scheduler* or *adversary*)
which defines one possible way of resolving the nondeterministic choices in a model.

For such models, the meaning of the property `P bound [ pathprop ]` is:
"the probability that `pathprop` is satisfied meets the bound `bound`
*for all possible policies/strategies*".
This means that properties using the `P` operator effectively reason about the
*minimum* or *maximum* probability, over all possible resolutions of nondeterminism,
that a certain type of behaviour is observed.

<a id="numerical"></a>
## Numerical properties

It is very often useful to compute the actual probability that some behaviour of a model is observed,
rather than just verifying whether or not the probability is above or below a given bound.
Hence, PRISM allows the `P` operator to take the following **numerical** form:

```prism
P=? [ pathprop ]
```

This asks "what is the propability that `pathprop` is satisfied?"
and returns a numerical rather than a Boolean value.
The [S](TheSOperator.md) and [R](Reward-basedProperties.md) operators, discussed later, can also be used in this way.

For nondeterministic models such as MDPs (discussed above),
either minimum or maximum probability values can be queried in this way:

```prism
Pmin=? [ pathprop ]
Pmax=? [ pathprop ]
```

which return the minimum and maximum probabilities, respectively, over all policies/strategies.
As well as computing an optimal value, PRISM can also [generate a strategy](../RunningPRISM/Strategies.md) that achieves it.

PRISM properties generalise the syntax for [expressions](../Reference/PRISMLanguageSyntax.md#expressions) in the PRISM language
so further numerical properties can be constructed by combining them with arithmetic operators and functions, e.g.:

```prism
1 - P=? [ pathprop ]
```

"the probability that `pathprop` is *not* true"

```prism
P=? [ F fail_A ] / P=? [ F any_fail ]
```

"the (conditional) probability that component A eventually fails, given
that at least one component fails"

<a id="path"></a>
## Path properties

PRISM supports a wide range of path properties that can be used with the `P` operator.
A path property is a formula that evaluates to either true or false for a single path in a model.
Here, we review some of the simpler properties that feature a single *temporal operator*,
as used for example in the logics PCTL and CSL. Later, we briefly describe how PRISM also supports more complex LTL-style path properties.

The basic different types of path property that can be used inside the `P` operator are:

- `X` : "ne**x**t"
- `U` : "**u**ntil"
- `F` : "eventually" (sometimes called "**f**uture")
- `G` : "always" (sometimes called "**g**lobally")
- `W` : "**w**eak until"
- `R` : "**r**elease"

In the following sections, we describe each of these *temporal operators*. We then discuss the (optional) use of time bounds with these operators. Finally, we also discuss LTL-style path properties.

<a id="next"></a>
## "Next" path properties

The property `X prop` is true for a path if `prop` is true in its second state,
An example of this type of property, used inside a `P` operator, is:

```prism
P<0.01 [ X y=1 ]
```

which is true in a state if "the probability of the expression `y=1` being true in the next state is less than 0.01".

<a id="until"></a>
## "Until" path properties

The property `prop1 U prop2` is true for a path if
`prop2` is true in some state of the path and `prop1` is true in all preceding states.
A simple example of this would be:

```prism
P>0.5 [ z<2 U z=2 ]
```

which is true in a state if "the probability that `z` is eventually equal to 2, and that `z` remains less than 2 up until that point, is greater than 0.5".

<a id="eventually"></a>
## "Eventually" path properties

The property `F prop` is true for a path if `prop` eventually becomes true at some point along the path. The `F` operator is in fact a special case of the `U` operator (you will often see `F prop` written as `true U prop`). A simple example is:

```prism
P<0.1 [ F z>2 ]
```

which is true in a state if "the probability that `z` is eventually greater than 2 is less than 0.1".

<a id="globally"></a>
## "Globally" path properties

Whereas the `F` operator is used for "reachability" properties, `G` represents "invariance". The property `G prop` is true of a path if `prop` remains true at all states along the path. Thus, for example:

```prism
P>=0.99 [ G z<10 ]
```

states that, with probability at least 0.99, `z` never exceeds 10.

<a id="weak-until-release"></a>
## "Weak until" and "release" path properties

Like `F` and `G`, the operators `W` and `R` are derivable from other temporal operators.

Weak until (`a W b`), which is equivalent to `(a U b) | G a`, requires that `a` remains true until `b` becomes true, but does not require that `b` ever becomes true (i.e. `a` remains true forever). For example, a weak form of the until example used above is:

```prism
P>0.5 [ z<2 W z=2 ]
```

which states that, with probability greater than 0.5, either `z` is always less than 2, or it is less than 2 until the point where `z` is 2.

Release (`a R b`),  which is equivalent to `!(!a U !b)`, informally means that `b` is true until `a` becomes true, or `b` is true forever.

<a id="bounded"></a>
## "Bounded" variants of path properties

All of the temporal operators given above, with the exception of `X`, have "bounded" variants, where an additional time bound is imposed on the property being satisfied.
The most common case is to use an upper time bound, i.e. of the form "`<=t`" or "`<t`", where `t` is a PRISM expression evaluating to a constant, non-negative value.

For example, a bounded until property `prop1 U<=t prop2`, is satisfied along a path if `prop2` becomes true within `t` steps and `prop1` is true in all states before that point.
A typical example of this would be:

```prism
P>=0.98 [ y<4 U<=7 y=4 ]
```

which is true in a state if "the probability of `y` first exceeding 3 within 7 time units is greater than or equal to 0.98". Similarly:

```prism
P>=0.98 [ F<=7 y=4 ]
```

is true in a state if "the probability of `y` being equal to 4 within 7 time units is greater than or equal to 0.98" and:

```prism
P>=0.98 [ G<=7 y=4 ]
```

is true if the probability of `y` staying equal to 4 for 7 time units is at least 0.98.

The time bound can be an arbitrary (constant) expression,
but note that you may need to bracket it,
as in the following example:

```prism
P>=0.98 [ G<=(2*k+1) y=4 ]
```

You can also use lower time-bounds (i.e. `>=t` or `>t`) and time intervals `[t1,t2]`, e.g.:

```prism
P>=0.98 [ F>=10 y=4 ]
P>=0.98 [ F[10,20] y=4 ]
```

which refer to the probability of `y` becoming equal to 4 after 10 or more time units, and after between 10 and 20 time-units respectively.

For CTMCs, the time bounds can be any (non-negative) numerical values - they are not restricted to integers, as for discrete-time models.
For example:

```prism
P>=0.25 [ y<=1 U<=6.5 y>1 ]
```

means that the probability of `y` being greater than 1 within 6.5 time-units (and remaining less than or equal to 1 at all preceding time-points) is at least 0.25.

<a id="transient"></a>
## Transient probabilities

We can also use the bounded `F` operator to refer to a single time instant, e.g.:

```prism
P=? [ F[10,10] y=6 ]
```

or, equivalently:

```prism
P=? [ F=10 y=6 ]
```

both of which give the probability of `y` being 6 at time instant 10.

<a id="ltl"></a>
## LTL-style path properties

PRISM also supports probabilistic model checking of the temporal logic LTL (and, in fact, PCTL\*). LTL provides a richer set of path properties for use with the `P` operator, by permitting temporal operators to be combined. Here are a few examples of properties expressible using this functionality:

```prism
P>0.99 [ F ( "request" & (X "ack") ) ]
```

"with probability greater than 0.99, a request is eventually received, followed immediately by an acknowledgement"

```prism
P>=1 [ G F "send" ]
```

"a message is sent infinitely often with probability 1"

```prism
P=? [ F G ("error" & !"repair") ]
```

"the probability of an error occurring that is never repaired”

Note that logical operators have precedence over temporal ones, so you will often need to include parentheses when using logical operators, e.g.:

```prism
P=? [ (F "error1") & (F "error2") ]
```

For temporal operators, unary operators (such as `F`, `G` and `X`) have precedence over binary ones (such as `U`). Unary operators can be nested, without parentheses, but binary ones cannot.

So, these are allowed:

```prism
P=? [ F X X X "a" ]
P=? [ "a" U X X X "error" ]
P=? [ ("a" U "b") U "c" "error" ]
```

but this is not:

```prism
P=? [ "a" U "b" U "c" "error" ]
```
