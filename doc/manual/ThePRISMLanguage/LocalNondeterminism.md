# Local Nondeterminism

PRISM models that support nondeterminism, such as are MDPs, can also exhibit *local nondeterminism*,
which allows the modules themselves to make nondeterministic choices.
In [Example 1](Example1.md), we can make the probabilistic choice in the first state of module `M1` nondeterministic by replacing the command:

```prism
[] x=0 -> 0.8:(x'=0) + 0.2:(x'=1);
```

with the commands:

```prism
[] x=0 -> (x'=0);
[] x=0 -> (x'=1);
```

Assuming we do the same for module `M2`, in state `(0,0)` of the MDP
there will be a nondeterministic choice between the three (trivial) probability distributions listed below. (There are three, not four, distributions because two possibilities result in identical behaviour: staying with probability 1 in the same state.)

- `1.0:(0,0)`
- `1.0:(1,0)`
- `1.0:(0,1)`

More generally, local nondeterminism can also arise when the guards of two commands overlap only partially, rather than completely as in the example above.

PRISM also permits local nondeterminism in models which are DTMCs,
although the nondeterministic choice is randomised when the parallel composition of the modules occurs.
Since the appearance of nondeterminism in a DTMC is often the result of
a user error in the model specification, PRISM displays a warning when local nondeterminism is detected in a DTMC.
Overlapping guards in [CTMCs](CTMCs.md) are not treated as nondeterministic choices.
