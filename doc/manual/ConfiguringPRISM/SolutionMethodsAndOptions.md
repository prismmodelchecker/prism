# Solution methods and options

Separately from the choice of [engines](ComputationEngines.md),
PRISM often offers several different solution methods
that can be used for the computation of probabilities and expected costs/rewards during model checking.
Many, but not all, of these are iterative numerical methods.
The choice of method (and their settings) depends on the type of analysis that is being done (i.e., what type of model and property).

<a id="linear"></a>
## Linear equation systems

For many properties of Markov chains
(e.g. "reachability"/"until" properties for DTMCs and CTMCs, steady-state properties for CTMCs and "reachability reward" properties for DTMCs),
PRISM solves a set of linear equation systems, for which several numerical methods are available.
Below is a list of the alternatives and the switches used to select them from the command-line.
The corresponding GUI option is "Linear equations method".

- Power method: [`-power`](../Reference/CommandLineSwitches.md#power) (or `-pow`, `-pwr`)
- Jacobi method: [`-jacobi`](../Reference/CommandLineSwitches.md#jacobi) (or `-jac`)
- Gauss-Seidel method: [`-gaussseidel`](../Reference/CommandLineSwitches.md#gaussseidel) (or `-gs`)
- Backwards Gauss-Seidel method: [`-bgaussseidel`](../Reference/CommandLineSwitches.md#bgaussseidel) (or `-bgs`)
- JOR method (Jacobi with over-relaxation): [`-jor`](../Reference/CommandLineSwitches.md#jor)
- SOR method: [`-sor`](../Reference/CommandLineSwitches.md#sor)
- Backwards SOR method: [`-bsor`](../Reference/CommandLineSwitches.md#bsor)

When using the MTBDD engine, Gauss-Seidel/SOR based methods are not available.
When using the hybrid engine, *pseudo* variants of Gauss-Seidel/SOR based method can also be used [[Par02](../Reference/References.md#Par02)]
(type `prism -help` at the command-line for details of the corresponding switches).
For methods which use over-relaxation (JOR/SOR), the over-relaxation parameter (between 0.0 and 2.0)
can also be specified with option "Over-relaxation parameter" (switch [`-omega <val>`](../Reference/CommandLineSwitches.md#omega)).

For options relating to convergence (of this and other iterative methods),
see the [Convergence](SolutionMethodsAndOptions.md#convergence) section below.

<a id="mdp"></a>
## MDP solution methods

When analysing MDPs, there are multiple solution methods on offer.
For most of these, you can select them under the "MDP solution method" setting from the GUI,
or use the command-line switches listed below.
Currently, all except value iteration are only supported by the [explicit engine](ComputationEngines.md),
which PRISM switches to automatically (with a warning) if one of them is selected;
the exception is Gauss-Seidel with the MTBDD engine, which gives an error.
For more details of the methods, see e.g. [[FKNP11](../Reference/References.md#FKNP11)] (about probabilistic verification of MDPs)
or classic MDP texts such as [[Put94](../Reference/References.md#Put94)]).

- **Value iteration** (switch [`-valiter`](../Reference/CommandLineSwitches.md#valiter)) [this is the default]
- **Gauss Seidel** (switch `-gs`)
- **Policy iteration** (switch [`-politer`](../Reference/CommandLineSwitches.md#politer))
- **Modified policy iteration** (switch [`-modpoliter`](../Reference/CommandLineSwitches.md#modpoliter))
- **Linear programming** (switch `-lp`)

Where the methods above use iterative numerical solution,
you can also use the settings under described in the [Convergence](SolutionMethodsAndOptions.md#convergence) section below.

<a id="iiter"></a>
## Interval iteration

Interval iteration [[HM14](../Reference/References.md#HM14)],[[BKLPW17](../Reference/References.md#BKLPW17)] is an alternative solution method for either MDPs or DTMCs
which performs two separate instances of numerical iterative solution,
one from below and one from above. This is designed to provide clearer information
about the accuracy of the computed values and avoid possible problems with premature convergence.
This can be enabled using the switch [`-intervaliter`](../Reference/CommandLineSwitches.md#intervaliter) (or `-ii`)
or via the "Use interval iteration" GUI option.
A variety of options can be configured, either using
`-intervaliter:option1,option2,...` or by
setting the string "`option1,option2,...`" under "Interval iteration options" in the GUI.
See [`-intervaliter`](../Reference/CommandLineSwitches.md#intervaliter) in the list of command-line switches (or type `prism -help intervaliter`) for the options
and see [[BKLPW17](../Reference/References.md#BKLPW17)] for the details.

<a id="topo"></a>
## Topological value iteration

Topological value iteration is a variant of value iteration which improves efficiency
by analysing the graph structure of the model and using this to update the values for
states in an alternative order which increases the speed of convergence.
Use switch [`-topological`](../Reference/CommandLineSwitches.md#topological) or GUI option "Use topological value iteration" to enable this.
In addition to standard value iteration for MDPs, the topological variant can be used to optimise
both interval iteration (see above) and the numerical solution of DTMCs.
It is currently only implemented in the [explicit engine](ComputationEngines.md),
which PRISM switches to automatically (with a warning) if another engine is selected.
The exception is DTMC/CTMC solution with a linear equation method that the explicit engine does not support
(such as pseudo Gauss-Seidel or SOR), where `-topological` is ignored (again, with a warning).

<a id="transient"></a>
## CTMC transient analysis

When computing transient probabilities of a CTMC
(either [directly](../RunningPRISM/ComputingSteady-stateAndTransientProbabilities.md) or when verifying [time-bounded operators](../PropertySpecification/ThePOperator.md#bounded) of CSL), there are two options:
*uniformisation* and *fast adaptive uniformisation* (FAU). These can be selected using the GUI option "Transient probability computation method", or using the command-line switch [`-transientmethod <name>`](../Reference/CommandLineSwitches.md#transientmethod), where `<name>` is either `unif` or `fau`.

**Uniformisation** is a standard iterative numerical method for computing transient probabilities on a CTMC, which works by reducing the problem to an analysis of a "uniformised" DTMC.
As an optimisation, when it is detected that the transient probabilities have converged, no further iterations are performed. If necessary (e.g. in case of round-off problems), this optimisation can be disabled with the "Use steady-state detection" option (command-line switch [`-nossdetect`](../Reference/CommandLineSwitches.md#nossdetect)).

<a id="fau"></a>
**Fast adaptive uniformisation** (FAU) [[MWDH10](../Reference/References.md#MWDH10)] is a method to efficiently approximate transient properties of large CTMCs. The basic idea is that only the parts of the model that are relevant for the current time period are kept in memory. In more detail, starting with the initial states, in each step FAU
explores further states in a DTMC which is a discrete-time version of the original CTMC. By combining the
probabilities there with those of a certain continuous-time stochastic process (a birth process), transient properties in the original CTMC can be computed. If it turns out that the probability of being in some state in the DTMC is below a given threshold, this state is removed from the model explored so far. After a given number of steps, which corresponds to the number of steps which are likely to happen within the time bound, the exploration can be stopped. In the implementation in PRISM [[DHK13](../Reference/References.md#DHK13)], FAU can be used to compute transient probability distributions and to model check the following types of non-nested CSL formulas: time-bounded until, instantaneous reward, cumulative reward.

The following options can be used to configure FAU:

- "FAU epsilon" (switch [`-fauepsilon <x>`](../Reference/CommandLineSwitches.md#fauepsilon)): FAU analyses the DTMC for a number of iterations such that the probability of more steps being relevant is below this value. The default is 1e-6.

- "FAU cut off delta" (switch [`-faudelta <x>`](../Reference/CommandLineSwitches.md#faudelta)): States that have a lower probability than this value are discarded. The default is 1e-12.

- "FAU array threshold" (switch [`-fauarraythreshold <x>`](../Reference/CommandLineSwitches.md#fauarraythreshold)): After this number of steps without any new states being explored or discarded, FAU will switch to a faster, fixed-size data structure until further states have to be explored or discarded. The default is 100.

- "FAU time intervals" (switch [`-fauintervals <x>`](../Reference/CommandLineSwitches.md#fauintervals)): In some cases, it is advantageous to divide the time interval the analysis is done for into several smaller intervals. This option dictates the number of (equal length) intervals used for this split. The default is 1, meaning that only one time interval is used.

- "FAU initial time interval" (switch [`-fauinitival <x>`](../Reference/CommandLineSwitches.md#fauinitival)): It is also possible to specify an additional initial time interval which is handled separately from the rest of the time. This is often advantageous, because in this interval certain parameters of the model can be explored, which can subsequently be used to speed up the computation of the remaining time interval. The default for this option is 1.0.

<a id="convergence"></a>
## Convergence

Common to all of these methods is the way that PRISM checks convergence, i.e. decides when to terminate the iterative methods because the answers have converged sufficiently. This is done by checking when the maximum difference between elements in the solution vectors from successive iterations drops below a given threshold (or, in the case of interval iteration, if the difference of the elements in the iterations from above and below are below the threshold).
The default value for this threshold is $10^{-6}$ but it can be altered with the "Termination epsilon" option (switch [`-epsilon <val>`](../Reference/CommandLineSwitches.md#epsilon)). The way that the maximum difference is computed can also be varied:
either "relative" or "absolute" (the default is "relative"). This can be changed using the "Termination criteria" option (command-line switches [`-relative`](../Reference/CommandLineSwitches.md#relative) and [`-absolute`](../Reference/CommandLineSwitches.md#absolute), or `-rel` and `-abs` for short).

Also, the maximum number of iterations performed is given an upper limit
in order to trap the cases when computation will not converge.
The default limit is 10,000 but can be changed with the "Termination max. iterations" option (switch [`-maxiters <val>`](../Reference/CommandLineSwitches.md#maxiters)). Computations that reach this upper limit will trigger an error during model checking to alert the user to this fact.
