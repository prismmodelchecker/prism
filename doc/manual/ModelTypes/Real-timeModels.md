# Probabilistic timed automata

PRISM supports *real-time* models, in particular,
*probabilistic timed automata* (PTAs), which extend MDPs with the ability to model real-time behaviour.
This is done in the style of *timed automata* [[AD94](../Reference/References.md#AD94)], by adding *clocks*,
real-valued variables which increase with time and can be reset. For background material on PTAs, see for example [[NPS13](../Reference/References.md#NPS13)].
You can also find several example PTA models included in the PRISM distribution. Look in the `prism-examples/ptas` directory.

This page describes how PTAs are modelled, the engines that PRISM uses to analyse them, and the properties that can be checked.

<a id="modelling"></a>
## Modelling PTAs

Before describing how PTA features are incorporated into the PRISM modelling language, we give a simple example. Here is a small PTA:

![PTA with three locations. Location 0 (invariant x≤2) has a send action, enabled when x≥1, that goes to location 1 (labelled true) with probability 0.9 or location 2 (invariant x≤3) with probability 0.1, resetting x to 0 in both cases. From location 2, a retry action enabled when x≥2 goes to location 1 with probability 0.95, or stays in location 2 with probability 0.05 and resets x to 0.](../files/pta.png)

and here is a corresponding PRISM model:

```prism
pta

module M

    s : [0..2] init 0;
    x : clock;

    invariant
        (s=0 => x<=2) &
        (s=2 => x<=3)
    endinvariant

    [send] s=0 & x>=1 -> 0.9:(s'=1)&(x'=0) + 0.1:(s'=2)&(x'=0);
    [retry] s=2 & x>=2 -> 0.95:(s'=1) + 0.05:(s'=2)&(x'=0);

endmodule
```

For modelling PTAs in PRISM, there is a new datatype, `clock`, used for variables that are clocks. Other types of PRISM variables can be defined in the usual way. In the example above, we use just a single integer variable `s` to represent the locations of the PTAs.

In a PTA, transitions can include a *guard*, which constrains when it can occur based on the current value of clocks, and *resets*, which specify that a clock's values should be set to a new (integer) value. These are both specified in PRISM commands in the usual way: see, for example, the inclusion of `x>=1` in the guard for the `send`-labelled command and the updates of the form `(x'=0)` which reset the clock `x` to 0.

The other new addition is an `invariant` construct, which is used to specify an expression describing the clock *invariants* for each PRISM module. These impose restrictions on the allowable values of clock variables, depending on the values of the other non-clock variables. The `invariant` construct should appear between the variable declarations and the commands of the module. Often, clock invariants are described separately for each PTA location; hence, the invariant will often take the form of a conjunction of implications, as in the example model above, but more general expressions are also permitted. In the example, the clock `x` must satisfy `x<=2` or `x<=3` when local variables `s` is 0 or 2, respectively. If `s` is 1, there is no restriction (since the invariant is effectively `true` in this case).

Expressions that include reference to clocks, whether in guards or invariants, must satisfy certain conditions to facilitate model checking. In particular, references to clocks must appear as conjunctions of *simple clock constraints*, i.e. conjunctions of expressions of the form `x~c` or `x~y` where `x` and `y` are clocks, `c` is an integer-valued expression and `~` is one of `<`, `<=`, `>=`, `>`, `=`).

PRISM makes several assumptions about PTAs, regardless of the engine used.

- Firstly PTAs should not exhibit *timelocks*, i.e. the possibility of reaching a state where no transitions are possible and time cannot elapse beyond a certain point (due to invariant conditions). PRISM checks for timelocks and reports an error if one is found.

- Secondly, PTAs should be *well-formed* and *non-zeno* (see e.g. [[KNSW07](../Reference/References.md#KNSW07)] for details). Currently, PRISM does not check automatically that these assumptions are satisfied.

There are also some further restrictions, which depend on the engine used: see [below](#engines).

<a id="engines"></a>
## Engines

The techniques used to model check PTAs are different to the ones used for DTMCs, MDPs and CTMCs. For PTAs, PRISM currently has three distinct engines that can be used:

- The **stochastic games** engine uses abstraction-refinement techniques based on stochastic two-player games [[KNP09c](../Reference/References.md#KNP09c)].

- The **digital clocks** engine performs a discretisation, in the form of a language-level model translation, that reduces the problem to one of model checking over a finite-state MDP [[KNPS06](../Reference/References.md#KNPS06)].

- The **backwards reachability** engine is a zone-based method based on a backwards traversal of the state space and solution of the resulting finite-state MDP [[KNSW07](../Reference/References.md#KNSW07)].

The default engine for PTAs is "stochastic games". The engine to be used can be specified using the "PTA model checking method" setting in the "PRISM" options panel in the GUI. From the command-line, switch `-ptamethod <name>` should be used where `<name>` is either `games`, `digital` or `backwards`.

The choice of engine affects the models that can be analysed, as well as the [properties](#properties) that can be checked.

For the **stochastic games** and **backwards reachability** engines:

- The model must also have a single initial state (i.e. the `init...endinit` construct is not permitted).

For the **digital clocks** engine:

- Clock constraints cannot use strict comparison operators, e.g. `x<=5` is allowed, but `x<5` is not.

- Diagonal clock constraints are not allowed, i.e. those containing references to two clocks, such as `x<=y`.

The digital clocks method is based on a language-level translation from a PTA model to an MDP one. If you want to see the MDP PRISM model that was generated, add the switch `-exportdigital digital.prism` when model checking property to export the model file to `digital.prism`.

<a id="properties"></a>
## Properties

The classes of property that can be checked for real-time models (PTAs and POPTAs) are currently more restricted than for the other kinds of models that PRISM supports. This is because the model checking procedures are quite different for this type of model. The situation also depends on which of the [engines](#engines) is being used.

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

This engine, like the "stochastic games" engine, does not allow nested properties. Also, references to clocks must, like in the modelling language, not use strict comparisons
(e.g. `x<=5` is allowed, `x<5` is not).

The digital clocks engine also has support for rewards:
it is possible to check reachability reward properties of the form:

```prism
Rmin=? [ F target ]
Rmax=? [ F target ]
```

Reward structures specified in the model, though, must not depend on clock variables.
Formally, the class of PTAs with this kind of reward structure is sometimes called *linearly priced PTAs* (see e.g. [[KNPS06](../Reference/References.md#KNPS06)]).
