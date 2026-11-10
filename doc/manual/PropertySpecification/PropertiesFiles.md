# Properties and properties files

Properties can be specified individually to PRISM
or can be collated together into a **properties file**,
a plain text file with file extension `.props`.
This contains one or more properties,
each of which should be terminated with a semicolon (but the parser is not strict on this).
Properties can refer to various aspects of the model to which they relate
(variables names, labels, etc.) but can also define further
[constants and labels](../ThePRISMLanguage/ConstantsLabelsAndFormulas.md)
in the same style as in model files.
Comments are also allowed.
An example file is:

[examples.props](../files/examples.props "include")

As in the examples above, **labels** (such as `"terminate"` or `"phase1"`) are frequently used in properties to identify sets of states.
These are essentially the same as **atomic propositions** in temporal logic.
Labels are just Boolean-valued expressions over the states variables of the model (and, optionally, constants).
Such expressions can also be used directly in properties, without labels, as in the final example above.
This is useful when a property is parameterised by a constant.

Two special cases of labels are the `"init"` and `"deadlock"` labels which are always defined.
These are true in initial states of the model and states where deadlocks were found (and, usually, fixed by adding self-loops), respectively.

<a id="names"></a>
## Property names

For convenience, properties can be annotated with *names*, as shown in the following example:

```prism
"surely_terminate": P>=1 [ F "terminate" ];
```

which gives the name `"surely_terminate"` to the property.

The name can be referred to when telling PRISM which properties to [model check](../RunningPRISM/ModelChecking.md#cl).
It can also be referred to in other properties, as in this example of a *nested* property:

```prism
// "Safe" states, where we can return to the base
// in at most 10 time units with probability at least 0.99
"safe_return": P>=0.99 [ F<=10 "base" ];

// What is the probability of always remaining in the safe states?
"stay_safe": P=? [ G "safe_return" ];
```
Notice that the syntax for referring to named properties is identical to the syntax for labels.
For this reason, property names must be disjoint from those of any existing labels.
