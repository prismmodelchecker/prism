# Constants, labels and formulas

<a id="constants"></a>
## Constants

PRISM models often contain **constants**, which can be used to parameterise some values within the model,
such as the range of a variable or the probability associated with an update.
These constants can then appear in expressions anywhere in the model.
For example, we can rewrite the earlier `walk.prism` with constants,
making it much easier to change the size of the random walk or its probabilities:

[walk-consts.prism](../files/walk-consts.prism "include")

Constants can be defined anywhere in a model file (but outside its modules)
and can be of type integer, double or Boolean.
The value of a constant can be specified as any constant expression
of an appropriate type, and can also refer to the values of other constants.
Examples of constant definitions are:

```prism
const int size = 10;
const double prob_fail = 1/size;
const bool enabled = true;
```

It is also possible to leave the values of one or more constants undefined,
which allows them to be specified at the time of model checking,
or to facilitate [experiments](../RunningPRISM/Experiments.md) over ranges of values:

```prism
const int size;
const double prob_fail = 1/size;
```

<a id="formulas-and-labels"></a>
<a id="labels"></a>
## Labels

Models can be annotated with **labels**,
which are used to identify sets of states that are of particular interest.
Primarily, these are used when specifying [properties](../PropertySpecification/README.md)
and are in this sense synonymous with the notion of **atomic propositions** in temporal logic.

A label is defined by a Boolean expression, and its name is written using quotation marks (`"..."`), as illustrated in the following examples:

```prism
label "top" = x=N;
```

```prism
label "safe" = temp<=100 | alarm=true;
label "fail" = temp>100 & alarm=false;
```

Unlike constants, though, labels cannot be used to define other labels,
and cannot appear in other parts of the model, such as guards or updates.

<a id="formulas"></a>
## Formulas

The PRISM language supports **formulas**, which are used to avoid duplication of
commonly repeated expressions within a model definition.
A formula definition gives its name (a valid [identifier](../Reference/PRISMLanguageSyntax.md#identifiers))
and an expression that defines it. The latter can be any valid expression,
of arbitrary type, and including constants, variables, or other formulas.
The formula name can then be used as shorthand for the expression anywhere
that an expression might usually be accepted.
A formula is defined as follows:

```prism
formula num_tokens = q1+q2+q3+q4+q5;
```

It can then be used anywhere within that file, as for example in this command:

```prism
[] p1=2 & num_tokens=5 -> (p1'=4);
```

The effect is exactly as if the following had been typed:

```prism
[] p1=2 & (q1+q2+q3+q4+q5)=5 -> (p1'=4);
```

Formulas defined in a model can also be used when specifying its [properties](../PropertySpecification/README.md).
Unlike labels, formulas can be of any type, not just Boolean,
and their names are written without quotation marks.

