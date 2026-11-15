# Non-probabilistic models

As well as probabilistic models, PRISM supports **non-probabilistic** models,
in particular **labelled transition systems** (LTSs).
An LTS is like an MDP but without probabilities:
in each state, there is a nondeterministic choice between one or more transitions,
each of which leads to a single successor state.

LTSs can be used to check functional (rather than quantitative) correctness of a system,
using the temporal logics CTL and LTL,
for example as a first step before adding probabilities to a model.

<a id="modelling"></a>
## Modelling

LTSs are modelled using the usual PRISM language features
(modules, variables, commands, synchronisation, formulas, labels, etc.),
but with no probabilities attached to the updates of commands.
Here is a simple example: two processes, each of which can be idle, trying or in a critical section,
which use a lock to ensure mutual exclusion.

[lts.prism](../files/lts.prism "include")

The two processes do not synchronise with each other,
but each one [synchronises](../ThePRISMLanguage/CompositionAndSynchronisation.md#synchronisation)
with the `Lock` module when it enters or leaves its critical section:
for example, the action `enter1` can only occur when the lock is free (`!l`),
and it then sets the lock.

There is no model type keyword for LTSs.
If a model has no keyword (which would usually result in the default model of an MDP) and does not use any probabilities, it is treated as an LTS.
If instead it starts with the `mdp` keyword, it is treated as an MDP
(in which each choice has a single transition with probability 1).

The resulting LTS is shown below, where each state shows the values of `x`, `y` and `l`,
and transitions are labelled with their actions.
Note that the lock is set exactly when one process is in its critical section,
and that there is no state with `x=2` and `y=2`, where both processes are in the critical section.

![LTS with 8 states, arranged in a 3x3 grid by the values of x (columns) and y (rows), from 0,0,false (initial) to 1,2,true, with no state where x and y are both 2. The lock l is true in the states where x or y is 2. Process 1 moves right, with action try1 from x=0 to x=1, enter1 from x=1 to x=2 (only if y is not 2) and exit1 from x=2 back to x=0. Process 2 moves down in the same way with actions try2, enter2 and exit2.](../files/lts.svg)

<a id="properties"></a>
## Properties

LTSs can be analysed with [non-probabilistic properties](../PropertySpecification/Non-probabilisticProperties.md),
i.e., CTL and LTL formulas using the `A` (for all paths) and `E` (there exists a path) operators.
For example, for the model above:

[lts.props](../files/lts.props "include")

The first two properties are true: the protocol ensures mutual exclusion,
and process 1 can reach its critical section.
The third is false, since process 2 can repeatedly enter its critical section
while process 1 waits forever.
PRISM does not make any fairness assumptions about the choices in the model.

For some properties, PRISM also generates a [counterexample or witness](../PropertySpecification/Non-probabilisticProperties.md#counterexamples),
e.g., a path along which process 1 reaches its critical section for the second property above.

<a id="solution-methods"></a>
## Solution methods

LTSs are analysed with the [explicit engine](../ConfiguringPRISM/ComputationEngines.md),
which PRISM switches to automatically.
CTL properties are checked with standard graph-based algorithms
and LTL properties by building the product of the LTS with a Büchi automaton.
