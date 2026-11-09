# Multi-objective properties

For MDPs, PRISM supports *multi-objective* properties. Consider a property that uses the `P` operator. For example:

```prism
P<0.01 [ F "error" ]
```

This states that, *for all* strategies (or policies) of the MDP, the probability of reaching an `"error"` state is less than 0.01.

Multi-objective queries differ in two important ways. Firstly, (by default) they ask about the *existence* of a strategy. Secondly they refer to *multiple* properties of a strategy. For example:

```prism
multi(P<0.01 [ F "error1" ], P<0.02 [ F "error2" ])
```

means: "does there exist a strategy of the MDP under which the probability of reaching an `"error1"` state is less than 0.01 *and* the probability of reaching an `"error2"` state is less than 0.02?"

To use the terminology from [[FKP12](../Main/References.md#FKP12)], the above is an "achievability" query (i.e., is this combination of objectives achievable by some strategy?). PRISM also supports two other kinds of multi-objective query: "numerical" and "Pareto" queries.

A "numerical" query looks like:

```prism
multi(Pmin=? [ F "error1" ], P<0.02 [ F "error2" ])
```

meaning "what is the minimum possible probability of reaching `"error1"`, over all strategies of the MDP for which the probability of reaching `"error2"` is less than 0.02?".

A "Pareto" queries leaves both of the objectives unbounded, e.g.:

```prism
multi(Pmin=? [ F "error1" ], Pmin=? [ F "error2" ])
```

This asks PRISM to compute (approximately), the *Pareto curve* for this pair of objectives. Intuitively, this is the set of pairs of probabilities (of reaching `"error1"`/`"error2"`) such that reducing one probability any more would necessitate an increase in the other probability.

## Types of objectives

For simplicity, the examples above all refer to the probability of reaching classes of states in the model. Other types of property (objective) are also possible.

Firstly, we can extend the examples above by referring to the probability of any
[LTL](ThePOperator.md#ltl) property. For example:

```prism
multi(Pmax=? [ G "good1" ], P>=0.9 [ G F "good2" ])
```

"What is the maximum probability of staying forever in `"good1"` states, such that the probability of visiting `"good2"` states infinitely often remains at least 0.9?".

We can also use more than 2 objectives, e.g.:

```prism
multi(Pmax=? [ G "good1" ], P>=0.9 [ G F "good2" ], P>=0.95 [ G F "good3" ])
```

"What is the maximum probability of staying forever in `"good1"` states, such that the probability of visiting `"good2"` states infinitely often remains at least 0.9 and the probability of visiting `"good3"` states infinitely often remains at least 0.95?".

Multi-objective queries can also refer to the expected total cumulative value of a reward structure. We write such properties in the form:

```prism
multi(R{"time"}min=?[ C ], R{"energy"}<=1.45 [ C ])
```

"What is the minimum expected cumulative value of reward structure `"time"`, such that the expected cumulative value of reward structure `"energy"` is below 1.45.

Note that this `C` reward operator differs from the `F "target"` operator, usually used for standard (single-objective) MDP model checking. Whereas the `F "target"` operator refers to the expected reward accumulated until a `"target"` state is reached the `C` operator refers to the expected *total* reward.

A few important notes regarding rewards:

- Currently only transition rewards are supported; state rewards are not.

- Certain assumptions are made regarding the finiteness of rewards; see p.7 of [[FKP12](../Main/References.md#FKP12)] for details.

Finally, time-bounded variants of both probabilistic reachability and expected cumulative rewards objectives can be used. Here is an example that uses the latter:

```prism
multi(R{"power"}min=? [ C<=k ], R{"queue"}<=r [ C<=k ])
```

## Solution methods

PRISM can perform multi-objective model checking using two distinct solution methods, which are described in [[FKN+11](../Main/References.md#FKN+11)] and [[FKP12](../Main/References.md#FKP12)]. The former is based on the use of linear programming; the latter reduces multi-objective model checking to a series of simpler problems, solved using value iteration (or the Gauss-Seidel variant of value iteration). The default is "Value iteration". You can change this in the GUI using the option "MDP multi-objective solution methods", or using the command-line switches `-lp`, `-valiter`, `-gs`.

There are some restrictions for the different methods, e.g.

- Linear programming does not support time-bounded properties or Pareto queries
