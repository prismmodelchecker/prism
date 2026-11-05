# Identifying A Set Of States

One of the most fundamental tasks when specifying properties of a model
is to identify particular sets or classes of states of the model.
For example, to verify a property such as
"the algorithm eventually terminates successfully with probability 1",
it is first necessary to identify the states of the model
which correspond to situations where "the algorithm has terminated successfully".
In terms of the way temporal logics are usually presented,
these correspond to *atomic propositions*.

In PRISM, this is achieved simply by writing an [expression](../ThePRISMLanguage/Expressions.md) in the PRISM language which evaluates to a Boolean value. This expression will typically contain references to variables (and constants) from the model to which it relates. The set of states corresponding to this expression is those for which it evaluates to `true`. We say that the expression is "satisfied" in these states.

For example, in the property given above:

```prism
P<0.1 [ F<=100 num_errors > 5 ]
```

the expression `num_errors > 5` is used to identify states of the model where more than 5 errors have occurred.

It is also common to use [labels](../ThePRISMLanguage/FormulasAndLabels.md) to identify states in this way, like `"terminate"` in the example:

```prism
P>=1 [ F "terminate" ]
```

Properties can refer to [labels](../ThePRISMLanguage/FormulasAndLabels.md) either from the model to which the property relates, or included in the same [properties file](PropertiesFiles.md).
