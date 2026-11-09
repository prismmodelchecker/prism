# Multiple initial states

Typically, a [variable](ModulesAndVariables.md) declaration
specifies the initial value for that variable.
The *initial state* for the model is then defined by the initial value for all variables.
It is possible, however, to specify that a model has *multiple* initial states.
This is done using the `init...endinit` construct,
which can be placed anywhere in the file except within a module definition,
and removing any initial values from variable declarations.
Between the `init` and `endinit` keywords, there should be a
predicate over all the variables of the model.
Any state which satisfies this predicate is an initial state.

Consider again [Example 1](Example1.md).
As it stands, there is a single initial state `(0,0)` (i.e.  `x=0` and `y=0`).
If we remove the `init 0` part of both variable declarations
and add the following to the end of the file:

```prism
init x=0 endinit
```

there will be three initial states: `(0,0)`, `(0,1)` and `(0,2)`.
Similarly, we could instead add:

```prism
init x+y=1 endinit
```

in which case there would be two initial states: `(0,1)` and `(1,0)`.
