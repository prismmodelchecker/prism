# Computation engines

PRISM contains multiple **engines**: these are distinct implementations of model checking,
differing in the data structures and technologies used, and in the range of models and properties that they can handle.

<a id="core"></a>
## Core engines

There are four **core engines**, listed below, which are general purpose and all cover the majority of PRISM's functionality
for the three most commonly used models of DTMCs, MDPs and CTMCs.
These should yield the same results for the same model and property
(assuming that the same algorithms and settings are used)
but vary in performance and scalability (i.e., time and memory usage).
The latter is discussed in more detail in "[Which engine should I use?](#choosing)" below.

- The **explicit** engine: implemented purely in Java, using *explicit-state* data structures
  (sparse matrices and variants). This supports the widest selection of solution methods,
  and also the most different model types, for example, it is the only engine that supports
  POMDPs and uncertain MDPs/DTMCs. PRISM will usually switch to this engine automatically
  if it is the only one that can handle a particular model, property or solution method.

- The **MTBDD** engine: fully *symbolic*, storing both the model and the value vectors computed from it
  as binary decision diagrams (BDDs) and multi-terminal BDDs (MTBDDs).
  This engine can handle extremely large models, as long as they are structured and regular.

- The **sparse** engine: builds the model symbolically, as an MTBDD, but then converts it to a sparse matrix
  for numerical computation. This typically makes it faster to solve, but may use more memory.

- The **hybrid** engine (the default): builds the model symbolically, like the sparse engine,
  and performs numerical computation with a combination of MTBDDs (for the model) and arrays (for vectors),
  representing a trade-off between memory usage and solution speed [[KNP04b](../Reference/References.md#KNP04b)].

PRISM's MTBDD, sparse and hybrid engines are referred to collectively as its **symbolic engines**,
since they all rely on a symbolic (MTBDD-based) representation of the model being built first.
These engines are mostly implemented in C++, using the CUDD library for BDDs and MTBDDs.

When using the PRISM GUI, the engine to be used for model checking can be selected from the "Engine" option under the "PRISM" tab of the "Options" dialog. From the command-line, engines are activated using the [`-explicit`](../Reference/CommandLineSwitches.md#explicit), [`-mtbdd`](../Reference/CommandLineSwitches.md#mtbdd), [`-sparse`](../Reference/CommandLineSwitches.md#sparse) and [`-hybrid`](../Reference/CommandLineSwitches.md#hybrid) (or `-ex`, `-m`, `-s` and `-h`, respectively) switches, e.g.:

```shell
prism model.prism model.props -ex
prism model.prism model.props -m
prism model.prism model.props -s
prism model.prism model.props -h
```

or, equivalently, with [`-engine <name>`](../Reference/CommandLineSwitches.md#engine), e.g. `-engine explicit`.

<a id="auto"></a>
If you don't choose an engine, PRISM uses the hybrid engine, unless the model, property or chosen solution method needs a different one.
PRISM makes these switches with any engine (see above), but if you picked a specific engine,
it warns you that it has switched; otherwise, it just reports which engine is used and why.
From the command line, `-engine auto` returns to this, e.g. to undo an engine chosen earlier in the arguments.

<a id="other"></a>
## Other engines

PRISM's other engines typically work on a specific set of models and/or properties,
and may also vary in the accuracy of the results that they compute.

- <a id="statistical"></a>The **statistical model checking** engine generates approximate results
  using discrete-event simulation, and without building the full model,
  often improving scalability but at the cost of accuracy.
  From the GUI, it is used by choosing "Simulate" menu items or tick boxes;
  from the command-line, add the [`-sim`](../Reference/CommandLineSwitches.md#sim) switch.
  See "[Statistical model checking](../RunningPRISM/StatisticalModelChecking.md)" for details.

- <a id="exact"></a>The **exact** engine computes exact numerical results, using arbitrary precision arithmetic.
  You can enable this functionality using the "Do exact model checking" option in the GUI
  or using switch [`-exact`](../Reference/CommandLineSwitches.md#exact) from the command line.
  This is implemented as a special case of [parametric model checking](../RunningPRISM/ParametricModelChecking.md),
  which limits its application to relatively small models.
  It can be used for analysing DTMCs/CTMCs (unbounded until, steady-state probabilities, reachability reward and steady-state reward)
  or MDPs (unbounded until and reachability rewards).

- <a id="parametric"></a>The **parametric** engine computes results exactly, as functions of the model's parameters.
  It is used with the [`-param`](../Reference/CommandLineSwitches.md#param) switch:
  see "[Parametric model checking](../RunningPRISM/ParametricModelChecking.md)".

- <a id="pta"></a>PTA model checking is performed with one of three specific engines,
  **stochastic games**, **digital clocks** and **backwards reachability**,
  which are described in "[Real-time models](../ModelTypes/Real-timeModels.md#solution-methods)".
  These are chosen with the "PTA model checking method" setting in the GUI
  or the [`-ptamethod`](../Reference/CommandLineSwitches.md#ptamethod) switch on the command line.

<a id="choosing"></a>
## Which engine should I use?

Sometimes a specific engine is required for a particular model (e.g., for PTAs)
or is requested in order to achieve a particular accuracy (e.g., exact or parametric results).
Similarly, the explicit engine is sometimes the only option for a model class, such as POMDPs and uncertain MDPs/DTMCs.

In other cases, any of the four core engines can normally be used, with the current **default** being the hybrid engine.
If PRISM is using too much time or memory with one engine, it may be worth experimenting with others,
since their performance can vary, depending on the size of the model and how it is described.
Details of the memory usage and model size are displayed during model checking (from the GUI, check the "Log" tab).

- The **explicit** engine has the most predictable performance, 
  with time and memory usage increasing as the size of the model does.
  Unlike the symbolic engines, performance does *not* depend on how compact
  a model's MTBDD-based representation is, which can be unpredictable.
  *Building* the model is often slower using the explicit engine,
  but the solution time is more comparable to that of the other core engines.
  The explicit engine is usually the best choice when:

  - **importing** from [explicit-state](../RunningPRISM/ImportingAndExportingModels.md) formats,
  such as UMB (`.umb`) or PRISM's own "explicit" file formats (`.pexp`, `.tra`, etc.);
  
  - building "explicit-style" PRISM models (i.e., essentially comprising an unstructured list of states/transitions,
  but encoded into a textual description in the PRISM modelling language);

  - building models that are *potentially* very large, in terms of the number of model variables
  and their range, but where only a small fraction of states are reachable.
  
- The **hybrid** engine, amongst the three symbolic engines,
  generally provides the best compromise between time and memory usage:
  it uses less memory than the sparse engine,
  but is typically slightly slower.

- The **sparse** engine can be a good alternative to the hybrid engine
  when model checking takes a long time, but the model is small enough to fit in memory
  (as a rule of thumb, the sparse engine scales up to models about a factor of 10 smaller than the hybrid one).

- The **MTBDD** engine has the potential to scale up to very large models,
  beyond the range of what can be handled by the other three,
  but is much more unpredictable in terms of performance.
  Roughly speaking, when the model description (in the PRISM language) 
  exhibits a lot of structure and regularity, it can be very effective.
  But the MTBDD engine often performs poorly when the model (or solutions computed from it)
  contain many distinct probabilities/rates; it performs best when there are few such values.
  For this reason the engine is often successfully applied to MDP models,
  but much less frequently to CTMCs.

For all three symbolic engines (MTBDD, sparse, hybrid),
various aspects of the (PRISM language) model description can impact performance,
including its *variable ordering*;
see "[PRISM modelling](../FrequentlyAskedQuestions/PRISMModelling.md#ordering)" in the FAQ for details.

<a id="engine-options"></a>
## Symbolic engine options

A few further settings fine-tune the symbolic engines. The defaults are usually fine.

- **Hybrid engine:** to speed up numerical computation, the hybrid engine replaces the lowest levels of its MTBDDs with sparse matrices [[Par02](../Reference/References.md#Par02)].
  The number of levels replaced is set with [`-sbl <n>`](../Reference/CommandLineSwitches.md#sbl) (the default, -1, lets PRISM choose)
  and the memory these sparse matrices can use, in KB, with [`-sbmax <n>`](../Reference/CommandLineSwitches.md#sbmax) (default 1024).
  For the hybrid engine's Gauss-Seidel and SOR methods, the matrix is also divided into blocks:
  [`-gsl <n>`](../Reference/CommandLineSwitches.md#gsl) and [`-gsmax <n>`](../Reference/CommandLineSwitches.md#gsmax) (or `-sorl` and `-sormax`)
  set the number of MTBDD levels used for this division and its memory limit in KB.
- **Sparse and hybrid engines:** by default, sparse matrices and vectors with many repeated values are compressed further;
  [`-nocompact`](../Reference/CommandLineSwitches.md#nocompact) switches this off.
- **CUDD**, the BDD/MTBDD library: besides its memory limit (see "[Memory and model size](MemoryAndModelSize.md#settings)"),
  [`-cuddepsilon <x>`](../Reference/CommandLineSwitches.md#cuddepsilon) sets the precision used when CUDD compares values in its cache (default $10^{-15}$),
  and [`-ddextrastatevars <n>`](../Reference/CommandLineSwitches.md#ddextrastatevars) and [`-ddextraactionvars <n>`](../Reference/CommandLineSwitches.md#ddextraactionvars)
  set how many extra MTBDD variables are preallocated for transformations of the model (default 20 each).
  [`-ddsanity`](../Reference/CommandLineSwitches.md#ddsanity) enables internal sanity checks, for debugging (this slows PRISM down).

