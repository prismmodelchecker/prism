# Introduction

PRISM is a *probabilistic model checker*,
a tool for the modelling and analysis of systems which exhibit probabilistic behaviour.
Probabilistic model checking is a *formal verification* technique.
It is based on the construction of a precise mathematical model of a system which is to be analysed.
Properties of this system are then expressed formally in temporal logic
and automatically analysed against the constructed model.

PRISM has support for a wide range of probabilistic models:

- discrete-time Markov chains (DTMCs)
- continuous-time Markov chains (CTMCs)
- Markov decision processes (MDPs)
- probabilistic timed automata (PTAs)
- partially observable Markov decision processes (POMDPs)
- partially observable probabilistic timed automata (POPTAs)

In fact, PRISM's support for MDPs extends to the more general model of
probabilistic automata (PAs) [[Seg95](References.md#Seg95)], which does not require unique action names in each state.
For background material on these models, look at the pointers to
[resources](https://www.prismmodelchecker.org/about.php)
on the PRISM web site.

PRISM also supports non-probabilistic models, notably labelled transition systems (LTSs).

Models are supplied to the tool by writing descriptions in the [PRISM language](../ThePRISMLanguage/Introduction.md), a simple, high-level modelling language.

Properties of these models are written in the PRISM [property specification language](../PropertySpecification/Introduction.md) which is based on temporal logic. It incorporates several well-known probabilistic temporal logics:

- PCTL (probabilistic computation tree logic),
- CSL (continuous stochastic logic),
- (probabilistic) LTL (linear time logic),
- PCTL\* (which subsumes both PCTL and LTL).

The property language also supports [costs and rewards](../PropertySpecification/Reward-basedProperties.md), "[numerical](../PropertySpecification/ThePOperator.md)" properties, several other custom features and extensions, and also incorporates the non-probabilistic temporal logics CTL (computation tree logic) and LTL.

PRISM performs probabilistic model checking, based on exhaustive search and numerical solution, to automatically analyse such properties. It also contains a [discrete-event simulation](../RunningPRISM/DebuggingModelsWithTheSimulator.md) engine for [approximate model checking](../RunningPRISM/StatisticalModelChecking.md).
