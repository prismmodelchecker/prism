# Real-time models

The classes of property that can be checked for real-time models (PTAs and POPTAs) are currently more restricted than for the other kinds of models that PRISM supports. This is because the model checking procedures are quite different for this type of model. We describe these restrictions here. The situation is also dependent on which of the PTA model checking [engines](../ConfiguringPRISM/ComputationEngines.md#pta) is being used.

For the "**stochastic games**" engine, we essentially only allow unbounded or time-bounded probabilistic reachability properties, i.e. properties of the form:

```prism
Pmin=? [ F target ]
Pmax=? [ F target ]
Pmin=? [ F<=T target ]
Pmax=? [ F<=T target ]
```

where `target` is a Boolean-valued expression that does not include references to any clock variables and `T` is an integer-valued expression. The `P` operator cannot be nested and the `S` and `R` operators are not supported.

The "**backwards reachability**" engine is similar but currently only handles maximum probabilities.

For the "**digital clocks**" engine, there is slightly more flexibility,
e.g. until (`U`) properties are allowed, as are clock variables in expressions and arithmetic expressions such as:

```prism
1 - Pmin=? [ F target ]
```

This engine, like the "stochastic games" engine, does not allowed nested properties. Also, references to clocks must, like in the modelling language, not use strict comparisons
(e.g. `x<=5` is allowed, `x<5` is not).

The digital clocks also has support for rewards:
it is possible to check reachability reward properties of the form:

```prism
Rmin=? [ F target ]
Rmax=? [ F target ]
```

Reward structures specified in the model, though, must not depend on clock variables.
Formally, the class of PTAs with this kind of reward structure is sometime called *linearly priced PTAs* (see e.g. [[KNPS06](../Main/References.md#KNPS06)].

The digital clocks method is based on a language-level translation from a PTA model to an MDP one. If you want to see the MDP PRISM model that was generated, add the switch `-exportdigital digital.nm` when model checking property to export the model file to `digital.nm`.
