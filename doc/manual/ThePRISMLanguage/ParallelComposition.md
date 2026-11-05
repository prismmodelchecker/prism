# Parallel Composition

The probabilistic model corresponding to a PRISM language description is constructed as the *parallel composition* of its modules. In every state of the model, there is a set of commands (belonging to any of the modules) which are enabled, i.e. whose guards are satisfied in that state. The choice between which command is performed (i.e. the *scheduling*) depends on the model type.

For an MDP, as in [Example 1](Example1.md), the choice is *nondeterministic*. By way of example, consider state `(0,0)` (i.e.  `x=0` and `y=0`). There are two commands enabled, one from each module:

```prism
[] x=0 -> 0.8:(x'=0) + 0.2:(x'=1);
```

```prism
[] y=0 -> 0.8:(y'=0) + 0.2:(y'=1);
```

In state `(0,0)` of the MDP, there would be a nondeterministic choice between these two probability distributions:

- `0.8:(0,0) + 0.2:(1,0)` (module `M1` moves)
- `0.8:(0,0) + 0.2:(0,1)` (module `M2` moves)

For a DTMC, the choice is *probabilistic*: each enabled command is selected with equal probability.
If [Example 1](Example1.md) was a DTMC, then in state `(0,0)` of the model
the following probability distribution would result:

- `0.8:(0,0) + 0.1:(1,0) + 0.1:(0,1)`

For a [CTMC](CTMCs.md), as will be discussed shortly,
the choice is modelled as a "race" between transitions.

See the later sections on "[Synchronisation](Synchronisation.md)" and "[Process Algebra Operators](ProcessAlgebraOperators.md)" for other topics related to parallel composition.
