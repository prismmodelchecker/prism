# Uncertain Models

For uncertain models, currently interval MDPs (IMDPs) or interval DTMCs (IDTMCs), PRISM performs *robust* verification, which considers the best- or worst-case behaviour that can arise depending on the way that probabilities are selected from intervals.

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

Similarly, for an IMDP, there are now two separate quantifications, firstly over strategies (policies) and secondly over the distinct ways that transition probabilities can be selected from intervals, for which `min` or `max` appear in that order in the query. For example:

```prism
Pmaxmin=? [ F "goal" ]
Pmaxmax=? [ F "goal" ]
```

return the minimum and maximum values, respectively, over resolutions of transition probabilities for the maximum probability of reaching `"goal"`. Similarly, `minmin` and `minmax` are used for the minimum probability of reaching `"goal"`. Model checking is supported for:

- the `P` operator, including single temporal operators and LTL formulae
- the `R` operator, for the expected reward to reach a target or satisfy a co-safe LTL formula, or total (possibly discounted) reward
