# Non-probabilistic properties

PRISM also supports model checking of the non-probabilistic temporal logics CTL (computation tree logic) and LTL (linear temporal logic).
Properties in these logics use the `A` (for all) and `E` (there exists) operators,
instead of the probabilistic `P` operator used in many other properties supported by PRISM.

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
The syntax for LTL formulas is the same as those allowed within the [P operator](ThePOperator.md#ltl).

Example properties include:

```prism
E [ F "goal" ] // There exists a path that reaches a state satisfying "goal"

A [ G x<=10 ] // Variable x is always at most 10 along all paths of the model

E [ F "ready" & (X "launch") ] // There exists a path along which label "ready" eventually becomes true and label "launch" is true immediately afterwards

A [ (G F x=1) | (G F x=2) ] // Along all paths, either x=1 or x=2 is true infinitely often
```

<a id="counterexamples"></a>
## Counterexamples and witnesses

If you check a CTL property of the form `A [ G "inv" ]` and it is false, PRISM will generate a counterexample in the form of a path that reaches a state where `"inv"` is not true. This is displayed either in the simulator (from the GUI) or at the command-line. Similarly, if you check `E [ F "goal" ]` and the result is true, a witness (a path reaching a `"goal"` state) will be generated.
