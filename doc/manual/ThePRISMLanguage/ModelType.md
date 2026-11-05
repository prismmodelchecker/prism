# Model Type

As mentioned above, the PRISM language can be used to describe several types of probabilistic models.
To indicate which type is being described, a PRISM model usually includes a model type keyword:

- `dtmc`: discrete-time Markov chain
- `ctmc`: continuous-time Markov chain
- `mdp`: Markov decision process (or probabilistic automaton)
- `pta`: probabilistic timed automaton
- `pomdp`: partially observable Markov decision process
- `popta`: partially observable probabilistic timed automaton

This is typically at the very start of the file,
but can actually occur anywhere in the file (except inside modules and other declarations).

If no such model type declaration is included, the model is by default assumed to be an MDP.
PRISM also performs some auto-detection of the model type;
for example, an MDP with [clock variables](Real-timeModels.md) is assumed to be a PTA,
and an MDP with [observables](PartiallyObservableModels.md) is assumed to be a POMDP.

**Note:** For compatibility with old versions of PRISM,
the keywords `probabilistic`, `stochastic` and `nondeterministic`
can be used as alternatives for `dtmc`, `ctmc` and `mdp`, respectively.
