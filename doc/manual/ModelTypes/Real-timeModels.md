# Real-time models

PRISM supports **real-time** models, in particular,
**probabilistic timed automata** (PTAs), which extend MDPs with the ability to model real-time behaviour.
This is done in the style of timed automata [[AD94](../Reference/References.md#AD94)], by adding **clocks**,
real-valued variables modelling the elapse of time.

The state of a PTA is made up of its clocks, plus other variables.
The non-clock part of the state is often called the **location** of the PTA.
The values of the clock variables are assumed to increase at the same rate, as time elapses.
But the model can also include **clock resets**, which reset a clock's
value to a (non-negative integer) value, usually `0`, at certain points.

Transitions of the model between its locations can be made
dependent on the values of the clocks:
**clock invariants** dictate the range of values within which clocks must stay
for each location before moving to another location;
**clock guards** can be used to restrict transitions
to when clocks have certain values.

For background material on PTAs, see for example [[NPS13](../Reference/References.md#NPS13)].
You can also find several example PTA models included in the PRISM distribution. Look in the `prism-examples/ptas` directory.

<a id="modelling"></a>
## Modelling

Here is a simple example of a PTA, modelled in PRISM.
It represents a system that attempts to send a message,
which happens at a time instant in the range `[1,2]`
and succeeds with probability 0.9.
In the case of failure, it then repeatedly retries to send the message,
waiting for a delay in the range `[2,3]` each time.

[pta.prism](../files/pta.prism "include")

The model type is indicated as `pta` at the top of the file
(in fact PRISM will also detect an MDP model with clock variables and treat it as a PTA).

Clocks use the variable type `clock`, and are declared in the same way as other variables.
This example uses a single clock variable `x`.
Other variables can be declared in the usual way and make up the rest of the state of the model (its location).
In the example above, we use just a single integer variable `s` with range `[0..2]`.

Clock guards are embedded within the usual guarded command notation
(see, for example, the inclusion of `x>=1` in the guard for the `send`-labelled command).
Clock resets are included with the variable updates of commands
(see, for example, the instances of `(x'=0)` in the example).

Additionally, an `invariant` construct in each module, placed after its variable declarations,
specifies clock invariants as an expression over clocks and other variables.
The format used in the example (a conjunction of implications)
is a convenient way to specify a separate clock invariant for each PTA location.
In the example, the clock `x` must satisfy `x<=2` or `x<=3`
when local variable `s` is 0 or 2, respectively.
If `s` is 1, there is no restriction
(since the invariant is effectively `true` in this case).

The resulting PTA is shown below, where states (locations) are identified by the value of `s`.

![PTA with three locations. Location 0 (invariant x≤2) has a send action, enabled when x≥1, that goes to location 1 (labelled true) with probability 0.9 or location 2 (invariant x≤3) with probability 0.1, resetting x to 0 in both cases. From location 2, a retry action enabled when x≥2 goes to location 1 with probability 0.95, or stays in location 2 with probability 0.05 and resets x to 0.](../files/pta.svg)

**Rewards:**
PTA models can include [reward structures](../ThePRISMLanguage/CostsAndRewards.md) in the same way as MDPs.
However, the reward structures for PTAs are restricted to only include state and transition rewards that do not depend on clock variables.
Formally, the class of PTAs with this kind of reward structure is sometimes called *linearly priced PTAs* (see e.g. [[KNPS06](../Reference/References.md#KNPS06)]).

**Restrictions:**
Expressions in PTA models that include reference to clocks, whether in guards or invariants, must satisfy certain conditions to facilitate model checking. In particular, references to clocks must appear as conjunctions of *simple clock constraints*, i.e., conjunctions of expressions of the form `x~c` or `x~y` where `x` and `y` are clocks, `c` is an integer-valued expression and `~` is one of `<`, `<=`, `>=`, `>` or `=`.

PRISM makes several assumptions about PTAs, regardless of the engine used.

- Firstly, PTAs should not exhibit **timelocks**, i.e., the possibility of reaching a state where no transitions are possible and time cannot elapse beyond a certain point (due to invariant conditions). PRISM checks for timelocks and reports an error if one is found.

- Secondly, PTAs should be **well-formed** and **non-zeno** (see e.g. [[KNSW07](../Reference/References.md#KNSW07)] for details). PRISM does not check automatically that these assumptions are satisfied.

There are also some further restrictions, which depend on the engine used: see [below](#engines).

<a id="properties"></a>
## Properties

The classes of property that can be checked for real-time models
are more restricted than for the other kinds of models that PRISM supports.
This is because the model checking procedures are quite different for this type of model.
The situation also depends on which of the [engines](#engines) is being used.

Broadly speaking, PRISM allows single (non-nested) instances of
unbounded or time-bounded probabilistic reachability properties
and reachability reward properties, i.e., queries of the form:

```prism
Pmin=? [ F target ]
Pmax=? [ F target ]
Pmin=? [ F<=T target ]
Pmax=? [ F<=T target ]
Rmin=? [ F target ]
Rmax=? [ F target ]
```

where `target` is a Boolean-valued expression that does not include
references to any clock variables and `T` is an integer-valued expression.
In some cases (see below), PRISM also allows until (`U`) properties.

<a id="engines"></a>
## Engines

PRISM has three distinct engines that can be used for analysing PTAs.

- The **stochastic games** engine uses abstraction-refinement techniques based on stochastic two-player games [[KNP09c](../Reference/References.md#KNP09c)].

- The **digital clocks** engine performs a discretisation, in the form of a language-level model translation, that reduces the problem to one of model checking over a finite-state MDP [[KNPS06](../Reference/References.md#KNPS06)].
  If you want to see the MDP PRISM model that was generated, add the switch `-exportdigital digital.prism` when model checking a property to export the model file to `digital.prism`.

- The **backwards reachability** engine is a zone-based method based on a backwards traversal of the state space and solution of the resulting finite-state MDP [[KNSW07](../Reference/References.md#KNSW07)].

The default engine for PTAs is "stochastic games". The engine to be used can be specified using the "PTA model checking method" setting in the "PRISM" options panel in the GUI. From the command-line, switch `-ptamethod <name>` should be used where `<name>` is either `games`, `digital` or `backwards`.

**Restrictions:**
The choice of engine affects the models that can be analysed, as well as the [properties](#properties) that can be checked.

For the stochastic games and backwards reachability engines:

- The model must have a single initial state (i.e., the `init...endinit` construct is not permitted).
- Only `Pmin=?` and `Pmax=?` queries are supported (not, e.g., `P>=0.9`); reward properties are not supported.
- Filters and the `"init"` and `"deadlock"` labels cannot be used in properties.

Additionally, for the backwards reachability engine:

- Only maximum probabilities are handled.

For the digital clocks engine:

- Until (`U`) formulas are also allowed in the `P` operator, in addition to reachability (`F`) formulas.
- Properties can refer to clock variables, so `target` is not restricted as above.
- Arithmetic expressions in properties, such as `1 - Pmin=? [ F target ]`, are allowed.
- Clock constraints in both models and properties
  cannot use **strict** comparison operators
  (e.g., `x<=5` is allowed, but `x<5` is not).
- In both models and properties, **diagonal** clock constraints are not allowed,
  i.e., those containing references to two clocks, such as `x<=y`.

