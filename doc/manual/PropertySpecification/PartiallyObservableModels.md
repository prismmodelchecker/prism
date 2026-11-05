# Partially Observable Models

For partially observable models (POMDPs and POPTAs),
PRISM uses the same property language as the their
fully observational equivalents (MDPs and PTAs).
However, a more limited range of properties are available.
For POMDPs, PRISM currently supports probabilistic reachability,
probabilistic until, or expected reachability rewards properties, i.e.:

```prism
Pmin=? [ F target ]
Pmax=? [ F target ]
Pmin=? [ remain U target ]
Pmax=? [ remain U target ]
Rmin=? [ F target ]
Rmax=? [ F target ]
```

or bounded variants with a probability/threshold instead
of the `min=?` or `max=?`.

For the verification methods currently implemented,
there are a few additional restrictions.
Firstly, the `target` (and `remain`) expression appearing
in the property must be an observable.
In other words, if any state of the POMDP satisfies the expression,
then all other observationally equivalent states must also satisfy it.
This is easily achieved by only using either observable variables
or named observables in the expression, but that is not required.
Secondly, probabilities and expected rewards are only computed from a single state.

POPTAs are currently verified using the "digital clocks" approach to
translate them into a POMDP, so they inherit the same
[restrictions](Real-timeModels.md)
(that strict or diagonal clock comparisons are not allowed).
However for POPTAs, time-bounded probabilistic reachability is also supported.
