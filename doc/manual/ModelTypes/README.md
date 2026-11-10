# Extended model types

Most of this manual describes PRISM's core model types:
discrete-time Markov chains (DTMCs), continuous-time Markov chains (CTMCs) and Markov decision processes (MDPs).
This section covers the other types of model that PRISM supports.
Each page describes how such models are written in the PRISM language,
which properties can be checked for them, and how they are analysed, including any restrictions.

## Contents

- [Probabilistic timed automata](Real-timeModels.md): PTAs, which add real-valued clocks to MDPs
- [Partially observable models](PartiallyObservableModels.md): POMDPs and POPTAs, in which strategies only see observations of the state
- [Interval models](UncertainModels.md): interval DTMCs and MDPs, whose transition probabilities are only known to lie within intervals
