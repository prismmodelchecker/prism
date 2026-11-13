# PRISM manual

PRISM is a *probabilistic model checker*,
a tool for the modelling and analysis of systems that exhibit probabilistic behaviour.
Probabilistic model checking is a *formal verification* technique:
you build a precise mathematical model of the system to be analysed,
express its properties formally in temporal logic,
and PRISM analyses them automatically against the model.

## Models

Models are written in the [PRISM language](ThePRISMLanguage/README.md), a simple, high-level modelling language.
PRISM supports these types of probabilistic model:

| Model | Keyword | Use it when |
| --- | --- | --- |
| Discrete-time Markov chain (DTMC) | `dtmc` | Behaviour is purely probabilistic, in discrete steps |
| Continuous-time Markov chain (CTMC) | `ctmc` | Events happen after exponentially distributed delays, in continuous time |
| Markov decision process (MDP) | `mdp` | There are also nondeterministic choices: decisions to optimise, or unknown scheduling or environment |
| Probabilistic timed automaton (PTA) | `pta` | An MDP that also needs real-valued clocks and timing constraints |
| Partially observable MDP (POMDP) | `pomdp` | Decisions can only depend on what is observable, not the full state |
| Partially observable PTA (POPTA) | `popta` | Likewise, for a PTA |

PRISM's MDPs are in fact the more general model of
probabilistic automata (PAs) [[Seg95](Reference/References.md#Seg95)], which do not require unique action names in each state.
Transition probabilities can also be given as intervals, giving [interval DTMCs and MDPs](ModelTypes/UncertainModels.md#modelling),
for when they are not known exactly.
PRISM also supports non-probabilistic models, notably labelled transition systems (LTSs).
For background on these models, see the [resources](https://www.prismmodelchecker.org/about/) on the PRISM website.

## Properties

Properties are written in PRISM's [property specification language](PropertySpecification/README.md),
which is based on temporal logic. It incorporates several well-known probabilistic temporal logics:
PCTL (probabilistic computation tree logic),
CSL (continuous stochastic logic),
probabilistic LTL (linear temporal logic),
and PCTL\* (which subsumes both PCTL and LTL),
as well as the non-probabilistic logics CTL and LTL.
It also covers [costs and rewards](PropertySpecification/Reward-basedProperties.md),
[numerical queries](PropertySpecification/ThePOperator.md) such as "what is the probability that…",
and [multi-objective](PropertySpecification/Multi-objectiveProperties.md) queries.

PRISM checks properties exhaustively, using numerical solution methods,
and can also [generate strategies](RunningPRISM/Strategies.md) that achieve optimal values.
It also includes a [discrete-event simulator](RunningPRISM/DebuggingModelsWithTheSimulator.md),
for [statistical model checking](RunningPRISM/StatisticalModelChecking.md).

## Where to start

New to PRISM? See [Getting started](GettingStarted/README.md),
then work through the [tutorial](https://www.prismmodelchecker.org/tutorial/)
and the sections on the [PRISM language](ThePRISMLanguage/README.md) and [properties](PropertySpecification/README.md).
Looking for an option or a file format? Use the search box, or see [Configuration and performance](ConfiguringPRISM/README.md)
and the [Reference](Reference/README.md) section.

## Contents

- [Getting started](GettingStarted/README.md): installing PRISM, solving common problems, and first steps
- [The PRISM language](ThePRISMLanguage/README.md): how to write models
- [Property specification](PropertySpecification/README.md): how to write properties
- [Extended model types](ModelTypes/README.md): probabilistic timed automata, partially observable models and interval models
- [Running PRISM](RunningPRISM/README.md): building models, model checking, experiments, strategies, simulation, and importing and exporting models
- [Configuration and performance](ConfiguringPRISM/README.md): engines, solution methods and other settings, and memory
- [Reference](Reference/README.md): language syntax, file formats and references
- [Frequently asked questions](FrequentlyAskedQuestions/README.md): memory problems, modelling and properties

How the files of this manual are organised is described in
[doc/README.md](https://github.com/prismmodelchecker/prism/blob/master/doc/README.md).
