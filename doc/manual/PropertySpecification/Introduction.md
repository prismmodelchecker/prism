# Introduction

In order to analyse a probabilistic model which has been specified and constructed in PRISM,
it is necessary to identify one or more *properties* of the model
which can be evaluated by the tool.
PRISM's *property specification language* subsumes several well-known probabilistic temporal logics, including PCTL, CSL, probabilistic LTL and PCTL\*.
PCTL is used for specifying properties of discrete-time models such as DTMCs or PTAs,
and also real-time models such as PTAs; CSL is an extension of PCTL for CTMCs;
LTL and PCTL\* can be used to specify properties of
discrete-time models (or untimed properties of CTMCs).
PRISM also supports most of the (non-probabilistic) temporal logic CTL.

In fact, PRISM also supports numerous additional customisations and extensions of these two logics.
Full details of the property specifications permitted in PRISM are provided in the following sections. The presentation given here is relatively informal. For the precise syntax and semantics of the various logics, see [[HJ94](../Main/References.md#HJ94)],[[BdA95](../Main/References.md#BdA95)] for PCTL, [[ASSB96](../Main/References.md#ASSB96)],[[BKH99](../Main/References.md#BKH99)] for CSL and, for example, [[Bai98](../Main/References.md#Bai98)] for LTL and PCTL\*. You can also find various pointers to useful papers in the [About](https://www.prismmodelchecker.org/about.php) and [Documentation](https://www.prismmodelchecker.org/doc/) sections of the PRISM website.

Before discussing property specifications in more detail,
it is perhaps instructive to first illustrate some typical examples of properties which PRISM can handle.
The following are a selection of such properties.
In each case, we give both the PRISM syntax and a natural language translation:

```prism
P>=1 [ F "terminate" ]
```

"the algorithm eventually terminates successfully with probability 1"

```prism
P<0.1 [ F<=100 num_errors > 5 ]
```

"the probability that more than 5 errors occur within the first 100 time units is less than 0.1"

```prism
S<0.01 [ num_sensors < min_sensors ]
```

"in the long-run, the probability that an inadequate number of sensors are operational is less than 0.01"

Note that the above properties are all assertions,
i.e. ones to which we would expect a "yes" or "no" answer.
This is because all references to probabilities are associated with an upper or lower bound
which can be checked to be either true or false.
In PRISM, we can also directly specify properties which evaluate to a numerical value, e.g.:

```prism
P=? [ !proc2_terminate U proc1_terminate ]
```

"the probability that process 1 terminates before process 2 does"

```prism
Pmax=? [ F<=T messages_lost > 10 ]
```

"the maximum probability that more than 10 messages have been lost by time `T`" (for an MDP/PTA)

```prism
S=? [ queue_size / max_size > 0.75 ]
```

"the long-run probability that the queue is more than 75% full"

Furthermore, PRISM makes it easy to combine such properties into more complex expressions,
compute their values for a range of parameters
and plot graphs of the results using [experiments](../RunningPRISM/Experiments.md).
This is often a very useful way of identifying interesting
patterns or trends in the behaviour of a system.
See the [Case Studies](https://www.prismmodelchecker.org/casestudies/) section of the [PRISM website](https://www.prismmodelchecker.org/) for many examples of this kind of analysis.
