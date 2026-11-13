# Other options

This page describes some further options, not covered elsewhere in this section.
All the options available from the command line are listed in "[Command-line switches](../Reference/CommandLineSwitches.md)".

<a id="output"></a>
## Output options

To increase the amount of information displayed by PRISM (in particular, to display lists of states and probability vectors), you can use the "Verbose output" option (activated with command-line switch [`-verbose`](../Reference/CommandLineSwitches.md#verbose) or `-v`). To display additional statistics about MTBDDs after model construction, use the "Extra MTBDD information" option (switch [`-extraddinfo`](../Reference/CommandLineSwitches.md#extraddinfo)) and, to view MTBDD sizes during the process of reachability, use option "Extra reachability information" (switch [`-extrareachinfo`](../Reference/CommandLineSwitches.md#extrareachinfo)).

<a id="fairness"></a>
## Fairness

Sometimes, model checking of properties for MDPs requires fairness constraints to be taken into account.
See e.g. [[BK98](../Reference/References.md#BK98)],[[Bai98](../Reference/References.md#Bai98)] for more information.
To enable the use of fairness constraints (for [`P` operator](../PropertySpecification/ThePOperator.md#nondeterminism) properties), use the [`-fair`](../Reference/CommandLineSwitches.md#fair) switch.

<a id="probchecks"></a>
## Probability/rate checks

By default, when constructing a model, PRISM checks that all probabilities and rates are within acceptable ranges (i.e. are between 0 and 1, or are non-negative, respectively). For DTMCs and MDPs, it also checks that the probabilities sum up to one for each command. These checks are often very useful for highlighting user modelling errors and it is strongly recommended that you keep them enabled, however if you need to disable them you can do so via option "do prob checks?" in the GUI or command-line switch [`-noprobchecks`](../Reference/CommandLineSwitches.md#noprobchecks).
You can also change the level of precision used to check that probabilities sum to 1 using the option "Probability sum threshold" (or command-line switch [`-sumroundoff`](../Reference/CommandLineSwitches.md#sumroundoff)).

<a id="precomputation"></a>
## Precomputation

By default, PRISM's probabilistic model checking algorithms use an initial *precomputation* step which uses graph-based techniques to efficient detect trivial cases where probabilities are 0 or 1. This can often result in improved performance and also reduce round-off errors. Occasionally, though, you may want to disable this step for efficiency (e.g. if you know that there are no/few such states and the precomputation process is slow). This can be done with the [`-nopre`](../Reference/CommandLineSwitches.md#nopre) switch. You can also disable the individual algorithms for probability 0/1 using switches [`-noprob0`](../Reference/CommandLineSwitches.md#noprob0) and [`-noprob1`](../Reference/CommandLineSwitches.md#noprob1).

<a id="timeouts"></a>
## Time-outs

The command-line version of PRISM has a time-out option, specified using the switch [`-timeout <n>`](../Reference/CommandLineSwitches.md#timeout).
This causes the program to exit after `<n>` seconds if it has not already terminated by that point.
This is particularly useful for benchmarking scenarios where you wish to ignore runs of PRISM that exceed a certain length of time.
