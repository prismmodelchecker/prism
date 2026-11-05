# Synchronisation

Another feature of PRISM introduced in [Example 2](Example2.md) is *synchronisation*.
In the style of many process algebras, we allow commands to be labelled with *actions*.
These are placed inside the square brackets which mark the start of the command,
for example `serve` in this command from [Example 2](Example2.md):

```prism
[serve] q>0 -> lambda:(q'=q-1);
```

These actions can be used to force two or more modules to make transitions simultaneously
(i.e. to *synchronise*).
For example, in state `(3,0)` (i.e.  `q=3` and `s=0`),
the composed model can move to state `(2,1)`,
synchronising over the `serve` action.
The rate of this transition is equal to the product of the two individual rates
(in this case, `lambda * 1 = lambda`).
The product of two rates does not always meaningfully represent the rate of a synchronised transition.
A common technique, as seen here, is to make one action *passive*, with rate 1 and one action *active*,
which actually defines the rate for the synchronised transition.
By default, all modules are combined using the standard CSP parallel composition
(i.e. modules synchronise over all their common actions).
