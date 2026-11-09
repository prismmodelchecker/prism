# Modules and variables

The [previous example](Example1.md) uses two modules, `M1` and `M2`, one representing each process.
A module is specified as:

```prism
module name ... endmodule
```

The definition of a module contains two parts: its *variables* and its *commands*.
The variables describe the possible states that the module can be in;
the [commands](Commands.md) describe its behaviour, i.e. the way in which the state changes over time.
Currently, PRISM supports just a few simple types of variables:
they can either be (finite ranges of) integers or Booleans
(we ignore [clocks](Real-timeModels.md) for now).

In the example above, each module has one integer variable with range `[0..2]`.
A variable declaration looks like:

```prism
x : [0..2] init 0;
```

Notice that the initial value of the variable is also specified.
A Boolean variable is declared as follows:

```prism
b : bool init false;
```

It is also possible to omit the initial value of a variable,
in which case it is assumed to be the lowest value in the range (or `false` for a Boolean).
Thus, the variable declarations shown below are equivalent to the ones above.
As will be described later, it is also possible to specify
[multiple initial states](MultipleInitialStates.md) for a model.

```prism
x : [0..2];
b : bool;
```

We also mention that, for a few kinds of model analysis (typically those based on simulation, such as [approximate model checking](../RunningPRISM/StatisticalModelChecking.md) or [fast adaptive uniformisation](../ConfiguringPRISM/SolutionMethodsAndOptions.md#fau)), it is also permissible to use integer variables with unbounded ranges, denoted as:

```prism
x : int;
y : int init 3;
```

Where the state space of the model remains finite, despite the presence of such unbounded variables, you can use the [explicit engine](../ConfiguringPRISM/ComputationEngines.md) to build and analyse the model.

## Identifiers

The names given to modules and variables are referred to as *identifiers*.
Identifiers can be made up of letters, digits and the underscore character, but cannot begin with a digit,
i.e. they must satisfy the regular expression `[A-Za-z_][A-Za-z0-9_]*`, and are case-sensitive.
Furthermore, identifiers cannot be any of the following, which are all reserved keywords in PRISM:
`A`,
`bool`,
`clock`,
`const`,
`ctmc`,
`C`,
`double`,
`dtmc`,
`E`,
`endinit`,
`endinvariant`,
`endmodule`,
`endobservables`,
`endrewards`,
`endsystem`,
`false`,
`formula`,
`filter`,
`func`,
`F`,
`global`,
`G`,
`init`,
`invariant`,
`I`,
`int`,
`label`,
`max`,
`mdp`,
`min`,
`module`,
`X`,
`nondeterministic`,
`observable`,
`observables`,
`of`,
`Pmax`,
`Pmin`,
`P`,
`pomdp`,
`popta`,
`probabilistic`,
`prob`,
`pta`,
`rate`,
`rewards`,
`Rmax`,
`Rmin`,
`R`,
`S`,
`stochastic`,
`system`,
`true`,
`U`,
`W`.
