# Non-probabilistic properties

PRISM also supports model checking of the non-probabilistic temporal logics CTL (computation tree logic) and LTL (linear temporal logic).
Properties in these logics use the `A` (for all) and `E` (there exists) operators,
instead of the probabilistic `P` operator used in many other properties supported by PRISM.
They can be checked on any model type, including
[non-probabilistic models](../ModelTypes/Non-probabilisticModels.md) such as labelled transition systems (LTSs).

Properties take the form:

```prism
A [ pathprop ]
E [ pathprop ]
```

which are true in a state *s* of a model if
"path property `pathprop` is satisfied by *all* paths from state *s*"
and
"path property `pathprop` is satisfied by *some* path from state *s*",
respectively.
The syntax for LTL formulas is the same as that allowed within the [P operator](ThePOperator.md#ltl).

Example properties include:

```prism
// There exists a path that reaches a state satisfying "goal"
E [ F "goal" ];

// Variable x is always at most 10 along all paths of the model
A [ G x<=10 ];

// There exists a path along which label "ready" eventually becomes true
// and label "launch" is true immediately afterwards
E [ F "ready" & (X "launch") ];

// Along all paths, either x=1 or x=2 is true infinitely often
A [ (G F x=1) | (G F x=2) ];
```

<a id="counterexamples"></a>
## Counterexamples and witnesses

If you check a CTL property of the form `A [ G "inv" ]` and it is false, PRISM will generate a counterexample in the form of a path that reaches a state where `"inv"` is not true. This is displayed either in the simulator (from the GUI) or at the command-line. Similarly, if you check `E [ F "goal" ]` and the result is true, a witness (a path reaching a `"goal"` state) will be generated.
