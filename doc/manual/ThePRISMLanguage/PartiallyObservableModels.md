# Partially Observable Models

PRISM supports analysis of *partially observable* probabilistic models,
most notably partially observable Markov decision processes (POMDPs),
but also partially observable probabilistic timed automata (POPTAs).
POMDPs are a variant of MDPs in which the strategy/policy
which resolves nondeterministic choices in the model is unable to
see the precise state of the model, but instead just *observations* of it.
For background material on POMDPs and POPTAs, see for example [[NPZ17](../Main/References.md#NPZ17)].
You can also find several example models included in the PRISM distribution.
Look in the `prism-examples/pomdps` and `prism-examples/poptas` directories.

PRISM currently supports state-based observations:
this means that, upon entering a new POMDP state,
the observation is determined by that state.
In the same way that a model state comprises the values or one or more variables,
an observation comprises one or more *observables*.
There are several way to define these observables.
The simplest is to specify a subset of the model's variables
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
The named observables can then be used in properties
in the same way that [labels](FormulasAndLabels.md) can.

The above two styles of definition can also be mixed
to specify a combined set of observables.

POPTAs (partially observable PTAs) combine the features of both PTAs and POMDPs.
They are are currently analysed using the digital clocks engine,
so inherit the [restrictions for that engine](Real-timeModels.md).
Furthermore, for a POPTA, all clock variables must be observable.
