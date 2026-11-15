# Extended model types

Most of this manual describes PRISM's core model types:
discrete-time Markov chains (DTMCs), continuous-time Markov chains (CTMCs) and Markov decision processes (MDPs).
This section covers features of the PRISM modelling language
that allow it to specify a wider range of model types.
Each page describes how such models are written in the PRISM language,
which properties can be checked for them, and how they are analysed, including any restrictions.

## Contents

- [Real-time models](../ModelTypes/Real-timeModels.md): using real-valued clocks to represent time, notably **probabilistic timed automata** (PTAs)
- [Partially observable models](../ModelTypes/PartiallyObservableModels.md): where strategies only see observations of the state,
  notably **partially observable Markov decision processes** (POMDPs)
- [Uncertain models](../ModelTypes/UncertainModels.md): where transition probabilities are not fully known,
  such as **interval discrete-time Markov chains** (IDTMCs) and **interval Markov decision processes** (IMDPs)
