# The PRISM language

In order to construct and analyse a model with PRISM,
it must be specified in the **PRISM modelling language**
(often referred to simply as the PRISM language).
This is a simple, state-based language,
based on the Reactive Modules formalism [[AH99](../Reference/References.md#AH99)],
which expresses the behaviour of a system in terms of
**guarded commands** that describe how its state evolves over time.

The PRISM language is used to describe all the types of model that the tool supports,
using files with the extension `.prism`.
The three most commonly used model types are:

- discrete-time Markov chains (DTMCs)
- continuous-time Markov chains (CTMCs)
- Markov decision processes (MDPs)

This section describes the PRISM language, focusing on how it can be used
to specify models of these three types, and giving some small illustrative examples.

Later parts of the manual (in the "[Extended model types](../ModelTypes/README.md)" section)
describe further modelling features
that support the specification of various other model types:

- [Real-time models](../ModelTypes/Real-timeModels.md#modelling) using real-valued clocks to represent time, notably probabilistic timed automata (PTAs)
- [Partially observable models](../ModelTypes/PartiallyObservableModels.md#modelling),
  notably partially observable Markov decision processes (POMDPs)
- [Uncertain models](../ModelTypes/UncertainModels.md#modelling),
  such as interval discrete-time Markov chains (IDTMCs) and interval Markov decision processes (IMDPs)
- [Non-probabilistic models](../ModelTypes/Non-probabilisticModels.md#modelling),
  notably labelled transition systems (LTSs), which have no probabilities

## Contents

This section covers the key modelling features and concepts for the PRISM language:

- [Modules and commands](ModulesAndCommands.md): the basics (modules, commands, updates) and an example, illustrated for DTMCs
- [Variables](Variables.md): how to use, declare and initialise variables
- [Constants, labels and formulas](ConstantsLabelsAndFormulas.md): parameterised values (constants), sets of states (labels) and re-usable expressions (formulas)
- [Actions and nondeterminism](ActionsAndNondeterminism.md): introducing nondeterminism, primarily for MDPs, and labelling choices with actions
- [Continuous time](ContinuousTime.md): how to model CTMCs, using rates instead of probabilities, and races between transitions
- [Composition and synchronisation](CompositionAndSynchronisation.md): how modules are combined into one model, synchronising on actions, module renaming and process algebra operators
- [Costs and rewards](CostsAndRewards.md): attaching costs or rewards to states and transitions

## Examples

One of the best ways to learn what can be done with the PRISM language is to look at some existing examples.
A number of these are included with the tool distribution in the `prism-examples` directory.
Many more can be found in the "[Case studies](https://www.prismmodelchecker.org/casestudies/)" section of the PRISM website.
Note that some examples of model files may use older conventions for filename extensions,
`.nm` for MDPs or PTAs, `.pm` for DTMCs and `.sm` for CTMCs, rather than `.prism` for all models.

