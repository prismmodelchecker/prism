# Introduction

In order to construct and analyse a model with PRISM,
it must be specified in the PRISM language,
a simple, state-based language,
based on the Reactive Modules formalism of Alur and Henzinger [[AH99](../Main/References.md#AH99)].
This is used for [all of the types of model](../Main/Introduction.md) that PRISM supports.

In this section, we describe the PRISM language and present a number of small illustrative examples.
A precise definition of the semantics of the language is available from the "[Documentation](https://www.prismmodelchecker.org/doc/)" section of the PRISM web site. One of the best ways to learn what can be done with the PRISM language is to look at some existing examples.
A number of these are included with the tool distribution in the `prism-examples` directory.
Many additional examples can be found on the "[Case Studies](https://www.prismmodelchecker.org/casestudies/)" section of the [PRISM website](https://www.prismmodelchecker.org/).

The fundamental components of the PRISM language are *modules* and *variables*.
A model is composed of a number of *modules* which can interact with each other.
A module contains a number of local *variables*.
The values of these variables at any given time constitute the state of the module.
The *global state* of the whole model is determined by the *local state* of all modules.
The behaviour of each module is described by a set of *commands*.
A command takes the form:

```prism
[action] guard -> prob_1 : update_1 + ... + prob_n : update_n;
```

The *guard* is a predicate over all the variables in the model (including those belonging to other modules). Each *update* describes a transition which the module can make if the guard is true. A transition is specified by giving the new values of the variables in the module, possibly as a function of other variables. Each update is assigned a probability (or in some cases a rate) which will be assigned to the corresponding transition. The command also optionally includes an *action*, either just to annotate it, or for [synchronisation](Synchronisation.md).
