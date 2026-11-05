# Properties Files

## Constants

Files containing properties to be analysed by PRISM can also contain constants, as is the case for model files.
These are defined in identical fashion, for example:

```prism
const int k = 7;
const double T = 9.5;
const double p = 0.01;

P<p [ F<=T x=k ];
```

As before, these constants can actually be left undefined and then later
assigned either a single value or a range of values using [experiments](../RunningPRISM/Experiments.md).

In fact, values such as the probability bounds for the `P` or `S` operators (like `P` above)
and upper or lower bounds for the `U` operator (like `T` above)
can be arbitrary expressions, provided they are constant.
Furthermore, expressions in the properties file can also refer to constants previous defined in the model file.

<a id="labels"></a>

## Labels

Another feature of properties files is *labels*. These are a way of defining sets of states that will be referred to in properties (they correspond to *atomic propositions* in a temporal logic setting). As described [earlier](../ThePRISMLanguage/FormulasAndLabels.md), labels can be defined in either model files or property files.

Labels are defined using the keyword `label`, followed by a name (identifier) in double quotes, and then an expression which evaluates to a Boolean. Definition and usage of labels are illustrated in the following example:

```prism
label "safe" = temp<=100 | alarm=true;
label "fail" = temp>100 & alarm=false;

P>=0.99 [ "safe" U "fail" ];
```

Two special cases are the `"init"` and `"deadlock"` labels which are always defined.
These are true in initial states of the model and states where deadlocks were found (and, usually, fixed by adding self-loops), respectively.

<a id="names"></a>

## Property names

For convenience, properties can be annotated with *names*, as shown in the following example:

```prism
"safe": P<0.01 [ F temperature > t_max ];
```

which gives the name `"safe"` to the property. It is then possible to include named properties as sub-expressions of other properties, e.g.:

```prism
filter(forall, num_sensors>0 => "safe");
```

Notice that the syntax for referring to named properties is identical to the syntax for labels. For this reason, property names must be disjoint from those of any existing labels.

You can refer to property names when using the command-line switch [`-prop`](../RunningPRISM/ModelChecking.md#cl) to specify which property is to be model checked.

## Properties files

A PRISM properties file can contain any number of properties.
It is good practice, as shown in the examples above, to terminate each property with a semicolon. Currently, this is not enforced by PRISM (to prevent incompatibility with old properties files) but this may change in the future.

Like model files, properties can also include any amount of white space (spaces, tabs, new lines, etc.) and C-style comments, which are both ignored.
The recommended file extension for PRISM properties is now `.props`.
Previously, though, the convention was to use extension `.pctl` for properties of DTMCs, MDPs or PTAs
and extension `.csl` for properties of CTMCs, so these are still also valid.
