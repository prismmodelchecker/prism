# Uncertain models

PRISM has support for *uncertain* models, in which there is epistemic uncertainty regarding some quantitative aspects of the probabilistic models being verified. In particular, it currently supports *interval MDPs* (IMDPs) and *interval DTMCs* (IDTMCs), which are MDPs or DTMCs in which transition probabilities can be specified as intervals, indicating that the exact probability is not precisely known. This can be useful, for example, when the transition probabilities have been estimated from data.

Currently, this is achieved by simply replacing the probabilities attached to updates in commands with intervals, e.g.:

```prism
[] x=0 -> [0.8,0.9]:(x'=0) + [0.1,0.2]:(x'=1);
```

As usual, the probability thresholds can be expressions involving state variables or constants, for example:

```prism
[] x=0 -> [p,p+0.1]:(x'=0) + [0.9-p,1-p]:(x'=1);
```

When two commands in different modules [synchronise](Synchronisation.md) and specify the probabilities for updates to variables as intervals, these are considered to be independent, i.e., the probability of each combined transition is taken to be the product of the two component intervals.

See the [property specification](../PropertySpecification/UncertainModels.md) section for details of how these models are analysed.
