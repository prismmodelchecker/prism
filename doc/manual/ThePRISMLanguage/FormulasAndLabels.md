# Formulas and labels

PRISM models can include *formulas* which are used to avoid duplication of code.
A formula comprises a name (an identifier) and an [expression](Expressions.md).
The formula name can then be used as shorthand for the expression anywhere an expression might usually be accepted.
A formula is defined as follows:

```prism
formula num_tokens = q1+q2+q3+q+q5;
```

It can then be used anywhere within that file, as for example in this command:

```prism
[] p1=2 & num_tokens=5 -> (p1'=4);
```

The effect is exactly as if the following had been typed:

```prism
[] p1=2 & (q1+q2+q3+q+q5)=5 -> (p1'=4);
```

Formulas defined in a model can also be used when specifying its properties.

## Formulas and renaming

During parsing of the model, expansion of formulas is done before module renaming so, if a module which uses formulas is renamed to another module, it is the contents of the formula which will be renamed, not the formula itself.

## Labels

PRISM models can also contain *labels*. These are a way of identifying sets of states that are of particular interest. Labels can only be used when specifying [properties](../PropertySpecification/Introduction.md) but, for convenience, can be defined in model files as well as property files.

Labels differ from formulas in two other ways: firstly, they must be of Boolean type;
secondly, they are written using quotation marks (`"..."`), as illustrated in the following example:

```prism
label "safe" = temp<=100 | alarm=true;
label "fail" = temp>100 & alarm=false;
```
