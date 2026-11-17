# Uncertain models

PRISM has support for **uncertain** models, in which there is epistemic uncertainty
regarding some quantitative aspects of the probabilistic models being verified.
In particular, it supports **interval MDPs** (IMDPs) and **interval DTMCs** (IDTMCs),
which are MDPs or DTMCs in which transition probabilities are specified as intervals,
indicating that the exact probability is not precisely known.
This can be useful, for example, when the transition probabilities have been estimated from data.

You can find some example models included in the PRISM distribution.
Look in the `prism-examples/imdps` and `prism-examples/idtmcs` directories.

<a id="modelling"></a>
## Modelling

Here is a simple example of an IMDP, modelled in PRISM.
A robot can choose between two routes to a goal:
a fast route, whose probability of success is only known to lie in the range `[0.7,0.9]`,
and a slower route, whose probability of success is known to be 0.8.

[routes.prism](../files/routes.prism "include")

Intervals are used by simply replacing the probabilities attached to updates in commands
with intervals, such as `[0.7,0.9]` above.
Single values (e.g., `0.8` in the example) are treated as single-point intervals (`[0.8,0.8]`).
There is no separate model type keyword for interval models:
a `dtmc` or `mdp` model containing intervals is treated as an IDTMC or IMDP, respectively.
Intervals can also be used in POMDPs, giving interval POMDPs (IPOMDPs),
but these can only be built, not model checked.

The resulting IMDP is shown below.

![IMDP with four states. From state 0 (start), action fast goes to state 2 (goal) with probability in the interval [0.7,0.9] or to state 3 (crash) with probability in [0.1,0.3]; action slow goes to state 1. From state 1, action slow goes to state 2 (goal) with probability 0.8 or state 3 (crash) with probability 0.2. States 2 and 3 have done self-loops.](../files/routes.svg)

As usual, the interval bounds can be expressions involving constants or state variables, for example:

```prism
[] x=0 -> [p,p+0.1]:(x'=0) + [0.9-p,1-p]:(x'=1);
```

The intervals in each command must allow at least one probability distribution,
i.e., the lower bounds must sum to at most 1 and the upper bounds to at least 1.
PRISM checks this when the model is built and reports an error otherwise.

When two commands in different modules [synchronise](../ThePRISMLanguage/CompositionAndSynchronisation.md#synchronisation)
and specify the probabilities for updates to variables as intervals, these are considered to be independent,
i.e., the probability of each combined transition is taken to be the product of the two component intervals.

<a id="properties"></a>
## Properties

For uncertain models, PRISM performs **robust** verification,
which considers the best- or worst-case behaviour that can arise
depending on the way that probabilities are selected from intervals.

For example, instead of a property for a DTMC such as

```prism
P=? [ F "goal" ]
```

which asks for the probability to reach a state satisfying `"goal"`, IDTMCs use MDP-style queries:

```prism
Pmin=? [ F "goal" ]
Pmax=? [ F "goal" ]
```

which compute the minimum or maximum possible probability that can arise.

For an IMDP, there are two separate quantifications,
firstly over strategies (policies) and secondly over the distinct ways
that transition probabilities can be selected from intervals.
For these, `min` or `max` appear in that order in the query.
For example, for the IMDP above:

[routes.props](../files/routes.props "include")

The first property asks for the maximum probability of reaching the goal,
assuming the worst case for the uncertain probabilities;
the result is 0.8, achieved by taking the slow route.
The second assumes the best case instead;
the result is 0.9, achieved by taking the fast route.
Similarly, `minmin` and `minmax` are used for the minimum probability of reaching `"goal"`.
If only one of `min` or `max` is given (e.g., `Pmax=?`), it is used for both.

Model checking is supported for:

- the `P` operator, including single temporal operators and LTL formulae
- the `R` operator, for the expected reward to reach a target or satisfy a co-safe LTL formula, or total (possibly discounted) reward

<a id="solution-methods"></a>
## Solution methods

Uncertain models are analysed with the [explicit engine](../ConfiguringPRISM/ComputationEngines.md),
which PRISM switches to automatically.
The method used is **robust value iteration**,
which is like the [value iteration](../ConfiguringPRISM/SolutionMethodsAndOptions.md#mdp) used for MDPs,
except that each iteration also finds the best or worst probabilities within the intervals.
By default, the Gauss-Seidel variant is used;
standard value iteration can be selected with the [`-valiter`](../Reference/CommandLineSwitches.md#valiter) switch
(or the "IMDP/DTMC solution method" setting in the GUI).

[Strategies](../RunningPRISM/Strategies.md) can be generated and exported for IMDPs as for MDPs.
For the example above, the strategy chooses `slow` in state 0 for `Pmaxmin=?`
and `fast` for `Pmaxmax=?`.
