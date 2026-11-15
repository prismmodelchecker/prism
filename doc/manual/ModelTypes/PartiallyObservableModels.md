# Partially observable models

PRISM supports analysis of **partially observable** probabilistic models,
most notably partially observable Markov decision processes (POMDPs),
but also partially observable probabilistic timed automata (POPTAs).

POMDPs are a variant of MDPs in which the strategy (or policy)
that resolves nondeterministic choices in the model cannot
see the precise state of the model, but instead just **observations** of it.
A strategy can therefore only base its choices on the history of observations so far
or, equivalently, on a **belief**: a probability distribution over the states that the model could be in.

For background material on POMDPs and POPTAs, see for example [[NPZ17](../Reference/References.md#NPZ17)].
You can also find several example models included in the PRISM distribution.
Look in the `prism-examples/pomdps` and `prism-examples/poptas` directories.

<a id="modelling"></a>
## Modelling

Here is a simple example of a POMDP, modelled in PRISM.
First, a hidden variable `h` is set randomly to 1, 2 or 3 (with probabilities 0.1, 0.3 and 0.6).
Then, a single guess is made of its value.

[pomdp.prism](../files/pomdp.prism "include")

The model type is indicated as `pomdp` at the top of the file
(in fact PRISM will also detect an MDP model with observables and treat it as a POMDP).

PRISM supports state-based observations:
this means that, upon entering a new POMDP state,
the observation is determined by that state.
In the same way that a model state comprises the values of one or more variables,
an observation comprises one or more **observables**.
In the example above, only the variable `s` is observable;
the hidden variable `h` is not.

The resulting POMDP is shown below. The three states in which the guess is made
all have the same observation (`s=1`), so a strategy cannot distinguish between them
and must make the same guess in each one.
For clarity, the final states are shown merged, since the value of `h` no longer matters there.

![POMDP with initial state 0 (h=0), where a toss action goes to three states, all with s=1, where h is 1, 2 or 3, with probabilities 0.1, 0.3 and 0.6. These three states are grouped as a single observation (s=1). From each one, the guess action matching h (guess1, guess2 or guess3) goes to state 2 (correct) and the other guesses go to state 3 (wrong). States 2 and 3 have done self-loops.](../files/pomdp.svg)

There are several ways to define observables.
The simplest, used above, is to specify a subset of the model's variables
that are designated as being observable. The rest are unobservable.
For example, in a POMDP with 3 variables, `s`, `l` and `h`, the following:

```prism
observables s, l endobservables
```

specifies that `s` and `l` are observable and `h` is not.

Alternatively, observables can be specified as arbitrary expressions over variables.
For example, assuming the same variables `s`, `l` and `h`, this specification:

```prism
observable "s" = s;
observable "pos" = l>0;
```

defines 2 observables. The first is, as above, the variable `s`.
The second, named `"pos"`, determines if variable `l` is positive.
Other than this, the values of `l` and `h` are unobservable.

The above two styles of definition can also be mixed
to specify a combined set of observables.

<a id="poptas"></a>

**POPTAs:**
Partially observable PTAs (POPTAs) combine the features of both [PTAs](Real-timeModels.md) and POMDPs.
The model type is indicated as `popta`
(PRISM will also detect a PTA model with observables and treat it as a POPTA).
For a POPTA, all clock variables must be observable.

<a id="properties"></a>
## Properties

For partially observable models (POMDPs and POPTAs),
PRISM uses the same property language as their fully observable equivalents (MDPs and PTAs).
However, a more limited range of properties is available.
For POMDPs, PRISM supports probabilistic reachability,
probabilistic until, probabilistic invariance (`G`) or expected reachability rewards properties, i.e.:

```prism
Pmin=? [ F target ]
Pmax=? [ F target ]
Pmin=? [ remain U target ]
Pmax=? [ remain U target ]
Pmin=? [ G safe ]
Pmax=? [ G safe ]
Rmin=? [ F target ]
Rmax=? [ F target ]
```

or variants with a probability or reward bound (e.g., `P>=0.5`)
instead of the `min=?` or `max=?`.

The expressions such as `target`, `remain` and `safe` above
can, as usual, refer to [labels](../ThePRISMLanguage/ConstantsLabelsAndFormulas.md#labels)
(with some restrictions, see below)
but also to additional named observables defined in the model
(e.g., `"pos"` in the example above).

An example property for the `pomdp.prism` model above is:

[pomdp.props](../files/pomdp.props "include")

This yields a result of 0.6: the best strategy is always to guess the most likely value (3).
In an MDP, where `h` would be visible, the probability would be 1.

**Restrictions:**
There are some additional constraints on properties for partially observable models:

- The `target` (and `remain` or `safe`) expression in the property must be **observable**.
  In other words, if any state of the POMDP satisfies the expression,
  then all other observationally equivalent states must also satisfy it.
  This is easily achieved by only using either observable variables
  or named observables in the expression, but that is not required.
- Values are only computed from a single initial state.
- Time-bounded properties (e.g., `F<=k`) and nested properties are not supported.

POPTAs are verified using the "digital clocks" approach to
translate them into a POMDP, so the same restrictions apply,
as well as those of the [digital clocks engine](Real-timeModels.md#solution-methods)
(e.g., strict or diagonal clock comparisons are not allowed).
However, for POPTAs, time-bounded probabilistic reachability is also supported.

<a id="solution-methods"></a>
## Solution methods

POMDPs are analysed with the [explicit engine](../ConfiguringPRISM/ComputationEngines.md),
which PRISM switches to automatically.
The method used is a **fixed-resolution grid** approximation of the (infinite) belief MDP [[Lov91](../Reference/References.md#Lov91)],
as described in [[NPZ17](../Reference/References.md#NPZ17)].
The resolution of the grid is set with the [`-gridresolution`](../Reference/CommandLineSwitches.md#gridresolution) switch (default 10).
Higher values give more accurate results, at the expense of time and memory.

Since the result is approximate, PRISM computes two bounds on the true value:
an *outer* bound, from the grid approximation,
and an *inner* bound, from the strategy that the approximation generates.
Both are shown in the log, and the result is reported as the interval between them, e.g.:

```shell
prism pomdp.prism pomdp.props
# ...
# Outer bound: 0.6 (exact floating point)
# Inner bound: 0.6 (exact floating point)
# Result bounds: [0.6,0.6]
```

If the bounds are far apart, try increasing the grid resolution.

The generated strategy can be exported or viewed,
as for MDPs (see [Strategies](../RunningPRISM/Strategies.md)).
It maps beliefs (rather than states) to actions.
