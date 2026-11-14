# PRISM manual

PRISM is a **probabilistic model checker**,
a tool for the modelling and analysis of systems that exhibit random or uncertain behaviour.
Probabilistic model checking is a formal verification technique:
you build a precise mathematical **model** of the system to be analysed,
express its **properties** formally in temporal logic,
and PRISM analyses them automatically against the model.

## Models

Models are written in the [PRISM language](ThePRISMLanguage/README.md), a simple, high-level modelling language.
PRISM supports many different types of probabilistic model. The three most common are:

| Model | Keyword | Use it when |
| --- | --- | --- |
| Discrete-time Markov chain (DTMC) | `dtmc` | Behaviour is purely probabilistic, in discrete steps |
| Continuous-time Markov chain (CTMC) | `ctmc` | Events happen after exponentially distributed delays, in continuous time |
| Markov decision process (MDP) | `mdp` | There are also nondeterministic choices: decisions to optimise, or an unknown scheduler/environment |

It also supports [real-time models](ModelTypes/Real-timeModels.md#modelling) (e.g., probabilistic timed automata, PTAs),
[partially observable models](ModelTypes/PartiallyObservableModels.md#modelling) (e.g., partially observable Markov decision processes, POMDPs),
[uncertain models](ModelTypes/UncertainModels.md#modelling) (e.g., interval Markov decision processes, IMDPs)
and non-probabilistic models (e.g., labelled transition systems, LTSs).

## Properties

Properties are written in PRISM's [property specification language](PropertySpecification/README.md),  which is based on temporal logic.
It incorporates several well-known probabilistic temporal logics, such as PCTL, probabilistic LTL and CSL.
It also covers [costs and rewards](PropertySpecification/Reward-basedProperties.md),
[multi-objective](PropertySpecification/Multi-objectiveProperties.md) queries, and much more.

PRISM can formally verify that these properties hold
(e.g., "the probability of a failure occurring within 10 time units is below 0.01")
or generate [strategies](RunningPRISM/Strategies.md)/policies/controllers that optimise properties
(e.g., "how can I maximise the probability of reaching a goal location whilst avoiding hazard locations?").

## Where to start

New to PRISM? See the "[Getting started](GettingStarted/README.md)" section
for details of [installing](GettingStarted/Installing.md)
and [running](GettingStarted/FirstSteps.md) PRISM for the first time.
Work through the [tutorial](https://www.prismmodelchecker.org/tutorial/) to learn what the tools can do,
or read the sections on the [modelling](ThePRISMLanguage/README.md)
and [properties](PropertySpecification/README.md) languages.

## Contents

- [Getting started](GettingStarted/README.md): installing PRISM, solving common problems and first steps
- [The PRISM language](ThePRISMLanguage/README.md): how to write models
- [Property specification](PropertySpecification/README.md): how to write properties
- [Extended model types](ModelTypes/README.md): real-time, partially observable and uncertain models
- [Running PRISM](RunningPRISM/README.md): building, simulating, solving, verifying, exporting/importing models
- [Configuration and performance](ConfiguringPRISM/README.md): engines, solution methods and other settings
- [Reference](Reference/README.md): language syntax, command-line switches, file formats and references
- [Frequently asked questions](FrequentlyAskedQuestions/README.md): memory problems, modelling and properties

