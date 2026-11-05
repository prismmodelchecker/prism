# Computation Engines

## Computation engines

PRISM contains four main *engines*,
which implement the majority of its model checking functionality:

- "MTBDD"
- "sparse"
- "hybrid"
- "explicit"

The first three of these engines are either wholly or partly *symbolic*,
meaning that they use data structures such as
binary decision diagrams (BDDs) and multi-terminal BDDs (MTBDDs).
For these three engines, the process of
constructing a probabilistic model (DTMC, MDP or CTMC)
is performed in a symbolic fashion,
representing the model as an MTBDD.
Subsequent numerical computation performed during model checking, however,
is carried out differently for the three engines.
The "MTBDD" engine is implemented purely using MTBDDs and BDDs;
the "sparse" engine uses sparse matrices;
and the "hybrid" engine uses a combination of the other two.
The "hybrid" engine is described in [[KNP04b](../Main/References.md#KNP04b)].
For detailed information about all three engines, see [[Par02](../Main/References.md#Par02)].

The fourth engine, "explicit", performs all aspects of model construction
and model checking using *explicit-state* data structures.
Models are typically stored as sparse matrices or variants of.
This engine is implemented purely in Java, unlike the other engines
which make use of code/libraries implemented in C/C++.
One goal of the "explicit" engine is to provide an easily extensible model
checking engine without the complication of symbolic data structures,
although it also has other benefits (see below).

The choice of engine ("MTBDD", "sparse", "hybrid" or "engine") should not affect the results of model checking - all engines perform essentially the same calculations. In some cases, though, certain functionality is not available with all engines and PRISM will either automatically switch to an appropriate engine, or prompt you to do so.
Performance (time and space), however, may vary significantly and if you are using too much time/memory with one engine, it may be worth experimenting. Below, we briefly summarise the key characteristics of each engine.

- The **hybrid** engine is enabled by default in PRISM. It uses a combination of *symbolic* and *explicit-state* data structures (as used in the MTBDD and sparse engines, respectively). In general it provides the best compromise between time and memory usage: it (almost) always uses less memory than the sparse engine, but is typically slightly slower. The size of model which can be handled with this engine is quite predictable. The limiting factor in terms of memory usage comes from the storage of 2-4 (depending on the computation being performed) arrays of 8-byte values, one for each state in the model. So, a typical PC can handle models with between 10<sup>7</sup> and 10<sup>8</sup> states (one vector for 10<sup>7</sup> states uses approximately 75 MB).

- The **sparse** engine can be a good option for smaller models where model checking takes a long time. For larger models, however, memory usage quickly becomes prohibitive. As a rule of thumb, the upper limit for this engine, in terms of model sizes which can be handled, is about a factor of 10 less than the hybrid engine.

- The **MTBDD** engine is much more unpredictable in terms of performance but, when a model exhibits a lot of structure and regularity, can be very effective. This engine has been successfully applied to extremely large structured (but non-trivial) models, in cases where the other two engines cannot be applied. The MTBDD engine often performs poorly when the model (or solutions computed from it) contain lots of distinct probabilities/rates; it performs best when there are few such values. For this reason the engine is often successfully applied to MDP models, but much less frequently to CTMCs. When using the MTBDD engine, the *variable ordering* of your model is especially important. This topic is covered in the [FAQ](../FrequentlyAskedQuestions/PRISMModelling.md#ordering) section.

- The **explicit** engine is similar to the sparse engine, in that it can be a good option for relatively small models, but will not scale up to some of the models that can be handled by the hybrid or MTBDD engines. However, unlike the sparse engine, the explicit engine does not use symbolic data structures for model construction, which can be beneficial in some cases. One example is models with a potentially very large state space, only a fraction of which is actually reachable.

When using the PRISM GUI, the engine to be used for model checking can be selected from the "Engine" option under the "PRISM" tab of the "Options" dialog. From the command-line, engines are activated using the `-mtbdd`, `-sparse`, `-hybrid` and `-explicit` (or `-m`, `-s`, `-h` and `-ex`, respectively) switches, e.g.:

```shell
prism poll2.sm -tr 1000 -m
prism poll2.sm -tr 1000 -s
prism poll2.sm -tr 1000 -h
prism poll2.sm -tr 1000 -ex
```

Note also that precise details regarding the memory usage of the current engine are displayed during model checking (from the GUI, check the "Log" tab). This can provide valuable feedback when experimenting with different engines.

PRISM also has some basic support for automatically selecting the engine (and other settings) heuristically,
based on the size and type of the model, and the property being checked.
Use, for example, `-heuristic speed` from the command-line to choose options
which target computation speed rather than saving memory.
This is also available from the "Heuristic" option under the "PRISM" tab of the "Options" dialog in the GUI.

## Approximate/statistical model checking

Although it is not treated as a separate "engine", like those above,
PRISM also provides approximate/statistical model checking,
which is based on the use of discrete-event simulation.
From the GUI, this is enabled by choosing "Simulate" menu items or tick boxes;
from the command-line, add the `-sim` switch.
See the "[Statistical Model Checking](../RunningPRISM/StatisticalModelChecking.md)"
section for more details.

<a id="exact"></a>

## Exact model checking

Most of PRISM's model checking functionality uses numerical solution based on floating point arithmetic and, often, this uses iterative numerical methods, which are run until some user-specified precision is reached. PRISM currently has some support for "exact" model checking, i.e., using arbitrary precision arithmetic to provide exact numerical values. Currently, this is implemented as a special case of [parametric model checking](../RunningPRISM/ParametricModelChecking.md), which limits is application to relatively small models. It can be used for analysing DTMCs/CTMCs (unbounded until, steady-state probabilities, reachability reward and steady-state reward) or MDPs (unbounded until and reachability rewards). You can enable this functionality using the "Do exact model checking" option in the GUI or using switch `-exact` from the command line.

<a id="pta"></a>

## PTA engines

The techniques used to model check PTAs are different to the ones used for DTMCs, MDPs and CTMCs. For PTAs, PRISM currently has three distinct engines that can be used:

- The **stochastic games** engine uses abstraction-refinement techniques based on stochastic two-player games [[KNP09c](../Main/References.md#KNP09c)].

- The **digital clocks** engine performs a discretisation, in the form of a language-level model translation, that reduces the problem to one of model checking over a finite-state MDP [[KNPS06](../Main/References.md#KNPS06)].

- The **backwards reachability** engine is a zone-based method based on a backwards traversal of the state space and solution of the resulting finite-state MDP [[KNSW07](../Main/References.md#KNSW07)].

The default engine for PTAs is "stochastic games". The engine to be used can be specified using the "PTA model checking method" setting in the "PRISM" options panel in the GUI. From the command-line, switch `-ptamethod <name>` should be used where `<name>` is either `games`, `digital` or `backwards`.

The choice of engine for PTA model checking affects restrictions that imposed on both
the [modelling language](../ThePRISMLanguage/Real-timeModels.md)
and the types of [properties](../PropertySpecification/Real-timeModels.md) that can be checked.
