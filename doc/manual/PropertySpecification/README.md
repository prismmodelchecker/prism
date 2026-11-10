# Property specification

In order to verify or analyse a probabilistic model with PRISM, one or more **properties** of the model are specified.
This is done using the **PRISM property specification language**, which is based on temporal logic
and subsumes several well-known logics:

* PCTL (probabilistic computation tree logic)
* PLTL (probabilistic linear temporal logic)
* PCTL* (which subsumes PCTL and PLTL)
* CSL (continuous stochastic logic)
* CTL (computation tree logic)
* LTL (linear temporal logic)

and also includes reward-based properties and various other extensions.

We give a few illustrative examples of properties below,
and then describe the property specification language in more detail in the rest of this section.
The presentation given here is relatively informal.
For the precise syntax and semantics of the various logics, see [[HJ94](../Reference/References.md#HJ94)],[[BdA95](../Reference/References.md#BdA95)] for PCTL, [[ASSB96](../Reference/References.md#ASSB96)],[[BKH99](../Reference/References.md#BKH99)] for CSL and, for example, [[Bai98](../Reference/References.md#Bai98)] for LTL and PCTL\*.
You can also find various pointers to useful papers in the [About](https://www.prismmodelchecker.org/about/) and [Documentation](https://www.prismmodelchecker.org/doc/) sections of the PRISM website.

## Examples

Some simple examples of specifications in the PRISM property specification language,
along with their natural language interpretations, are given below:

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

```prism
R{"time"}<=10 [ F "terminate" ]
```

"the expected time for the algorithm to terminate successfully is at most 10"

Note that the above properties are all assertions,
i.e., ones to which we would expect a "yes" or "no" answer.
This is because all references to probabilities or rewards are associated with an upper or lower bound
which can be checked to be either true or false.
In PRISM, we can also directly specify **numerical properties**:

```prism
P=? [ !proc2_terminate U proc1_terminate ]
```

"the probability that process 1 terminates before process 2 does"

```prism
Pmax=? [ F<=T messages_lost > 10 ]
```

"the maximum probability that more than 10 messages have been lost by time `T`"

```prism
Rmax=? [ F "terminate" ]
```

"the worst-case expected termination time for the algorithm"

Furthermore, PRISM makes it easy to combine such properties into more complex expressions,
compute their values for a range of parameters
and plot graphs of the results using [experiments](../RunningPRISM/Experiments.md).
This is often a very useful way of identifying interesting
patterns or trends in the behaviour of a system.
See the [Case studies](https://www.prismmodelchecker.org/casestudies/) section of the PRISM website for many examples of this kind of analysis.

## Contents

- [Properties and properties files](PropertiesFiles.md): identifying states, combining operators, the value of a property, and properties files with constants, labels and names
- [The P operator](ThePOperator.md): the probability of an event, with path properties such as "until" and "eventually"; minimum and maximum probabilities for MDPs and other nondeterministic models
- [The S operator](TheSOperator.md): long-run (steady-state) behaviour
- [Reward-based properties](Reward-basedProperties.md): expected costs and rewards, with the R operator
- [Multi-objective properties](Multi-objectiveProperties.md): several objectives for MDPs at once, and the trade-offs between them
- [Non-probabilistic properties](Non-probabilisticProperties.md): CTL and LTL, without probabilities
- [Filters](Filters.md): results for sets of states other than the initial ones

Properties of [PTAs](../ModelTypes/Real-timeModels.md#properties), [partially observable models](../ModelTypes/PartiallyObservableModels.md#properties)
and [interval models](../ModelTypes/UncertainModels.md#properties) are described with those models, in "[Extended model types](../ModelTypes/README.md)".
