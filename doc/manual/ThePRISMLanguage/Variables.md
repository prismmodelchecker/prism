# Variables

Variables are used to describe the state space of a model.
Currently, PRISM supports just a few simple types of variables:
they can either be (finite ranges of) integers or Booleans
(we ignore [clocks](../ModelTypes/Real-timeModels.md#modelling) for now, which are used in real-time models).

<a id="variable-declarations"></a>
## Variable declarations

As in the earlier example, **variable declarations** are usually placed within a module definition.
In fact, these are *local* variables and it is also possible to declare *global* variables.
This distinction is discussed later in the context of [composing](CompositionAndSynchronisation.md#global-variables) multiple modules.

The typical form of a variable declaration is:

```prism
name : type init initial_value;
```

where `name` is a valid [identifier](../Reference/PRISMLanguageSyntax.md#identifiers).
The most commonly used type is an integer with bounded range, specified as `[lower..upper]`,
as in the earlier `walk.prism` example:

```prism
x : [0..3] init 0;
```

A Boolean variable is declared as, for example:

```prism
b : bool init false;
```

The initial value of a variable can be omitted from its declaration,
in which case it starts with its *default* value: 
the lowest value in the range for an integer, or `false` for a Boolean.
Thus, the variable declarations shown below are equivalent to the ones above.

```prism
x : [0..3];
b : bool;
```

All the above examples are variables of bounded range.
In some cases, this restriction can be relaxed,
for example when using techniques such as
[statistical model checking](../RunningPRISM/StatisticalModelChecking.md) or [fast adaptive uniformisation](../ConfiguringPRISM/SolutionMethodsAndOptions.md#fau)
which do not exhaustively explore the state space of a model.
In these cases, integer variables with **unbounded ranges** can be used:

```prism
x : int init 1;
y : int;
```

For variable `y` above, where the initial value is omitted,
the initial value is taken to be `0`.
PRISM's [explicit engine](../ConfiguringPRISM/ComputationEngines.md) can also be applied
to models with unbounded integer variables, as long as the reachable state space of the model remains finite.

<a id="multiple-initial-states"></a>
## Multiple initial states

The above examples assume that the model has a single initial state,
defined by the initial values of each of its variables.
It is possible to specify that a model has *multiple* initial states.
This is done using the `init...endinit` construct,
which can be placed anywhere in the file except within a module definition,
and removing any initial values from variable declarations.
Between the `init...endinit` is a predicate over all variables:
any state that satisfies this predicate is an initial state.

For example, given the following variable declarations:

```prism
x : [0..3];
b : bool;
```

this definition:

```prism
init x<2 endinit
```

results in a model with four initial states: `(0,false)`, `(0,true)`, `(1,false)` and `(1,true)`.
