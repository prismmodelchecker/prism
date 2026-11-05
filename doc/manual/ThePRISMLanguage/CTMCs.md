# CTMCs

Specifying the behaviour of a continuous-time Markov chain (CTMC)
is done in similar fashion to a DTMC or an MDP, as discussed so far.
The main difference is that updates in commands are
labelled with (positive-valued) *rates*, rather than probabilities.
The notation used in commands, however, to associate rates to transitions is identical to
the one used to assign probabilities:

```prism
rate_1:update_1 + rate_2:update_2 + ...
```

In a CTMC, when multiple possible transitions are available in a state, a *race condition* occurs
(see e.g. [[KNP07a](../Main/References.md#KNP07a)] for more details).
In terms of PRISM commands, this can arise in several ways.
Firstly, within in a module, multiple transitions can be specified either as several different updates in a command, or as multiple commands with overlapping guards. The following, for example. are equivalent:

```prism
[] x=0 -> 50:(x'=1) + 60:(x'=2);
```

```prism
[] x=0 -> 50:(x'=1);
[] x=0 -> 60:(x'=2);
```

Furthermore, parallel composition between modules in a CTMC is modelled as a race condition,
rather as a nondeterministic choice, like for [MDPs](ParallelComposition.md).
